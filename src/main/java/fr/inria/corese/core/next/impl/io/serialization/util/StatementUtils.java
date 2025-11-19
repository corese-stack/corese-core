package fr.inria.corese.core.next.impl.io.serialization.util;

import fr.inria.corese.core.next.api.*;

import java.util.Map;

/**
 * Utility class for handling Statement manipulation during RDFC-1.0 canonicalization.
 * This class provides methods to create new statements with replaced blank node identifiers
 * and to serialize them for comparison and hashing according to the RDFC-1.0 specification.
 * <p>
 * Key functionalities:
 * - Replacement of blank node identifiers with canonical IDs
 * - Serialization of RDF values for lexicographic comparison
 * - Conversion of statements to N-Quads format
 * - Blank node identification and manipulation
 */
public class StatementUtils {

    private final ValueFactory valueFactory;

    /**
     * Constructs a new StatementUtils instance.
     *
     * @param valueFactory The factory for creating RDF values, used for creating new statements
     *                     with replaced blank node identifiers.
     */
    public StatementUtils(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    /**
     * Creates a new statement with blank nodes replaced according to the canonical mapping.
     * This method is used during the canonicalization process to replace original blank node
     * identifiers with their canonical counterparts.
     *
     * @param originalStatement The original statement containing blank nodes to be replaced.
     * @param canonicalMapping  A map from original blank node IDs to canonical IDs.
     * @return A new statement with blank node identifiers replaced according to the mapping.
     */
    public Statement replaceBlankNodes(Statement originalStatement, Map<String, String> canonicalMapping) {
        Resource newSubject = replaceIfBlankNodeResource(originalStatement.getSubject(), canonicalMapping);
        IRI newPredicate = originalStatement.getPredicate();
        Value newObject = replaceIfBlankNodeValue(originalStatement.getObject(), canonicalMapping);
        Resource newContext = replaceIfBlankNodeResource(originalStatement.getContext(), canonicalMapping);

        return valueFactory.createStatement(newSubject, newPredicate, newObject, newContext);
    }

    /**
     * Replaces a blank node Resource with its canonical identifier if it exists in the mapping.
     * If the resource is not a blank node or has no mapping, returns the original resource unchanged.
     *
     * @param original The original Resource to potentially replace.
     * @param mapping  The canonical mapping from original to canonical blank node IDs.
     * @return The replaced Resource or the original if no replacement is needed.
     */
    private Resource replaceIfBlankNodeResource(Resource original, Map<String, String> mapping) {
        if (original != null && isBlankNode(original)) {
            String originalId = getBlankNodeId(original);
            String canonicalId = mapping.get(originalId);
            if (canonicalId != null) {
                return valueFactory.createBNode(canonicalId);
            }
        }
        return original;
    }

    /**
     * Replaces a blank node Value with its canonical identifier if it exists in the mapping.
     * If the value is not a blank node or has no mapping, returns the original value unchanged.
     *
     * @param original The original Value to potentially replace.
     * @param mapping  The canonical mapping from original to canonical blank node IDs.
     * @return The replaced Value or the original if no replacement is needed.
     */
    private Value replaceIfBlankNodeValue(Value original, Map<String, String> mapping) {
        if (original != null && isBlankNode(original)) {
            String originalId = getBlankNodeId(original);
            String canonicalId = mapping.get(originalId);
            if (canonicalId != null) {
                return valueFactory.createBNode(canonicalId);
            }
        }
        return original;
    }

    /**
     * Checks if a given Value is a blank node.
     * Blank nodes are anonymous resources that don't have a URI identifier.
     *
     * @param value The Value to check.
     * @return true if the value is a blank node, false otherwise.
     */
    public static boolean isBlankNode(Value value) {
        return value != null && value.isBNode();
    }

    /**
     * Extracts the identifier string from a blank node Value.
     * For blank nodes, this returns the local identifier without the ":_" prefix.
     *
     * @param value The blank node Value from which to extract the identifier.
     * @return The blank node identifier string, or null if the value is not a blank node.
     */
    public static String getBlankNodeId(Value value) {
        if (value == null) return null;
        if (isBlankNode(value)) {
            String str = value.stringValue();
            if (str.startsWith(SerializationConstants.BLANK_NODE_PREFIX)) {
                return str.substring(2);
            }
            return str;
        }
        return null;
    }

    /**
     * Serializes a Value for lexicographic comparison according to RDFC-1.0 specifications.
     * This method produces a string representation suitable for deterministic sorting and hashing.
     *
     * @param value The Value to serialize.
     * @return A string representation of the value for comparison purposes.
     */
    public static String serializeForComparison(Value value) {
        if (value == null) {
            return SerializationConstants.EMPTY_STRING;
        }

        if (value instanceof IRI) {
            IRI iri = (IRI) value;
            String uri = iri.stringValue();

            return SerializationConstants.LT + uri + SerializationConstants.GT;
        }


        if (value instanceof BNode) {
            return serializeBNode((BNode) value);
        }

        if (value instanceof Literal) {
            return serializeLiteral((Literal) value);
        }

        return value.toString();
    }


    /**
     * Serializes a blank node for comparison.
     * Blank nodes are serialized with the ":_" prefix followed by their identifier.
     *
     * @param bnode The blank node to serialize.
     * @return The serialized blank node string.
     */
    private static String serializeBNode(BNode bnode) {
        return SerializationConstants.BLANK_NODE_PREFIX + bnode.getID();
    }

    /**
     * Serializes a literal for comparison according to RDFC-1.0 specifications.
     * Handles string escaping, datatypes, and language tags appropriately.
     *
     * @param literal The literal to serialize.
     * @return The serialized literal string.
     */
    private static String serializeLiteral(Literal literal) {
        StringBuilder sb = new StringBuilder();

        // Escape special characters in the literal label
        String escapedLabel = literal.getLabel()
                .replace(SerializationConstants.BACK_SLASH, "\\\\")
                .replace(SerializationConstants.QUOTE, "\\\"");

        sb.append('"').append(escapedLabel).append('"');

        // Handle datatype or language tag
        if (literal.getDatatype() != null) {
            String datatypeUri = literal.getDatatype().stringValue();
            // Omit xsd:string datatype for brevity (implied by default)
            if (!"http://www.w3.org/2001/XMLSchema#string".equals(datatypeUri)) {
                sb.append(SerializationConstants.DATATYPE_SEPARATOR).append(serializeForComparison(literal.getDatatype()));
            }
        } else if (literal.getLanguage() != null) {
            sb.append(SerializationConstants.AT).append(literal.getLanguage());
        }

        return sb.toString();
    }

    /**
     * Converts a Statement to N-Quads format for lexicographic comparison.
     * This produces a canonical string representation suitable for sorting and hashing
     * according to the RDFC-1.0 specification.
     *
     * @param statement The statement to convert.
     * @return The N-Quads representation of the statement.
     */
    public static String toNQuad(Statement statement) {
        if (statement == null) {
            return SerializationConstants.EMPTY_STRING;
        }

        StringBuilder sb = new StringBuilder();

        // Serialize subject, predicate, and object
        sb.append(serializeForComparison(statement.getSubject()))
                .append(SerializationConstants.SPACE);
        sb.append(serializeForComparison(statement.getPredicate()))
                .append(SerializationConstants.SPACE);
        sb.append(serializeForComparison(statement.getObject()));

        // Serialize context (graph) if present
        if (statement.getContext() != null) {
            sb.append(SerializationConstants.SPACE)
                    .append(serializeForComparison(statement.getContext()));
        }

        // Terminate with space and period
        sb.append(SerializationConstants.SPACE)
                .append(SerializationConstants.POINT);

        return sb.toString();
    }


}