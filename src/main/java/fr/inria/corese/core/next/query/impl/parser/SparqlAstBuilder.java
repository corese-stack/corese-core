package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Build a minimal SPARQL AST for:
 * - Triple patterns (?s ?p ?o)
 * - Basic Graph Patterns (BGP) via TriplesBlock
 * - GroupGraphPattern as a container (can contain multiple TriplesBlock and later OPTIONAL/UNION/etc.)
 * - ASK query
 * Compatible with the common SPARQL grammar shape:
 * GroupGraphPattern
 * : '{'
 * ( TriplesBlock?
 * ( GraphPatternNotTriples '.'? TriplesBlock? )*
 * )?
 * '}'
 * This builder expects the listener to call:
 * - enterGroup()/exitGroup() on enter/exitGroupGraphPattern
 * - enterBgp()/exitBgp() on enter/exitTriplesBlock
 * - addTriple(s,p,o) whenever a triple pattern is recognized (usually on exitTriplesSameSubject)
 * - enterAskQuery at the start of the declaration of an ASK query
 * - enterSelectQuery at the start of the declaration of a Select query
 * - enterConstructQuery / enterConstructTemplate / exitConstructTemplate / exitConstructQuery for CONSTRUCT
 * - addConstructTriple for triples inside the CONSTRUCT template (not WHERE)
 */
public final class SparqlAstBuilder {

    private ASTConstants.QUERY_TYPE queryType = ASTConstants.QUERY_TYPE.UNDEFINED;

    // --- Internal stacks (scopes) ---

    /**
     * Stack of groups; each group is a list of patterns (BgpAst now, later OptionalAst/UnionAst/...)
     */
    private final Deque<List<PatternAst>> groupStack = new ArrayDeque<>();

    /**
     * Stack of current BGP triples (TriplesBlock). Nested blocks are rare but stack keeps it safe.
     */
    private final Deque<List<TriplePatternAst>> bgpStack = new ArrayDeque<>();

    /**
     * At enterOptional(), we push groupStack.size(). At exitGroup(), if groupStack.size() equals peek, we wrap in OptionalAst.
     */
    private final Deque<Integer> optionalGroupDepths = new ArrayDeque<>();

    /**
     * Top-level WHERE clause, set when the root group is closed in exitGroup().
     */
    private GroupGraphPatternAst whereClause;

    /**
     * SELECT projection (* or explicit variables). Set by SelectQueryFeature in enterSelectQuery.
     */
    private ProjectionAst projection = ProjectionAsts.selectAll();

    /**
     * Dataset clause (FROM/FROM NAMED)
     */
    private final Set<IriAst> datasetDefaultGraphs = new LinkedHashSet<>();
    private final Set<IriAst> datasetNamedGraphs = new LinkedHashSet<>();

    /** SELECT DISTINCT / REDUCED. Set by SelectQueryFeature when parsing SELECT (DISTINCT | REDUCED)? ... */
    private boolean distinct;
    private boolean reduced;

    /**
     * Variable to hold the value of LIMIT and OFFSET.
     */
    private Long limit;
    private Long offset;

    /**
     * Order conditions
     */
    private final List<OrderConditionAst> orderConditions = new ArrayList<>();

    /**
     * Parser options (e.g. for future use: strict mode, base IRI).
     */
    private final SparqlParserOptions options;

    /**
     * Stack of UNION branch lists currently being collected.
     */
    private final Deque<List<GroupGraphPatternAst>> unionStack = new ArrayDeque<>();

    /**
     * DESCRIBE resources (IRIs or variables). Set by DescribeQueryFeature.
     */
    private final List<TermAst> describeResources = new ArrayList<>();

    /**
     * Triples of the CONSTRUCT template (not the WHERE BGP). Filled between
     * {@link #enterConstructTemplate()} and {@link #exitConstructTemplate()}.
     */
    private final List<TriplePatternAst> constructTriples = new ArrayList<>();

    /**
     * Template AST after {@link #exitConstructTemplate()}; consumed in {@link #getResult()}.
     */
    private ConstructTemplateAst constructTemplate;

    /**
     * Helper used to compute visible and referenced variables.
     */
    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

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

    public void enterConstructQuery() {
        queryType = ASTConstants.QUERY_TYPE.CONSTRUCT;
        constructTemplate = null;
        constructTriples.clear();
    }

    public void exitConstructQuery() {
    }

    public void enterConstructTemplate() {
        constructTriples.clear();
    }

    public void exitConstructTemplate() {
        this.constructTemplate = new ConstructTemplateAst(List.copyOf(constructTriples));
    }

    /**
     * Adds a triple to the CONSTRUCT template (inside {@code ConstructTriples}, not WHERE).
     */
    public void addConstructTriple(TermAst s, TermAst p, TermAst o) {
        constructTriples.add(new TriplePatternAst(s, p, o));
    }

    /**
     * Sets SELECT * (project all variables from the body).
     */
    public void setProjectionAll() {
        this.projection = ProjectionAsts.selectAll();
    }

    /**
     * Sets explicit SELECT variables (e.g. SELECT ?s ?p). Variable names may include ? or $ prefix.
     */
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

    /**
     * Sets the projection from an existing AST.
     */
    public void setProjection(ProjectionAst projection) {
        this.projection = projection != null ? projection : ProjectionAsts.selectAll();
    }

    /**
     * Sets SELECT DISTINCT. Called by SelectQueryFeature when {@code DISTINCT} is present.
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Sets SELECT REDUCED. Called by SelectQueryFeature when {@code REDUCED} is present.
     */
    public void setReduced(boolean reduced) {
        this.reduced = reduced;
    }

    public void addFromGraph(IriAst graph) {
        this.datasetDefaultGraphs.add(graph);
    }

    public void addFromNamedGraph(IriAst graph) {
        this.datasetNamedGraphs.add(graph);
    }

    /**
     * Sets the LIMIT for pagination
     *
     * @param limit
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }

    /**
     * Sets the OFFSET for pagination
     *
     * @param offset
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Enter a { ... } groupGraphPattern.
     */
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

    /**
     * Enter a TriplesBlock -> begin collecting triples for a BGP.
     */
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
        if (this.hasCurrentGroup()) {
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
    public void exitOptional() {
    }

    // --- Result ---

    /**
     * Returns the final AST.
     * The top-level WHERE clause must have been set by exitGroup() (root group closed).
     * One of the enter*Query() methods must have been called for the corresponding query form.
     *
     * @return the root {@link QueryAst}
     * @throws QueryEvaluationException if query type could not be determined (no enter*Query() called)
     * @throws IllegalStateException    if no WHERE clause was set (exitGroup() not called for root) or unhandled query type
     */
    public QueryAst getResult() {
        if (whereClause == null) {
            throw new IllegalStateException("No WHERE clause: did you call exitGroup() for the top-level GroupGraphPattern?");
        }
        DatasetClauseAst datasetClauseAst = new DatasetClauseAst(datasetDefaultGraphs, datasetNamedGraphs);
        return switch (this.queryType) {
            case ASK -> buildAskQueryAst(datasetClauseAst);
            case CONSTRUCT -> buildConstructQueryAst(datasetClauseAst);
            case DESCRIBE -> buildDescribeQueryAst(datasetClauseAst);
            case SELECT -> buildSelectQueryAst(datasetClauseAst);
            case UNDEFINED -> throw new QueryEvaluationException("Could not determine the type of query during parsing");
        };
    }

    // --- Internal helpers ---

    private boolean hasCurrentGroup() {
        return !this.groupStack.isEmpty();
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
     * Builds the AST for ASK queries.
     */
    private AskQueryAst buildAskQueryAst(DatasetClauseAst datasetClauseAst) {
        return new AskQueryAst(datasetClauseAst, whereClause);
    }

    /**
     * Builds the AST for SELECT queries.
     */
    private SelectQueryAst buildSelectQueryAst(DatasetClauseAst datasetClauseAst) {
        validateSelectQueryScope();
        return new SelectQueryAst(projection, datasetClauseAst, whereClause, buildSolutionModifier());
    }

    /**
     * Builds the AST for DESCRIBE queries.
     */
    private DescribeQueryAst buildDescribeQueryAst(DatasetClauseAst datasetClauseAst) {
        // TODO #306: validate variable scope for DESCRIBE modifiers when DescribeQueryAst carries them.
        return new DescribeQueryAst(datasetClauseAst, describeResources, whereClause);
    }

    /**
     * Builds the AST for CONSTRUCT queries.
     */
    private ConstructQueryAst buildConstructQueryAst(DatasetClauseAst datasetClauseAst) {
        // TODO #306: validate variable scope for CONSTRUCT modifiers when ConstructQueryAst carries them.
        return new ConstructQueryAst(
                constructTemplate != null ? constructTemplate : new ConstructTemplateAst(List.of()),
                datasetClauseAst,
                whereClause,
                buildSolutionModifier());
    }

    /**
     * Validates SELECT projection and ORDER BY variables against the WHERE clause scope.
     */
    private void validateSelectQueryScope() {
        // TODO #306: extend this validation to GROUP BY when it is supported by the next parser.
        Set<String> visibleVariables = variableScopeAnalyzer.collectVisibleVariables(whereClause);

        // SELECT * still needs ORDER BY validation.
        if (!projection.selectAll()) {
            validateProjectionVariables(visibleVariables);
        }

        validateOrderVariables(collectOrderByAvailableVariables(visibleVariables));
    }

    /**
     * Validates explicit projection variables against the WHERE clause scope.
     *
     * @param visibleVariables variable names visible from the WHERE clause
     */
    private void validateProjectionVariables(Set<String> visibleVariables) {
        for (VarAst projectedVar : projection.variables()) {
            if (!visibleVariables.contains(projectedVar.name())) {
                throw new QueryValidationException(buildOutOfScopeVariableMessage(
                        projectedVar.name(),
                        "SELECT projection"));
            }
        }
    }

    /**
     * Collects variables that may be referenced from ORDER BY.
     *
     * <p>SPARQL applies ORDER BY before the final projection step. In the current next parser,
     * explicit projection variables are already validated against the WHERE clause, so adding
     * them here mainly keeps the availability rule explicit while staying within the current
     * SPARQL 1.0 feature set.
     *
     * @param visibleVariables variable names visible from the WHERE clause
     * @return variable names available to ORDER BY validation
     */
    private Set<String> collectOrderByAvailableVariables(Set<String> visibleVariables) {
        Set<String> availableVariables = new LinkedHashSet<>(visibleVariables);
        if (!projection.selectAll()) {
            for (VarAst projectedVar : projection.variables()) {
                availableVariables.add(projectedVar.name());
            }
        }
        return availableVariables;
    }

    /**
     * Validates ORDER BY variables against the variables available at ORDER BY time.
     *
     * @param availableOrderVariables variable names available to ORDER BY
     */
    private void validateOrderVariables(Set<String> availableOrderVariables) {
        for (OrderConditionAst orderCondition : orderConditions) {
            Set<String> referencedVariables = variableScopeAnalyzer
                    .collectReferencedVariables(orderCondition.expression());

            for (String variableName : referencedVariables) {
                if (!availableOrderVariables.contains(variableName)) {
                    throw new QueryValidationException(buildOutOfScopeVariableMessage(
                            variableName,
                            "ORDER BY"));
                }
            }
        }
    }

    private String buildOutOfScopeVariableMessage(String variableName, String clause) {
        return "Variable ?" + variableName + " used in " + clause + " is not visible in WHERE clause";
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

    /**
     * Builds the solution modifier (DISTINCT, REDUCED, ORDER BY, LIMIT, OFFSET) for SELECT.
     */
    private SolutionModifierAst buildSolutionModifier() {
        return new SolutionModifierAst(distinct, reduced, this.orderConditions, limit, offset);
    }

    public boolean isOrdered() {
        return ! this.orderConditions.isEmpty();
    }

    /**
     * Set the order condition
     * @param expr either a variable or a contraint
     */
    public void addOrderExpression(ASTConstants.OrderDirection direction, TermAst expr) {
        this.orderConditions.add(new OrderConditionAst(direction, expr));
    }

    /**
     * Signals the start of a {@code DESCRIBE} query declaration.
     * Sets the internal query type to {@link ASTConstants.QUERY_TYPE#DESCRIBE}.
     */
    public void enterDescribeQuery() {
        queryType = ASTConstants.QUERY_TYPE.DESCRIBE;
    }

    /**
     * Called when the parser exits a {@code DESCRIBE} query. No-op.
     */
    public void exitDescribeQuery() {
    }

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

    /**
     * Variable token text can be "?s" or "$s" depending on grammar.
     */
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
        switch (constraint) {
            case ASTConstants.OPERATOR.NOT -> {
                return new BooleanNotAst(args);
            }
            case ASTConstants.OPERATOR.PLUS -> {
                return new UnaryPlusAst(args);
            }
            case ASTConstants.OPERATOR.MINUS -> {
                return new UnaryMinusAst(args);
            }
            case ASTConstants.FUNCTION_CALL.BOUND -> {
                return new BoundAst(args);
            }
            case ASTConstants.FUNCTION_CALL.IS_IRI -> {
                return new IsIriAst(args);
            }
            case ASTConstants.FUNCTION_CALL.IS_BLANK -> {
                return new IsBlankAst(args);
            }
            case ASTConstants.FUNCTION_CALL.IS_LITERAL -> {
                return new IsLiteralAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STR -> {
                return new StrAst(args);
            }
            case ASTConstants.FUNCTION_CALL.LANG -> {
                return new LangAst(args);
            }
            case ASTConstants.FUNCTION_CALL.DATATYPE -> {
                return new DatatypeAst(args);
            }
            case ASTConstants.OPERATOR.OR -> {
                return new OrAst(args);
            }
            case ASTConstants.OPERATOR.AND -> {
                return new AndAst(args);
            }
            case ASTConstants.OPERATOR.EQ -> {
                return new EqualsAst(args);
            }
            case ASTConstants.OPERATOR.NE -> {
                return new DifferentAst(args);
            }
            case ASTConstants.OPERATOR.LT -> {
                return new LowerThanAst(args);
            }
            case ASTConstants.OPERATOR.LE -> {
                return new LowerOrEqualThanAst(args);
            }
            case ASTConstants.OPERATOR.GT -> {
                return new GreaterThanAst(args);
            }
            case ASTConstants.OPERATOR.GE -> {
                return new GreaterOrEqualThanAst(args);
            }
            case ASTConstants.OPERATOR.MUL -> {
                return new MultiplyAst(args);
            }
            case ASTConstants.OPERATOR.DIV -> {
                return new DivideAst(args);
            }
            case ASTConstants.OPERATOR.ADD -> {
                return new AddAst(args);
            }
            case ASTConstants.OPERATOR.SUB -> {
                return new SubtractAst(args);
            }
            case ASTConstants.FUNCTION_CALL.SAMETERM -> {
                return new SameTermAst(args);
            }
            case ASTConstants.FUNCTION_CALL.LANGMATCHES -> {
                return new LangMatchesAst(args);
            }
            case ASTConstants.FUNCTION_CALL.REGEX -> {
                if (args.size() == 2) {
                    return new BinaryRegexAst(args);
                } else if (args.size() == 3) {
                    return new TrinaryRegexAst(args);
                } else {
                    throw new QueryEvaluationException("Unexpected number of arguments (3) for REGEX keyword");
                }
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
        if (ctx.var_() != null) {
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
        if (ctx.brackettedExpression() != null) {
            return termFromBrackettedExpression(ctx.brackettedExpression());
        } else if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.iriRefOrFunction() != null) {
            return termFromIriRefOrFunction(ctx.iriRefOrFunction());
        } else if (ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        } else if (ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        } else if (ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        } else if (ctx.var_() != null) {
            return termFromVar(ctx.var_());
        } else {
            throw new QueryEvaluationException("Unexpected content of bracketed termFromExpression");
        }
    }

    public TermAst termFromVar(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.Var_Context ctx) {
        return this.var(ctx.getText());
    }

    public TermAst termFromBooleanLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BooleanLiteralContext ctx) {
        if (ctx.FALSE() != null) {
            return new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        } else if (ctx.TRUE() != null) {
            return new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected value for boolean literal");
        }
    }

    public TermAst termFromIriRefOrFunction(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.IriRefOrFunctionContext ctx) {
        if (ctx.iriRef() != null && ctx.argList() == null) {
            return termFromIriRef(ctx.iriRef());
        } else if (ctx.iriRef() != null && ctx.argList() != null) {
            List<TermAst> args = termListFromArgList(ctx.argList());
            IriAst iriRef = (IriAst) termFromIriRef(ctx.iriRef());
            return createFunCall(iriRef, args);
        } else {
            throw new QueryEvaluationException("Unexpected element in IRI ref or function");
        }
    }

    public List<TermAst> termListFromArgList(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ArgListContext ctx) {
        return ctx.expression().stream().map(arg -> (TermAst) termFromExpression(arg)).toList();
    }

    public TermAst termFromIriRef(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.IriRefContext ctx) {
        return this.iri(ctx.getText());
    }

    public TermAst termFromNumericLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralContext ctx) {
        if (ctx.numericLiteralUnsigned() != null) {
            return termFromNumericLiteralUnsigned(ctx.numericLiteralUnsigned());
        } else if (ctx.numericLiteralPositive() != null) {
            return termFromNumericLiteralPositive(ctx.numericLiteralPositive());
        } else if (ctx.numericLiteralNegative() != null) {
            return termFromNumericLiteralNegative(ctx.numericLiteralNegative());
        } else {
            throw new QueryEvaluationException("Unexpected content for numeric literal");
        }
    }

    public TermAst termFromNumericLiteralNegative(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralNegativeContext ctx) {
        if (ctx.INTEGER_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdNegativeInteger.getIRI().stringValue());
        } else if (ctx.DECIMAL_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_NEGATIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for negative numeric literal");
        }
    }

    public TermAst termFromNumericLiteralPositive(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralPositiveContext ctx) {
        if (ctx.INTEGER_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdPositiveInteger.getIRI().stringValue());
        } else if (ctx.DECIMAL_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_POSITIVE() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    public TermAst termFromNumericLiteralUnsigned(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralUnsignedContext ctx) {
        if (ctx.INTEGER() != null) {
            return this.literal(ctx.getText(), null, XSD.xsdUnsignedInt.getIRI().stringValue());
        } else if (ctx.DECIMAL() != null) {
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

    public TermAst termFromConstraint(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConstraintContext ctx) {
        if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.functionCall() != null) {
            IriAst functionTermAst = new IriAst(ctx.functionCall().iriRef().getText());
            List<TermAst> args = ctx.functionCall().argList().expression().stream().map(this::termFromExpression).toList();
            return new FunctionCallAst(functionTermAst, args);
        } else if (ctx.brackettedExpression() != null && ctx.brackettedExpression().expression() != null) {
            return termFromBrackettedExpression(ctx.brackettedExpression());
        } else {
            throw new QueryEvaluationException("No createFunCall found in filter");
        }
    }

    public TermAst termFromBuiltInCall(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BuiltInCallContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            if (ctx.STR() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
            } else if (ctx.LANG() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
            } else if (ctx.LANGMATCHES() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.LANGMATCHES, args);
            } else if (ctx.DATATYPE() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
            } else if (ctx.SAME_TERM() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
            } else if (ctx.IS_URI() != null || ctx.IS_IRI() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, args);
            } else if (ctx.IS_BLANK() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, args);
            } else if (ctx.IS_LITERAL() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, args);
            } else if (ctx.BOUND() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(this.var(ctx.var_().getText())));
            } else if (ctx.regexExpression() != null) {
                return termFromRegex(ctx.regexExpression());
            } else {
                throw new QueryEvaluationException("Unexpected function for a  BuiltInCall for token " + ctx.getText());
            }
        } else {
            throw new QueryEvaluationException("Unable to resolve BuiltInCall for token " + ctx.getText());
        }
    }

    public TermAst termFromExpression(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            return this.termFromConditionalOr(ctx.conditionalOrExpression());
        } else {
            throw new QueryEvaluationException("No conditional OR found");
        }
    }

    public TermAst termFromConditionalOr(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConditionalOrExpressionContext ctx) {
        if (ctx.conditionalAndExpression() != null && !ctx.conditionalAndExpression().isEmpty()) {
            if (ctx.conditionalAndExpression().size() > 1) {
                List<TermAst> args = ctx.conditionalAndExpression().stream().map(this::termFromConditionalAnd).toList();
                return createConstraint(ASTConstants.OPERATOR.OR, args);
            } else {
                return termFromConditionalAnd(ctx.conditionalAndExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No conditional AND  found");
        }
    }

    public TermAst termFromConditionalAnd(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConditionalAndExpressionContext ctx) {
        if (ctx.valueLogical() != null && !ctx.valueLogical().isEmpty()) {
            if (ctx.valueLogical().size() > 1) {
                List<TermAst> args = ctx.valueLogical().stream().map(this::termFromValueLogical).toList();
                return createConstraint(ASTConstants.OPERATOR.AND, args);
            } else {
                return termFromValueLogical(ctx.valueLogical().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No logical value found");
        }
    }

    public TermAst termFromValueLogical(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ValueLogicalContext ctx) {
        if (ctx.relationalExpression() != null) {
            return this.termFromRelational(ctx.relationalExpression());
        } else {
            throw new QueryEvaluationException("No relational termFromExpression found");
        }
    }

    public TermAst termFromRelational(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.RelationalExpressionContext ctx) {
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
                    throw new QueryEvaluationException("Unexpected operator in relational termFromExpression");
                }
                List<TermAst> args = ctx.numericExpression().stream().map(this::termFromNumeric).toList();
                return createConstraint(op, args);
            } else {
                return termFromNumeric(ctx.numericExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No numeric termFromExpression found");
        }
    }

    public TermAst termFromNumeric(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            return this.termFromAdditive(ctx.additiveExpression());
        } else {
            throw new QueryEvaluationException("No additive termFromExpression found");
        }
    }

    public TermAst termFromAdditive(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.AdditiveExpressionContext ctx) {
        if (ctx.multiplicativeExpression() != null && !ctx.multiplicativeExpression().isEmpty()) {
            if (ctx.multiplicativeExpression().size() > 1
                    || !ctx.numericLiteralNegative().isEmpty()
                    || !ctx.numericLiteralPositive().isEmpty()) {
                TermAst leftHand = termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
                ASTConstants.OPERATOR op = ASTConstants.OPERATOR.ADD;
                for (int i = 1; i < ctx.getChildCount(); i++) {
                    ParseTree child = ctx.getChild(i);
                    if (child instanceof TerminalNode) {
                        if (Objects.equals(child.getText(), "+")) {
                            op = ASTConstants.OPERATOR.ADD;
                        } else if (Objects.equals(child.getText(), "-")) {
                            op = ASTConstants.OPERATOR.SUB;
                        } else {
                            throw new QueryEvaluationException("Unexpected operator in additive termFromExpression " + child.getText());
                        }
                    } else {
                        TermAst rightHand = switch (child) {
                            case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.MultiplicativeExpressionContext multiplicativeExpressionContext ->
                                    termFromMultiplicative(multiplicativeExpressionContext);
                            case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralPositiveContext numericLiteralPositiveContext ->
                                    (ExprAst) termFromNumericLiteralPositive(numericLiteralPositiveContext);
                            case fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.NumericLiteralNegativeContext numericLiteralNegativeContext ->
                                    (ExprAst) termFromNumericLiteralNegative(numericLiteralNegativeContext);
                            case null, default ->
                                    throw new QueryEvaluationException("Unexpected left hand termFromExpression in additive termFromExpression " + ctx.getText() + " " + child.getText());
                        };
                        leftHand = createConstraint(op, List.of(leftHand, rightHand));
                    }
                }
                return leftHand;
            } else {
                return termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No multiplicative termFromExpression found");
        }
    }

    public TermAst termFromMultiplicative(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.MultiplicativeExpressionContext ctx) {
        if (ctx.unaryExpression() != null && !ctx.unaryExpression().isEmpty()) {
            if (ctx.unaryExpression().size() > 1) {
                TermAst head = termFromUnary(ctx.unaryExpression().getFirst());
                TermAst rightHand = null;
                ASTConstants.OPERATOR op = null;
                for (int i = 1; i < ctx.getChildCount(); i+=2) {
                    ParseTree operatorContext = ctx.getChild(i);
                    ParseTree rightHandContext = ctx.getChild(i+1);

                    if(rightHandContext instanceof SparqlParser.UnaryExpressionContext unaryExpressionContext) {
                        rightHand = termFromUnary( unaryExpressionContext);
                    }

                    if(operatorContext instanceof TerminalNode terminalNode) {
                        if(Objects.equals(terminalNode.getText(), "*")) {
                            op = ASTConstants.OPERATOR.MUL;
                        } else if(Objects.equals(terminalNode.getText(), "/")) {
                            op = ASTConstants.OPERATOR.DIV;
                        }
                    }
                    if(op != null && rightHand != null) {
                        head = createConstraint(op, List.of(head, rightHand));
                    } else {
                        throw new QuerySyntaxException("Unexpected operator or right hand content in " + ctx.getText());
                    }
                }
                return head;
            } else {
                return termFromUnary(ctx.unaryExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No unary termFromExpression found");
        }
    }

    public TermAst termFromUnary(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.UnaryExpressionContext ctx) {
        ASTConstants.Constraint op = null;
        if (ctx.PLUS() != null) {
            op = ASTConstants.OPERATOR.PLUS;
        } else if (ctx.MINUS() != null) {
            op = ASTConstants.OPERATOR.MINUS;
        } else if (ctx.EXCLAMATION() != null) {
            op = ASTConstants.OPERATOR.NOT;
        }
        if (op != null) {
            return createConstraint(op, List.of(termFromPrimary(ctx.primaryExpression())));
        } else {
            return termFromPrimary(ctx.primaryExpression());
        }
    }

    public TermAst termFromBrackettedExpression(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.BrackettedExpressionContext ctx) {
        return termFromExpression(ctx.expression());
    }

    public TermAst termFromRegex(SparqlParser.RegexExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return this.createConstraint(ASTConstants.FUNCTION_CALL.REGEX, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REGEX call");
        }
    }
}
