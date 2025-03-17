package fr.inria.corese.core.edge;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 * Buffer Edge as Quad to deliver complete edge for Producer
 *
 * @author Olivier Corby, Wimmics, INRIA I3S, 2014
 */
public class EdgeGeneric extends EdgeTop implements Edge {
    static int pcount = 0;
    protected Node graph, predicate, subject, object;
    int index = -1;
    private Object prov;

    public EdgeGeneric() {
    }

    public EdgeGeneric(Node pred) {
        predicate = pred;
    }

    EdgeGeneric(Node g, Node p) {
        graph = g;
        predicate = p;
    }

    EdgeGeneric(Node g, Node pred, Node subject, Node object) {
        this(g, pred);
        this.subject = subject;
        this.object = object;
    }

    EdgeGeneric(Node g, Node pred, Node subject, Node object, Node arg1) {
        this(g, pred, subject, object);
    }

    EdgeGeneric(Node g, Node p, Node[] args) {
        this(g, p);
    }

    public static EdgeGeneric create(Node g, Node subject, Node pred, Node object) {
        return new EdgeGeneric(g, pred, subject, object);
    }

    @Override
    public void replicate(Edge cur) {
        setNode(0, cur.getNode(0));
        setNode(1, cur.getNode(1));
        setEdgeIndex(cur.getEdgeIndex());
        setLevel(cur.getLevel());
        setProvenance(cur.getProvenance());
    }

    @Override
    public void duplicate(Edge cur) {
        setEdgeNode(cur.getEdgeNode());
        setGraph(cur.getGraph());
        replicate(cur);
    }

    public void add(Node node) {

    }

    @Override
    public void setTag(Node node) {
    }

    public String toParse() {
        StringBuilder sb = new StringBuilder();
        sb.append("tuple");
        sb.append("(");
        sb.append(getEdgeNode());
        sb.append(subject);
        sb.append(" ");
        sb.append(object);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Node getEdgeNode() {
        return predicate;
    }

    @Override
    public void setEdgeNode(Node node) {
        predicate = node;
    }

    @Override
    public int getEdgeIndex() {
        return index;
    }

    @Override
    public void setEdgeIndex(int n) {
        index = n;
    }

    @Override
    public String getEdgeLabel() {
        return getEdgeNode().getLabel();
    }

    @Override
    public Node getNode(int n) {
        if (n == Graph.IGRAPH) {
            return getGraph();
        } else if (n == 0) {
            return subject;
        } else if (n == 1) {
            return object;
        }
        return null;
    }

    @Override
    public void setNode(int i, Node n) {
        if (i == 0) {
            subject = n;
        } else if (i == 1) {
            object = n;
        }
    }

    @Override
    public int nbNode() {
        return 2;
    }

    @Override
    public int nbGraphNode() {
        return 2;
    }

    @Override
    public Edge getEdge() {
        return this;
    }

    @Override
    public Node getGraph() {
        return graph;
    }

    @Override
    public void setGraph(Node gNode) {
        graph = gNode;
    }

    @Override
    public Node getNode() {
        return DatatypeMap.createObject(this.toString(), this);
    }

    @Override
    public Node getEdgeVariable() {
        return null;
    }

    @Override
    public Object getProvenance() {
        if (prov != null && !(prov instanceof Node)) {
            prov = DatatypeMap.createObject("p" + pcount++, prov);
        }
        return prov;
    }

    @Override
    public void setProvenance(Object obj) {
        prov = obj;
    }
}
