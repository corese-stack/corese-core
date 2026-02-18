package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.sparql.ast.VarAst;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Build a minimal SPARQL AST for:
 * - Triple patterns (?s ?p ?o)
 * - Basic Graph Patterns (BGP) via TriplesBlock
 * - GroupGraphPattern as a container (can contain multiple TriplesBlock and later OPTIONAL/UNION/etc.)
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
 */
public final class SparqlAstBuilder {

    // --- Internal stacks (scopes) ---

    /** Stack of groups; each group is a list of patterns (BgpAst now, later OptionalAst/UnionAst/...) */
    private final Deque<List<PatternAst>> groupStack = new ArrayDeque<>();

    /** Stack of current BGP triples (TriplesBlock). Nested blocks are rare but stack keeps it safe. */
    private final Deque<List<TriplePatternAst>> bgpStack = new ArrayDeque<>();

    /** Final result after exitGroupGraphPattern of the top-level WHERE group. */
    private QueryAst result;

    private final SparqlParserOptions options;

    public SparqlAstBuilder(SparqlParserOptions options) {
        this.options = options;
    }

    // --- Construction entry points (called by listener) ---

    /** Enter a { ... } groupGraphPattern. */
    public void enterGroup() {
        groupStack.push(new ArrayList<>());
    }

    /**
     * Exit a { ... } groupGraphPattern.
     * It finalizes the group into a GroupGraphPatternAst.
     *
     * If this is the top-level group (stack becomes empty), we produce the final QueryAst.
     * If there is a parent group (nested group), we add it as a pattern (optional, depending on your AST design).
     */
    public void exitGroup() {
        ensureNoOpenBgp("exitGroup() called while a TriplesBlock/BGP is still open");

        List<PatternAst> patterns = groupStack.pop();
        GroupGraphPatternAst groupAst = new GroupGraphPatternAst(List.copyOf(patterns));

        if (groupStack.isEmpty()) {
            this.result = new SelectQueryAst(groupAst);
        } else {
            // If you want nested groups to be representable as patterns:
            // you can add a GroupPatternAst wrapper, or just flatten.
            // For now we flatten by adding contained patterns into parent (simple + OK for MVP).
            groupStack.peek().addAll(groupAst.patterns());
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

    /** Returns the final AST. Valid only after top-level exitGroup(). */
    public QueryAst getResult() {
        if (result == null) {
            throw new IllegalStateException("AST not finalized. Did you call exitGroup() for the top-level GroupGraphPattern?");
        }
        return result;
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
     * - "<http://...>"
     * - "foaf:Person"
     * - "a" shortcut (you may later normalize to rdf:type in the bridge/algebra step)
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
