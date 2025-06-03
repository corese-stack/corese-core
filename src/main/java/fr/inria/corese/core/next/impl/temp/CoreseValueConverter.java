package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.kgram.api.core.ExpType;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.BNode;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;


/**
 * Utility class for converting between Corese-compatible Node objects
 * and other Value representations.
 */
public class CoreseValueConverter {

    // Factory for creating Corese-compatible Value instances
    private final ValueFactory factory = new CoreseAdaptedValueFactory();

    private final org.eclipse.rdf4j.model.ValueFactory rdf4jFactory = SimpleValueFactory.getInstance();

    // Constant representing the default Corese graph context
    private static final Node DEFAULT_CORESE_CONTEXT = DatatypeMap.createResource(ExpType.DEFAULT_GRAPH);

    private static final String HTTP_SCHEME_PREFIX = "http://";
    private static final String COLON = ":";

    // --- Rdf4j to Corese conversion methods ---

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
     * @param context RDF4J Resource context
     * @return Corese Node representing the context
     */
    public Node toCoreseContext(Resource context) {
        return (context != null) ? toCoreseNode(context) : DEFAULT_CORESE_CONTEXT;
    }

    /**
     * Converts an array of RDF4J Resource contexts into an array of Corese Nodes.
     *
     * @param contexts RDF4J contexts array
     * @return Corese Node array following RDF4J context conventions
     */
    public Node[] toCoreseContextArray(Resource[] contexts) {
        if (contexts == null || (contexts.length == 1 && contexts[0] == null)) {
            return new Node[]{DEFAULT_CORESE_CONTEXT};
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

    // --- Corese to Rdf4j conversion methods ---

    /**
     * Converts a Corese Node to an RDF4J Value.
     *
     * @param node Corese Node to convert
     * @return RDF4J Value equivalent
     */
    public Value toRdf4jValue(Node node) {
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
                return factory.createLiteral(dt.getLabel(), dt.getDatatypeURI());
            }
            return factory.createLiteral(dt.getLabel());
        }

        throw new IllegalArgumentException("Unsupported Node type: " + dt.getClass());
    }

    /**
     * Converts a Corese context node back to an RDF4J Resource.
     *
     * @param node Corese context node
     * @return RDF4J Resource or null if it's the default context
     */
    public Resource toRdf4jValueContext(Node node) {
        return DEFAULT_CORESE_CONTEXT.equals(node) ? null : (Resource) toRdf4jValue(node);
    }

    /**
     * Converts a Corese context node (which is an IDatatype) back to an RDF4J Resource.
     *
     * @param node Corese context node
     * @return RDF4J Resource or null if it's the default context
     */
    public org.eclipse.rdf4j.model.Resource resourcetoRdf4jValueContext(Node node) {
        if (node == null) {
            return null;
        }
        if (DEFAULT_CORESE_CONTEXT.equals(node)) {
            return null;
        }

        return (org.eclipse.rdf4j.model.Resource) valuetoRdf4jValue(node.getDatatypeValue());
    }

    /**
     * Converts a Corese IDatatype to an RDF4J Value.
     * This is the primary conversion logic from Corese internal value representation to RDF4J.
     *
     * @param dt Corese IDatatype to convert
     * @return RDF4J Value equivalent
     */
    public org.eclipse.rdf4j.model.Value valuetoRdf4jValue(IDatatype dt) {
        if (dt == null) {
            return null;
        }
        if (dt.isURI()) {
            return rdf4jFactory.createIRI(dt.getLabel());
        }
        if (dt.isBlank()) {
            return rdf4jFactory.createBNode(dt.getLabel());
        }
        if (dt.isLiteral()) {

            boolean hasValidLangTag = dt.getLang() != null && !dt.getLang().isEmpty() && !dt.getLang().contains(COLON);
            boolean isRdfLangStringDatatype = dt.getDatatypeURI() != null && dt.getDatatypeURI().equals(RDF.LANGSTRING.stringValue());
            if (isRdfLangStringDatatype && hasValidLangTag) {
                return rdf4jFactory.createLiteral(dt.getLabel(), dt.getLang());
            }

            boolean isImplicitXsdString = dt.getDatatypeURI() != null && dt.getDatatypeURI().equals(XMLSchema.STRING.stringValue());
            if (dt.getDatatypeURI() != null && !isRdfLangStringDatatype) {
                return rdf4jFactory.createLiteral(dt.getLabel(), rdf4jFactory.createIRI(dt.getDatatypeURI()));
            }

            if (isImplicitXsdString || (dt.getLang() != null && dt.getLang().contains(HTTP_SCHEME_PREFIX)) || !hasValidLangTag) {
                return rdf4jFactory.createLiteral(dt.getLabel());
            }
            if (dt.getLang() != null) {
                return rdf4jFactory.createLiteral(dt.getLabel(), dt.getLang());
            }
            return rdf4jFactory.createLiteral(dt.getLabel());
        }

        throw new IllegalArgumentException("Unsupported Corese IDatatype type for conversion to RDF4J Value: " + dt.getClass());
    }


}
