package fr.inria.corese.core.next.query.impl.kgram.tool;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.path.Path;

/**
 * Factory for Corese-specific KGRAM nodes used by the Corese-next runtime model.
 *
 * <p>These nodes encode runtime conventions that are not ordinary query terms.
 * Keeping them here makes the convention explicit and prevents bridge classes
 * from depending on historical parser constants.</p>
 */
public final class KgramNodes {

    /**
     * Runtime predicate used by KGRAM to represent a variable predicate edge.
     */
    public static final String ROOT_PROPERTY_URI = "http://www.inria.fr/acacia/corese#Property";

    private KgramNodes() {
    }

    /**
     * Creates the KGRAM root property node.
     *
     * <p>KGRAM represents a triple pattern such as {@code ?s ?p ?o} with a
     * root-property edge node and the predicate variable stored separately as
     * the edge variable.</p>
     */
    public static Node rootProperty() {
        return new RootPropertyNode();
    }

    private static final class RootPropertyNode implements Node {

        private static final DatatypeValue VALUE = Values.factory().createIRI(ROOT_PROPERTY_URI);

        private int index = -1;
        private String key = INITKEY;

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public void setIndex(int n) {
            index = n;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public void setKey(String str) {
            key = str;
        }

        @Override
        public boolean same(Node n) {
            return n != null && !n.isVariable() && VALUE.sameTerm(n.getDatatypeValue());
        }

        @Override
        public boolean match(Node n) {
            return n != null && !n.isVariable() && VALUE.sameTerm(n.getDatatypeValue());
        }

        @Override
        public int compare(Node node) {
            return VALUE.compare(node.getDatatypeValue());
        }

        @Override
        public String getLabel() {
            return ROOT_PROPERTY_URI;
        }

        @Override
        public boolean isVariable() {
            return false;
        }

        @Override
        public boolean isConstant() {
            return true;
        }

        @Override
        public boolean isBlank() {
            return false;
        }

        @Override
        public DatatypeValue getValue() {
            return VALUE;
        }

        @Override
        public DatatypeValue getDatatypeValue() {
            return getValue();
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
        public Object getNodeObject() {
            return null;
        }

        @Override
        public void setObject(Object o) {
            // Root property is an immutable runtime constant; no attached object is stored.
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public String toString() {
            return '<' + ROOT_PROPERTY_URI + '>';
        }
    }
}
