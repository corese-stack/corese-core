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
        return switch (value) {
            case null -> null;
            case CoreseNodeAdapter coreseNodeAdapter -> coreseNodeAdapter.getCoreseNode();
            case IRI iri -> ((CoreseNodeAdapter) factory.createIRI(iri.stringValue())).getCoreseNode();
            case BNode bnode -> ((CoreseNodeAdapter) factory.createBNode(bnode.getID())).getCoreseNode();
            case Literal literal -> literal.getLanguage()
                    .map(lang -> ((CoreseNodeAdapter) factory.createLiteral(literal.getLabel(), lang)).getCoreseNode())
                    .orElseGet(
                            () -> ((CoreseNodeAdapter) factory.createLiteral(literal.getLabel(), literal.getDatatype()))
                                    .getCoreseNode());
            default -> throw new IllegalArgumentException("Unsupported Value type: " + value.getClass());
        };

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
