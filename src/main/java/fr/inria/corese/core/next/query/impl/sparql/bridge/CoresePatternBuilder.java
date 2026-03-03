package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;

/**
 * Builds KGRAM structures from AST pattern nodes.
 * <p>
 * This class is part of the SPARQL → KGRAM bridge layer. It converts
 * SPARQL AST pattern elements (e.g. {@link BgpAst}, {@link TriplePatternAst})
 * into executable KGRAM query model objects used by the Corese next engine.
 * </p>
 *
 * <h3>Current Scope</h3>
 * <p>
 * At this stage, only <b>Basic Graph Patterns (BGP)</b> are supported.
 * </p>
 * <ul>
 *   <li>{@link TriplePatternAst} → {@link Edge} (via {@link AstEdge})</li>
 *   <li>{@link BgpAst} → {@link Exp} of type {@link ExpType.Type#BGP}</li>
 * </ul>
 *
 * <p>
 * Other SPARQL constructs such as OPTIONAL, UNION, FILTER, GRAPH, etc.
 * are not yet handled and will result in an {@link IllegalArgumentException}
 * if encountered via {@link #toExp(PatternAst)}.
 * </p>
 *
 * <p>
 * Term conversion ({@link TermAst} → {@link Node}) is delegated to
 * {@link CoreseTermAdapter}.
 * </p>
 *
 */
public final class CoresePatternBuilder {

    /**
     * Default constructor.
     *
     * <p>This builder is stateless and can be reused.</p>
     */
    public CoresePatternBuilder() {
    }

    /**
     * Converts a SPARQL AST term into a KGRAM {@link Node}.
     *
     * <p>
     * Delegates to {@link CoreseTermAdapter} to handle variables,
     * IRIs and literals.
     * </p>
     *
     * @param term SPARQL AST term (variable, IRI or literal)
     * @return corresponding KGRAM node
     */
    private static Node toNode(TermAst term) {
        return CoreseTermAdapter.toNode(term);
    }

    /**
     * Converts a {@link TriplePatternAst} into a KGRAM {@link Edge}.
     *
     * <p>
     * The subject, predicate and object AST terms are individually
     * converted into KGRAM {@link Node} instances before constructing
     * an {@link AstEdge}.
     * </p>
     *
     * @param triple triple pattern AST (must not be null)
     * @return KGRAM edge representing the triple pattern
     */
    public Edge toEdge(TriplePatternAst triple) {
        Node sub = toNode(triple.subject());
        Node pred = toNode(triple.predicate());
        Node obj = toNode(triple.object());
        return AstEdge.create(sub, pred, obj);
    }

    /**
     * Converts a {@link BgpAst} into a KGRAM {@link Exp}
     * of type {@link ExpType.Type#BGP}.
     *
     * <p>
     * Each triple pattern contained in the BGP is converted
     * into an {@link Edge} and added to the resulting expression.
     * </p>
     *
     * @param bgp basic graph pattern AST
     * @return KGRAM BGP expression containing all triple edges
     */
    public Exp toBgpExp(BgpAst bgp) {
        Exp exp = Exp.create(ExpType.Type.BGP);
        for (TriplePatternAst t : bgp.triples()) {
            exp.add(toEdge(t));
        }
        return exp;
    }

    /**
     * Converts a generic {@link PatternAst} into a KGRAM {@link Exp}.
     *
     * <p>
     * Currently, only {@link BgpAst} is supported.
     * Future extensions may add support for OPTIONAL, UNION,
     * FILTER and other SPARQL pattern types.
     * </p>
     *
     * @param pattern pattern AST node
     * @return corresponding KGRAM expression
     * @throws IllegalArgumentException if the pattern type is not supported
     */
    public Exp toExp(PatternAst pattern) {
        if (pattern instanceof BgpAst bgp) {
            return toBgpExp(bgp);
        }
        throw new IllegalArgumentException("Unsupported pattern type: " + pattern.getClass().getName());
    }
}