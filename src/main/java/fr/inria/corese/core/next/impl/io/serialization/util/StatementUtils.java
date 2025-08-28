package fr.inria.corese.core.next.impl.io.serialization.util;

import fr.inria.corese.core.next.api.*;

import java.util.Map;

/**
 * Utility class for handling Statement manipulation during RDFC-1.0 canonicalization.
 * This class provides methods to create new statements with replaced blank node identifiers
 * and to serialize them for comparison and hashing.
 */
public class StatementUtils {

    private final ValueFactory valueFactory;

    public StatementUtils(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    /**
     * Creates a new statement with blank nodes replaced according to the canonical mapping.
     *
     * @param originalStatement The original statement
     * @param canonicalMapping  Map from original blank node IDs to canonical IDs
     * @return A new statement with replaced blank node identifiers
     */
    public Statement replaceBlankNodes(Statement originalStatement, Map<String, String> canonicalMapping) {

        Resource newSubject = replaceIfBlankNodeResource(originalStatement.getSubject(), canonicalMapping);
        IRI newPredicate = originalStatement.getPredicate();
        Value newObject = replaceIfBlankNodeValue(originalStatement.getObject(), canonicalMapping);
        Resource newContext = replaceIfBlankNodeResource(originalStatement.getContext(), canonicalMapping);

        return valueFactory.createStatement(newSubject, newPredicate, newObject, newContext);
    }

    private Resource replaceIfBlankNodeResource(Resource original, Map<String, String> mapping) {

        if (original != null && isBlankNode(original)) {
            String canonicalId = mapping.getOrDefault(getBlankNodeId(original), getBlankNodeId(original));
            return valueFactory.createBNode(canonicalId);
        }
        return original;
    }

    private Value replaceIfBlankNodeValue(Value original, Map<String, String> mapping) {

        if (original != null && isBlankNode(original)) {
            String canonicalId = mapping.getOrDefault(getBlankNodeId(original), getBlankNodeId(original));
            return valueFactory.createBNode(canonicalId);
        }
        return original;
    }

    /**
     * Checks if a value is a blank node.
     *
     * @param value The value to check.
     * @return True if the value is a blank node, false otherwise.
     */
    public static boolean isBlankNode(Value value) {
        return value != null && value.isBNode();
    }

    /**
     * Gets the identifier string for a blank node.
     *
     * @param value The blank node value.
     * @return The string identifier.
     */
    public static String getBlankNodeId(Value value) {
        return value.stringValue();
    }

    /**
     * Converts a value to a string for lexicographic comparison, as defined by RDFC-1.0.
     *
     * @param value The value to convert.
     * @return The N-Quads representation for comparison.
     */
    public static String serializeForComparison(Value value) {
        if (value == null) return SerializationConstants.EMPTY_STRING;
        String valueStr = value.stringValue();

        if (value.isBNode()) {
            return valueStr;
        }

        if (value.isIRI()) {
            return SerializationConstants.LT + valueStr + SerializationConstants.GT;
        }

        return SerializationConstants.QUOTE + valueStr + SerializationConstants.QUOTE;
    }

    /**
     * Converts a statement to N-Quads format for lexicographic comparison.
     * This uses a simplified serialization for comparison purposes only.
     *
     * @param statement The statement to convert
     * @return The N-Quads representation
     */
    public static String toNQuad(Statement statement) {
        StringBuilder sb = new StringBuilder();

        sb.append(serializeForComparison(statement.getSubject())).append(SerializationConstants.SPACE);
        sb.append(serializeForComparison(statement.getPredicate())).append(SerializationConstants.SPACE);
        sb.append(serializeForComparison(statement.getObject()));

        if (statement.getContext() != null) {
            sb.append(SerializationConstants.SPACE).append(serializeForComparison(statement.getContext()));
        }

        sb.append(SerializationConstants.SPACE_POINT);
        return sb.toString();
    }
}
