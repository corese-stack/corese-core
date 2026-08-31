package fr.inria.corese.core.next.query.impl.kgram.tool;

import fr.inria.corese.core.next.query.impl.kgram.path.Path;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.core.TripleStore;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;

public class NodeImpl implements Node {



    Atom atom;
    int index = -1;

    public NodeImpl(Atom at) {
        atom = at;
    }

    /** Creates a constant node for an IRI. */
    public static NodeImpl forIRI(String iri) {
        return new NodeImpl(Constant.create(DatatypeMap.newResource(iri)));
    }

    /** Creates a constant node for a blank node. */
    public static NodeImpl forBlank(String id) {
        return new NodeImpl(Constant.create(DatatypeMap.createBlank(id)));
    }

    /**
     * Creates a constant node for a literal.
     *
     * @param label       lexical value
     * @param datatypeUri datatype IRI, or {@code null}
     * @param lang        language tag, or {@code null}
     */
    public static NodeImpl forLiteral(String label, String datatypeUri, String lang) {
        return new NodeImpl(Constant.create(DatatypeMap.createLiteral(label, datatypeUri, lang)));
    }

    /** Creates a variable node with the given name. */
    public static NodeImpl forVariable(String name) {
        return new NodeImpl(new Variable(name));
    }

    /** Creates a constant node wrapping the given {@link IDatatype} value. */
    public static NodeImpl forDatatype(IDatatype dt) {
        return new NodeImpl(Constant.create(dt));
    }

    @Override
    public IDatatype getValue() {
        return atom.getDatatypeValue();
    }

    public IDatatype getValue(Node n) {
        return  n.getValue();
    }

    @Override
    public IDatatype getDatatypeValue() {
        return atom.getDatatypeValue();
    }

    @Override
    public void setDatatypeValue(IDatatype dt) {
        atom = Constant.create(dt);
    }

    @Override
    public Node getGraph() {
        return null;
    }

    @Override
    public Node getNode() {
        return this;
    }

    @Override
    public String toString() {
        return atom.toSparql(); // + "[" + getIndex() +"]";
    }

    @Override
    public int compare(Node node) {
        if (node.getValue() != null) {
            return getValue().compareTo(getValue(node));
        }
        return getLabel().compareTo(node.getLabel());
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getLabel() {
        if (atom.isResource()) {
            return atom.getLongName();
        }
        return atom.getName();
    }

    @Override
    public boolean isConstant() {
        return atom.isConstant();
    }

    @Override
    public boolean isVariable() {
        return atom.isVariable();
    }

    // Constant bnode or sparql variable as bnode
    @Override
    public boolean isBlank() {
        return atom.isBlankOrBlankNode();
    }

    @Override
    public boolean isFuture() {
        return isConstant() && getDatatypeValue().isFuture();
    }

    @Override
    public boolean same(Node n) {
        if (isVariable() || n.isVariable()) {
            return sameVariable(n);
        }
        return getValue().sameTerm(getValue(n));
    }

    boolean sameVariable(Node n) {
        return isVariable() && n.isVariable() && getLabel().equals(n.getLabel());
    }

    @Override
    public boolean match(Node n) {
        if (isVariable() || n.isVariable()) {
            return sameVariable(n);
        }
        return getValue().match(getValue(n));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            return equals((Node) o); // was same
        }
        return false;
    }

    public boolean equals(Node n) {
        if (isVariable() || n.isVariable()) {
            return sameVariable(n);
        }
        return getValue().equals(getValue(n));
    }

    @Override
    public void setIndex(int n) {
        index = n;
    }

    @Override
    public Object getNodeObject() {
        return null;
    }

     @Override
    public Edge getEdge() {
        return (Edge) getDatatypeValue().getEdge();
    }

    @Override
    public void setObject(Object o) {
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getKey() {
        return INITKEY;
    }

    @Override
    public void setKey(String str) {
    }

    @Override
    public TripleStore getTripleStore() {
        return null;
    }


}
