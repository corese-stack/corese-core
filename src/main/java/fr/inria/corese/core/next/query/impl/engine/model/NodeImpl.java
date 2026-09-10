package fr.inria.corese.core.next.query.impl.engine.model;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.path.Path;

import java.util.Objects;

/** Native KGRAM node backed exclusively by Corese-next value contracts. */
public final class NodeImpl implements Node {

    private DatatypeValue value;
    private final String variableName;
    private final boolean blankVariable;
    private int index = -1;
    private String key = INITKEY;
    private Object payload;

    private NodeImpl(DatatypeValue value, String variableName) {
        this(value, variableName, false);
    }

    private NodeImpl(DatatypeValue value, String variableName, boolean blankVariable) {
        this.value = value;
        this.variableName = variableName;
        this.blankVariable = blankVariable;
    }

    /** Creates a constant node carrying a Corese-next RDF value. */
    public static NodeImpl forValue(DatatypeValue value) {
        return new NodeImpl(Objects.requireNonNull(value, "value"), null);
    }

    /** Creates a constant node for an IRI. */
    public static NodeImpl forIRI(String iri) {
        return forValue(Values.factory().createIRI(iri));
    }

    /** Creates a constant node for a blank node. */
    public static NodeImpl forBlank(String id) {
        return forValue(Values.factory().createBNode(id));
    }

    /**
     * Creates a constant node for a literal.
     *
     * @param label       lexical value
     * @param datatypeUri datatype IRI, or {@code null}
     * @param lang        language tag, or {@code null}
     */
    public static NodeImpl forLiteral(String label, String datatypeUri, String lang) {
        if (lang != null && !lang.isEmpty()) {
            return forValue(Values.factory().createLiteral(label, lang));
        }
        if (datatypeUri != null && !datatypeUri.isEmpty()) {
            return forValue(Values.factory().createLiteral(
                    label, Values.factory().createIRI(datatypeUri)));
        }
        return forValue(Values.factory().createLiteral(label));
    }

    /** Creates a variable node with the given name. */
    public static NodeImpl forVariable(String name) {
        return new NodeImpl(null, Objects.requireNonNull(name, "name"));
    }

    /** Creates an existential BGP variable, excluded from SELECT * projection. */
    public static NodeImpl forBlankVariable(String label) {
        return new NodeImpl(null, Objects.requireNonNull(label, "label"), true);
    }

    @Override
    public DatatypeValue getValue() {
        return value;
    }

    @Override
    public DatatypeValue getDatatypeValue() {
        return getValue();
    }

    @Override
    public void setDatatypeValue(DatatypeValue datatypeValue) {
        if (isVariable()) {
            throw new IllegalStateException("A variable node cannot become a constant");
        }
        value = Objects.requireNonNull(datatypeValue, "datatypeValue");
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
        if (isVariable()) {
            return variableName.startsWith("?") ? variableName : "?" + variableName;
        }
        return value.toString();
    }

    @Override
    public int compare(Node node) {
        Objects.requireNonNull(node, "node");
        if (value != null && node.getDatatypeValue() != null) {
            return value.compare(node.getDatatypeValue());
        }
        return getLabel().compareTo(node.getLabel());
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getLabel() {
        return isVariable() ? variableName : value.getLabel();
    }

    @Override
    public boolean isConstant() {
        return value != null;
    }

    @Override
    public boolean isVariable() {
        return variableName != null;
    }

    /** Returns whether this node is an RDF blank node or an existential variable. */
    @Override
    public boolean isBlank() {
        return blankVariable || (value != null && value.isBNode());
    }

    @Override
    public boolean same(Node n) {
        if (isVariable() || n.isVariable()) {
            return sameVariable(n);
        }
        return value.sameTerm(n.getDatatypeValue());
    }

    private boolean sameVariable(Node node) {
        return isVariable() && node.isVariable() && getLabel().equals(node.getLabel());
    }

    @Override
    public boolean match(Node n) {
        return same(n);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Node node && equals(node);
    }

    public boolean equals(Node node) {
        if (isVariable() || node.isVariable()) {
            return sameVariable(node);
        }
        return value.equals(node.getDatatypeValue());
    }

    @Override
    public void setIndex(int n) {
        index = n;
    }

    @Override
    public Object getNodeObject() {
        return payload;
    }

    @Override
    public Edge getEdge() {
        return payload instanceof Edge edge ? edge : null;
    }

    @Override
    public void setObject(Object o) {
        payload = o;
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String str) {
        key = Objects.requireNonNull(str, "str");
    }

    @Override
    public TripleStore getTripleStore() {
        return null;
    }

    @Override
    public int hashCode() {
        return isVariable() ? variableName.hashCode() : value.hashCode();
    }
}
