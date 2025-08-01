package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import org.xml.sax.*;

import java.util.List;
import java.util.Optional;

/**
 * Utility methods for processing RDF/XML constructs.
 * <p>
 * This class provides helpers for handling RDF/XML syntax attributes,
 * QName expansion, datatype resolution, subject extraction, and RDF collections.
 * </p>
 */
public class RDFXMLUtils {
    private RDFXMLUtils() {
        // Utility class; no instantiation.
    }


    /**
     * Expands a QName using the given namespace URI and local name.
     *
     * @param uri      the namespace URI
     * @param localName the local name
     * @param qName     the qualified name (used as fallback)
     * @return the expanded IRI, or the qName if the URI is null or empty
     */
    public static String expandQName(String uri, String localName, String qName) {
        return (uri != null && !uri.isEmpty()) ? uri + localName : qName;
    }


    /**
     * Resolves a datatype URI to a known XSD enum constant.
     *
     * @param datatypeUri the datatype URI
     * @return an Optional containing the matching XSD type if found
     */
    public static Optional<XSD> resolveDatatype(String datatypeUri) {
        for (XSD xsd : XSD.values()) {
            if (xsd.getIRI().stringValue().equals(datatypeUri)) return Optional.of(xsd);
        }
        return Optional.empty();
    }

    /**
     * Extracts a subject resource from RDF/XML attributes.
     * Supports rdf:about, rdf:nodeID, rdf:ID.
     *
     * @param attrs    the XML attributes
     * @param factory  the value factory
     * @param baseURI  the base URI for resolving relative IRIs
     * @return a Resource representing the subject
     */
    public static Resource extractSubject(Attributes attrs, ValueFactory factory, String baseURI) {
        String about = attrs.getValue(RDF.type.getNamespace(), "about");
        if (about != null) return factory.createIRI(resolveAgainstBase(about, baseURI));

        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");
        if (nodeID != null) return factory.createBNode("_:" + nodeID);

        String id = attrs.getValue(RDF.type.getNamespace(), "ID");
        if (id != null) return factory.createIRI(resolveAgainstBase("#" + id, baseURI));

        // Default to blank node
        return factory.createBNode();
    }

    /**
     * Resolves a relative IRI against a base URI.
     *
     * @param iri      the relative or absolute IRI
     * @param baseURI  the base URI
     * @return the resolved IRI
     * @throws IncorrectFormatException if URI resolution fails
     */
    public static String resolveAgainstBase(String iri, String baseURI) {
        if (iri == null) return null;
        if (iri.isEmpty()) return baseURI;
        if (baseURI == null || iri.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return iri;
        }
        try {
            return new java.net.URI(baseURI).resolve(iri).toString();
        } catch (Exception e) {
            throw new IncorrectFormatException("Failed to resolve IRI: " + iri + " against base: " + baseURI, e);
        }
    }

    /**
     * Determines whether the element is a rdf:Description.
     *
     * @param localName the local name of the element
     * @param uri       the namespace URI
     * @return {@code true} if it's an RDF description element
     */
    public static boolean isDescription(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) && "Description".equals(localName);
    }


    /**
     * Checks if the attributes define a subject node (via about, nodeID, or ID).
     *
     * @param attrs the attributes to check
     * @return true if any node-identifying attribute is present
     */
    public static boolean isNodeElement(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "about") != null ||
                attrs.getValue(RDF.type.getNamespace(), "nodeID") != null ||
                attrs.getValue(RDF.type.getNamespace(), "ID") != null;
    }


    /**
     * Retrieves the value of rdf:parseType from attributes.
     *
     * @param attrs the attributes
     * @return the parseType value, or null if not present
     */
    public static String getParseType(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "parseType");
    }


    /**
     * Determines whether a given attribute is an RDF/XML syntax attribute.
     *
     * @param uri       the namespace URI
     * @param localName the local name
     * @param qName     the qualified name
     * @return true if the attribute is considered syntax-related
     */
    public static boolean isSyntaxAttribute(String uri, String localName, String qName) {
        if (uri != null && RDF.type.getNamespace().equals(uri)) {
            return switch (localName) {
                case "about", "ID", "nodeID", "resource", "parseType", "datatype" -> true;
                default -> false;
            };
        }
        return qName.startsWith("xml:");
    }

    /**
     * Resolves an XSD datatype from a URI.
     *
     * @param uri the datatype URI
     * @return an Optional containing the XSD constant if matched
     */
    public static Optional<XSD> fromURI(String uri) {
        for (XSD xsd : XSD.values()) {
            if (xsd.getIRI().stringValue().equals(uri)) {
                return Optional.of(xsd);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if an element is the top-level rdf:RDF wrapper.
     *
     * @param uri       the namespace URI
     * @param localName the local name
     * @return true if the element is rdf:RDF
     */
    public static boolean isRdfRDF(String uri, String localName) {
        return RDF.type.equals(uri) && "RDF".equals(localName);
    }

    /**
     * Determines if an element is a recognized RDF container: Bag, Seq, or Alt.
     *
     * @param localName the local name
     * @param uri       the namespace URI
     * @return true if the element is a container type
     */
    public static boolean isContainer(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) &&
                ("Seq".equals(localName) || "Bag".equals(localName) || "Alt".equals(localName));
    }

    /**
     * Creates a linked RDF collection using rdf:first and rdf:rest.
     *
     * @param items   the list of resource items
     * @param model   the RDF model to populate
     * @param factory the RDF value factory
     * @return the head resource of the RDF collection
     */
    public static Resource createRdfCollection(List<Resource> items, Model model, ValueFactory factory) {
        Resource head = factory.createBNode();
        Resource current = head;

        for (int i = 0; i < items.size(); i++) {
            Resource next = (i < items.size() - 1)
                    ? factory.createBNode()
                    : RDF.nil.getIRI();  // rdf:nil

            model.add(factory.createStatement(current,
                    RDF.first.getIRI(),
                    items.get(i)));

            model.add(factory.createStatement(current,
                    RDF.rest.getIRI(),
                    next));

            current = next;
        }
        return head;
    }
}