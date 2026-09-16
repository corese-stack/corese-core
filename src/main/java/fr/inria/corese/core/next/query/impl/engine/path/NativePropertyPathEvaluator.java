package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates SPARQL 1.1 paths in the active graph through the producer SPI.
 * Sequences and alternatives preserve bags. Repetition uses an iterative ALP
 * visited set, shared by all first steps of a plus, to terminate on cycles.
 * All adjacency and traversal state belongs to a single evaluation.
 */
public final class NativePropertyPathEvaluator {
    private final Map<Node, List<Edge>> outgoing = new LinkedHashMap<>();
    private final Map<Node, List<Edge>> incoming = new LinkedHashMap<>();
    private final Set<Node> nodes = new LinkedHashSet<>();

    private NativePropertyPathEvaluator(Iterable<Edge> edges) {
        for (Edge edge : edges) {
            Node subject = edge.getNode(0);
            Node object = edge.getNode(1);
            outgoing.computeIfAbsent(subject, key -> new ArrayList<>()).add(edge);
            incoming.computeIfAbsent(object, key -> new ArrayList<>()).add(edge);
            nodes.add(subject);
            nodes.add(object);
        }
    }

    public static Iterable<Edge> evaluate(Producer producer, Node graph, List<Node> from,
            PropertyPathEdge pattern, Environment environment) {
        NativePropertyPathEvaluator evaluator = new NativePropertyPathEvaluator(
                producer.getEdges(graph, from, new ScanEdge(), environment));
        return evaluator.matches(pattern, resolve(graph, environment), environment);
    }

    private List<Edge> matches(PropertyPathEdge pattern, Node graph, Environment environment) {
        Node subject = resolve(pattern.subject(), environment);
        Node object = resolve(pattern.object(), environment);
        boolean reverse = isReverse(pattern, subject, object);
        Node fixed = reverse ? object : subject;
        Iterable<Node> starts = starts(pattern, fixed);
        List<Edge> matches = new ArrayList<>();
        for (Node start : starts) {
            for (Node end : walk(pattern.path(), start, reverse)) {
                Node source = reverse ? end : start;
                Node target = reverse ? start : end;
                if (compatible(object, target) && compatible(subject, source)) {
                    matches.add(new PropertyPathEdge(source, pattern.path(), target, graph));
                }
            }
        }
        return matches;
    }

    private static boolean isReverse(PropertyPathEdge pattern, Node subject, Node object) {
        return pattern.object().isConstant() && !pattern.subject().isConstant()
                || subject == null && object != null;
    }

    private Iterable<Node> starts(PropertyPathEdge pattern, Node fixed) {
        if (fixed == null) {
            return nodes;
        }
        // A joined binding is not a syntactic constant: variable-variable paths
        // range over nodes(G), even when VALUES binds a term absent from G.
        if (pattern.subject().isVariable() && pattern.object().isVariable() && !nodes.contains(fixed)) {
            return List.of();
        }
        return List.of(fixed);
    }

    private static boolean compatible(Node expected, Node actual) {
        return expected == null || expected.same(actual);
    }

    private static Node resolve(Node node, Environment environment) {
        if (node == null || node.isConstant()) {
            return node;
        }
        return environment.getNode(node);
    }

    private List<Node> walk(PropertyPath path, Node start, boolean reverse) {
        return switch (path) {
            case PropertyPath.Predicate(Node predicate) -> step(start, reverse, predicate, List.of());
            case PropertyPath.Inverse(PropertyPath operand) -> walk(operand, start, !reverse);
            case PropertyPath.Sequence p -> sequence(p, start, reverse);
            case PropertyPath.Alternative p -> alternative(p, start, reverse);
            case PropertyPath.Repetition p -> repetition(p, start, reverse);
            case PropertyPath.Negated p -> negated(p, start, reverse);
        };
    }

    private List<Node> sequence(PropertyPath.Sequence path, Node start, boolean reverse) {
        PropertyPath first = reverse ? path.right() : path.left();
        PropertyPath second = reverse ? path.left() : path.right();
        List<Node> result = new ArrayList<>();
        for (Node intermediate : walk(first, start, reverse)) {
            result.addAll(walk(second, intermediate, reverse));
        }
        return result;
    }

    private List<Node> alternative(PropertyPath.Alternative path, Node start, boolean reverse) {
        List<Node> result = new ArrayList<>(walk(path.left(), start, reverse));
        result.addAll(walk(path.right(), start, reverse));
        return result;
    }

    private List<Node> repetition(PropertyPath.Repetition path, Node start, boolean reverse) {
        Set<Node> visited = new LinkedHashSet<>();
        if (path.includeZero()) {
            visited.add(start);
        }
        Deque<Node> pending = new ArrayDeque<>(walk(path.operand(), start, reverse));
        while (!pending.isEmpty()) {
            Node next = pending.removeFirst();
            if (visited.add(next) && path.recursive()) {
                pending.addAll(walk(path.operand(), next, reverse));
            }
        }
        return new ArrayList<>(visited);
    }

    private List<Node> negated(PropertyPath.Negated path, Node start, boolean reverse) {
        List<Node> result = new ArrayList<>();
        if (!path.forward().isEmpty() || path.backward().isEmpty()) {
            result.addAll(step(start, reverse, null, path.forward()));
        }
        if (!path.backward().isEmpty()) {
            result.addAll(step(start, !reverse, null, path.backward()));
        }
        return result;
    }

    private List<Node> step(Node start, boolean reverse, Node predicate, List<Node> excluded) {
        Map<Node, List<Edge>> adjacency = reverse ? incoming : outgoing;
        List<Node> result = new ArrayList<>();
        for (Edge edge : adjacency.getOrDefault(start, List.of())) {
            Node property = edge.getEdgeNode();
            if (compatible(predicate, property) && excluded.stream().noneMatch(property::same)) {
                result.add(edge.getNode(reverse ? 0 : 1));
            }
        }
        return result;
    }

    /** Null components request an unrestricted triple scan of the active graph. */
    private record ScanEdge() implements Edge {
        @Override
        public Node getNode(int index) {
            return null;
        }

        @Override
        public Node getProperty() {
            return null;
        }

        @Override
        public String getEdgeLabel() {
            return "";
        }

        @Override
        public Node getGraph() {
            return null;
        }

        @Override
        public Edge getEdge() {
            return this;
        }
    }
}
