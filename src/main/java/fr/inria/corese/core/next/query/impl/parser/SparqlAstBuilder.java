package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    /** Parser options (e.g. for future use: strict mode, base IRI). */
    private final SparqlParserOptions options;

    /**
     * Stack of UNION branch lists currently being collected.
     */
    private final Deque<List<GroupGraphPatternAst>> unionStack = new ArrayDeque<>();

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
            case CONSTRUCT, DESCRIBE -> null; // not yet implemented
            case SELECT -> new SelectQueryAst(projection, whereClause, buildSolutionModifier());
            case UNDEFINED -> throw new QueryEvaluationException("Could not determine the type of query during parsing");
        };
    }

    // --- Term helpers (triple pattern building) ---

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

    // --- Internal helpers ---

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
        return new SolutionModifierAst(distinct, reduced, List.of(), null, null);
    }
}