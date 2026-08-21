package fr.inria.corese.core.next.data.impl.adapter;

import fr.inria.corese.core.next.data.impl.adapter.node.CoreseNodeAdapter;

import fr.inria.corese.core.kgram.api.core.ExpType;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 * Utility class for converting between Corese-compatible Node objects
 * and other Value representations.
 */
public final class CoreseValueConverter {

    // Factory for creating Corese-compatible Value instances
    private final ValueFactory factory = new CoreseValueFactory();

    // Constant representing the default Corese graph context
    private static final Node DEFAULT_CORESE_CONTEXT = DatatypeMap.createResource(ExpType.DEFAULT_GRAPH);

    // --- Data API to Corese conversion methods ---

    /**
     * Converts a generic Value into a Corese Node.
     *
     * @param value the input Value to convert
     * @return the corresponding Corese Node, or null if input is null
     * @throws IllegalArgumentException if the Value type is unsupported
     */
    public Node toCoreseNode(Value value) {
        if (value == null) {
            return null;
        }

        if (value instanceof CoreseNodeAdapter) {
            return ((CoreseNodeAdapter) value).getCoreseNode();
        }

        if (value instanceof IRI) {
            IRI iri = (IRI) value;
            return ((CoreseNodeAdapter) factory.createIRI(iri.stringValue())).getCoreseNode();
        }

        if (value instanceof BNode) {
            BNode bnode = (BNode) value;
            return ((CoreseNodeAdapter) factory.createBNode(bnode.getID())).getCoreseNode();
        }

        if (value instanceof Literal) {
            Literal literal = (Literal) value;
            return literal.getLanguage()
                    .map(lang -> ((CoreseNodeAdapter) factory.createLiteral(literal.getLabel(), lang)).getCoreseNode())
                    .orElseGet(
                            () -> ((CoreseNodeAdapter) factory.createLiteral(literal.getLabel(), literal.getDatatype()))
                                    .getCoreseNode());
        }

        throw new IllegalArgumentException("Unsupported Value type: " + value.getClass());
    }

    /**
     * Converts a Resource (used as context) to a Corese Node.
     *
     * @param context data API resource context
     * @return Corese Node representing the context
     */
    public Node toCoreseContext(Resource context) {
        return (context != null) ? toCoreseNode(context) : DEFAULT_CORESE_CONTEXT;
    }

    /**
     * Converts an array of data API resource contexts into Corese nodes.
     *
     * @param contexts resource contexts
     * @return the corresponding Corese context nodes
     */
    public Node[] toCoreseContextArray(Resource[] contexts) {
        if (contexts == null || (contexts.length == 1 && contexts[0] == null)) {
            return new Node[] { DEFAULT_CORESE_CONTEXT };
        }
        if (contexts.length == 0) {
            return new Node[0];
        }

        Node[] result = new Node[contexts.length];
        for (int i = 0; i < contexts.length; i++) {
            result[i] = toCoreseContext(contexts[i]);
        }
        return result;
    }

    // --- Corese to data API conversion methods ---

    /**
     * Converts a Corese node to a data API value.
     *
     * @param node Corese Node to convert
     * @return equivalent data API value
     */
    public Value fromCoreseNode(Node node) {
        if (node == null) {
            return null;
        }

        IDatatype dt = node.getDatatypeValue();

        if (dt.isURI()) {
            return factory.createIRI(dt.getLabel());
        }
        if (dt.isBlank()) {
            return factory.createBNode(dt.getLabel());
        }
        if (dt.isLiteral()) {
            if (dt.getLang() != null) {
                return factory.createLiteral(dt.getLabel(), dt.getLang());
            }
            if (dt.getDatatypeURI() != null) {
                return factory.createLiteral(dt.getLabel(), factory.createIRI(dt.getDatatypeURI()));
            }
            return factory.createLiteral(dt.getLabel());
        }

        throw new IllegalArgumentException("Unsupported Node type: " + dt.getClass());
    }

    /**
     * Converts a Corese context node back to a data API resource.
     *
     * @param node Corese context node
     * @return resource, or {@code null} for the default context
     */
    public Resource fromCoreseContext(Node node) {
        return DEFAULT_CORESE_CONTEXT.equals(node) ? null : (Resource) fromCoreseNode(node);
    }

}
