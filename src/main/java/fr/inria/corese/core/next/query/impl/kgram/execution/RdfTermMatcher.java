package fr.inria.corese.core.next.query.impl.kgram.execution;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Matcher;

/**
 * Matcher for RDF term comparisons during KGRAM graph pattern execution.
 *
 * <p>This implementation follows the strict RDF-term matching needed by SPARQL
 * graph pattern evaluation: constants must match the candidate RDF term,
 * unbound variables can bind to any candidate term, and already-bound variables
 * must keep the same value.</p>
 *
 * <p>It intentionally does not perform entailment, type subsumption, or
 * {@code owl:sameAs} expansion. Those semantics should be added explicitly,
 * either by extending this matcher or by introducing a dedicated matcher.</p>
 */
public final class RdfTermMatcher implements Matcher {

    private int mode = Matcher.UNDEF;

    @Override
    public boolean match(Edge query, Edge target, Environment environment) {
        return match(query.getNode(0), target.getNode(0), environment)
                && match(query.getNode(1), target.getNode(1), environment)
                && match(predicateNode(query), target.getEdgeNode(), environment);
    }

    @Override
    public boolean match(Node query, Node target, Environment environment) {
        // Null is an absent KGRAM node, not a wildcard. Wildcards are represented by unbound variables.
        if (query == null || target == null) {
            return query == target;
        }
        if (query.isVariable()) {
            Node bound = environment == null ? null : environment.getNode(query);
            return bound == null || bound.match(target);
        }
        return query.match(target);
    }

    @Override
    public boolean same(Node queryNode, Node left, Node right, Environment environment) {
        // queryNode is unused for strict RDF-term equality, but richer matchers may need it.
        return left != null && left.same(right);
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int mode) {
        this.mode = mode;
    }

    private static Node predicateNode(Edge edge) {
        return edge.getEdgeVariable() == null ? edge.getEdgeNode() : edge.getEdgeVariable();
    }
}
