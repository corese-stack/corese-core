package fr.inria.corese.core.next.api.base.model.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializes a Corese {@link Model} into N-Triples format.
 * This class provides a method to write the statements of a model to a given {@link Writer}
 * according to the N-Triples specification.
 */
public class NTriplesFormat {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NTriplesFormat.class);

    /**
     * A constant string representing a single space character, used for separating elements in N-Triples output.
     */
    private static final String SPACE = " ";

    /**
     * A constant string representing the prefix for blank nodes in N-Triples format.
     */
    private static final String SPACE_POINT = " .";

    private final Model model;
    private NFormatConfig config;

    /**
     * Constructs a new {@code NTriplesFormat} instance with the specified model and default configuration.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NTriplesFormat(Model model) {

        this(model, new NFormatConfig.Builder().build());
    }

    /**
     * Constructs a new {@code NTriplesFormat} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link NFormatConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NTriplesFormat(Model model, NFormatConfig config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    }

    /**
     * Writes the model to the given writer in N-Triples format.
     * Each statement in the model is written on a new line, terminated by a dot and a newline character.
     *
     * @param writer the {@link Writer} to which the N-Triples output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    public void write(Writer writer) throws SerializationException {
        try {
            for (Statement stmt : model) {
                writeStatement(writer, stmt);
                writer.write("\n");
            }
        } catch (IOException e) {
            logger.error("An I/O error occurred during N-Triples serialization: {}", e.getMessage(), e);
            throw new SerializationException("Failed to write", "NTriples", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data encountered during N-Triples serialization: {}", e.getMessage(), e);
            throw new SerializationException("Invalid data: " + e.getMessage(), "NTriples", e);
        }
    }

    /**
     * Writes a single {@link Statement} to the writer in N-Triples format.
     * The statement is written as "$subject $predicate $object ."
     * If the statement has a context, it is written as "$subject $predicate $object $context ."
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write.
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
     * Handles literals, resources (blank nodes and IRIs), and other value types by calling their {@code stringValue()} method.
     *
     * @param writer the {@link Writer} to which the value will be written.
     * @param value  the {@link Value} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the provided value is null.
     */
    private void writeValue(Writer writer, Value value) throws IOException {
        if (value == null) {
            logger.warn("Encountered a null value where a non-null value was expected for N-Triples serialization.");
            throw new IllegalArgumentException("Value cannot be null in N-Triples format");
        }

        if (value.isLiteral()) {
            writer.write(value.stringValue());
        } else if (value.isResource()) {
            writeResource(writer, (Resource) value);
        } else {
            writer.write(value.stringValue());
        }
    }

    /**
     * Writes a {@link Resource} (either a blank node or an IRI) to the writer.
     * Blank nodes are prefixed with "_:", and IRIs are written directly.
     *
     * @param writer   the {@link Writer} to which the resource will be written.
     * @param resource the {@link Resource} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeResource(Writer writer, Resource resource) throws IOException {
        if (resource.isResource()) {
            writer.write(config.getBlankNodePrefix());
        } else {
            writeIRI(writer, (IRI) resource);
        }
    }

    /**
     * Writes an {@link IRI} to the writer.
     * The IRI's string representation (including angle brackets if necessary) is written directly.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri    the {@link IRI} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeIRI(Writer writer, IRI iri) throws IOException {
        writer.write(iri.stringValue());
    }
}