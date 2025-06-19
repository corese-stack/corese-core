package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

public class NQuadsFormat implements FormatSerializer {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NQuadsFormat.class);

    private final Model model;
    private final FormatConfig config;

    /**
     * Constructs a new {@code NQuadsFormat} instance with the specified model and default N-Quads configuration.
     * The default configuration is obtained from {@link FormatConfig#nquadsConfig()}.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NQuadsFormat(Model model) {
        this(model, FormatConfig.nquadsConfig());
    }

    /**
     * Constructs a new {@code NQuadsFormat} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link FormatConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NQuadsFormat(Model model, FormatConfig config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    }

    /**
     * Writes the model to the given writer in N-Quads format.
     * Each statement (quad) in the model is written on a new line, terminated by a dot and a newline character.
     *
     * @param writer the {@link Writer} to which the N-Quads output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            for (Statement stmt : model) {
                writeStatement(writer, stmt);
            }
            writer.flush();
        } catch (IOException e) {
            throw new SerializationException("Failed to write", "NQuads", e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid data: " + e.getMessage(), "NQuads", e);
        }
    }

    /**
     * Writes a single {@link Statement} (quad) to the writer in N-Quads format.
     * The statement is written as "$subject $predicate $object $context ." if a context is present
     * and {@code config.includeContext()} is true.
     * If no context is present or {@code config.includeContext()} is false, it's written as "$subject $predicate $object .".
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeStatement(Writer writer, Statement stmt) throws IOException {
        writeValue(writer, stmt.getSubject());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getPredicate());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getObject());

        Resource context = stmt.getContext();
        if (context != null && config.includeContext()) {
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, context);
        } else if (context != null && logger.isWarnEnabled()) {
            logger.warn("Context '{}' will be ignored for statement: {} because includeContext is false in configuration.",
                    context.stringValue(), stmt);
        }

        if (config.trailingDot()) {
            writer.write(SerializationConstants.SPACE);
            writer.write(SerializationConstants.POINT);
        }

        writer.write(config.getLineEnding());
    }

    /**
     * Writes a single {@link Value} to the writer.
     * Handles literals, blank nodes, and IRIs, applying validation based on configuration.
     *
     * @param writer the {@link Writer} to which the value will be written.
     * @param value  the {@link Value} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the provided value is null or an unsupported type, especially in strict mode.
     */
    private void writeValue(Writer writer, Value value) throws IOException {
        if (config.isStrictMode()) {
            validateValue(value);
        } else if (value == null) {
            logger.warn("Encountered a null value during N-Quads serialization. This might lead to invalid output.");
            throw new IllegalArgumentException("Value cannot be null for N-Quads serialization.");
        }


        if (value.isLiteral()) {
            writeLiteral(writer, (Literal) value);
        } else if (value.isResource()) {
            if (value.isIRI()) {
                writeIRI(writer, (IRI) value);
            } else if (value.isBNode()) {
                writeBlankNode(writer, (Resource) value);
            } else {
                throw new IllegalArgumentException("Unsupported resource type for N-Quads serialization: " + value.getClass().getName());
            }
        } else {
            throw new IllegalArgumentException("Unsupported value type for N-Quads serialization: " + value.getClass().getName());
        }
    }

    /**
     * Writes a {@link Literal} to the writer in N-Quads format.
     * Handles plain literals, language-tagged literals, and typed literals,
     * applying escaping and datatype/language tag rules based on configuration.
     *
     * @param writer  the {@link Writer} to which the literal will be written.
     * @param literal the {@link Literal} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeLiteral(Writer writer, Literal literal) throws IOException {
        writer.write(SerializationConstants.QUOTE);
        writer.write(escapeLiteral(literal.stringValue()));
        writer.write(SerializationConstants.QUOTE);

        literal.getLanguage().ifPresent(lang -> {
            try {
                writer.write(SerializationConstants.AT_SIGN + lang);
            } catch (IOException e) {
                throw new UncheckedIOException("Error writing language tag to stream", e);
            }
        });

        IRI datatype = literal.getDatatype();
        if (!literal.getLanguage().isPresent() && datatype != null &&
                (config.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.ALWAYS_TYPED ||
                        (config.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.MINIMAL && !datatype.stringValue().equals(SerializationConstants.XSD_STRING)))) {
            writer.write(SerializationConstants.DATATYPE_SEPARATOR);
            writeIRI(writer, datatype);
        }
    }

    /**
     * Writes an {@link IRI} to the writer.
     * The IRI's string representation must be enclosed in angle brackets for N-Quads.
     * Applies URI validation and Unicode escaping based on configuration.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri    the {@link IRI} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the IRI is invalid (e.g., contains spaces) and strict mode/URI validation is enabled.
     */
    private void writeIRI(Writer writer, IRI iri) throws IOException {
        if (config.isStrictMode() && config.validateURIs()) {
            validateIRI(iri);
        }

        writer.write(SerializationConstants.LT);
        writer.write(escapeNQuadsIRI(iri.stringValue()));
        writer.write(SerializationConstants.GT);
    }

    /**
     * Writes a blank node to the writer.
     * Blank nodes are prefixed with "_:", and the identifier is appended.
     *
     * @param writer    the {@link Writer} to which the blank node will be written.
     * @param blankNode the {@link Resource} representing the blank node.
     * @throws IOException if an I/O error occurs.
     */
    private void writeBlankNode(Writer writer, Resource blankNode) throws IOException {
        writer.write(SerializationConstants.BNODE_PREFIX);
        writer.write(blankNode.stringValue());
    }

    /**
     * Validates and potentially escapes an IRI string for N-Quads.
     * Throws an {@link IllegalArgumentException} if the IRI contains characters
     * that are not allowed in N-Quads unescaped form (like spaces, quotes, angle brackets)
     * when strict mode and URI validation are enabled.
     * Applies Unicode escaping if {@code config.escapeUnicode()} is true.
     *
     * @param iri The string value of the IRI to validate and escape.
     * @return The validated and potentially escaped IRI string.
     * @throws IllegalArgumentException if the IRI string is invalid according to rules.
     */
    private String escapeNQuadsIRI(String iri) {
        Objects.requireNonNull(iri, "IRI string cannot be null");

        if (config.isStrictMode() && config.validateURIs() &&
                (iri.contains(SerializationConstants.SPACE) ||
                        iri.contains(SerializationConstants.QUOTE) ||
                        iri.contains(SerializationConstants.LT) ||
                        iri.contains(SerializationConstants.GT))) {
            throw new IllegalArgumentException(
                    "Invalid IRI for N-Quads (contains illegal characters inside '<>'): " + iri);
        }

        return config.escapeUnicode() ? escapeUnicodeString(iri) : iri;
    }

    /**
     * Escape special characters in N-Quads string literals.
     * Handles backslash, double quote, and common control characters.
     * Unicode escape sequences are used for unprintable characters if `config.escapeUnicode()` is true.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for N-Quads literal.
     */
    private String escapeLiteral(String value) {
        StringBuilder sb = new StringBuilder();
        int len = value.length(); // Cache length for invariant stop condition
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    sb.append(SerializationConstants.BACK_SLASH).append('n');
                    break;
                case '\r':
                    sb.append(SerializationConstants.BACK_SLASH).append('r');
                    break;
                case '\t':
                    sb.append(SerializationConstants.BACK_SLASH).append('t');
                    break;
                case '\b':
                    sb.append(SerializationConstants.BACK_SLASH).append('b');
                    break;
                case '\f':
                    sb.append(SerializationConstants.BACK_SLASH).append('f');
                    break;
                case '"':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.QUOTE);
                    break;
                case '\\':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.BACK_SLASH);
                    break;
                default:
                    i += appendDefaultCharOrUnicodeEscape(sb, c, value, i, config);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Helper method to append a character to StringBuilder, applying unicode escaping based on config
     * and handling supplementary characters. Returns the additional increment for the loop index.
     *
     * @param sb           The StringBuilder to append to.
     * @param c            The current character to process.
     * @param value        The original string (needed for codePointAt for supplementary characters).
     * @param currentIndex The current index in the original string.
     * @param config       The FormatConfig to check for unicode escaping preference.
     * @return The additional increment to the loop index (0 or 1 for supplementary characters).
     */
    private int appendDefaultCharOrUnicodeEscape(StringBuilder sb, char c, String value, int currentIndex, FormatConfig config) {
        if (config.escapeUnicode()) {
            if (c <= 0x1F || c == 0x7F) {
                sb.append(String.format("\\u%04X", (int) c));
            } else if (Character.isHighSurrogate(c)) {
                if (currentIndex + 1 < value.length() && Character.isLowSurrogate(value.charAt(currentIndex + 1))) {
                    int codePoint = value.codePointAt(currentIndex);
                    sb.append(String.format("\\U%08X", codePoint));
                    return 1;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        } else {
            sb.append(c);
        }
        return 0;
    }

    /**
     * Escapes non-ASCII and control characters into Unicode escape sequences.
     * This is a helper for `escapeNQuadsIRI` and `escapeLiteral`
     * if `escapeUnicode` is true in config.
     *
     * @param value The string to escape.
     * @return The string with Unicode characters escaped.
     */
    private String escapeUnicodeString(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0xFFFF)) {
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
        return sb.toString();
    }


    /**
     * Validates RDF values before serialization to ensure they conform to N-Quads rules.
     * Only called if {@code config.isStrictMode()} is enabled.
     *
     * @param value The {@link Value} to validate.
     * @throws IllegalArgumentException if the value is null or invalid based on N-Quads rules.
     */
    private void validateValue(Value value) {
        if (value == null) {
            logger.warn("Encountered a null value where a non-null value was expected for N-Quads serialization. This will result in an IllegalArgumentException if strict mode is enabled.");
            throw new IllegalArgumentException("Value cannot be null in N-Quads format when strictMode is enabled.");
        }

        if (value.isLiteral()) {
            validateLiteral((Literal) value);
        } else if (value.isIRI()) {
            validateIRI((IRI) value);
        }
    }

    /**
     * Validates a {@link Literal} to ensure it conforms to RDF/N-Quads rules.
     * Specifically checks for consistency between language tags and the rdf:langString datatype.
     * Only called if {@code config.isStrictMode()} is enabled.
     *
     * @param literal The {@link Literal} to validate.
     * @throws IllegalArgumentException if the literal is invalid (e.g., language tag with wrong datatype,
     *                                  or rdf:langString literal missing a language tag).
     */
    private void validateLiteral(Literal literal) {
        IRI datatype = literal.getDatatype();

        if (literal.getLanguage().isPresent()) {
            if (datatype == null || !datatype.stringValue().equals(SerializationConstants.RDF_LANGSTRING)) {
                throw new IllegalArgumentException(
                        "Literal with language tag must use rdf:langString datatype. Found: " + (datatype != null ? datatype.stringValue() : "null"));
            }
        } else {
            if (datatype != null && datatype.stringValue().equals(SerializationConstants.RDF_LANGSTRING)) {
                throw new IllegalArgumentException(
                        "rdf:langString literal must have a language tag.");
            }
        }
    }

    /**
     * Validates an {@link IRI} to ensure it conforms to N-Quads rules.
     * Checks if the IRI string contains characters that are not allowed in N-Quads
     * unescaped form, such as spaces, quotes, or angle brackets.
     * Only called if {@code config.isStrictMode()} and {@code config.validateURIs()} are enabled.
     *
     * @param iri The {@link IRI} to validate.
     * @throws IllegalArgumentException if the IRI contains invalid characters.
     */
    private void validateIRI(IRI iri) {
        String iriString = iri.stringValue();
        if (iriString.contains(SerializationConstants.SPACE) ||
                iriString.contains(SerializationConstants.QUOTE) ||
                iriString.contains(SerializationConstants.LT) ||
                iriString.contains(SerializationConstants.GT)) {
            throw new IllegalArgumentException("IRI contains illegal characters (space, quote, angle brackets) for N-Quads unescaped form: " + iriString);
        }
    }
}