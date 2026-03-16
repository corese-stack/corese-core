package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Build a minimal SPARQL AST for:
 * - Triple patterns (?s ?p ?o)
 * - Basic Graph Patterns (BGP) via TriplesBlock
 * - GroupGraphPattern as a container (can contain multiple TriplesBlock and later OPTIONAL/UNION/etc.)
 * - ASK query
 * Compatible with the common SPARQL grammar shape:
 * GroupGraphPattern
 *   : '{'
 *       ( TriplesBlock?
 *         ( GraphPatternNotTriples '.'? TriplesBlock? )*
 *       )?
 *     '}'
 * This builder expects the listener to call:
 * - enterGroup()/exitGroup() on enter/exitGroupGraphPattern
 * - enterBgp()/exitBgp() on enter/exitTriplesBlock
 * - addTriple(s,p,o) whenever a triple pattern is recognized (usually on exitTriplesSameSubject)
 * - enterAskQuery at the start of the declaration of an ASK query
 * - enterSelectQuery at the start of the declaration of a Select query
 */
public final class SparqlAstBuilder {

    private ASTConstants.QUERY_TYPE queryType = ASTConstants.QUERY_TYPE.UNDEFINED;
    private static final Logger logger = LoggerFactory.getLogger(SparqlAstBuilder.class);

    // --- Internal stacks (scopes) ---

    /** Stack of groups; each group is a list of patterns (BgpAst now, later OptionalAst/UnionAst/...) */
    private final Deque<List<PatternAst>> groupStack = new ArrayDeque<>();

    /** Stack of current BGP triples (TriplesBlock). Nested blocks are rare but stack keeps it safe. */
    private final Deque<List<TriplePatternAst>> bgpStack = new ArrayDeque<>();

    /** At enterOptional(), we push groupStack.size(). At exitGroup(), if groupStack.size() equals peek, we wrap in OptionalAst. */
    private final Deque<Integer> optionalGroupDepths = new ArrayDeque<>();

    /** Top-level WHERE clause, set when the root group is closed in exitGroup(). */
    private GroupGraphPatternAst whereClause;

    /** SELECT projection (* or explicit variables). Set by SelectQueryFeature in enterSelectQuery. */
    private ProjectionAst projection = ProjectionAsts.selectAll();

    /** SELECT DISTINCT / REDUCED. Set by SelectQueryFeature when parsing SELECT (DISTINCT | REDUCED)? ... */
    private boolean distinct;
    private boolean reduced;

    /**  Variable to hold the value of LIMIT and OFFSET. */
    private Long limit;
    private Long offset;

    /** Parser options (e.g. for future use: strict mode, base IRI). */
    private final SparqlParserOptions options;

    /**
     * Stack of UNION branch lists currently being collected.
     */
    private final Deque<List<GroupGraphPatternAst>> unionStack = new ArrayDeque<>();

    /** DESCRIBE resources (IRIs or variables). Set by DescribeQueryFeature. */
    private final List<TermAst> describeResources = new ArrayList<>();

    public SparqlAstBuilder(SparqlParserOptions options) {
        this.options = options;
    }

    // --- Construction entry points (called by listener) ---

    public void enterAskQuery() {
        queryType = ASTConstants.QUERY_TYPE.ASK;
    }

    public void exitAskQuery() {
    }

    public void enterSelectQuery() {
        queryType = ASTConstants.QUERY_TYPE.SELECT;
    }

    public void exitSelectQuery() {
    }

    /** Sets SELECT * (project all variables from the body). */
    public void setProjectionAll() {
        this.projection = ProjectionAsts.selectAll();
    }

    /** Sets explicit SELECT variables (e.g. SELECT ?s ?p). Variable names may include ? or $ prefix. */
    public void setProjectionVariables(List<String> variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            setProjectionAll();
            return;
        }
        List<VarAst> vars = variableNames.stream()
                .map(s -> s == null ? "" : (s.startsWith("?") || s.startsWith("$") ? s.substring(1).trim() : s.trim()))
                .filter(s -> !s.isBlank())
                .map(VarAst::new)
                .toList();
        this.projection = vars.isEmpty() ? ProjectionAsts.selectAll() : ProjectionAsts.of(vars);
    }

    /** Sets the projection from an existing AST. */
    public void setProjection(ProjectionAst projection) {
        this.projection = projection != null ? projection : ProjectionAsts.selectAll();
    }

    /** Sets SELECT DISTINCT. Called by SelectQueryFeature when {@code DISTINCT} is present. */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /** Sets SELECT REDUCED. Called by SelectQueryFeature when {@code REDUCED} is present. */
    public void setReduced(boolean reduced) {
        this.reduced = reduced;
    }

    /**
     * Sets the LIMIT for pagination
     * @param limit
     */
    public void setLimit(long limit) { this.limit = limit; }

    /**
     * Sets the OFFSET for pagination
     * @param offset
     */
    public void setOffset(long offset) { this.offset = offset; }

    /** Enter a { ... } groupGraphPattern. */
    public void enterGroup() {
        groupStack.push(new ArrayList<>());
    }

    /**
     * Exit a { ... } groupGraphPattern.
     * Pops the current group, wraps it in {@link GroupGraphPatternAst}. If we had entered OPTIONAL
     * and the parent group depth matches {@link #optionalGroupDepths}, wraps in {@link OptionalAst} and adds to parent;
     * otherwise adds the group to parent or sets as top-level WHERE.
     * Must not be called while a TriplesBlock is still open (no pending enterBgp without exitBgp).
     */
    public void exitGroup() {
        ensureNoOpenBgp();
        List<PatternAst> popped = groupStack.pop();
        GroupGraphPatternAst group = new GroupGraphPatternAst(popped);
        if (!optionalGroupDepths.isEmpty() && groupStack.size() == optionalGroupDepths.peek()) {
            optionalGroupDepths.pop();
            currentGroup().add(new OptionalAst(group));
        } else if (groupStack.isEmpty()) {
            whereClause = group;
        } else {
            currentGroup().add(group);
        }
    }

    /** Enter a TriplesBlock -> begin collecting triples for a BGP. */
    public void enterBgp() {
        bgpStack.push(new ArrayList<>());
    }

    /**
     * Exit a TriplesBlock -> finalize into BgpAst and attach it to the current group.
     * Empty triples blocks produce no pattern.
     */
    public void exitBgp() {
        List<TriplePatternAst> triples = bgpStack.pop();
        if (!triples.isEmpty()) {
            currentGroup().add(new BgpAst(List.copyOf(triples)));
        }
    }

    /**
     * Add a triple pattern (?s ?p ?o) to the current BGP (TriplesBlock).
     * This must be called while inside a TriplesBlock.
     */
    public void addTriple(TermAst s, TermAst p, TermAst o) {
        if (bgpStack.isEmpty()) {
            // This should not happen if listener wiring is correct, but we fail loudly
            // because BGP boundaries matter (TriplesBlock).
            throw new IllegalStateException("addTriple() called outside of TriplesBlock (BGP). " +
                    "Ensure you call enterBgp() on enterTriplesBlock.");
        }
        bgpStack.peek().add(new TriplePatternAst(s, p, o));
    }

    // --- Filters ---

    /**
     * Exits a Filter, builds FilterAst and adds it to the current group
     */
    public void addFilter(FilterAst filter) {
        logger.info("FILTER {}", filter);
        if(this.hasCurrentGroup()) {
            this.currentGroup().add(filter);
        }
    }

    // --- Optional ---

    /**
     * Enter OPTIONAL scope. Records current group stack size so that when we exitGroup() and the stack
     * is back to that size, we wrap the popped group in {@link OptionalAst}.
     */
    public void enterOptional() {
        optionalGroupDepths.push(groupStack.size());
    }

    /**
     * Exit OPTIONAL scope. No-op: the optional content was already wrapped in {@link OptionalAst} in {@link #exitGroup()}.
     */
    public void exitOptional() {}

    // --- Result ---

    /**
     * Returns the final AST.
     * The top-level WHERE clause must have been set by exitGroup() (root group closed).
     * One of enterAskQuery() or enterSelectQuery() must have been called before, or this throws.
     *
     * @return the root QueryAst (AskQueryAst or SelectQueryAst)
     * @throws QueryEvaluationException if query type could not be determined (no enter*Query() called)
     * @throws IllegalStateException if no WHERE clause was set (exitGroup() not called for root) or unhandled query type
     */
    public QueryAst getResult() {
        if (whereClause == null) {
            throw new IllegalStateException("No WHERE clause: did you call exitGroup() for the top-level GroupGraphPattern?");
        }
        return switch (this.queryType) {
            case ASK -> new AskQueryAst(whereClause);
            case CONSTRUCT -> null;
            case DESCRIBE -> new DescribeQueryAst(describeResources, whereClause);
            case SELECT -> new SelectQueryAst(projection, whereClause, buildSolutionModifier());
            case UNDEFINED -> throw new QueryEvaluationException("Could not determine the type of query during parsing");
        };
    }

    // --- Internal helpers ---

    private boolean hasCurrentGroup() {
        return ! this.groupStack.isEmpty();
    }

    private List<PatternAst> currentGroup() {
        if (groupStack.isEmpty()) {
            throw new IllegalStateException("No current group. Did you call enterGroup() on enterGroupGraphPattern?");
        }
        return groupStack.peek();
    }

    /**
     * Asserts that no {@code TriplesBlock} (BGP) is currently open.
     *
     * @throws IllegalStateException if the BGP stack is not empty
     */
    private void ensureNoOpenBgp() {
        if (!bgpStack.isEmpty()) {
            throw new IllegalStateException(
                    "exitGroup() called while a TriplesBlock/BGP is still open" +
                            " (open bgpStack depth=" + bgpStack.size() + ")");
        }
    }

    /**
     * Signals the start of a {@code GroupOrUnionGraphPattern}.
     */
    public void enterUnion() {
        unionStack.push(new ArrayList<>());
    }

    /**
     * Collects the most recently closed {@link GroupGraphPatternAst} as the next
     * branch of the current {@code UNION}.
     */
    public void collectUnionBranch() {
        List<GroupGraphPatternAst> currentUnion = unionStack.peek();
        if (currentUnion == null) {
            return;
        }
        if (groupStack.isEmpty()) {
            return;
        }
        List<PatternAst> current = currentGroup();
        if (!current.isEmpty() && current.getLast() instanceof GroupGraphPatternAst g) {
            current.removeLast();
            currentUnion.add(g);
        }
    }

    /**
     * Finalises the current {@code GroupOrUnionGraphPattern} and adds its result
     * to the enclosing group.
     */
    public void exitUnion() {
        List<GroupGraphPatternAst> branches = unionStack.pop();

        if (branches.isEmpty()) {
            return;
        }

        if (branches.size() == 1) {
            // Single branch: no UNION keyword was present, add the group directly
            currentGroup().add(branches.getFirst());
            return;
        }

        // Two or more branches: fold left-associatively into binary UnionAst nodes
        PatternAst result = new UnionAst(branches.get(0), branches.get(1));
        for (int i = 2; i < branches.size(); i++) {
            result = new UnionAst(
                    new GroupGraphPatternAst(List.of(result)),
                    branches.get(i)
            );
        }

        currentGroup().add(result);
    }

    /** Builds the solution modifier (DISTINCT, REDUCED, ORDER BY, LIMIT, OFFSET) for SELECT. */
    private SolutionModifierAst buildSolutionModifier() {
        return new SolutionModifierAst(distinct, reduced, List.of(), limit, offset);
    }
    /**
     * Signals the start of a {@code DESCRIBE} query declaration.
     * Sets the internal query type to {@link ASTConstants.QUERY_TYPE#DESCRIBE}.
     */
    public void enterDescribeQuery() {
        queryType = ASTConstants.QUERY_TYPE.DESCRIBE;
    }

    /** Called when the parser exits a {@code DESCRIBE} query. No-op. */
    public void exitDescribeQuery() {}

    /**
     * Adds a resource (IRI or variable) to the DESCRIBE target list.
     *
     * @param term the IRI or variable to describe; must not be {@code null}
     */
    public void addDescribeResource(TermAst term) {
        if (term == null) throw new IllegalArgumentException("DESCRIBE resource term is null");
        describeResources.add(term);
    }

    // --- Creation Term helpers ---

    /** Variable token text can be "?s" or "$s" depending on grammar. */
    public TermAst var(String tokenText) {
        if (tokenText == null || tokenText.isBlank()) {
            throw new IllegalArgumentException("Variable token text is null/blank");
        }
        String t = tokenText.trim();
        if (t.startsWith("?") || t.startsWith("$")) t = t.substring(1);
        return new VarAst(t);
    }

    /**
     * IRI term as raw text:
     * <ul>
     *   <li>{@code <http://...>}</li>
     *   <li>{@code foaf:Person}</li>
     *   <li>{@code a}</li>
     * </ul>
     */
    public TermAst iri(String raw) {
        if (raw == null) throw new IllegalArgumentException("IRI raw is null");
        return new IriAst(raw);
    }

    /**
     * Literal term.
     * - lexical should be the literal lexical form (often including quotes at this stage)
     * - lang without '@' (e.g., "fr"), or null
     * - datatype as IRI/QName text (e.g., "xsd:integer"), or null
     */
    public TermAst literal(String lexical, String lang, String datatype) {
        if (lexical == null) throw new IllegalArgumentException("Literal lexical is null");
        return new LiteralAst(lexical, lang, datatype);
    }


    /**
     * Creates the right createFunCall AST according to keyword and argument list
     *
     * @param constraint keyword
     * @param args       arguments list
     * @return An ConstraintAst
     */
    public ConstraintAst createConstraint(ASTConstants.Constraint constraint, List<TermAst> args) {
        switch (args.size()) {
            case 1 -> {
                switch (constraint) {
                    case ASTConstants.OPERATOR.NOT -> {
                        return new BooleanNotAst(args.getFirst());
                    }
                    case ASTConstants.OPERATOR.ADD -> {
                        return new UnaryPlusAst(args.getFirst());
                    }
                    case ASTConstants.OPERATOR.SUB -> {
                        return new UnaryMinusAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.BOUND -> {
                        return new BoundAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.IS_IRI -> {
                        return new IsIriAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.IS_BLANK -> {
                        return new IsBlankAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.IS_LITERAL -> {
                        return new IsLiteralAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.STR -> {
                        return new StrAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.LANG -> {
                        return new LangAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.DATATYPE -> {
                        return new DatatypeAst(args.getFirst());
                    }
                    default ->
                            throw new QueryEvaluationException("Unexpected number of arguments (1) for createFunCall " + constraint);
                }
            }
            case 2 -> {
                switch (constraint) {
                    case ASTConstants.OPERATOR.OR -> {
                        return new OrAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.AND -> {
                        return new AndAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.EQ -> {
                        return new EqualsAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.NE -> {
                        return new DifferentAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.LT -> {
                        return new LowerThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.LE -> {
                        return new LowerOrEqualThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.GT -> {
                        return new GreaterThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.GE -> {
                        return new GreaterOrEqualThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.MUL -> {
                        return new MultiplyAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.DIV -> {
                        return new DivideAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.ADD -> {
                        return new AddAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.SUB -> {
                        return new SubtractAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.SAMETERM -> {
                        return new SameTermAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.LANGMATCHES -> {
                        return new LangMatchesAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.REGEX -> {
                        return new BinaryRegexAst(args.getFirst(), args.getLast());
                    }
                    default ->
                            throw new QueryEvaluationException("Unexpected number of arguments (2) for " + constraint + " keyword");
                }
            }
            case 3 -> {
                if (Objects.requireNonNull(constraint) == ASTConstants.FUNCTION_CALL.REGEX) {
                    return new TrinaryRegexAst(args.getFirst(), args.get(1), args.getLast());
                }
                throw new QueryEvaluationException("Unexpected number of arguments (3) for " + constraint + " keyword");
            }
            default ->
                    throw new QueryEvaluationException("Unexpected number of arguments (" + args.size() + ") for " + constraint);
        }
    }

    public ConstraintAst createFunCall(IriAst functionName, List<TermAst> args) {
        return new FunctionCallAst(functionName, args);
    }

    // ---- term helpers ----

    public TermAst termFromVerb(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return this.iri("a");
        return termFromVarOrIriRef(ctx.varOrIRIref());
    }

    public TermAst termFromVarOrTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return termFromVar(ctx.var_());
        return termFromGraphTerm(ctx.graphTerm());
    }

    public TermAst termFromVarOrIriRef(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrIRIrefContext ctx) {
        if(ctx.var_() != null) {
            return termFromVar(ctx.var_());
        }
        return termFromIriRef(ctx.iriRef());
    }

    public TermAst termFromGraphTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphTermContext ctx) {
        if (ctx.iriRef() != null) {
            return termFromIriRef(ctx.iriRef());
        }
        if (ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        }
        if (ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        }
        if (ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        }
        if (ctx.blankNode() != null) {
            return termFromBlankNode(ctx.blankNode());
        }
        if (ctx.NIL() != null) {
            return this.iri("()");
        } // NIL = () in SPARQL
        return this.iri(ctx.getText());
    }

    public List<TermAst> termListFromObjectList(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ObjectListContext ctx) {
        List<TermAst> out = new ArrayList<>();
        for (var obj : ctx.object_()) {
            out.add(termFromObject(obj));
        }
        return out;
    }

    public TermAst termFromObject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.Object_Context ctx) {
        // object_ : graphNode
        return termFromGraphNode(ctx.graphNode());
    }

    public TermAst termFromGraphNode(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphNodeContext ctx) {
        if (ctx.varOrTerm() != null) return termFromVarOrTerm(ctx.varOrTerm());
        if (ctx.triplesNode() != null) {
            // MVP: pas encore supporté ( [ ... ] ou ( ... ) )
            return this.iri(ctx.triplesNode().getText());
        }

        return this.iri(ctx.getText());
    }

    public TermAst termFromRdfLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.RdfLiteralContext ctx) {
        // rdfLiteral : string_ ( LANGTAG | '^^' iriRef )?

        String lexical = ctx.string_().getText();
        String lang = null;
        String datatype = null;

        if (ctx.LANGTAG() != null) {
            String t = ctx.LANGTAG().getText(); // ex: "@fr"
            lang = t.startsWith("@") ? t.substring(1) : t;
        } else if (ctx.DOUBLE_CARET() != null && ctx.iriRef() != null) {
            datatype = ctx.iriRef().getText(); // ex: xsd:integer ou <iri>
        }
        return this.literal(lexical, lang, datatype);
    }

    public TermAst termFromPrimary(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.PrimaryExpressionContext ctx) {
        if(ctx.brackettedExpression() != null) {
            return expressionFromBrackettedExpression(ctx.brackettedExpression());
        } else if (ctx.builtInCall() != null) {
            return expressionFromBuiltInCall(ctx.builtInCall());
        } else if(ctx.iriRefOrFunction() != null) {
            return termFromIriRefOrFunction(ctx.iriRefOrFunction());
        } else if(ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        } else if(ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        } else if(ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        } else if(ctx.var_() != null) {
            return termFromVar(ctx.var_());
        } else {
            throw new QueryEvaluationException("Unexpected content of bracketed expression");
        }
    }

    public TermAst termFromVar(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.Var_Context ctx) {
        return this.var(ctx.getText());
    }

    public TermAst termFromBooleanLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BooleanLiteralContext ctx) {
        if(ctx.FALSE() != null) {
            return new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        } else if(ctx.TRUE() != null) {
            return new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected value for boolean literal");
        }
    }

    public TermAst termFromIriRefOrFunction(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.IriRefOrFunctionContext ctx) {
        if(ctx.iriRef() != null && ctx.argList() == null) {
            return termFromIriRef(ctx.iriRef());
        } else if(ctx.iriRef() != null && ctx.argList() != null) {
            List<TermAst> args = termListFromArgList(ctx.argList());
            IriAst iriRef = (IriAst) termFromIriRef(ctx.iriRef());
            return new FunctionCallAst(iriRef, args);
        } else {
            throw new QueryEvaluationException("Unexpected element in IRI ref or function");
        }
    }

    public List<TermAst> termListFromArgList(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ArgListContext ctx) {
        return ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
    }

    public TermAst termFromIriRef(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.IriRefContext ctx) {
        return this.iri(ctx.getText());
    }

    public TermAst termFromNumericLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralContext ctx) {
        if(ctx.numericLiteralUnsigned() != null) {
            return termFromNumericLiteralUnsigned(ctx.numericLiteralUnsigned());
        } else if(ctx.numericLiteralPositive() != null) {
            return termFromNumericLiteralPositive(ctx.numericLiteralPositive());
        } else if(ctx.numericLiteralNegative() != null) {
            return termFromNumericLiteralNegative(ctx.numericLiteralNegative());
        } else {
            throw new QueryEvaluationException("Unexpected content for numeric literal");
        }
    }

    public TermAst termFromNumericLiteralNegative(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralNegativeContext ctx) {
        if(ctx.INTEGER_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdNegativeInteger.getIRI().stringValue());
        } else if(ctx.DECIMAL_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for negative numeric literal");
        }
    }

    public TermAst termFromNumericLiteralPositive(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralPositiveContext ctx) {
        if(ctx.INTEGER_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdPositiveInteger.getIRI().stringValue());
        } else if(ctx.DECIMAL_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    public TermAst termFromNumericLiteralUnsigned(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralUnsignedContext ctx) {
        if(ctx.INTEGER() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdUnsignedInt.getIRI().stringValue());
        } else if(ctx.DECIMAL() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    public TermAst termFromBlankNode(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BlankNodeContext ctx) {
        return this.iri(ctx.getText());
    }

    public ExprAst expressionFromConstraint(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConstraintContext ctx) {
        if (ctx.builtInCall() != null) {
            return expressionFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.functionCall() != null) {
            IriAst functionTermAst = new IriAst(ctx.functionCall().iriRef().getText());
            List<TermAst> args = ctx.functionCall().argList().expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            return new FunctionCallAst(functionTermAst, args);
        } else if (ctx.brackettedExpression() != null && ctx.brackettedExpression().expression() != null) {
            return expressionFromBrackettedExpression(ctx.brackettedExpression());
        } else {
            throw new QueryEvaluationException("No createFunCall found in filter");
        }
    }

    public ExprAst expressionFromBuiltInCall(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BuiltInCallContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            if (ctx.STR() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
            } else if (ctx.LANG() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
            } else if (ctx.LANGMATCHES() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.LANGMATCHES, args);
            } else if (ctx.DATATYPE() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
            } else if (ctx.SAME_TERM() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
            } else if (ctx.IS_URI() != null || ctx.IS_IRI() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, args);
            } else if (ctx.IS_BLANK() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, args);
            } else if (ctx.IS_LITERAL() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, args);
            } else {
                throw new QueryEvaluationException("Unexpected function for a  BuiltInCall for token " + ctx.getText());
            }
        } else if (ctx.BOUND() != null) {
            return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(this.var(ctx.var_().getText())));
        } else if (ctx.regexExpression() != null) {
            return expressionFromRegex(ctx.regexExpression());
        } else {
            throw new QueryEvaluationException("Unable to resolve BuiltInCall for token " + ctx.getText());
        }
    }

    public ExprAst expression(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            return this.expressionFromConditionalOr(ctx.conditionalOrExpression());
        } else {
            throw new QueryEvaluationException("No conditional OR found");
        }
    }

    public ExprAst expressionFromConditionalOr(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConditionalOrExpressionContext ctx) {
        if (ctx.conditionalAndExpression() != null && !ctx.conditionalAndExpression().isEmpty()) {
            if (ctx.conditionalAndExpression().size() > 1) {
                List<TermAst> args = ctx.conditionalAndExpression().stream().map(arg -> (TermAst) expressionFromConditionalAnd(arg)).toList();
                return (ExprAst) createConstraint(ASTConstants.OPERATOR.OR, args);
            } else {
                return expressionFromConditionalAnd(ctx.conditionalAndExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No conditional AND  found");
        }
    }

    public ExprAst expressionFromConditionalAnd(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConditionalAndExpressionContext ctx) {
        if (ctx.valueLogical() != null && !ctx.valueLogical().isEmpty()) {
            if (ctx.valueLogical().size() > 1) {
                List<TermAst> args = ctx.valueLogical().stream().map(arg -> (TermAst) expressionFromValueLogical(arg)).toList();
                return (ExprAst) createConstraint(ASTConstants.OPERATOR.AND, args);
            } else {
                return expressionFromValueLogical(ctx.valueLogical().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No logical value found");
        }
    }

    public ExprAst expressionFromValueLogical(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ValueLogicalContext ctx) {
        if (ctx.relationalExpression() != null) {
            return this.expressionFromRelational(ctx.relationalExpression());
        } else {
            throw new QueryEvaluationException("No relational expression found");
        }
    }

    public ExprAst expressionFromRelational(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.RelationalExpressionContext ctx) {
        if (ctx.numericExpression() != null && !ctx.numericExpression().isEmpty()) {
            if (ctx.numericExpression().size() > 1) {
                ASTConstants.OPERATOR op;
                if (ctx.EQUAL() != null) {
                    op = ASTConstants.OPERATOR.EQ;
                } else if (ctx.NOT_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.NE;
                } else if (ctx.LESS() != null) {
                    op = ASTConstants.OPERATOR.LT;
                } else if (ctx.LESS_OR_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.LE;
                } else if (ctx.GREATER() != null) {
                    op = ASTConstants.OPERATOR.GT;
                } else if (ctx.GREATER_OR_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.GE;
                } else {
                    throw new QueryEvaluationException("Unexpected operator in relational expression");
                }
                List<TermAst> args = ctx.numericExpression().stream().map(arg -> (TermAst) expressionFromNumeric(arg)).toList();
                return (ExprAst) createConstraint(op, args);
            } else {
                return expressionFromNumeric(ctx.numericExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No numeric expression found");
        }
    }

    public ExprAst expressionFromNumeric(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            return this.expressionFromAdditive(ctx.additiveExpression());
        } else {
            throw new QueryEvaluationException("No additive expression found");
        }
    }

    public ExprAst expressionFromAdditive(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.AdditiveExpressionContext ctx) {
        if(ctx.multiplicativeExpression() != null && ! ctx.multiplicativeExpression().isEmpty()) {
            if (ctx.multiplicativeExpression().size() > 1
                    || ! ctx.numericLiteralNegative().isEmpty()
                    || ! ctx.numericLiteralPositive().isEmpty()) {
                ExprAst leftHand = expressionFromMultiplicative(ctx.multiplicativeExpression().getFirst());
                for(int i = 1; i < ctx.getChildCount() ; i++) {
                    ParseTree numericChild = ctx.getChild(i);
                    ExprAst rightHand = switch (numericChild) {
                        case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.MultiplicativeExpressionContext multiplicativeExpressionContext ->
                                expressionFromMultiplicative(multiplicativeExpressionContext);
                        case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralPositiveContext numericLiteralPositiveContext ->
                                (ExprAst) termFromNumericLiteralPositive(numericLiteralPositiveContext);
                        case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralNegativeContext numericLiteralNegativeContext ->
                                (ExprAst) termFromNumericLiteralNegative(numericLiteralNegativeContext);
                        case null, default ->
                                throw new QueryEvaluationException("Unexpected left hand expression in additive expression");
                    };
                    ASTConstants.OPERATOR op;
                    if (ctx.getToken(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.PLUS, i) != null) {
                        op = ASTConstants.OPERATOR.PLUS;
                    } else if (ctx.getToken(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.MINUS, i) != null) {
                        op = ASTConstants.OPERATOR.MINUS;
                    } else {
                        throw new QueryEvaluationException("Unexpected operator in additive expression");
                    }
                    leftHand = (ExprAst) createConstraint(op, List.of(leftHand, rightHand));
                }
                return leftHand;
            } else {
                return expressionFromMultiplicative(ctx.multiplicativeExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No multiplicative expression found");
        }
    }

    public ExprAst expressionFromMultiplicative(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.MultiplicativeExpressionContext ctx) {
        if(ctx.unaryExpression() != null && ! ctx.unaryExpression().isEmpty()) {
            if (ctx.unaryExpression().size() > 1) {
                ExprAst head = expressionFromUnary(ctx.unaryExpression().getFirst());
                for(int i = 1; i < ctx.getChildCount() ; i++) {
                    ParseTree numericChild = ctx.getChild(i);
                    ExprAst leftHand = expressionFromUnary((fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.UnaryExpressionContext) numericChild);
                    ASTConstants.OPERATOR op;
                    if (ctx.getToken(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.STAR, i) != null) {
                        op = ASTConstants.OPERATOR.MUL;
                    } else if (ctx.getToken(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.SLASH, i) != null) {
                        op = ASTConstants.OPERATOR.DIV;
                    } else {
                        throw new QueryEvaluationException("Unexpected operator in multiplicative expression");
                    }
                    head = (ExprAst) createConstraint(op, List.of(head, leftHand));
                }
                return head;
            } else {
                return expressionFromUnary(ctx.unaryExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No unary expression found");
        }
    }

    public ExprAst expressionFromUnary(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.UnaryExpressionContext ctx) {
        ASTConstants.Constraint op = null;
        if(ctx.PLUS() != null) {
            op = ASTConstants.OPERATOR.PLUS;
        } else if (ctx.MINUS() != null) {
            op = ASTConstants.OPERATOR.MINUS;
        } else if(ctx.EXCLAMATION() != null) {
            op = ASTConstants.OPERATOR.NOT;
        }
        if(op != null) {
            return (ExprAst) createConstraint(op, List.of(termFromPrimary(ctx.primaryExpression())));
        } else {
            return (ExprAst) termFromPrimary(ctx.primaryExpression());
        }
    }

    public ExprAst expressionFromBrackettedExpression(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BrackettedExpressionContext ctx) {
        return expression(ctx.expression());
    }

    public ExprAst expressionFromRegex(SparqlParser.RegexExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.REGEX, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REGEX call");
        }
    }
}