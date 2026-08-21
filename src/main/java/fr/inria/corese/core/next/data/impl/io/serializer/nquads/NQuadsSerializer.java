package fr.inria.corese.core.next.data.impl.io.serializer.nquads;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeIdGenerationOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.support.AbstractLineBasedSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.option.AbstractNFamilyOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.support.SerializationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializes a Corese {@link Model} into N-Quads format.
 * This class extends {@link AbstractLineBasedSerializer} to provide
 * N-Quads specific serialization behavior.
 */
public class NQuadsSerializer extends AbstractLineBasedSerializer {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NQuadsSerializer.class);

    /**
     * Constructs a new {@code NQuadsSerializer} instance with the specified model and default N-Quads configuration.
     * The default configuration is obtained from {@link NQuadsSerializerOptions#defaultConfig()}.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NQuadsSerializer(Model model) {
        super(model, NQuadsSerializerOptions.defaultConfig());
    }

    /**
     * Constructs a new {@code NQuadsSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link NQuadsSerializerOptions} to use for serialization. Must not be null.
     *               This config object should be an instance of {@code NQuadsConfig} or a subclass thereof.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NQuadsSerializer(Model model, IOOptions config) {
        this(model);
        Objects.requireNonNull(config, "NQuadsConfig cannot be null");
        if(config instanceof AbstractNFamilyOptions nFamilyOptions) {
            this.config = nFamilyOptions;
        } else {
            NTriplesSerializerOptions.Builder optionBuilder = new NTriplesSerializerOptions.Builder();
            if(config instanceof BaseIRIOptions baseIRIOptions) {
                optionBuilder.baseIRI(baseIRIOptions.getBaseIRI());
            }
            if(config instanceof LineEndingOptions lineEndingOptions) {
                optionBuilder.lineEnding(lineEndingOptions.getLineEnding());
            }
            if(config instanceof BlankNodeIdGenerationOptions blankNodeIdGenerationOptions) {
                optionBuilder.stableBlankNodeIds(blankNodeIdGenerationOptions.stableBlankNodeIds());
            }
            this.config = optionBuilder.build();
        }
    }

    /**
     * Retrieves the RDF format supported by this serializer, which is N-Quads.
     *
     * @return {@link RDFFormat#NQUADS}.
     */
    @Override
    public RDFFormat getFormat() {
        return RDFFormat.NQUADS;
    }

    /**
     * Writes the context (named graph) part of a statement.
     * For N-Quads, the context is written as the fourth component if present
     * and {@code config.includeContext()} is true.
     *
     * @param writer the {@link Writer} to which the context will be written.
     * @param stmt   the {@link Statement} whose context should be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void writeContext(Writer writer, Statement stmt) throws IOException {
        Resource context = stmt.getContext();
        if (context != null && config.includeContext()) {
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, context);
        } else if (context != null && logger.isWarnEnabled()) {
            logger.warn("Context '{}' will be ignored for statement: {} because includeContext is false in configuration.",
                    context.stringValue(), stmt);
        }
    }
}
