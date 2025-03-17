package fr.inria.corese.core.edge;

import fr.inria.corese.core.GraphObject;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.core.PointerType;
import fr.inria.corese.core.kgram.api.core.TripleStore;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.AccessRight;

import java.util.ArrayList;
import java.util.Objects;

import static fr.inria.corese.core.kgram.api.core.PointerType.TRIPLE;

/**
 * @author Olivier Corby, Wimmics Inria I3S, 2014
 */
public abstract class EdgeTop extends GraphObject implements Edge {
    public static final String NL = "\n";
    private AccessRight.AccessRights level = AccessRight.DEFAULT;
    private boolean nested = false;
    // created by values, bind or triple()
    private boolean created = false;

    public static Edge create(Node source, Node subject, Node predicate, Node objet) {
        return null;
    }

    public Edge copy() {
        return create(getGraph(), getNode(0), getEdgeNode(), getNode(1));
    }

    // manage access right
    @Override
    public AccessRight.AccessRights getLevel() {
        return level;
    }

    @Override
    public Edge setLevel(AccessRight.AccessRights b) {
        level = b;
        return this;
    }

    @Override
    public String getDatatypeLabel() {
        return toString();
    }

    @Override
    public Node getEdgeNode() {
        return null;
    }

    public void setEdgeNode(Node pred) {
    }

    @Override
    public Node getProperty() {
        return getEdgeNode();
    }

    @Override
    public void setProperty(Node pred) {
        setEdgeNode(pred);
    }

    public void setTag(Node node) {
    }

    @Override
    public void setGraph(Node node) {
    }

    @Override
    public Object getProvenance() {
        return null;
    }

    @Override
    public void setProvenance(Object o) {
    }

    public void replicate(Edge cur) {
    }

    public void duplicate(Edge cur) {
    }

    @Override
    public Iterable<IDatatype> getLoop() {
        return getNodeList();
    }

    ArrayList<IDatatype> getNodeList() {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(getValue(null, i));
        }
        return list;
    }

    @Override
    public IDatatype getValue(String varString, int n) {
        if (n == 0) {
            return nodeValue(getNode(0));
        } else if (n == 1) {
            return nodeValue(getEdgeNode());
        } else if (n == 2) {
            return nodeValue(getNode(1));
        } else if (n == 3) {
            return nodeValue(getGraph());
        }
        return null;
    }

    IDatatype nodeValue(Node n) {
        return n.getDatatypeValue();
    }

    @Override
    public PointerType pointerType() {
        return TRIPLE;
    }

    @Override
    public Edge getEdge() {
        return this;
    }

    @Override
    public Node getNode() {
        return DatatypeMap.createObject(this.toString(), this);
    }

    @Override
    public boolean contains(Node node) {
        return getNode(0).same(node) || getNode(1).same(node);
    }

    @Override
    public int nbGraphNode() {
        return 2;
    }

    @Override
    public int nbNode() {
        return 2;
    }

    @Override
    public String getEdgeLabel() {
        return getEdgeNode().getLabel();
    }

    @Override
    public Node getEdgeVariable() {
        return null;
    }

    @Override
    public TripleStore getTripleStore() {
        return getNode(0).getTripleStore();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSubjectNode(), this.getPropertyNode(), this.getObjectNode(), this.getGraphNode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge t = (Edge) o;
        return Objects.equals(getSubjectNode(), t.getSubjectNode())
                && Objects.equals(getPropertyNode(), t.getPropertyNode())
                && Objects.equals(getObjectNode(), t.getObjectNode())
                && Objects.equals(getGraphNode(), t.getGraphNode());
    }

    @Override
    public String toString() {
        return toRDFString();
    }

    public String toRDFString() {
        return String.format("%s %s %s %s", getGraphValue(), getSubjectValue(), getPredicateValue(), getObjectValue());
    }

    @Override
    public boolean isNested() {
        return nested;
    }

    @Override
    public void setNested(boolean nested) {
        this.nested = nested;
    }

    @Override
    public boolean isCreated() {
        return created;
    }

    @Override
    public void setCreated(boolean created) {
        this.created = created;
    }
}
