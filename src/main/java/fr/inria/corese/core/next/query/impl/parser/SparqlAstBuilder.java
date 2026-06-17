package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Root class of the query builders.
 * Offers functions to create ASTs and convert contexts from the ANTLR parser to ASTs
 */
public abstract class SparqlAstBuilder {

    /**
     * Parser options (e.g. for future use: strict mode, base IRI).
     */
    protected final SparqlParserOptions options;

    /**
     * Effective base URI after prologue (parser options, then possibly {@code BASE}).
     */
    protected String baseUri;

    /**
     * Prefix declarations in source order (including redeclarations).
     */
    protected final List<PrefixDeclarationAst> prefixDeclarations = new ArrayList<>();

    /**
     * Dataset clause (FROM/FROM NAMED)
     */
    protected final Set<IriAst> datasetDefaultGraphs = new LinkedHashSet<>();
    protected final Set<IriAst> datasetNamedGraphs = new LinkedHashSet<>();

    /**
     * Used for BIND scope checks (variable must not already be visible in the same group).
     */
    protected final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    /**
     * Stack of groups; each group is a list of patterns (BgpAst now, later OptionalAst/UnionAst/...)
     */
    protected final Deque<List<PatternAst>> groupStack = new ArrayDeque<>();

    /**
     * Stack of current BGP triples (TriplesBlock). Nested blocks are rare but stack keeps it safe.
     */
    protected final Deque<List<TriplePatternAst>> bgpStack = new ArrayDeque<>();

    /**
     * Stack of currently open SELECT operations (top-level SELECT and nested SELECT subqueries).
     */
    protected final Deque<SelectFrame> selectStack = new ArrayDeque<>();

    /**
     * When true, the next {@link #enterGroup()} opens the {@code groupGraphPattern} of a
     * {@code whereClause} for the innermost active {@link SparqlQueryAstBuilder.SelectFrame}. On matching
     * {@link #exitGroup()}, that group is stored as {@link SparqlQueryAstBuilder.SelectFrame#whereClause} instead of
     * being appended to the enclosing graph group (required when outer WHERE groups are still open,
     * e.g. subqueries {@code { SELECT ... WHERE { ... }}}).
     */
    protected boolean selectWhereGroupFollows;

    /**
     * Depths ({@code groupStack.size()} after push) of SELECT WHERE groups still to be closed,
     * innermost last. Matched on {@link #exitGroup()} while {@link #hasCurrentSelect()}.
     */
    protected final Deque<Integer> selectWhereGroupDepths = new ArrayDeque<>();

    /**
     * At enterOptional(), we push groupStack.size(). At exitGroup(), if groupStack.size() equals peek, we wrap in OptionalAst.
     */
    protected final Deque<Integer> optionalGroupDepths = new ArrayDeque<>();

    /**
     * At enterMinus(), we push groupStack.size(). At exitGroup(), if groupStack.size() equals peek, we wrap in MinusAst.
     */
    protected final Deque<Integer> minusGroupDepths = new ArrayDeque<>();

    /**
     * At enterExistsFunc()/enterNotExistsFunc(), we push groupStack.size().
     * At exitGroup(), if groupStack.size() equals peek, we capture in capturedExistsPattern.
     */
    protected final Deque<Integer> existsGroupDepths = new ArrayDeque<>();

    /**
     * Stack of UNION branch lists currently being collected.
     */
    protected final Deque<List<GroupGraphPatternAst>> unionStack = new ArrayDeque<>();

    /**
     * Holds the endpoint and silent flag for each active SERVICE scope.
     * Pushed on enterService(), matched (by groupDepth) in exitGroup() to produce a ServiceAst.
     *
     * @param groupDepth the {@code groupStack.size()} at the time {@link #enterService} was called,
     *                   i.e. the depth before the SERVICE body's {@link SparqlQueryAstBuilder#enterGroup()} is invoked.
     *                   After {@link SparqlQueryAstBuilder#exitGroup()} pops the service body the stack returns to this
     *                   depth, which is used as the signal to wrap the group in a
     *                   {@link ServiceAst}.
     * @param endpoint   the remote service endpoint (IRI or variable)
     * @param silent     whether the {@code SILENT} keyword was present
     */
    protected record ServiceEntry(int groupDepth, TermAst endpoint, boolean silent) {}

    /**
     * Stack of pending SERVICE entries, innermost last.
     * At enterService(), we push a ServiceEntry with the current groupStack depth.
     * At exitGroup(), if groupStack.size() equals the top entry's groupDepth, we wrap the closed
     * group in a ServiceAst.
     */
    protected final Deque<ServiceEntry> serviceStack = new ArrayDeque<>();

    /*
     * having conditions
     */
    protected final List<TermAst> havingConditions = new ArrayList<>();

    /**
     * Top-level WHERE clause, set when the root group is closed in exitGroup().
     */
    protected GroupGraphPatternAst whereClause;

    /**
     * Captured GroupGraphPattern for the last closed EXISTS/NOT EXISTS block.
     * Consumed by termFromBuiltInCall via popCapturedExistsPattern().
     */
    protected final Deque<GroupGraphPatternAst> capturedExistsStack = new ArrayDeque<>();

    protected SparqlAstBuilder(SparqlParserOptions options) {
        this.options = options;
        this.baseUri = options.getBaseIRI();
    }

    // --- Construction entry points (called by listener) ---

    public void setBaseUri(String uri) {
        if(this.baseUri != null && !this.baseUri.equals(options.getBaseIRI())) {
            throw new QuerySyntaxException("Base URI already set, multiple BASE declarations are forbidden.");
        }
        this.baseUri = uri;
    }

    public void addPrefix(String prefix, String uri) {
        PrefixDeclarationAst declarationAst = new PrefixDeclarationAst(prefix, new IriAst(uri));
        if(this.prefixDeclarations.stream().anyMatch(declaration ->
                Objects.equals(declaration.prefix(), declarationAst.prefix())
        )) {
            throw new QuerySyntaxException("Prefix " + prefix + " has already been declared");
        }
        this.prefixDeclarations.add(declarationAst);
    }
    
    public String getBaseUri() {
        return this.baseUri;
    }
    
    public List<PrefixDeclarationAst> getPrefixDeclaration() {
        return this.prefixDeclarations;
    }

    // --- Abstract query creation class ---
    public abstract QueryAst getResult();

    // --- Pattern creation ---

    protected boolean hasCurrentGroup() {
        return !this.groupStack.isEmpty();
    }

    protected List<PatternAst> currentGroup() {
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
    protected void ensureNoOpenBgp() {
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

    public void addFromGraph(IriAst graph) {
        this.datasetDefaultGraphs.add(graph);
    }

    public void addFromNamedGraph(IriAst graph) {
        this.datasetNamedGraphs.add(graph);
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
     * Called when the parser enters a {@code whereClause}
     * The following {@link SparqlQueryAstBuilder#enterGroup()} for that clause must attach to the current {@link SparqlQueryAstBuilder.SelectFrame}
     * when one is active.
     */
    public void enterWhereClause() {
        if (hasCurrentSelect()) {
            selectWhereGroupFollows = true;
        }
    }

    /**
     * Enter a { ... } groupGraphPattern.
     */
    public void enterGroup() {
        groupStack.push(new ArrayList<>());
        if (selectWhereGroupFollows && hasCurrentSelect()) {
            selectWhereGroupDepths.push(groupStack.size());
            selectWhereGroupFollows = false;
        } else {
            selectWhereGroupFollows = false;
        }
    }

    /**
     * Exit a { ... } groupGraphPattern.
     * Pops the current group, wraps it in {@link GroupGraphPatternAst}. If we had entered OPTIONAL
     * or MINUS and the parent group depth matches the corresponding stack, wraps in the dedicated AST node and adds to parent;
     * otherwise adds the group to parent or sets as top-level WHERE.
     * Must not be called while a TriplesBlock is still open (no pending enterBgp without exitBgp).
     */
    public void exitGroup() {
        ensureNoOpenBgp();
        int depthBeforePop = groupStack.size();

        if (!selectWhereGroupDepths.isEmpty()
                && hasCurrentSelect()
                && depthBeforePop == selectWhereGroupDepths.peek()) {
            selectWhereGroupDepths.pop();
            List<PatternAst> popped = groupStack.pop();
            getCurrentSelectFrame().whereClause = new GroupGraphPatternAst(popped);
            return;
        }

        List<PatternAst> popped = groupStack.pop();
        GroupGraphPatternAst group = new GroupGraphPatternAst(popped);

        if (!optionalGroupDepths.isEmpty() && groupStack.size() == optionalGroupDepths.peek()) {
            optionalGroupDepths.pop();
            currentGroup().add(new OptionalAst(group));
        } else if (!minusGroupDepths.isEmpty() && groupStack.size() == minusGroupDepths.peek()) {
            minusGroupDepths.pop();
            currentGroup().add(new MinusAst(group));
        } else if (!existsGroupDepths.isEmpty() && groupStack.size() == existsGroupDepths.peek()) {
            existsGroupDepths.pop();
            capturedExistsStack.push(group);
        } else if (!serviceStack.isEmpty() && groupStack.size() == serviceStack.peek().groupDepth()) {
            ServiceEntry entry = serviceStack.pop();
            currentGroup().add(new ServiceAst(entry.endpoint(), entry.silent(), group));
        } else if (groupStack.isEmpty()) {
            if (hasCurrentSelect()) getCurrentSelectFrame().whereClause = group;
            else whereClause = group;
        } else {
            currentGroup().add(group);
        }
    }

    /**
     * Check current select stack
     */
    protected boolean hasCurrentSelect() {
        return !selectStack.isEmpty();
    }

    /**
     * Peek current select stack
     */
    protected SelectFrame getCurrentSelectFrame() {
        if (selectStack.isEmpty()) {
            throw new IllegalStateException("No Current SELECT frame");
        }
        return selectStack.peek();
    }

    /**
     * Add a triple pattern to the current BGP (TriplesBlock).
     * This must be called while inside a TriplesBlock.
     */
    public void addTriple(TermAst s, PathAst p, TermAst o) {
        if (bgpStack.isEmpty()) {
            throw new IllegalStateException("addTriple() called outside of TriplesBlock (BGP). " +
                    "Ensure you call enterBgp() on enterTriplesBlock.");
        }
        bgpStack.peek().add(new TriplePatternAst(s, p, o));
    }

    /**
     * Add a triple whose predicate is a single term (IRI, variable, etc.).
     */
    public void addTriple(TermAst s, TermAst p, TermAst o) {
        addTriple(s, new PredicatePathAst(p), o);
    }

    // --- Filters ---

    /**
     * Exits a Filter and adds it to the current group
     */
    public void addFilter(FilterAst filter) {
        if (this.hasCurrentGroup()) {
            this.currentGroup().add(filter);
        }
    }

    /**
     * Adds {@code BIND(expression AS ?var)} to the current group. The variable must not already be
     * visible from earlier patterns in the same group (SPARQL 1.1 scoping).
     */
    public void addBind(BindAst bind) {
        if (bind == null) {
            throw new IllegalArgumentException("bind is null");
        }
        if (!hasCurrentGroup()) {
            throw new IllegalStateException("addBind() called outside of a group graph pattern");
        }
        List<PatternAst> group = currentGroup();
        Set<String> visibleSoFar = new LinkedHashSet<>();
        for (PatternAst prior : group) {
            visibleSoFar.addAll(
                    variableScopeAnalyzer.collectVisibleVariables(new GroupGraphPatternAst(List.of(prior))));
        }
        String name = bind.variable().name();
        if (visibleSoFar.contains(name)) {
            throw new QueryValidationException(
                    "Variable ?" + name + " used in BIND is already declared in the same group graph pattern");
        }
        group.add(bind);
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
     * Exit OPTIONAL scope. No-op: the optional content was already wrapped in {@link OptionalAst} in {@link SparqlQueryAstBuilder#exitGroup()}.
     */
    public void exitOptional() {
    }

    // --- Minus ---

    /**
     * Enter MINUS scope. Records current group stack size so that when we exitGroup() and the stack
     * is back to that size, we wrap the popped group in {@link MinusAst}.
     */
    public void enterMinus() {
        minusGroupDepths.push(groupStack.size());
    }

    /**
     * Exit MINUS scope. No-op: the MINUS content was already wrapped in {@link MinusAst} in {@link SparqlQueryAstBuilder#exitGroup()}.
     */
    public void exitMinus() {
        // No-op: the MINUS content was already wrapped in MinusAst in exitGroup().
    }

    // --- Service ---

    /**
     * Enter SERVICE scope.  Records the current group stack size, the endpoint term and the
     * {@code SILENT} flag so that when {@link SparqlQueryAstBuilder#exitGroup()} is called and the stack returns to
     * that size, the closed group is wrapped in a
     * {@link ServiceAst}.
     *
     * @param endpoint the remote endpoint (IRI or variable)
     * @param silent   {@code true} when the {@code SILENT} keyword was present
     */
    public void enterService(TermAst endpoint, boolean silent) {
        serviceStack.push(new ServiceEntry(groupStack.size(), endpoint, silent));
    }

    /**
     * Exit SERVICE scope.  No-op: the service body was already wrapped in
     * {@link ServiceAst} in {@link SparqlQueryAstBuilder#exitGroup()}.
     */
    public void exitService() {
    }


    /**
     * Enter EXISTS scope. Records current group stack size so that when we exitGroup() and the stack
     * is back to that size.
     */
    public void enterExistsFunc() {
        existsGroupDepths.push(groupStack.size());
    }

    /**
     * Enter NOT EXISTS scope. Same mechanism as EXISTS.
     */
    public void enterNotExistsFunc() {
        existsGroupDepths.push(groupStack.size());
    }

    /**
     * Pops the captured GroupGraphPattern from the last closed EXISTS/NOT EXISTS block.
     * Called by {@link #termFromBuiltInCall} after the listener has closed the group.
     */
    public GroupGraphPatternAst popCapturedExistsPattern() {
        return capturedExistsStack.pollFirst();
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
        if(raw.equals("a")) return new IriAst("<" + RDF.type.getIRI().stringValue() + ">");
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
            case ASTConstants.FUNCTION_CALL.UCASE -> {
                return new UcaseAst(args);
            }
            case ASTConstants.FUNCTION_CALL.LCASE -> {
                return new LcaseAst(args);
            }
            case ASTConstants.FUNCTION_CALL.LANG -> {
                return new LangAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRDT -> {
                return new StrDtAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRLANG -> {
                return new StrLangAst(args);
            }
            case ASTConstants.FUNCTION_CALL.DATATYPE -> {
                return new DatatypeAst(args);
            }
            case ASTConstants.FUNCTION_CALL.IRI -> {
                return new IriFunctionAst(args);
            }
            case ASTConstants.FUNCTION_CALL.BNODE -> {
                return new BnodeAst(args);
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
            case ASTConstants.FUNCTION_CALL.CONTAINS -> {
                return new ContainsAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRSTARTS -> {
                return new StrStartsAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRENDS -> {
                return new StrEndsAst(args);
            }
            case ASTConstants.FUNCTION_CALL.SUBSTR -> {
                return new SubstrAst(args);
            }
            case ASTConstants.FUNCTION_CALL.CONCAT -> {
                return new ConcatAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRBEFORE -> {
                return new StrBeforeAst(args);
            }
            case ASTConstants.FUNCTION_CALL.STRAFTER -> {
                return new StrAfterAst(args);
            }
            case ASTConstants.FUNCTION_CALL.REPLACE -> {
                return new ReplaceAst(args);
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

    public AggregateAst createAggregate(
            AggregateFunction function, boolean distinct, TermAst expression, String groupConcatSeparator) {
        return new AggregateAst(function, distinct, expression, groupConcatSeparator);
    }

    // ---- term helpers ----

    public TermAst termFromVerb(SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return this.iri("a");
        return termFromVarOrIriRef(ctx.varOrIri());
    }

    public TermAst termFromVarOrTerm(SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return termFromVar(ctx.var_());
        return termFromGraphTerm(ctx.graphTerm());
    }

    public TermAst termFromVarOrIriRef(SparqlParser.VarOrIriContext ctx) {
        if (ctx.var_() != null) {
            return termFromVar(ctx.var_());
        }
        return termFromIriRef(ctx.iriRef());
    }

    public TermAst termFromGraphTerm(SparqlParser.GraphTermContext ctx) {
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

    public TermAst termFromGraphRef(SparqlParser.GraphRefContext ctx) {
        if (ctx.iriRef() != null) {
            return termFromIriRef(ctx.iriRef());
        } else {
            throw new QueryEvaluationException("Expecting an IRIRef in the Graph clause");
        }
    }

    public List<TermAst> termListFromObjectList(SparqlParser.ObjectListContext ctx) {
        List<TermAst> out = new ArrayList<>();
        for (var obj : ctx.object_()) {
            out.add(termFromObject(obj));
        }
        return out;
    }

    public TermAst termFromObject(SparqlParser.Object_Context ctx) {
        // object_ : graphNode
        return termFromGraphNode(ctx.graphNode());
    }

    public TermAst termFromGraphNode(SparqlParser.GraphNodeContext ctx) {
        if (ctx.varOrTerm() != null) return termFromVarOrTerm(ctx.varOrTerm());
        if (ctx.triplesNode() != null) {
            // MVP: pas encore supporté ( [ ... ] ou ( ... ) )
            return this.iri(ctx.triplesNode().getText());
        }

        return this.iri(ctx.getText());
    }

    public TermAst termFromRdfLiteral(SparqlParser.RdfLiteralContext ctx) {
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

    public TermAst termFromPrimary(SparqlParser.PrimaryExpressionContext ctx) {
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

    public TermAst termFromVar(SparqlParser.Var_Context ctx) {
        return this.var(ctx.getText());
    }

    public TermAst termFromBooleanLiteral(SparqlParser.BooleanLiteralContext ctx) {
        if (ctx.FALSE() != null) {
            return new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        } else if (ctx.TRUE() != null) {
            return new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected value for boolean literal");
        }
    }

    public TermAst termFromIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx) {
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

    public List<TermAst> termListFromArgList(SparqlParser.ArgListContext ctx) {
        return ctx.expression().stream().map(this::termFromExpression).toList();
    }

    public TermAst termFromIriRef(SparqlParser.IriRefContext ctx) {
        return this.iri(ctx.getText());
    }

    public TermAst termFromNumericLiteral(SparqlParser.NumericLiteralContext ctx) {
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

    public TermAst termFromNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx) {
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

    public TermAst termFromNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx) {
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

    public TermAst termFromNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx) {
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

    public TermAst termFromBlankNode(SparqlParser.BlankNodeContext ctx) {
        return this.iri(ctx.getText());
    }

    /**
     * One {@code groupCondition}&nbsp;:&nbsp;{@code builtInCall | functionCall | '(' expression ('AS' var_)? ')' | var_}.
     * <p>La forme {@code (expr AS ?v)} est résolue comme l’expression de regroupement (le nom optionnel rejoint la
     * spec en étant porté par la projection SELECT&nbsp;; l’AST minimal ne le duplique pas ici).
     */
    public TermAst termFromGroupCondition(SparqlParser.GroupConditionContext ctx) {
        if (ctx.var_() != null) {
            return termFromVar(ctx.var_());
        }
        if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        }
        if (ctx.functionCall() != null) {
            SparqlParser.FunctionCallContext fc = ctx.functionCall();
            IriAst functionName = new IriAst(fc.iriRef().getText());
            if (fc.argList() == null) {
                return createFunCall(functionName, List.of());
            }
            if (fc.argList().NIL() != null) {
                return createFunCall(functionName, List.of());
            }
            List<TermAst> args = fc.argList().expression().stream().map(this::termFromExpression).toList();
            return createFunCall(functionName, args);
        }
        if (ctx.expression() != null) {
            return termFromExpression(ctx.expression());
        }
        throw new QueryEvaluationException("Unsupported group condition: " + ctx.getText());
    }

    public TermAst termFromConstraint(SparqlParser.ConstraintContext ctx) {
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

    public TermAst termFromBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        if (ctx.aggregate() != null) {
            return termFromAggregate(ctx.aggregate());
        }
        if (ctx.existsFunc() != null) {
            GroupGraphPatternAst existsPattern = popCapturedExistsPattern();
            if (existsPattern == null) {
                throw new QueryEvaluationException("EXISTS { ... } inner pattern was not captured; check listener order");
            }
            return new ExistsAst(existsPattern);
        }
        if (ctx.notExistsFunc() != null) {
            GroupGraphPatternAst notExistsPattern = popCapturedExistsPattern();
            if (notExistsPattern == null) {
                throw new QueryEvaluationException("NOT EXISTS { ... } inner pattern was not captured; check listener order");
            }
            return new NotExistsAst(notExistsPattern);
        }
        if (ctx.regexExpression() != null) {
            return termFromRegex(ctx.regexExpression());
        } else if (ctx.strReplaceExpression() != null) {
            return termFromReplace(ctx.strReplaceExpression());
        } else if (ctx.BOUND() != null) {
            return this.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(this.var(ctx.var_().getText())));
        } else if (ctx.BNODE() != null) {
            List<TermAst> args = ctx.expression() == null
                    ? List.of()
                    : ctx.expression().stream().map(this::termFromExpression).toList();
            return this.createConstraint(ASTConstants.FUNCTION_CALL.BNODE, args);
        } else if (ctx.IF() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new IfAst(args.get(0), args.get(1), args.get(2));
        } else if (ctx.RAND() != null) {
            return new RandAst();
        } else if (ctx.UUID() != null) {
            return new UuidAst();
        } else if (ctx.STRUUID() != null) {
            return new StrUuidAst();
        } else if (ctx.CONCAT() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return this.createConstraint(ASTConstants.FUNCTION_CALL.CONCAT, args);
        } else if (ctx.COALESCE() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new CoalesceAst(args);
        } else if (ctx.subStringExpression() != null) {
            List<TermAst> args = ctx.subStringExpression().expression().stream().map(this::termFromExpression).toList();
            return this.createConstraint(ASTConstants.FUNCTION_CALL.SUBSTR, args);
        } else if (ctx.NOW() != null) {
            return new NowAst();
        } else if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            if (ctx.STR() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
            } else if (ctx.UCASE() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.UCASE, args);
            } else if (ctx.LCASE() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.LCASE, args);
            } else if (ctx.ENCODE_FOR_URI() != null) {
                return new EncodeForUriAst(args);
            } else if (ctx.LANG() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
            } else if (ctx.LANGMATCHES() != null) {
                return new LangMatchesAst(args);
            } else if (ctx.CONTAINS() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.CONTAINS, args);
            } else if (ctx.STRSTARTS() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRSTARTS, args);
            } else if (ctx.STRENDS() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRENDS, args);
            } else if (ctx.STRDT() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRDT, args);
            } else if (ctx.STRLANG() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRLANG, args);
            } else if (ctx.STRBEFORE() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRBEFORE, args);
            } else if (ctx.STRAFTER() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.STRAFTER, args);
            } else if (ctx.YEAR() != null) {
                return new YearAst(args);
            } else if (ctx.MONTH() != null) {
                return new MonthAst(args);
            } else if (ctx.DAY() != null) {
                return new DayAst(args);
            } else if (ctx.HOURS() != null) {
                return new HoursAst(args);
            } else if (ctx.MINUTES() != null) {
                return new MinutesAst(args);
            } else if (ctx.SECONDS() != null) {
                return new SecondsAst(args);
            } else if (ctx.TIMEZONE() != null) {
                return new TimezoneAst(args);
            } else if (ctx.TZ() != null) {
                return new TzAst(args);
            } else if (ctx.DATATYPE() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
            } else if (ctx.IRI() != null || ctx.URI() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IRI, args);
            } else if (ctx.SAME_TERM() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
            } else if (ctx.IS_URI() != null || ctx.IS_IRI() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, args);
            } else if (ctx.IS_BLANK() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, args);
            } else if (ctx.IS_LITERAL() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, args);
            } else if (ctx.MD5() != null) {
                return new Md5Ast(args);
            } else if (ctx.SHA1() != null) {
                return new Sha1Ast(args);
            } else if (ctx.SHA256() != null) {
                return new Sha256Ast(args);
            } else if (ctx.SHA384() != null) {
                return new Sha384Ast(args);
            } else if (ctx.SHA512() != null) {
                return new Sha512Ast(args);
            } else if (ctx.ABS() != null) {
                return new AbsAst(args);
            } else if (ctx.CEIL() != null) {
                return new CeilAst(args);
            } else if (ctx.FLOOR() != null) {
                return new FloorAst(args);
            } else if (ctx.ROUND() != null) {
                return new RoundAst(args);
            } else if (ctx.BOUND() != null) {
                return this.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(this.var(ctx.var_().getText())));
            } else if (ctx.regexExpression() != null) {
                return termFromRegex(ctx.regexExpression());
            } else if (ctx.STRLEN() != null) {
                return new StrLenAst(args.getFirst());
            } else {
                throw new QueryEvaluationException("Unexpected function for a  BuiltInCall for token " + ctx.getText());
            }
        } else {
            throw new QueryEvaluationException("Unable to resolve BuiltInCall for token " + ctx.getText());
        }
    }

    public TermAst termFromExpression(SparqlParser.ExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            return this.termFromConditionalOr(ctx.conditionalOrExpression());
        } else {
            throw new QueryEvaluationException("No conditional OR found");
        }
    }

    public TermAst termFromConditionalOr(SparqlParser.ConditionalOrExpressionContext ctx) {
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

    public TermAst termFromConditionalAnd(SparqlParser.ConditionalAndExpressionContext ctx) {
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

    public TermAst termFromValueLogical(SparqlParser.ValueLogicalContext ctx) {
        if (ctx.relationalExpression() != null) {
            return this.termFromRelational(ctx.relationalExpression());
        } else {
            throw new QueryEvaluationException("No relational termFromExpression found");
        }
    }

    public TermAst termFromRelational(SparqlParser.RelationalExpressionContext ctx) {
        if (ctx.numericExpression() == null || ctx.numericExpression().isEmpty()) {
            throw new QueryEvaluationException("No numeric termFromExpression found");
        }
        TermAst lhs = termFromNumeric(ctx.numericExpression().getFirst());

        if (ctx.IN() != null) {
            List<TermAst> candidates = termFromExpressionList(ctx.expressionList());
            if (ctx.NOT() != null) {
                return new NotInAst(lhs, candidates);
            }
            return new InAst(lhs, candidates);
        }

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
        }

        return lhs;
    }

    /**
     * Right-hand side of {@code IN} / {@code NOT IN}: {@code NIL} ({@code ()}) or parenthesized expression list.
     */
    public List<TermAst> termFromExpressionList(SparqlParser.ExpressionListContext ctx) {
        if (ctx == null) {
            throw new QueryEvaluationException("expressionList missing for IN / NOT IN");
        }
        if (ctx.NIL() != null) {
            return List.of();
        }
        if (ctx.expression() != null && !ctx.expression().isEmpty()) {
            return ctx.expression().stream().map(this::termFromExpression).toList();
        }
        return List.of();
    }

    public TermAst termFromNumeric(SparqlParser.NumericExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            return this.termFromAdditive(ctx.additiveExpression());
        } else {
            throw new QueryEvaluationException("No additive termFromExpression found");
        }
    }

    public TermAst termFromAdditive(SparqlParser.AdditiveExpressionContext ctx) {
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
                            case SparqlParser.MultiplicativeExpressionContext multiplicativeExpressionContext ->
                                    termFromMultiplicative(multiplicativeExpressionContext);
                            case SparqlParser.NumericLiteralPositiveContext numericLiteralPositiveContext ->
                                    (ExprAst) termFromNumericLiteralPositive(numericLiteralPositiveContext);
                            case SparqlParser.NumericLiteralNegativeContext numericLiteralNegativeContext ->
                                    (ExprAst) termFromNumericLiteralNegative(numericLiteralNegativeContext);
                            case null, default ->
                                    throw new QueryEvaluationException(
                                            "Unexpected left hand termFromExpression in additive termFromExpression "
                                                    + ctx.getText()
                                                    + " "
                                                    + (child != null ? child.getText() : "null")
                                    );
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

    public TermAst termFromMultiplicative(SparqlParser.MultiplicativeExpressionContext ctx) {
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

    public TermAst termFromUnary(SparqlParser.UnaryExpressionContext ctx) {
        ASTConstants.Constraint op = null;
        if (ctx.PLUS() != null) {
            op = ASTConstants.OPERATOR.PLUS;
        } else if (ctx.MINUS_SIGN() != null) {
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

    public TermAst termFromBrackettedExpression(SparqlParser.BrackettedExpressionContext ctx) {
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

    public TermAst termFromReplace(SparqlParser.StrReplaceExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return this.createConstraint(ASTConstants.FUNCTION_CALL.REPLACE, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REPLACE call");
        }
    }

    /**
     * Predicate as a SPARQL 1.1 property path.
     */
    public PathAst pathFromVerbPath(SparqlParser.VerbPathContext ctx) {
        return pathFromPath(ctx.path());
    }

    public PathAst pathFromPath(SparqlParser.PathContext ctx) {
        return pathFromPathAlternative(ctx.pathAlternative());
    }

    private PathAst pathFromPathAlternative(SparqlParser.PathAlternativeContext ctx) {
        return foldAlternatives(ctx.pathSequence().stream().map(this::pathFromPathSequence).toList());
    }

    private PathAst pathFromPathSequence(SparqlParser.PathSequenceContext ctx) {
        return foldSequences(ctx.pathEltOrInverse().stream().map(this::pathFromPathEltOrInverse).toList());
    }

    private PathAst pathFromPathEltOrInverse(SparqlParser.PathEltOrInverseContext ctx) {
        if (ctx.CARET() != null) {
            return new InversePathAst(pathFromPathElt(ctx.pathElt()));
        }
        return pathFromPathElt(ctx.pathElt());
    }

    private PathAst pathFromPathElt(SparqlParser.PathEltContext ctx) {
        PathAst primary = pathFromPathPrimary(ctx.pathPrimary());
        SparqlParser.PathModContext mod = ctx.pathMod();
        if (mod == null) {
            return primary;
        }
        if (mod.QUESTION() != null) {
            return new OptionalPathAst(primary);
        }
        if (mod.STAR() != null) {
            return new ZeroOrMorePathAst(primary);
        }
        if (mod.PLUS() != null) {
            return new OneOrMorePathAst(primary);
        }
        throw new QueryEvaluationException("Unexpected path modifier in " + ctx.getText());
    }

    private PathAst pathFromPathPrimary(SparqlParser.PathPrimaryContext ctx) {
        if (ctx.iriRef() != null) {
            return new PredicatePathAst(termFromIriRef(ctx.iriRef()));
        }
        if (ctx.A() != null) {
            return new PredicatePathAst(iri("a"));
        }
        if (ctx.EXCLAMATION() != null) {
            return pathFromPathNegatedPropertySet(ctx.pathNegatedPropertySet());
        }
        if (ctx.path() != null) {
            return pathFromPath(ctx.path());
        }
        throw new QueryEvaluationException("Unexpected path primary in " + ctx.getText());
    }

    private PathAst pathFromPathNegatedPropertySet(SparqlParser.PathNegatedPropertySetContext ctx) {
        List<PathAst> excluded;
        if (ctx.L_PAREN() != null) {
            excluded = ctx.pathOneInPropertySet().stream().map(this::pathFromPathOneInPropertySet).toList();
        } else {
            excluded = List.of(pathFromPathOneInPropertySet(ctx.pathOneInPropertySet().getFirst()));
        }
        return new NegatedPropertySetPathAst(excluded);
    }

    private PathAst pathFromPathOneInPropertySet(SparqlParser.PathOneInPropertySetContext ctx) {
        if (ctx.CARET() != null) {
            if (ctx.iriRef() != null) {
                return new InversePathAst(new PredicatePathAst(termFromIriRef(ctx.iriRef())));
            }
            return new InversePathAst(new PredicatePathAst(iri("a")));
        }
        if (ctx.iriRef() != null) {
            return new PredicatePathAst(termFromIriRef(ctx.iriRef()));
        }
        return new PredicatePathAst(iri("a"));
    }

    private PathAst foldAlternatives(List<PathAst> parts) {
        if (parts.isEmpty()) {
            throw new QueryEvaluationException("Empty property path alternative");
        }
        PathAst result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            result = new AlternativePathAst(result, parts.get(i));
        }
        return result;
    }

    private PathAst foldSequences(List<PathAst> parts) {
        if (parts.isEmpty()) {
            throw new QueryEvaluationException("Empty property path sequence");
        }
        PathAst result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            result = new SequencePathAst(result, parts.get(i));
        }
        return result;
    }

    /**
     * Predicate as a simple variable (e.g. ?p used as a predicate).
     */
    public TermAst termFromVerbSimple(SparqlParser.VerbSimpleContext ctx) {
        return termFromVar(ctx.var_());
    }

    /**
     * List of objects inside a property path triple.
     * Blank node property lists and collections are not yet fully supported (raw text fallback).
     */
    public List<TermAst> termListFromObjectListPath(SparqlParser.ObjectListPathContext ctx) {
        List<TermAst> out = new ArrayList<>();
        for (var objPath : ctx.objectPath()) {
            var graphNodePath = objPath.graphNodePath();
            if (graphNodePath.varOrTerm() != null) {
                out.add(termFromVarOrTerm(graphNodePath.varOrTerm()));
            } else {
                out.add(this.iri(graphNodePath.getText()));
            }
        }
        return out;
    }

    public TermAst termFromAggregate(SparqlParser.AggregateContext ctx) {
        boolean distinct = ctx.DISTINCT() != null;

        if (ctx.COUNT() != null) {
            TermAst expression = null;

            if (ctx.STAR() == null && ctx.expression() != null && !ctx.expression().isEmpty()) {
                expression = termFromExpression(ctx.expression());
            }
            return createAggregate(AggregateFunction.COUNT, distinct, expression, null);
        }

        if (ctx.SUM() != null) {
            return createAggregate(AggregateFunction.SUM, distinct, termFromExpression(ctx.expression()), null);
        }

        if (ctx.AVG() != null) {
            return createAggregate(AggregateFunction.AVG, distinct, termFromExpression(ctx.expression()), null);
        }

        if (ctx.MIN() != null) {
            return createAggregate(AggregateFunction.MIN, distinct, termFromExpression(ctx.expression()), null);
        }

        if (ctx.MAX() != null) {
            return createAggregate(AggregateFunction.MAX, distinct, termFromExpression(ctx.expression()), null);
        }

        if (ctx.SAMPLE() != null) {
            return createAggregate(AggregateFunction.SAMPLE, distinct, termFromExpression(ctx.expression()), null);
        }

        if (ctx.GROUP_CONCAT() != null) {
            String sep = ctx.string_() != null ? ctx.string_().getText() : null;
            return createAggregate(
                    AggregateFunction.GROUP_CONCAT, distinct, termFromExpression(ctx.expression()), sep);
        }

        throw new QueryEvaluationException("Unsupported aggregate: " + ctx.getText());
    }

    public GraphRefAst graphRefFromGraphOrDefault(SparqlParser.GraphOrDefaultContext ctx) {
        if (ctx.DEFAULT() != null) {
            return GraphRefAsts.defaultGraph();
        }
        if(ctx.iriRef() != null) {
            IriAst graphIri = (IriAst) termFromIriRef(ctx.iriRef());
            return GraphRefAsts.graph(graphIri);
        }
        throw new QueryEvaluationException("Unexpected value for Graph reference or default " + ctx.getText());
    }

    public GraphRefAst graphRefFromGraphRef(SparqlParser.GraphRefContext ctx) {
        if(ctx.iriRef() != null) {
            IriAst graphIri = (IriAst) termFromIriRef(ctx.iriRef());
            return GraphRefAsts.graph(graphIri);
        }
        throw new QueryEvaluationException("Unexpected value for Graph reference " + ctx.getText());
    }

    public GraphRefAst graphRefFromGraphRefAll(SparqlParser.GraphRefAllContext ctx) {
        if (ctx.DEFAULT() != null) {
            return GraphRefAsts.defaultGraph();
        }
        if(ctx.NAMED() != null) {
            return GraphRefAsts.named();
        }
        if(ctx.ALL() != null) {
            return GraphRefAsts.all();
        }
        if(ctx.graphRef() != null) {
            return graphRefFromGraphRef(ctx.graphRef());
        }
        throw new QueryEvaluationException("Unexpected value for Graph reference or default or named or all " + ctx.getText());
    }

    /**
     * Per-SELECT frame used to support nested SELECT subqueries.
     */
    protected static final class SelectFrame {
        protected GroupGraphPatternAst whereClause;
        protected ProjectionAst projection = ProjectionAsts.selectAll();
        protected boolean distinct;
        protected boolean reduced;
        protected Long limit;
        protected Long offset;
        protected GroupByAst groupBy = new GroupByAst(List.of());
        protected final List<OrderConditionAst> orderConditions = new ArrayList<>();
        protected final List<TermAst> havingConditions = new ArrayList<>();
    }
}
