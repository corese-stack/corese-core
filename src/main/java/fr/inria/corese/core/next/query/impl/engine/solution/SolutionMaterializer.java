package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.List;
import java.util.Objects;

/**
 * Materializes variable bindings, projected expressions, and sorting keys
 * into query solution {@link Mapping} instances.
 */
final class SolutionMaterializer {

    private static final String SERVICE_REPORT_ZERO = "?_service_report_0";
    private static final Edge[] EMPTY_EDGES = new Edge[0];

    private final Memory memory;

    SolutionMaterializer(Memory memory) {
        this.memory = Objects.requireNonNull(memory, "memory");
    }

    Mapping materialize(Query query, Producer producer, boolean subEvaluation) {
        Node reportNode = bindServiceReport();
        int capacity = solutionNodeCapacity(query, subEvaluation);
        Edge[] queryEdgeCopies = memory.isEdge ? new Edge[memory.nbEdge] : EMPTY_EDGES;
        Edge[] targetEdgeCopies = memory.isEdge ? new Edge[memory.nbEdge] : EMPTY_EDGES;
        copySolutionEdges(producer, queryEdgeCopies, targetEdgeCopies);

        Node[] queryNodeCopies = new Node[capacity];
        Node[] targetNodeCopies = new Node[capacity];
        int boundNodes = copySolutionNodes(queryNodeCopies, targetNodeCopies);
        Node[] orderByNodes = new Node[query.getOrderBy().size()];
        Node[] groupByNodes = new Node[query.getGroupBy().size()];

        if (!subEvaluation && !projectSolution(query, producer, queryNodeCopies, targetNodeCopies, boundNodes)) {
            return null;
        }
        Mapping solution = new Mapping(queryEdgeCopies, targetEdgeCopies, queryNodeCopies, targetNodeCopies);
        memory.mapping = solution;
        if (!subEvaluation) {
            solution.init();
            // Ordering expressions may inspect the projected solution through the environment.
            orderGroup(query.getOrderBy(), orderByNodes, producer);
            orderGroup(query.getGroupBy(), groupByNodes, producer);
            popProjectionBindings(query);
        }
        if (reportNode != null) {
            memory.pop(reportNode);
            memory.setReport(null);
        }
        solution.setOrderBy(orderByNodes);
        solution.setGroupBy(groupByNodes);
        memory.clear();
        memory.mapping = null;
        return solution;
    }

    private Node bindServiceReport() {
        if (memory.getReport() == null) {
            return null;
        }
        Node reportNode = memory.getQuery().getSelectNode(SERVICE_REPORT_ZERO);
        if (reportNode != null) {
            memory.push(reportNode, (Node) memory.getReport());
        }
        return reportNode;
    }

    private int solutionNodeCapacity(Query query, boolean subEvaluation) {
        int capacity = memory.nbNode;
        if (!subEvaluation) {
            for (Exp projection : query.getSelectFun()) {
                if (projection.getFilter() != null && !memory.isBound(projection.getNode())) {
                    capacity++;
                }
            }
        }
        return capacity;
    }

    private void copySolutionEdges(Producer producer, Edge[] queryEdgeCopies, Edge[] targetEdgeCopies) {
        if (!memory.isEdge) {
            return;
        }
        int targetIndex = 0;
        for (int sourceIndex = 0; sourceIndex < memory.qEdges.length; sourceIndex++) {
            if (memory.qEdges[sourceIndex] != null) {
                queryEdgeCopies[targetIndex] = memory.qEdges[sourceIndex];
                targetEdgeCopies[targetIndex] = producer.copy(memory.result[sourceIndex]);
                targetIndex++;
            }
        }
    }

    private int copySolutionNodes(Node[] queryNodeCopies, Node[] targetNodeCopies) {
        int targetIndex = 0;
        for (int sourceIndex = 0; sourceIndex < memory.qNodes.length; sourceIndex++) {
            if (memory.qNodes[sourceIndex] != null) {
                queryNodeCopies[targetIndex] = memory.qNodes[sourceIndex];
                targetNodeCopies[targetIndex] = memory.nodes[sourceIndex];
                targetIndex++;
            }
        }
        return targetIndex;
    }

    private boolean projectSolution(Query query, Producer producer, Node[] queryNodeCopies,
                                    Node[] targetNodeCopies, int targetIndex) {
        int pushedBindings = 0;
        int currentIndex = targetIndex;
        for (Exp projection : query.getSelectFun()) {
            if (projection.getFilter() == null) {
                continue;
            }
            ProjectionResult result = applySingleProjection(projection, producer);
            if (!result.success()) {
                popProjectionPrefix(query, pushedBindings);
                return false;
            }
            if (result.pushed()) {
                pushedBindings++;
            }
            if (result.unboundBefore()) {
                queryNodeCopies[currentIndex] = projection.getNode();
                targetNodeCopies[currentIndex] = result.value();
                currentIndex++;
            }
        }
        return true;
    }

    private record ProjectionResult(boolean success, boolean pushed, boolean unboundBefore, Node value) {
    }

    private ProjectionResult applySingleProjection(Exp projection, Producer producer) {
        Filter filter = projection.getFilter();
        boolean previouslyBound = memory.isBound(projection.getNode());
        Node value = null;
        boolean pushed = false;
        if (!projection.isAggregate()) {
            value = evaluateExpressionNode(filter, producer);
            Eval kgram = memory.getEval();
            if (kgram != null && kgram.getVisitor() != null) {
                kgram.getVisitor().select(kgram, filter.getExp(), value == null ? null : value.getDatatypeValue());
            }
            if (!memory.push(projection.getNode(), value)) {
                return new ProjectionResult(false, false, false, null);
            }
            pushed = true;
        }
        return new ProjectionResult(true, pushed, !previouslyBound, value);
    }

    private void popProjectionPrefix(Query query, int count) {
        int popped = 0;
        for (Exp projection : query.getSelectFun()) {
            if (popped >= count) {
                return;
            }
            if (projection.getFilter() != null) {
                memory.pop(projection.getNode());
                popped++;
            }
        }
    }

    private void popProjectionBindings(Query query) {
        for (Exp projection : query.getSelectFun()) {
            if (projection.getFilter() != null && !projection.isAggregate()) {
                memory.pop(projection.getNode());
            }
        }
    }

    void orderGroup(List<Exp> lExp, Node[] nodes, Producer p) {
        int n = 0;
        for (Exp e : lExp) {
            Node qNode = e.getNode();
            if (qNode != null) {
                nodes[n] = memory.getNode(qNode);
            }
            if (nodes[n] == null) {
                Filter f = e.getFilter();
                if (f != null && !e.isAggregate()) {
                    nodes[n] = evaluateExpressionNode(f, p);
                }
            }
            if (qNode != null && nodes[n] != null && memory.mapping != null) {
                memory.mapping.setNode(qNode, nodes[n]);
            }
            n++;
        }
    }

    private Node evaluateExpressionNode(Filter filter, Producer producer) {
        try {
            Eval kgram = memory.getEval();
            if (kgram == null) {
                return null;
            }
            return producer.getNode(kgram.eval(filter, memory, producer));
        } catch (QueryTypeErrorException error) {
            // A scalar type error leaves a projected value or sorting key unbound.
            return null;
        }
    }
}
