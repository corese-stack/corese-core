package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.base.AbstractLineBasedSerializer;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * Serializes a Corese {@link Model} into an RDFC-1.0 canonical RDF format.
 * This serializer is designed to integrate with the W3C RDFC-1.0 algorithm
 * to ensure a deterministic output by re-labeling blank nodes and sorting all statements
 * according to the specification. The output format is canonicalized N-Quads.
 * <p>
 * This implementation now acts as a wrapper, preparing the model for a dedicated
 * RDFC-1.0 canonicalization component and then writing the resulting canonical statements.
 */
public class CanonicalSerializer extends AbstractLineBasedSerializer implements RDFSerializer {

    private final CanonicalOption config;
    private final Rdfc10Canonicalizer canonicalizer;
    private final Model model;

    /**
     * Constructs a new CanonicalSerializer.
     * This constructor is now adapted to be used by the DefaultSerializerFactory.
     *
     * @param model         The model to be serialized.
     * @param config        The configuration options for the canonicalization process.
     * @param valueFactory  The factory for creating RDF values.
     * @param canonicalizer The canonicalizer component to use.
     */
    public CanonicalSerializer(Model model, CanonicalOption config, ValueFactory valueFactory, Rdfc10Canonicalizer canonicalizer) {
        super(model, config);
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
        this.canonicalizer = Objects.requireNonNull(canonicalizer);
    }

    @Override
    public String getFormatName() {
        return "RDFC-1.0";
    }

    /**
     * Serializes the model into the specified writer using the RDFC-1.0 canonical format.
     * The model is first canonicalized by the internal canonicalizer component, and then
     * the resulting statements are written line by line to the writer.
     *
     * @param writer the {@link Writer} to which the serialized model will be written.
     * @throws SerializationException if serialization fails due to an I/O error or invalid data.
     */
    public void write(Writer writer) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            List<Statement> canonicalStatements = canonicalizer.canonicalize(model);

            for (Statement stmt : canonicalStatements) {
                writeCanonicalStatement(bufferedWriter, stmt);
            }

        } catch (IOException e) {
            throw new SerializationException(getFormatName() + " serialization failed", getFormatName(), e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid " + getFormatName() + " data: " + e.getMessage(), getFormatName(), e);
        }
    }

    /**
     * Writes the context (graph URI) of a statement to the writer.
     *
     * @param writer the {@link BufferedWriter} to which the context will be written.
     * @param stmt   the statement whose context will be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void writeContext(Writer writer, Statement stmt) throws IOException {
        Resource context = stmt.getContext();
        if (context != null) {
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, context);
        }
    }

    /**
     * Writes a single canonical {@link Statement} to the writer.
     * This method is designed to write a statement that has already been processed
     * by the RDFC-1.0 canonicalization algorithm.
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write (already canonicalized).
     * @throws IOException if an I/O error occurs.
     */
    private void writeCanonicalStatement(Writer writer, Statement stmt) throws IOException {
        writeValue(writer, stmt.getSubject());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getPredicate());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getObject());

        writeContext(writer, stmt);

        if (config.trailingDot()) {
            writer.write(SerializationConstants.SPACE);
            writer.write(SerializationConstants.POINT);
        }

        writer.write(config.getLineEnding());
    }


}
