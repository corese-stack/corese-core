package fr.inria.corese.core.next.query.impl.engine.model;

public class EdgeInv implements Edge {

    private final Edge edge;
    private final Edge ent;

    public EdgeInv(Edge e) {
        this.ent = e;
        this.edge = e;
    }

    @Override
    public String toString() {
        return "inverse(" + edge + ")";
    }

    @Override
    public boolean contains(Node n) {
        return edge.contains(n);
    }

    @Override
    public int nbNode() {
        return edge.nbNode();
    }

    @Override
    public int nbGraphNode() {
        return edge.nbGraphNode();
    }

    @Override
    public Edge getEdge() {
        return edge;
    }

    @Override
    public Node getGraph() {
        return ent.getGraph();
    }

    public Edge getEdgeEntity() {
        return ent;
    }

    @Override
    public Node getEdgeNode() {
        return edge.getEdgeNode();
    }

    @Override
    public int getEdgeIndex() {
        return edge.getEdgeIndex();
    }

    @Override
    public String getEdgeLabel() {
        return edge.getEdgeLabel();
    }

    @Override
    public Node getNode(int n) {
        return switch (n) {
            case 0 -> edge.getNode(1);
            case 1 -> edge.getNode(0);
            default -> edge.getNode(n);
        };
    }

    @Override
    public Node getEdgeVariable() {
        return edge.getEdgeVariable();
    }

    @Override
    public Node getProperty() {
        return edge.getProperty();
    }

    public Node getNode() {
        return null;
    }

    @Override
    public Object getProvenance() {
        return ent.getProvenance();
    }

    @Override
    public void setProvenance(Object obj) {
        ent.setProvenance(obj);
    }
}
