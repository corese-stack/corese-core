package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.Constant;

/**
 * Conversion helpers for the StorageManager/KGRAM boundary.
 *
 * <p>The storage layer exposes RDF values through the Corese-next data model, while KGRAM
 * nodes still carry Corese {@link IDatatype} values. This helper keeps that transitional
 * conversion in one place and avoids routing storage results through the legacy KGRAM node API.
 */
final class StorageManagerKgramValues {

    private StorageManagerKgramValues() {
    }

    /**
     * Converts a storage RDF value to a KGRAM node.
     *
     * @param value storage value to wrap
     * @return KGRAM node carrying the equivalent Corese datatype
     */
    static Node node(Value value) {
        return node(datatypeValue(value));
    }

    /**
     * Wraps a Corese datatype as a KGRAM node.
     *
     * @param datatype Corese datatype to wrap
     * @return KGRAM node backed by a parser constant
     */
    static Node node(IDatatype datatype) {
        return new NodeImpl(Constant.create(datatype));
    }

    /**
     * Converts a KGRAM node to a storage RDF value.
     *
     * @param node KGRAM node to convert
     * @param valueFactory factory used to create storage values
     * @return equivalent storage value
     */
    static Value rdfValue(Node node, ValueFactory valueFactory) {
        return rdfValue(node.getDatatypeValue(), valueFactory);
    }

    /**
     * Converts a Corese datatype to the storage RDF value model.
     *
     * <p>Language-tagged literals are converted through the language branch, and therefore
     * become RDF {@code langString} literals in the storage model.
     *
     * @param datatype Corese datatype to convert
     * @param valueFactory factory used to create storage values
     * @return equivalent storage value
     * @throws IllegalArgumentException when the datatype cannot be represented as RDF
     */
    static Value rdfValue(IDatatype datatype, ValueFactory valueFactory) {
        if (datatype.isURI()) {
            return valueFactory.createIRI(datatype.getLabel());
        }
        if (datatype.isBlank()) {
            return valueFactory.createBNode(datatype.getLabel());
        }
        if (datatype.isLiteral()) {
            String language = datatype.getLang();
            if (language != null && !language.isEmpty()) {
                return valueFactory.createLiteral(datatype.getLabel(), language);
            }
            String datatypeUri = datatype.getDatatypeURI();
            if (datatypeUri != null && !datatypeUri.isEmpty()) {
                return valueFactory.createLiteral(datatype.getLabel(), valueFactory.createIRI(datatypeUri));
            }
            return valueFactory.createLiteral(datatype.getLabel());
        }
        throw new IllegalArgumentException("Unsupported KGRAM datatype value: " + datatype);
    }

    /**
     * Converts a storage RDF value to the Corese datatype model used by KGRAM nodes.
     *
     * @param value storage value to convert
     * @return equivalent Corese datatype
     * @throws IllegalArgumentException when the value is not an RDF IRI, blank node or literal
     */
    static IDatatype datatypeValue(Value value) {
        if (value instanceof IRI iri) {
            return DatatypeMap.newResource(iri.stringValue());
        }
        if (value instanceof BNode bNode) {
            return DatatypeMap.createBlank(bNode.getID());
        }
        if (value instanceof Literal literal) {
            return literal.getLanguage()
                    .<IDatatype>map(language -> DatatypeMap.createLiteral(literal.getLabel(), null, language))
                    .orElseGet(() -> DatatypeMap.createLiteral(
                            literal.getLabel(),
                            literal.getDatatype().stringValue(),
                            null));
        }
        throw new IllegalArgumentException("Unsupported RDF value: " + value);
    }
}
