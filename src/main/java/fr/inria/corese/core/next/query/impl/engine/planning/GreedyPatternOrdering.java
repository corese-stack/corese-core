package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;

/**
 * Orders a pattern graph using a greedy depth-first traversal. The least-cost
 * node starts each connected component, with connectivity used to break ties.
 * FILTER, VALUES, and BIND dependencies are considered as nodes become available.
 * Instances hold traversal state and are confined to one planning operation.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class GreedyPatternOrdering implements PatternOrderingStrategy {

    private final List<QueryPlanNode> visited;
    private final List<QueryPlanNode> notVisited;
    private final List<QueryPlanNode> binds;
    private QueryPlanGraph g = null;

    public GreedyPatternOrdering() {
        this.visited = new ArrayList<>();
        this.notVisited = new ArrayList<>();
        this.binds = new ArrayList<>();
    }

    @Override
    public List<QueryPlanNode> sort(QueryPlanGraph graph) {
        this.g = graph;
        //EDGE GRAPH
        List<QueryPlanNode> sortableNodes = g.getAllNodes(Type.EDGE);
        sortableNodes.addAll(g.getAllNodes(Type.GRAPH));
        notVisited.addAll(sortableNodes);
        binds.addAll(g.getAllNodes(Type.BIND));

        //each loop is a sub graph
        while (!notVisited.isEmpty()) {
            Map<QueryPlanNode, Double> nextNodesPool = new LinkedHashMap<>();
            QueryPlanNode first = findFirst(notVisited, nextNodesPool);
            route(first, nextNodesPool);
        }

        //for other left nodes, ex. filter/values/bind not related to any vairables, optional
        //just add them at the end of visited list
        for (QueryPlanNode n : g.getAllNodes()) {
            if (!visited.contains(n)) {
                visited.add(n);
            }
        }
        return visited;
    }

    //search in one connected graph, depth-first
    private void route(QueryPlanNode previous, Map<QueryPlanNode, Double> nextNodesPool) {
        if (previous == null) return;

        // while there is still node not visited in the pool
        QueryPlanNode next;
        while ((next = findNext(nextNodesPool)) != null) {
            route(next, nextNodesPool);
        }
    }

    //Find the bpg node with the smallest selectivity as starting routing point
    private QueryPlanNode findFirst(List<QueryPlanNode> lNodes, Map<QueryPlanNode, Double> pool) {
        if (lNodes == null || lNodes.isEmpty()) return null;

        QueryPlanNode minNode = lNodes.getFirst();
        for (QueryPlanNode node : lNodes) {
            double cost = node.getCost();
            if (cost == SelectivityEstimator.NA_COST) continue;
            //number of linked edges
            int es1 = this.g.getEdges(node) == null ? 0 : this.g.getEdges(node).size();
            int es2 = this.g.getEdges(minNode) == null ? 0 : this.g.getEdges(minNode).size();

            //if two nodes with same cost, on choose the one with more edges
            if ((cost < minNode.getCost())
                    || (cost == minNode.getCost() && es1 > es2)) {
                minNode = node;
            }
        }
        this.queue(minNode, pool);
        return minNode;
    }

    //find next node to be routed
    private QueryPlanNode findNext(Map<QueryPlanNode, Double> pool) {
        // 1. if no nodes left, return null
        if (pool.isEmpty()) return null;

        // 2. find the node with smallest cost
        QueryPlanNode next = null;
        double cost = Double.MAX_VALUE;
        for (Map.Entry<QueryPlanNode, Double> entry : pool.entrySet()) {
            QueryPlanNode qPGNode = entry.getKey();
            Double double1 = entry.getValue();
            if (double1 < cost) {
                cost = double1;
                next = qPGNode;
            }
        }

        //3 add to list
        this.queue(next, pool);

        return next;
    }

    private void queue(QueryPlanNode minNode, Map<QueryPlanNode, Double> pool) {
        //1.0 check VALUES, if linked to this node, put it before
        this.addValues(minNode);

        //2 add found node to list
        visited.add(minNode);
        //2.1 remove from relevant list
        notVisited.remove(minNode);
        pool.remove(minNode);

        //2.5 add binds
        addBinds(minNode);
        //3 check (and add) filter
        this.addFilters();

        //4. find adjacent nodes
        findAdjacentNodes(minNode, pool);
    }

    //for a filter, if all linked vairables have been visited,
    //then add this filter just after these triple patterns
    private void addFilters() {
        List<QueryPlanNode> filters = this.g.getAllNodes(Type.FILTER);

        for (QueryPlanNode f : filters) {
            //if there are no edges left linking to the filter (values, etc...)
            //add this filter to the sequence, just after the triples patterns
            if (!visited.contains(f) && isDisjoint(this.g.getLinkedNodes(f), notVisited, binds)) {
                visited.add(f);
            }
        }
    }

    //for VALUES, put it just before the vairables being used
    private void addValues(QueryPlanNode min) {
        List<QueryPlanNode> values = this.g.getAllNodes(Type.VALUES);

        for (QueryPlanNode v : values) {
            //if the VALUES use this (first) triple pattern, then add it to
            //visited list, before all linked nodes
            if (!visited.contains(v) && g.getLinkedNodes(v).contains(min)) {
                visited.add(v);
            }
        }
    }

    //Find the adjacent nodes linked to a given node and add them to a pool
    private void findAdjacentNodes(QueryPlanNode minNode, Map<QueryPlanNode, Double> pool) {
        //add adjacent unvisted nodes to a pool
        List<QueryPlanEdge> lEdges = this.g.getEdges(minNode, QueryPlanEdge.BI_DIRECT);

        for (QueryPlanEdge e : lEdges) {
            // get the linked node
            QueryPlanNode linkedNode = e.get(minNode);
            //get the cost of the linked node
            double cost = e.getCost() * linkedNode.getCost();

            //check the cost between the linked node and existing visited node
            for (QueryPlanNode vn : this.visited) {
                //if there is edge between vn and node
                QueryPlanEdge linkedEdge = this.g.getEdge(minNode, vn);
                if (linkedEdge != null) {
                    double cost2 = linkedEdge.getCost() * vn.getCost();
                    if (cost2 < cost) {
                        cost = cost2;
                    }
                }
            }

            if (!visited.contains(linkedNode)) {
                pool.put(linkedNode, cost);
            }
        }
    }

    //Check BIND expressions
    private void addBinds(QueryPlanNode min) {
        for (QueryPlanNode q : g.getLinkedNodes(min)) {
            List<QueryPlanNode> linkedBinds = g.getLinkedNodes(q, true, true);

            //for BIND, it also can have FILTERs and VALUES, and also it can depends on the
            //other BINDs expressions, so need to check recusively
            while (q.getType()== Type.BIND && !visited.contains(q) && isDisjoint(linkedBinds, notVisited, binds)) {
                this.addValues(q);

                visited.add(q);
                binds.remove(q);
                addBinds(q);

                this.addFilters();
            }
        }
    }

    /**
     * Check if the first list has same nodes with the other two lists of nodes
     */
    private boolean isDisjoint(List<QueryPlanNode> list, List<QueryPlanNode> notVisited, List<QueryPlanNode> binds) {
        for (QueryPlanNode q : list) {
            if (notVisited.contains(q) || binds.contains(q)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void rewrite(Exp exp, List<QueryPlanNode> nodes, int start) {
        for (int i = 0; i < nodes.size(); i++) {
            QueryPlanNode node = nodes.get(i);
            exp.set(start + i, node.getExp());
        }
    }
}
