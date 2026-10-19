package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a plannable expression group as nodes connected by shared variables.
 * The mutable graph and its cost estimates belong to one planning operation.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class QueryPlanGraph {

    // list of nodes
    private final List<QueryPlanNode> nodes;
    // list of edges
    private final List<QueryPlanEdge> edges;
    // identified bound values
    private final List<Exp> bindings;

    // Adjacency index from each node to its incident edges.
    private Map<QueryPlanNode, List<QueryPlanEdge>> graph = null;

    public QueryPlanGraph(List<Exp> exps, List<Exp> bindings) {
        this.bindings = bindings;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();

        createNodes(exps);
        createEdges();
    }

    // Preserve expression order when creating graph nodes.
    private void createNodes(List<Exp> exps) {
        for (Exp ee : exps) {
            nodes.add(new QueryPlanNode(ee, this.bindings));
        }
    }

    //Create graph structure by finding variables sharing between nodes
    private void createEdges() {
        // Retain the existing directed edge order for deterministic traversal.
        graph = new HashMap<>();
        for (QueryPlanNode bpn1 : nodes) {
            for (QueryPlanNode bpn2 : nodes) {
                if (bpn1.equals(bpn2)) continue;

                //make sure not repeated
                if (bpn1.isShared(bpn2)) {
                    QueryPlanEdge edge = new QueryPlanEdge(bpn1, bpn2);
                    edges.add(edge);
                    createIndex(edge);
                }
            }
        }
    }

    private void createIndex(QueryPlanEdge edge) {
        QueryPlanNode n1 = edge.get(0);
        QueryPlanNode n2 = edge.get(1);

        //Node 1
        if (graph.containsKey(n1)) {
            graph.get(n1).add(edge);
        } else {
            List<QueryPlanEdge> ledges = new ArrayList<>();
            ledges.add(edge);
            graph.put(n1, ledges);
        }

        //Node 2
        if (graph.containsKey(n2)) {
            graph.get(n2).add(edge);
        } else {
            List<QueryPlanEdge> ledges = new ArrayList<>();
            ledges.add(edge);
            graph.put(n2, ledges);
        }
    }

    /**
     * Return all the edges with certain type contained in the graph
     *
     * @param edgeType type of edge: SIMPLE | BI_DIRECT
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanEdge> getEdges(int edgeType) {
        List<QueryPlanEdge> lEdges = new ArrayList<>();
        for (QueryPlanEdge e : this.edges) {
            if (e.getType() == edgeType) {
                lEdges.add(e);
            }
        }

        return lEdges;
    }

    /**
     * Get all edges linked to a given node
     *
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanEdge> getEdges(QueryPlanNode node, int edgeType) {
        List<QueryPlanEdge> lEdges = new ArrayList<>();
        if (this.graph.get(node) == null) {
            return lEdges;
        }

        for (QueryPlanEdge e : this.graph.get(node)) {
            if (e.getType() == edgeType) {
                lEdges.add(e);
            }
        }

        return lEdges;
    }

    /**
     * Return the edges linked to the given node
     *
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanEdge> getEdges(QueryPlanNode node) {
        return this.graph.get(node);
    }

    public QueryPlanEdge getEdge(QueryPlanNode n1, QueryPlanNode n2) {
        for (QueryPlanEdge e : this.graph.get(n1)) {
            if (e.get(n1).equals(n2)) {
                return e;
            }
        }

        return null;
    }

    /**
     * Return list of nodes contained in the triple pattern graph
     *
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanNode> getAllNodes() {
        return this.getAllNodes(ExpType.Type.EMPTY);
    }

    /**
     * Return list of nodes that contain triple pattern expression (edge)
     *
     * @param type EDGE, VALUES, FILTER, GRAPH, otherwise return all
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanNode> getAllNodes(ExpType.Type type) {
        if (PlanningConstants.plannable(type)) {
            List<QueryPlanNode> list = new ArrayList<>();
            for (QueryPlanNode node : this.nodes) {
                if (node.getType() == type) {
                    list.add(node);
                }
            }
            return list;
        }
        return this.nodes;
    }

    /**
     * Get nodes linked to the given node
     *
     * @param n node
     * @return list
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanNode> getLinkedNodes(QueryPlanNode n) {
        List<QueryPlanNode> l = new ArrayList<>();
        if(!graph.containsKey(n)){
            return l;
        }

        for (QueryPlanEdge e : graph.get(n)) {
            l.add(e.get(n));
        }
        return l;
    }

    /**
     * Get nodes linked to the given node
     *
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<QueryPlanNode> getLinkedNodes(QueryPlanNode n, boolean directed, boolean in) {
        if (!directed) {
            return this.getLinkedNodes(n);
        }

        List<QueryPlanNode> l = new ArrayList<>();
        if (!graph.containsKey(n)) {
            return l;
        }

        for (QueryPlanEdge e : graph.get(n)) {
            if (in) {
                if (e.get(1).equals(n)) {
                    l.add(e.get(0));
                }
            } else {
                if (e.get(0).equals(n)) {
                    l.add(e.get(1));
                }
            }

        }
        return l;
    }

}
