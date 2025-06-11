package fr.inria.corese.core.next.api.base.model.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializes a Corese {@link Model} into N-Quads format.
 * This class provides a method to write the statements (quads) of a model to a given {@link Writer}
 * according to the N-Quads specification, including support for named graphs (contexts).
 */
public class NQuadsFormat {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NQuadsFormat.class);

    /**
     * A constant string representing a single space character, used for separating elements in N-Quads output.
     */
    private static final String SPACE = " ";

    /**
     * A constant string representing the end of a statement in N-Quads format (space, dot, newline).
     */
    private static final String SPACE_POINT = " .\n";

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
     * @param model the {@link Model} to be serialized. Must not be null.
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
    public void write(Writer writer) throws SerializationException {
        try {
            for (Statement stmt : model) {
                writeStatement(writer, stmt);
            }
            writer.flush();
        } catch (IOException e) {
            logger.error("An I/O error occurred during N-Quads serialization: {}", e.getMessage(), e);
            throw new SerializationException("Failed to write", "NQuads", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data encountered during N-Quads serialization: {}", e.getMessage(), e);
            throw new SerializationException("Invalid data: " + e.getMessage(), "NQuads", e);
        }
    }

    /**
     * Writes a single {@link Statement} (quad) to the writer in N-Quads format.
     * The statement is written as "$subject $predicate $object $context ." if a context is present,
     * or "$subject $predicate $object ." if no context is present (default graph).
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt the {@link Statement} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeStatement(Writer writer, Statement stmt) throws IOException {
        writeValue(writer, stmt.getSubject());
        writer.write(SPACE);
        writeValue(writer, stmt.getPredicate());
        writer.write(SPACE);
        writeValue(writer, stmt.getObject());

        Resource context = stmt.getContext();
        if (context != null) {
            writer.write(SPACE);
            writeValue(writer, context);
        }

        writer.write(SPACE_POINT);
    }

    /**
     * Writes a single {@link Value} to the writer.
     * Handles literals, blank nodes, and IRIs.
     *
     * @param writer the {@link Writer} to which the value will be written.
     * @param value the {@link Value} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the provided value is null or an unsupported type.
     */
    private void writeValue(Writer writer, Value value) throws IOException {
        if (value == null) {
            logger.warn("Encountered a null value where a non-null value was expected for N-Quads serialization.");
            throw new IllegalArgumentException("Value cannot be null in N-Quads format");
        }

        if (value.isLiteral()) {
            writer.write(value.stringValue());
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
     * Writes an {@link IRI} to the writer.
     * The IRI's string representation must be enclosed in angle brackets for N-Quads.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri the {@link IRI} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeIRI(Writer writer, IRI iri) throws IOException {
        writer.write("<");
        writer.write(iri.stringValue());
        writer.write(">");
    }

    /**
     * Writes a blank node to the writer.
     * Blank nodes are prefixed with "_:", and the identifier is appended.
     *
     * @param writer the {@link Writer} to which the blank node will be written.
     * @param blankNode the {@link Resource} representing the blank node.
     * @throws IOException if an I/O error occurs.
     */
    private void writeBlankNode(Writer writer, Resource blankNode) throws IOException {
        writer.write(config.getBlankNodePrefix());
        writer.write(blankNode.stringValue());
    }
}