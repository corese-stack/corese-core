package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.base.AbstractGraphSerializer;
import fr.inria.corese.core.next.impl.common.serialization.config.SerializerConfig;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * Serializes a {@link Model} to Turtle format with comprehensive syntax support.
 * This class provides a method to write the declarations of a model to a {@link Writer}
 * in accordance with the Turtle specification, taking into account configuration options.
 *
 * <p>This implementation handles:</p>
 * <ul>
 * <li>Declaration and usage of prefixes for IRIs, including auto-declaration and sorting.</li>
 * <li>The 'a' shortcut for 'rdf:type'.</li>
 * <li>Escaping of special characters in literals (single-line and multi-line) and IRIs.</li>
 * <li>Basic pretty-printing (indentation, end-of-line dots).</li>
 * <li>Management of literal datatype policies (minimal or always typed).</li>
 * <li>Serialization of compact triples (semicolons, commas) to group subjects and predicates.</li>
 * <li>Serialization of nested blank nodes using the '[]' syntax.</li>
 * <li>Serialization of RDF collections (lists) using the '()' syntax.</li>
 * <li>Detection and prevention of infinite loops during serialization of nested blank nodes and lists.</li>
 * <li>Sorting of subjects and predicates if configured.</li>
 * </ul>
 * <p>Advanced features such as strict adherence to maximum line length
 * and generation of stable blank node identifiers are not fully implemented in this version.</p>
 */
public class TurtleSerializer extends AbstractGraphSerializer {

    private static final Logger logger = LoggerFactory.getLogger(TurtleSerializer.class);

    /**
     * Constructs a new {@code TurtleSerializer} instance with the specified model and default configuration.
     * The default configuration is returned by {@link SerializerConfig#turtleConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public TurtleSerializer(Model model) {
        this(model, SerializerConfig.turtleConfig());
    }

    /**
     * Constructs a new {@code TurtleSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link SerializationConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public TurtleSerializer(Model model, SerializationConfig config) {
        super(model, config);
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return "Turtle"
     */
    @Override
    protected String getFormatName() {
        return "Turtle";
    }

    /**
     * Implements the main statement writing logic for the Turtle format.
     * Turtle does not support named graphs, so this method handles the serialization
     * of simple or optimized triples.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void doWriteStatements(Writer writer) throws IOException {
        if (config.useCompactTriples() && config.groupBySubject()) {
            writeOptimizedStatements(writer);
        } else {
            writeSimpleStatements(writer);
        }
    }

    /**
     * Escapes characters in an IRI string for Turtle output.
     * This method primarily focuses on control characters and problematic characters within angle brackets.
     *
     * @param iri The IRI to escape.
     * @return The escaped IRI.
     */
    @Override
    protected String escapeIRIString(String iri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);
            if (c < 0x20 || c == 0x7F || c == SerializationConstants.LT.charAt(0) || c == SerializationConstants.GT.charAt(0) || c == SerializationConstants.QUOTE.charAt(0) || c == '{' || c == '}' || c == '|' || c == '^' || c == '`' || c == SerializationConstants.BACK_SLASH.charAt(0)) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in Turtle string literals.
     * Handles backslashes, double quotes, and common control characters.
     * Unicode escape sequences are used for unprintable characters if `escapeUnicode` is true.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a Turtle literal.
     */
    @Override
    protected String escapeLiteralString(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    sb.append(SerializationConstants.BACK_SLASH).append("n");
                    break;
                case '\r':
                    sb.append(SerializationConstants.BACK_SLASH).append("r");
                    break;
                case '\t':
                    sb.append(SerializationConstants.BACK_SLASH).append("t");
                    break;
                case '\b':
                    sb.append(SerializationConstants.BACK_SLASH).append("b");
                    break;
                case '\f':
                    sb.append(SerializationConstants.BACK_SLASH).append("f");
                    break;
                case '"':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.QUOTE);
                    break;
                case '\\':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.BACK_SLASH);
                    break;
                default:
                    if (config.escapeUnicode() && (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0xFFFF))) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (Character.isHighSurrogate(c)) {
                        int codePoint = value.codePointAt(i);
                        if (Character.isValidCodePoint(codePoint)) {
                            sb.append(String.format("\\U%08X", codePoint));
                            i++;
                        } else {
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in multi-line literals (triple-quotes).
     * Primarily used to escape occurrences of `"""` within the literal.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a Turtle multi-line literal.
     */
    @Override
    protected String escapeMultilineLiteralString(String value) {
        return value.replace(SerializationConstants.QUOTE + SerializationConstants.QUOTE + SerializationConstants.QUOTE,
                SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE);
    }

    /**
     * Logs a warning if a context (named graph) is present in a statement,
     * as the Turtle format does not support named graphs.
     *
     * @param stmt The statement to check.
     */
    private void logContextWarning(Statement stmt) {
        if (stmt.getContext() != null && logger.isWarnEnabled()) {
            logger.warn("Turtle format does not support named graphs. Context '{}' will be ignored for statement: {}",
                    stmt.getContext().stringValue(), stmt);
        }
    }

    @Override
    protected void writeStatement(Writer writer, Statement stmt) throws IOException {
        super.writeStatement(writer, stmt);
        logContextWarning(stmt);
    }
}
