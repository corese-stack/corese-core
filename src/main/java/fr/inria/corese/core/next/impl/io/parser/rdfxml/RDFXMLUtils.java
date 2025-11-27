package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.common.IOConstants;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
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
     * Expands a QName string (e.g. "xsd:integer") into a full URI if known.
     * Currently supports "xsd:" → XML Schema namespace.
     *
     * @param qname the QName string
     * @return expanded full URI if a known prefix, otherwise returns qname unchanged
     */
    public static String expandQNameFromQName(String qname) {
        if (qname == null) return null;
        String xsdPrefix = fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdString.getPreferredPrefix() + ":";
        if (qname.startsWith(xsdPrefix)) {
            return fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdString.getNamespace()
                    + qname.substring(xsdPrefix.length());
        }
        return qname;
    }

    /**
     * Validates if a string is a valid XML Name according to XML 1.0 specification.
     * An XML Name must start with a letter, underscore, or colon, and can contain
     * letters, digits, hyphens, underscores, colons, and periods.
     * Special RDF/XML rule: Names cannot start with "_:" (reserved for blank nodes).
     *
     * @param name the string to validate
     * @param isRdfIdAttribute true if validating rdf:ID or rdf:bagID (disallows "_:" prefix)
     * @return true if the string is a valid XML Name
     */
    public static boolean isValidXMLName(String name, boolean isRdfIdAttribute) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // RDF/XML specific: rdf:ID and rdf:bagID cannot start with "_:"
        // RDF/XML specific: rdf:nodeID cannot start with "_:" or contain ":"
        if (name.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return false;
        }

        // For rdf:nodeID, colons are not allowed (NCName restriction)
        if (!isRdfIdAttribute && name.contains(IOConstants.COLON)) {
            return false;
        }

        char first = name.charAt(0);
        // XML Name must start with: Letter | '_' | ':'
        // For simplicity, we check: not a digit, not a hyphen, not a period
        if (Character.isDigit(first) || first == '-' || first == '.') {
            return false;
        }

        // Additional validation: check all characters are valid
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Valid chars: letters, digits, '.', '-', '_', ':'
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_' && c != ':') {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts a subject resource from RDF/XML attributes.
     * Supports rdf:about, rdf:nodeID, rdf:ID, and rdf:bagID.
     *
     * @param attrs    the XML attributes
     * @param factory  the value factory
     * @param baseURI  the base URI for resolving relative IRIs
     * @return a Resource representing the subject
     * @throws ParsingErrorException if rdf:ID, rdf:nodeID, or rdf:bagID value is not a valid XML Name,
     *                               if conflicting attributes are present, or if obsolete attributes are used
     */
    public static Resource extractSubject(Attributes attrs, ValueFactory factory, String baseURI) {
        // Check for obsolete attributes (removed in RDF 1.1)
        String aboutEach = attrs.getValue(RDF.type.getNamespace(), "aboutEach");
        String aboutEachPrefix = attrs.getValue(RDF.type.getNamespace(), "aboutEachPrefix");

        if (aboutEach != null) {
            throw new ParsingErrorException("rdf:aboutEach is not supported. " +
                    "This attribute was removed from RDF specifications.");
        }
        if (aboutEachPrefix != null) {
            throw new ParsingErrorException("rdf:aboutEachPrefix is not supported. " +
                    "This attribute was removed from RDF specifications.");
        }

        String about = attrs.getValue(RDF.type.getNamespace(), "about");
        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");
        String id = attrs.getValue(RDF.type.getNamespace(), "ID");
        String bagID = attrs.getValue(RDF.type.getNamespace(), "bagID");

        // Check for conflicting attributes
        int count = (about != null ? 1 : 0) + (nodeID != null ? 1 : 0) + (id != null ? 1 : 0) + (bagID != null ? 1 : 0);
        if (count > 1) {
            throw new ParsingErrorException("Cannot have multiple subject-identifying attributes. " +
                    "Only one of rdf:about, rdf:nodeID, rdf:ID, or rdf:bagID is allowed per element.");
        }

        if (about != null) {
            return factory.createIRI(resolveAgainstBase(about, baseURI));
        }

        if (nodeID != null) {
            if (!isValidXMLName(nodeID, false)) {
                throw new ParsingErrorException("rdf:nodeID value '" + nodeID + "' is not a valid XML Name. " +
                        "XML Names must start with a letter, underscore, or colon, not a digit or hyphen.");
            }
            return factory.createBNode(ParserConstants.BLANK_NODE_PREFIX + nodeID);
        }

        if (id != null) {
            if (!isValidXMLName(id, true)) {
                throw new ParsingErrorException("rdf:ID value '" + id + "' is not a valid XML Name. " +
                        "XML Names must start with a letter, underscore, or colon, not a digit or hyphen. " +
                        "Additionally, rdf:ID cannot start with '_:'.");
            }
            return factory.createIRI(resolveAgainstBase("#" + id, baseURI));
        }

        if (bagID != null) {
            if (!isValidXMLName(bagID, true)) {
                throw new ParsingErrorException("rdf:bagID value '" + bagID + "' is not a valid XML Name. " +
                        "XML Names must start with a letter, underscore, or colon, not a digit or hyphen. " +
                        "Additionally, rdf:bagID cannot start with '_:'.");
            }
            return factory.createIRI(resolveAgainstBase("#" + bagID, baseURI));
        }

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
            throw new ParsingErrorException("Failed to resolve IRI: " + iri + " against base: " + baseURI, e);
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
        if (RDF.type.getNamespace().equals(uri)) {
            return switch (localName) {
                case "about", "ID", "nodeID", "resource", "parseType", "datatype" -> true;
                default -> false;
            };
        }
        return qName.startsWith("xml:");
    }

    /**
     * Checks if an element is the top-level rdf:RDF wrapper.
     *
     * @param uri       the namespace URI
     * @param localName the local name
     * @return true if the element is rdf:RDF
     */
    public static boolean isRdfRDF(String uri, String localName) {
        return RDF.type.getNamespace().equals(uri) && "RDF".equals(localName);
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
                (RDF.Seq.getIRI().getLocalName().equals(localName) || RDF.Bag.getIRI().getLocalName().equals(localName) || RDF.Alt.getIRI().getLocalName().equals(localName));
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