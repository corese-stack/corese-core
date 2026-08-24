package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import org.xml.sax.Attributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Utility methods for processing RDF/XML constructs.
 * <p>
 * This class provides helpers for handling RDF/XML syntax attributes,
 * QName expansion, datatype resolution, subject extraction, and RDF collections.
 * </p>
 */
public class RDFXMLUtils {

    private static final String ATTR_ABOUT_EACH = "aboutEach";
    private static final String ATTR_ABOUT_EACH_PREFIX = "aboutEachPrefix";
    private static final String ATTR_ABOUT = "about";
    private static final String ATTR_NODE_ID = "nodeID";
    private static final String ATTR_BAG_ID = "bagID";
    private static final String ATTR_RESOURCE = "resource";
    private static final String ATTR_DATATYPE = "datatype";
    private static final String ATTR_PARSE_TYPE = "parseType";
    private static final String ELEMENT_DESCRIPTION = "Description";

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
    public static Optional<XSDDatatype> resolveDatatype(String datatypeUri) {
        for (XSDDatatype xsd : XSDDatatype.values()) {
            if (xsd.getIRI().stringValue().equals(datatypeUri)) return Optional.of(xsd);
        }
        return Optional.empty();
    }


    /**
     * Validates if a string is a valid XML Name according to XML 1.0 specification.
     * An XML Name must start with a letter, underscore, or colon, and can contain
     * letters, digits, hyphens, underscores, colons, and periods.
     * Special RDF/XML rule: Names cannot start with "_:" (reserved for blank nodes).
     *
     * @param name the string to validate
     * @param isRdfIdAttribute true if validating rdf:ID or rdf:bagID (stricter rules)
     * @return true if the string is INVALID, false if valid
     */
    public static boolean isInvalidXMLName(String name, boolean isRdfIdAttribute) {
        if (name == null || name.isEmpty()) {
            return true;
        }

        if (isRdfIdAttribute && name.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return true;
        }

        if (name.contains(IOConstants.COLON)) {
            return true;
        }

        char first = name.charAt(0);

        if (Character.isDigit(first) || first == '-' || first == '.') {
            return true;
        }

        // First char must be letter or underscore (NCName)
        if (!Character.isLetter(first) && first != '_') {
            return true;
        }

        // Validate all characters (NCName rules - no colon anywhere)
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            // Valid chars for NCName: letters, digits, '.', '-', '_' (NO colon)
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_') {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts a subject resource from RDF/XML attributes.
     * Supports rdf:about, rdf:nodeID, rdf:ID, and rdf:bagID.
     *
     * @param attrs    the XML attributes
     * @param factory  the value factory
     * @param baseURI  the base URI for resolving relative IRIs
     * @param usedIDs  set to track used rdf:ID values (can be null if not tracking)
     * @return a Resource representing the subject
     * @throws ParsingException if rdf:ID, rdf:nodeID, or rdf:bagID value is not a valid XML Name,
     *                               if conflicting attributes are present, or if obsolete attributes are used
     */
    @SuppressWarnings("java:S3776")
    public static Resource extractSubject(Attributes attrs, ValueFactory factory, String baseURI, Set<String> usedIDs) {
        // Check for obsolete attributes (removed in RDF 1.1)
        String aboutEach = attrs.getValue(RDF.type.getNamespace(), ATTR_ABOUT_EACH);
        String aboutEachPrefix = attrs.getValue(RDF.type.getNamespace(), ATTR_ABOUT_EACH_PREFIX);

        if (aboutEach != null) {
            throw new ParsingException("rdf:aboutEach is not supported. " +
                    "This attribute was removed from RDF specifications.");
        }
        if (aboutEachPrefix != null) {
            throw new ParsingException("rdf:aboutEachPrefix is not supported. " +
                    "This attribute was removed from RDF specifications.");
        }

        String about = attrs.getValue(RDF.type.getNamespace(), ATTR_ABOUT);
        String nodeID = attrs.getValue(RDF.type.getNamespace(), ATTR_NODE_ID);
        String id = attrs.getValue(RDF.type.getNamespace(), "ID");
        String bagID = attrs.getValue(RDF.type.getNamespace(), ATTR_BAG_ID);

        // Check for conflicting attributes
        int count = (about != null ? 1 : 0) + (nodeID != null ? 1 : 0) + (id != null ? 1 : 0) + (bagID != null ? 1 : 0);
        if (count > 1) {
            throw new ParsingException("Cannot have multiple subject-identifying attributes. " +
                    "Only one of rdf:about, rdf:nodeID, rdf:ID, or rdf:bagID is allowed per element.");
        }

        if (about != null) {
            return factory.createIRI(resolveAgainstBase(about, baseURI));
        }

        if (nodeID != null) {
            if (isInvalidXMLName(nodeID, false)) {
                throw new ParsingException("rdf:nodeID value '" + nodeID + "' is not a valid NCName. " +
                        "NCNames cannot contain colons and must start with a letter or underscore.");
            }
            return factory.createBNode(IOConstants.BLANK_NODE_PREFIX + nodeID);
        }

        if (id != null) {
            if (isInvalidXMLName(id, true)) {
                throw new ParsingException("rdf:ID value '" + id + "' is not a valid NCName. " +
                        "NCNames cannot contain colons and must start with a letter or underscore. " +
                        "Additionally, rdf:ID cannot start with '_:'.");
            }
            String fullId = resolveAgainstBase("#" + id, baseURI);
            if (usedIDs != null && !usedIDs.add(fullId)) {
                throw new ParsingException("rdf:ID value '" + id + "' has already been used in this document. " +
                        "Each rdf:ID must be unique within a document.");
            }
            return factory.createIRI(fullId);
        }

        if (bagID != null) {
            if (isInvalidXMLName(bagID, true)) {
                throw new ParsingException("rdf:bagID value '" + bagID + "' is not a valid NCName. " +
                        "NCNames cannot contain colons and must start with a letter or underscore. " +
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
            throw new ParsingException("Failed to resolve IRI: " + iri + " against base: " + baseURI, e);
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
        return RDF.type.getNamespace().equals(uri) && ELEMENT_DESCRIPTION.equals(localName);
    }



    /**
     * Retrieves the value of rdf:parseType from attributes.
     *
     * @param attrs the attributes
     * @return the parseType value, or null if not present
     */
    public static String getParseType(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), ATTR_PARSE_TYPE);
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
                case ATTR_ABOUT, "ID", ATTR_NODE_ID, ATTR_RESOURCE, ATTR_PARSE_TYPE, ATTR_DATATYPE -> true;
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
     * Validates that an element name from the RDF namespace is allowed as a node element.
     * FIXES FOR error-001, error-011, error-012:
     * - Rejects rdf:RDF as node element (nested rdf:RDF is forbidden)
     * - Rejects all forbidden RDF names
     *
     * @param uri       the namespace URI
     * @param localName the local name of the element
     * @throws ParsingException if the element name is a forbidden RDF name
     */
    public static void validateNodeElementName(String uri, String localName) {
        if (!RDF.type.getNamespace().equals(uri)) {
            return;
        }

        switch (localName) {

            case "RDF","ID", ATTR_ABOUT, ATTR_BAG_ID, ATTR_PARSE_TYPE, ATTR_RESOURCE, ATTR_NODE_ID, ATTR_DATATYPE,
                 ATTR_ABOUT_EACH, ATTR_ABOUT_EACH_PREFIX,"li":
                throw new ParsingException("'" + localName + "' is not allowed as a node element name from the RDF namespace. " +
                        "RDF namespace names like rdf:ID, rdf:about, rdf:bagID, etc. cannot be used as typed node elements.");

            default:
                break;
        }
    }

    /**
     * Validates that a property element name from the RDF namespace is allowed.
     * According to RDF/XML specification, certain RDF namespace names cannot be used as property elements.
     *
     * @param uri       the namespace URI
     * @param localName the local name of the element
     * @throws ParsingException if the property name is a forbidden RDF name
     */
    public static void validatePropertyElementName(String uri, String localName) {
        if (!RDF.type.getNamespace().equals(uri)) {
            return;
        }

        switch (localName) {
            case "RDF", "ID", ATTR_ABOUT, ATTR_BAG_ID, ATTR_PARSE_TYPE, ATTR_RESOURCE, ATTR_NODE_ID,
                 ATTR_DATATYPE, ELEMENT_DESCRIPTION, ATTR_ABOUT_EACH, ATTR_ABOUT_EACH_PREFIX:
                throw new ParsingException("'" + localName + "' is not allowed as a property element name from the RDF namespace. " +
                        "Only rdf:type, rdf:_n (container membership), and rdf:li are valid RDF property names.");

            default:
                break;
        }
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


    /**
     * Validates parseType attribute values.
     * Only "Resource", "Literal", and "Collection" are valid values.
     *
     * @param parseType the parseType value to validate
     * @throws ParsingException if the parseType value is invalid
     */
    public static void validateParseType(String parseType) {
        if (parseType == null) {
            return;
        }

        switch (parseType) {
            case "Resource", "Literal", "Collection":
                return; // Valid
            default:
                throw new ParsingException(
                        "Invalid rdf:parseType value: '" + parseType + "'. " +
                                "Only 'Resource', 'Literal', and 'Collection' are allowed.");
        }
    }

    /**
     * Checks if an element is an RDF node element type (Description, Bag, Seq, Alt).
     * Used for determining when to pop the subject stack.
     *
     * @param uri       the namespace URI
     * @param localName the local name
     * @return true if this is an RDF node element type
     */
    public static boolean isRdfNodeElementType(String uri, String localName) {
        if (!RDF.type.getNamespace().equals(uri)) {
            return false;
        }

        return ELEMENT_DESCRIPTION.equals(localName) ||
                "Bag".equals(localName) ||
                "Seq".equals(localName) ||
                "Alt".equals(localName);
    }
}
