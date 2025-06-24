package fr.inria.corese.core.next.impl.io.serialization.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.serialization.FormatSerializer;
import fr.inria.corese.core.next.impl.common.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.FormatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializes a Corese {@link Model} into N-Quads format.
 * This class provides a method to write the statements (quads) of a model to a given {@link Writer}
 * according to the N-Quads specification, including support for named graphs (contexts).
 */
public class NQuadsFormat implements FormatSerializer {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NQuadsFormat.class);

    private final Model model;
    private final FormatConfig config;

    /**
     * Constructs a new {@code NQuadsFormat} instance with the specified model and default configuration.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NQuadsFormat(Model model) {
        this(model, new FormatConfig.Builder().build());
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
     * The statement is written as "$subject $predicate $object $context ." if a context is present,
     * or "$subject $predicate $object ." if no context is present (default graph).
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
        if (context != null) {
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, context);
        }

        writer.write(SerializationConstants.SPACE_POINT);
    }

    /**
     * Writes a single {@link Value} to the writer.
     * Handles literals, blank nodes, and IRIs.
     *
     * @param writer the {@link Writer} to which the value will be written.
     * @param value  the {@link Value} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the provided value is null or an unsupported type.
     */
    private void writeValue(Writer writer, Value value) throws IOException {
        validateValue(value);

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
     * Handles plain literals, language-tagged literals, and typed literals.
     *
     * @param writer  the {@link Writer} to which the literal will be written.
     * @param literal the {@link Literal} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeLiteral(Writer writer, Literal literal) throws IOException {
        writer.write(SerializationConstants.QUOTE);
        writer.write(escapeLiteral(literal.stringValue()));
        writer.write(SerializationConstants.QUOTE);

        // Gestion du langage
        literal.getLanguage().ifPresent(lang -> {
            try {
                writer.write(SerializationConstants.AT_SIGN + lang);
            } catch (IOException e) {
                throw new UncheckedIOException("Error writing language tag", e);
            }
        });

        if (!literal.getLanguage().isPresent()) {
            IRI datatype = literal.getDatatype();
            if (datatype != null && !datatype.stringValue().equals(SerializationConstants.XSD_STRING)) {
                writer.write(SerializationConstants.DATATYPE_SEPARATOR);
                writeIRI(writer, datatype);
            }
        }
    }

    /**
     * Writes an {@link IRI} to the writer.
     * The IRI's string representation must be enclosed in angle brackets for N-Quads.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri    the {@link IRI} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeIRI(Writer writer, IRI iri) throws IOException {
        writer.write(SerializationConstants.LT);
        writer.write(escapeIRI(iri.stringValue()));
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
        writer.write(config.getBlankNodePrefix());
        writer.write(blankNode.stringValue());
    }

    /**
     * Validates and potentially escapes an IRI string.
     * Throws an {@link IllegalArgumentException} if the IRI contains characters
     * that are not allowed in N-Quads unescaped form (like spaces, quotes, angle brackets).
     *
     * @param iri The string value of the IRI to validate and escape.
     * @return The validated and potentially escaped IRI string.
     * @throws IllegalArgumentException if the IRI string is invalid.
     */
    private String escapeIRI(String iri) {

        if (iri.contains(SerializationConstants.SPACE) || iri.contains(SerializationConstants.QUOTE) ||
                iri.contains(SerializationConstants.LT) || iri.contains(SerializationConstants.GT)) {
            throw new IllegalArgumentException("Invalid IRI: contains illegal characters for N-Quads unescaped form: " + iri);
        }
        return iri;
    }

    /**
     * Escape special characters in N-Quads string literals.
     * Handles backslash, double quote, and common control characters.
     * Unicode escape sequences are used for unprintable characters.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for N-Quads literal.
     */
    private String escapeLiteral(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
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
                    if (c <= 0x1F || c == 0x7F) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Validates RDF values before serialization to ensure they conform to N-Quads rules.
     *
     * @param value The {@link Value} to validate.
     * @throws IllegalArgumentException if the value is null or invalid.
     */
    private void validateValue(Value value) {
        if (value == null) {
            logger.warn("Encountered a null value where a non-null value was expected for N-Quads serialization.");
            throw new IllegalArgumentException("Value cannot be null in N-Quads format");
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
     * unescaped form, such as spaces.
     *
     * @param iri The {@link IRI} to validate.
     * @throws IllegalArgumentException if the IRI contains spaces or is otherwise invalid.
     */
    private void validateIRI(IRI iri) {
        if (iri.stringValue().contains(SerializationConstants.SPACE)) {
            throw new IllegalArgumentException("IRI contains spaces, which is not allowed in N-Quads unescaped form: " + iri.stringValue());
        }
    }
}