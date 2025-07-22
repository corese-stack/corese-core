package fr.inria.corese.core.next.impl.io.serialization.base;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.option.AbstractSerializerOption;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

/**
 * Base class for line-based RDF serializers (N-Triples, N-Quads).
 * Contains all the common logic for writing statements line by line.
 * Subclasses only need to implement how to handle the context part.
 */
public abstract class AbstractLineBasedSerializer implements RDFSerializer {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractLineBasedSerializer.class);

    protected final Model model;
    protected final AbstractSerializerOption config;

    /**
     * Constructs a new line-based serializer.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link AbstractSerializerOption} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or config is null.
     */
    protected AbstractLineBasedSerializer(Model model, AbstractSerializerOption config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    }

    /**
     * Writes the model to the given writer.
     * Each statement in the model is written on a new line, terminated by a dot and a newline character.
     *
     * @param writer the {@link Writer} to which the output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            Set<Resource> processedNodes = preprocessModel();

            for (Statement stmt : model) {
                if (shouldProcess(stmt, processedNodes)) {
                    writeStatement(bufferedWriter, stmt);
                }
            }

        } catch (IOException e) {
            throw new SerializationException(getFormatName() + " serialization failed", getFormatName(), e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid " + getFormatName() + " data: " + e.getMessage(), getFormatName(), e);
        }
    }

    /**
     * Returns the name of the format for error messages.
     */
    protected abstract String getFormatName();

    /**
     * Handles writing the context part of a statement.
     * This is where N-Triples and N-Quads differ.
     *
     * @param writer the writer to write to
     * @param stmt the statement containing the context
     * @throws IOException if an I/O error occurs
     */
    protected abstract void writeContext(Writer writer, Statement stmt) throws IOException;

    private Set<Resource> preprocessModel() {
        return Collections.emptySet();
    }

    private boolean shouldProcess(Statement stmt, Set<Resource> processedNodes) {
        return !processedNodes.contains(stmt.getSubject());
    }

    /**
     * Writes a single {@link Statement} to the writer.
     * The statement is written as "$subject $predicate $object" followed by context handling.
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

        // Let subclass handle the context
        writeContext(writer, stmt);

        if (config.trailingDot()) {
            writer.write(SerializationConstants.SPACE);
            writer.write(SerializationConstants.POINT);
        }

        writer.write(config.getLineEnding());
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
    protected void writeValue(Writer writer, Value value) throws IOException {
        validateValue(value);

        if (value.isLiteral()) {
            writeLiteral(writer, (Literal) value);
        } else if (value.isResource()) {
            if (value.isIRI()) {
                writeIRI(writer, (IRI) value);
            } else if (value.isBNode()) {
                writeBlankNode(writer, (Resource) value);
            } else {
                throw new IllegalArgumentException("Unsupported resource type for " + getFormatName() + " serialization: " + value.getClass().getName());
            }
        } else {
            throw new IllegalArgumentException("Unsupported value type for " + getFormatName() + " serialization: " + value.getClass().getName());
        }
    }

    /**
     * Writes a {@link Literal} to the writer.
     * Applies escaping and datatype/language tag rules based on configuration.
     *
     * @param writer  the {@link Writer} to which the literal will be written.
     * @param literal the {@link Literal} to write.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeLiteral(Writer writer, Literal literal) throws IOException {
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
     * The IRI's string representation must be enclosed in angle brackets.
     * Applies URI validation based on configuration.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri    the {@link IRI} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the IRI is invalid (e.g., contains spaces) and strict mode/URI validation is enabled.
     */
    protected void writeIRI(Writer writer, IRI iri) throws IOException {
        if (config.isStrictMode() && config.validateURIs()) {
            validateIRI(iri);
        }
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
    protected void writeBlankNode(Writer writer, Resource blankNode) throws IOException {
        writer.write(SerializationConstants.BNODE_PREFIX);
        writer.write(blankNode.stringValue());
    }

    /**
     * Validates and potentially escapes an IRI string.
     * Throws an {@link IllegalArgumentException} if the IRI contains characters
     * that are not allowed in unescaped form (like spaces, quotes, angle brackets).
     * This method is called if strictMode and validateURIs are enabled.
     *
     * @param iri The string value of the IRI to validate and escape.
     * @return The validated and potentially escaped IRI string.
     * @throws IllegalArgumentException if the IRI string is invalid.
     */
    protected String escapeIRI(String iri) {
        if (iri.contains(SerializationConstants.SPACE) ||
                iri.contains(SerializationConstants.QUOTE) ||
                iri.contains(SerializationConstants.LT) ||
                iri.contains(SerializationConstants.GT)) {

            throw new IllegalArgumentException("Invalid IRI for " + getFormatName() + " (contains illegal characters inside '<>'): " + iri);
        }

        return config.escapeUnicode() ? escapeUnicodeString(iri) : iri;
    }

    /**
     * Escape special characters in string literals.
     * Handles backslash, double quote, and common control characters.
     * Unicode escape sequences are used for unprintable characters if `escapeUnicode` is true.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for literal.
     */
    protected String escapeLiteral(String value) {
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
                case '\b': // backspace
                    sb.append(SerializationConstants.BACK_SLASH).append('b');
                    break;
                case '\f': // form feed
                    sb.append(SerializationConstants.BACK_SLASH).append('f');
                    break;
                case '"':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.QUOTE);
                    break;
                case '\\':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.BACK_SLASH);
                    break;
                default:
                    if (config.escapeUnicode()) {
                        if (c <= 0x1F || c == 0x7F) {
                            sb.append(String.format("\\u%04X", (int) c));
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
     * Escapes non-ASCII and control characters into Unicode escape sequences.
     * This is a helper for `escapeIRI` and potentially `escapeLiteral`
     * if `escapeUnicode` is true in config.
     *
     * @param value The string to escape.
     * @return The string with Unicode characters escaped.
     */
    protected String escapeUnicodeString(String value) {
        StringBuilder sb = new StringBuilder();
        int len = value.length(); // Cache length for invariant stop condition
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0xFFFF)) { // Basic Multilingual Plane characters and control characters
                sb.append(String.format("\\u%04X", (int) c));
            } else if (Character.isHighSurrogate(c)) { // Supplementary characters
                int codePoint = value.codePointAt(i);
                if (Character.isValidCodePoint(codePoint)) {
                    sb.append(String.format("\\U%08X", codePoint));
                    i++; // Skip the low surrogate char
                } else {
                    sb.append(c); // Append invalid surrogate char directly
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Validates RDF values before serialization to ensure they conform to rules.
     * Only called if strictMode is enabled.
     *
     * @param value The {@link Value} to validate.
     * @throws IllegalArgumentException if the value is null or invalid based on rules.
     */
    protected void validateValue(Value value) {
        if (value == null) {
            logger.warn("Encountered a null value where a non-null value was expected for " + getFormatName() + " serialization. This will result in an IllegalArgumentException if strict mode is enabled.");
            throw new IllegalArgumentException("Value cannot be null in " + getFormatName() + " format when strictMode is enabled.");
        }

        if (value.isLiteral()) {
            validateLiteral((Literal) value);
        } else if (value.isIRI()) {
            validateIRI((IRI) value);
        }
    }

    /**
     * Validates a {@link Literal} to ensure it conforms to RDF rules.
     * Specifically checks for consistency between language tags and the rdf:langString datatype.
     * Only called if strictMode is enabled.
     *
     * @param literal The {@link Literal} to validate.
     * @throws IllegalArgumentException if the literal is invalid (e.g., language tag with wrong datatype,
     * or rdf:langString literal missing a language tag).
     */
    protected void validateLiteral(Literal literal) {
        IRI datatype = literal.getDatatype();

        if (literal.getLanguage().isPresent()) {
            if (datatype == null || !datatype.stringValue().equals(RDF.LANGSTRING.getIRI().stringValue())) {
                throw new IllegalArgumentException(
                        "Literal with language tag must use rdf:langString datatype. Found: " + (datatype != null ? datatype.stringValue() : "null"));
            }
        } else {
            if (datatype != null && datatype.stringValue().equals(RDF.LANGSTRING.getIRI().stringValue())) {
                throw new IllegalArgumentException(
                        "rdf:langString literal must have a language tag.");
            }
        }
    }

    /**
     * Validates an {@link IRI} to ensure it conforms to rules.
     * Checks if the IRI string contains characters that are not allowed
     * unescaped form, such as spaces, quotes, or angle brackets.
     * Only called if strictMode and validateURIs are enabled.
     *
     * @param iri The {@link IRI} to validate.
     * @throws IllegalArgumentException if the IRI contains invalid characters.
     */
    protected void validateIRI(IRI iri) {
        String iriString = iri.stringValue();
        if (iriString.contains(SerializationConstants.SPACE) ||
                iriString.contains(SerializationConstants.QUOTE) ||
                iriString.contains(SerializationConstants.LT) ||
                iriString.contains(SerializationConstants.GT)) {
            throw new IllegalArgumentException("IRI contains illegal characters (space, quote, angle brackets) for " + getFormatName() + " unescaped form: " + iriString);
        }
    }
}
