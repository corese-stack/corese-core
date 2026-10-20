package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Stable greedy ordering of contiguous ordinary triple patterns. Every other
 * expression is a semantic barrier, including BIND, VALUES, paths, and graph
 * operators. Only planning allocates scratch space; statement traversal is untouched.
 */
final class CardinalityPatternOrdering {

    private CardinalityPatternOrdering() {
    }

    /**
     * Chooses the smallest known pattern first, then favors connected joins.
     * Unknown or equal counts fall back to binding connectivity and source order.
     * Each pattern is counted once, never from a sorting comparator.
     *
     * @param query query supplying the active dataset
     * @param graph enclosing graph, or {@code null} for the default graph
     * @param expression expression group to order in place
     * @param variables variables available from the enclosing scope
     * @param bindings constant-binding hints collected by the planner
     * @param statistics producer's session-visible statistics
     */
    static void sort(Query query, Node graph, Exp expression, List<String> variables,
            List<Exp> bindings, QueryStatistics statistics) {
        flattenTripleGroups(expression);
        List<Node> bound = new ArrayList<>();
        int start = 0;
        while (start < expression.size()) {
            if (!isTriple(expression.get(start))) {
                recordBindings(expression.get(start++), bound);
                continue;
            }
            int end = start + 1;
            while (end < expression.size() && isTriple(expression.get(end))) {
                end++;
            }
            if (end - start > 1) {
                long[] counts = cardinalities(query, graph, expression, start, end, statistics);
                orderRun(expression, start, end, counts, bound, variables, bindings);
            } else {
                recordBindings(expression.get(start), bound);
            }
            start = end;
        }
    }

    // The parser may emit one BGP per dotted triple. Adjacent pure triple groups
    // are associative joins; explicit groups remain JOIN operators and are retained.
    private static void flattenTripleGroups(Exp expression) {
        int i = 0;
        while (i < expression.size()) {
            Exp child = expression.get(i);
            if (isTripleGroup(child)) {
                expression.remove(child);
                for (int j = 0; j < child.size(); j++) {
                    expression.add(i + j, child.get(j));
                }
                i += child.size();
            } else {
                i++;
            }
        }
    }

    private static boolean isTripleGroup(Exp expression) {
        if (!expression.isBGP() || expression.size() == 0) {
            return false;
        }
        for (Exp child : expression) {
            if (!isTriple(child)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTriple(Exp expression) {
        return expression.isEdge() && !(expression.getEdge() instanceof PropertyPathEdge);
    }

    private static long[] cardinalities(Query query, Node graph, Exp expression, int start, int end,
            QueryStatistics statistics) {
        long[] counts = new long[end - start];
        for (int i = start; i < end; i++) {
            counts[i - start] = statistics.estimateCardinality(query, graph, expression.get(i).getEdge());
        }
        return counts;
    }

    private static void orderRun(Exp expression, int start, int end, long[] counts,
            List<Node> bound, List<String> variables, List<Exp> bindings) {
        int first = start;
        int seed = SelectiveFilterPushdown.seed(expression, start, end);
        if (seed >= 0) {
            moveFirst(expression, counts, start, first, seed);
            recordBindings(expression.get(first), bound);
            first++;
        }
        for (int i = first; i < end; i++) {
            int best = i;
            for (int j = i + 1; j < end; j++) {
                if (before(expression.get(j), expression.get(best), counts[j - start], counts[best - start],
                        bound, variables, bindings)) {
                    best = j;
                }
            }
            moveFirst(expression, counts, start, i, best);
            recordBindings(expression.get(i), bound);
        }
    }

    private static void recordBindings(Exp expression, List<Node> bound) {
        expression.bind(bound);
        if (expression.isEdge()) {
            Node predicate = expression.getEdge().getEdgeVariable();
            if (predicate != null && !bound.contains(predicate)) {
                bound.add(predicate);
            }
        }
    }

    private static boolean before(Exp candidate, Exp current, long candidateCount, long currentCount,
            List<Node> bound, List<String> variables, List<Exp> bindings) {
        boolean candidateConnected = connected(candidate.getEdge(), bound, variables);
        boolean currentConnected = connected(current.getEdge(), bound, variables);
        if (candidateConnected != currentConnected) {
            return candidateConnected;
        }
        if (candidateCount >= 0 && currentCount >= 0 && candidateCount != currentCount) {
            return candidateCount < currentCount;
        }
        return candidate.nBind(bound, variables, bindings) > current.nBind(bound, variables, bindings);
    }

    private static boolean connected(Edge edge, List<Node> bound, List<String> variables) {
        return bound(edge.getNode(0), bound, variables)
                || bound(edge.getNode(1), bound, variables)
                || bound(edge.getEdgeVariable(), bound, variables);
    }

    private static boolean bound(Node node, List<Node> bound, List<String> variables) {
        return node != null && node.isVariable()
                && (bound.contains(node) || variables.contains(node.getLabel()));
    }

    private static void moveFirst(Exp expression, long[] counts, int start, int position, int best) {
        Exp selected = expression.get(best);
        long count = counts[best - start];
        for (int i = best; i > position; i--) {
            expression.set(i, expression.get(i - 1));
            counts[i - start] = counts[i - start - 1];
        }
        expression.set(position, selected);
        counts[position - start] = count;
    }
}
