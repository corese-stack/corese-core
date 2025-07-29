package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import org.xml.sax.*;

import java.util.List;
import java.util.Optional;


public class RDFXMLUtils {
    private RDFXMLUtils() {
    }

    public static String expandQName(String uri, String localName, String qName) {
        return (uri != null && !uri.isEmpty()) ? uri + localName : qName;
    }

    public static Optional<XSD> resolveDatatype(String datatypeUri) {
        for (XSD xsd : XSD.values()) {
            if (xsd.getIRI().stringValue().equals(datatypeUri)) return Optional.of(xsd);
        }
        return Optional.empty();
    }

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

    public static boolean isDescription(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) && "Description".equals(localName);
    }

    public static boolean isNodeElement(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "about") != null ||
                attrs.getValue(RDF.type.getNamespace(), "nodeID") != null ||
                attrs.getValue(RDF.type.getNamespace(), "ID") != null;
    }

    public static String getParseType(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "parseType");
    }

    public static boolean isSyntaxAttribute(String uri, String localName, String qName) {
        if (uri != null && RDF.type.getNamespace().equals(uri)) {
            return switch (localName) {
                case "about", "ID", "nodeID", "resource", "parseType", "datatype" -> true;
                default -> false;
            };
        }
        return qName.startsWith("xml:");
    }

    public static Optional<XSD> fromURI(String uri) {
        for (XSD xsd : XSD.values()) {
            if (xsd.getIRI().stringValue().equals(uri)) {
                return Optional.of(xsd);
            }
        }
        return Optional.empty();
    }

    public static boolean isRdfRDF(String uri, String localName) {
        return RDF.type.equals(uri) && "RDF".equals(localName);
    }

    public static boolean isContainer(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) &&
                ("Seq".equals(localName) || "Bag".equals(localName) || "Alt".equals(localName));
    }

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