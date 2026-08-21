package fr.inria.corese.core.next.query.impl.kgram.sorter.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1.QPGNodeCostModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.QuerySorterConst.*;

/**
 * The node for triple pattern graph, which encapsualtes an expression (contain
 * an object exp) with cost
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class QPGNode {

    // the expression that the node encapsulates
    private final Exp exp;
    private final ExpType.Type type;
    private final QPGNodeCostModel costModel;
    private double cost = -1;
    //the nested QPG in a QPG node, ex, GRAPH
    //and for future extionson, ex, UNION
    //private QPGraph nested = null;

    public QPGNode(Exp exp, List<Exp> bindings) {
        this.exp = exp;
        this.type = exp.type();

        this.costModel = new QPGNodeCostModel(this, bindings);
    }

    public QPGNodeCostModel getCostModel() {
        return costModel;
    }

    public Exp getExp() {
        return this.exp;
    }

    public ExpType.Type getType() {
        return this.type;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * Get the Kgram Node in the triple pattern (EDGE) according to the type
     * subject, predicate, or object
     *
     * @param i type
     */
    public Node getExpNode(int i) {
        if (this.type != ExpType.Type.EDGE) {
            return null;
        }

        return switch (i) {
            case SUBJECT -> this.exp.getEdge().getNode(0);
            case PREDICATE -> {
                Edge e = this.exp.getEdge();
                yield (e.getEdgeVariable() == null ? e.getEdgeNode() : e.getEdgeVariable());
            }
            case OBJECT -> this.exp.getEdge().getNode(1);
            default -> null;
        };
    }

    /**
     * Check if two QPG node share same variables
     *
     * @param n BP node
     * @return true: shared; false: not share
     */
    public boolean isShared(QPGNode n) {
        return !this.shared(n).isEmpty();
    }

    public List<String> shared(QPGNode n) {
        return this.shared(this, n);
    }

    public List<String> shared(QPGNode bpn1, QPGNode bpn2) {
        ExpType.Type type1 = bpn1.exp.type();
        ExpType.Type type2 = bpn2.exp.type();

        switch (type1) {
            case EDGE:
                switch (type2) {
                    case EDGE:
                        return this.isShared(bpn1.exp.getEdge(), bpn2.exp.getEdge());
                    case GRAPH:
                        return this.isShared(bpn2.exp, bpn1.exp.getEdge());
                    case FILTER, BIND:
                        return this.isShared(bpn2.exp.getFilter(), bpn1.exp.getEdge());
                    case VALUES:
                        return this.isShared(bpn2.exp.getNodeList(), bpn1.exp.getEdge());
                    default:
                }
                break;
            case GRAPH:
                switch (type2) {
                    case EDGE:
                        return this.isShared(bpn1.exp, bpn2.exp.getEdge());
                    case GRAPH:
                        return this.isShared(bpn1.exp, bpn2.exp);
                    case FILTER:
                        return this.isShared(bpn2.exp.getFilter(), bpn1.exp);
                    case VALUES:
                        return this.isShared(bpn2.exp.getNodeList(), bpn1.exp);
                    case BIND:
                        if (bpn2.exp.hasNodeList()){
                             return this.isShared(bpn2.exp.getNodeList(), bpn1.exp);
                        }
                        else {
                            return this.isShared(bpn2.exp.getNode(), bpn1.exp);
                        }
                    default:
                }
            case BIND:
                switch (type2) {
                    case FILTER:
                        return this.isShared(bpn2.exp.getFilter(), bpn1.exp.getNode());
                    case VALUES:
                        return this.isShared(bpn2.exp.getNodeList(), bpn1.exp.getNode());//td
                    case BIND:
                        if (bpn2.exp.hasNodeList()){
                            return this.compare(bpn2.exp.getNodeList(), bpn1.exp.getNode());
                        }
                        else {
                            return this.compare(bpn2.exp.getNode(), bpn1.exp.getNode());
                        }
                    default:
                }
            default:
                break;
        }

        return new ArrayList<>();
    }

    //check between edge and values
    public List<String> isShared(List<Node> values, Edge e) {
        return this.compare(values, getVariablesInEdge(e));
    }

    //check between edge and filter
    public List<String> isShared(Filter f, Edge e) {
        return this.compareString(f.getVariables(), getVariablesInEdge(e));
    }

    //check between two edges
    public List<String> isShared(Edge e1, Edge e2) {
        return this.compare(getVariablesInEdge(e1), getVariablesInEdge(e2));
    }

    //check between edge and graph
    public List<String> isShared(Exp graph, Edge e) {
        return this.compare(getVariablesInEdge(e), getVariablesInGraph(graph));
    }

    //check between two graphs
    public List<String> isShared(Exp g1, Exp g2) {
        return this.compare(getVariablesInGraph(g1), getVariablesInGraph(g2));
    }

    //check between graph and values
    public List<String> isShared(List<Node> values, Exp graph) {
        return this.compare(values, getVariablesInGraph(graph));
    }

    //check between graph and filter
    public List<String> isShared(Filter f, Exp graph) {
        return this.compareString(f.getVariables(), getVariablesInGraph(graph));
    }

    //check between bind and filter
    public List<String> isShared(Filter f, Node n) {
        return this.compareString(f.getVariables(), n);
    }

    //check between bind and values
    public List<String> isShared(List<Node> values, Node n) {
        return this.compare(values, n);
    }

    //check between graph and filter
    public List<String> isShared(Node n, Exp graph) {
        return this.compare(getVariablesInGraph(graph), n);
    }

    private List<Node> getVariablesInEdge(Edge e) {
        return Stream.of(
                        e.getNode(0),
                        e.getEdgeVariable(),
                        e.getNode(1)
                )
                .filter(Objects::nonNull)
                .filter(Node::isVariable)
                .distinct()
                .collect(Collectors.toList());
    }

    //GRAPH{GRAPHNODE{NODE{data:aliceFoaf } } AND{EDGE{?alice foaf:mbox <mailto:alice@work.example>} ...}}
    private List<Node> getVariablesInGraph(Exp graph) {
        Set<Node> uniqueVariables = new LinkedHashSet<>();

        for (Exp outerExp : graph) {
            for (Exp innerExp : outerExp) {
                if (innerExp.type() == ExpType.Type.NODE) {
                    Node node = innerExp.getNode();
                    if (node != null && node.isVariable()) {
                        uniqueVariables.add(node);
                    }
                } else if (innerExp.type() == ExpType.Type.EDGE) {
                    Edge edge = innerExp.getEdge();
                    if (edge != null) {
                        uniqueVariables.addAll(getVariablesInEdge(edge));
                    }
                }
            }
        }

        return new ArrayList<>(uniqueVariables);
    }
    //compare between a list of strings and a list of nodes
    private List<String> compareString(List<String> l1, List<Node> l2) {
        List<String> l = new ArrayList<>();
        for (String n1 : l1) {
            for (Node n2 : l2) {
                if (n1.equalsIgnoreCase(n2.getLabel())) {
                    l.add(n1);
                    break;
                }
            }
        }

        return l;
    }

    //compare between a list of strings and a list of nodes
    private List<String> compareString(List<String> l1, Node n) {
        List<Node> l2 = new ArrayList<>();
        l2.add(n);
        return compareString(l1, l2);
    }

    //compare between two lists of nodes
    private List<String> compare(List<Node> l1, List<Node> l2) {
        List<String> l = new ArrayList<>();
        for (Node n1 : l1) {
            for (Node n2 : l2) {
                if (n1.same(n2)) {
                    l.add(n1.getLabel());
                    break;
                }
            }
        }

        return l;
    }

    //compare between a single node and a list of nodes
    private List<String> compare(List<Node> l2, Node n) {
        List<Node> l1 = new ArrayList<>();
        l1.add(n);
        return this.compare(l1, l2);
    }

    private List<String> compare(Node n1, Node n2) {
        List<String> l = new ArrayList<>();
        if (n1.same(n2)) {
            l.add(n1.getLabel());
        }
        return l;
    }

    @Override
    public String toString() {
        return exp.toString() + "," + this.cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QPGNode)) {
            return false;
        } else {
            return this.exp.equals(((QPGNode) obj).exp);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.exp != null ? this.exp.hashCode() : 0);
        return hash;
    }
}
