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
 *
 * Compatible with the common SPARQL grammar shape:
 *
 * GroupGraphPattern
 *   : '{'
 *       ( TriplesBlock?
 *         ( GraphPatternNotTriples '.'? TriplesBlock? )*
 *       )?
 *     '}'
 *
 * This builder expects the listener to call:
 * - enterGroup()/exitGroup() on enter/exitGroupGraphPattern
 * - enterBgp()/exitBgp() on enter/exitTriplesBlock
 * - addTriple(s,p,o) whenever a triple pattern is recognized (usually on exitTriplesSameSubject)
 * - enterAskQuery at the start of the declaration of an ASK query
 */
public final class SparqlAstBuilder {

    private ASTConstants.QUERY_TYPE queryType = ASTConstants.QUERY_TYPE.UNDEFINED;

    // --- Internal stacks (scopes) ---

    /** Stack of groups; each group is a list of patterns (BgpAst now, later OptionalAst/UnionAst/...) */
    private final Deque<List<PatternAst>> groupStack = new ArrayDeque<>();

    /** Stack of current BGP triples (TriplesBlock). Nested blocks are rare but stack keeps it safe. */
    private final Deque<List<TriplePatternAst>> bgpStack = new ArrayDeque<>();

    /** Top-level WHERE clause, set when the root group is closed in exitGroup(). */
    private GroupGraphPatternAst whereClause;

    /** Parser options (e.g. for future use: strict mode, base IRI). */
    private final SparqlParserOptions options;

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

    /** Enter a { ... } groupGraphPattern. */
    public void enterGroup() {
        groupStack.push(new ArrayList<>());
    }

    /**
     * Exit a { ... } groupGraphPattern.
     * Pops the current group, wraps it in {@link GroupGraphPatternAst}; if there is a parent group,
     * adds it there; otherwise stores it as the top-level WHERE clause for {@link #getResult()}.
     * Must not be called while a TriplesBlock is still open (no pending enterBgp without exitBgp).
     */
    public void exitGroup() {
        ensureNoOpenBgp("exitGroup() called while a TriplesBlock/BGP is still open");
        List<PatternAst> popped = groupStack.pop();
        GroupGraphPatternAst group = new GroupGraphPatternAst(popped);
        if (groupStack.isEmpty()) {
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
            case CONSTRUCT -> null; // not yet implemented
            case DESCRIBE -> null; // not yet implemented
            case SELECT -> new SelectQueryAst(whereClause);
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

    private void ensureNoOpenBgp(String message) {
        if (!bgpStack.isEmpty()) {
            throw new IllegalStateException(message + " (open bgpStack depth=" + bgpStack.size() + ")");
        }
    }
}
