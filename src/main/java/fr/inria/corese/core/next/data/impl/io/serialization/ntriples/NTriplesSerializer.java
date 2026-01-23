package fr.inria.corese.core.next.data.impl.io.serialization.ntriples;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.io.IOOptions;
import fr.inria.corese.core.next.data.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.data.io.serializer.BlankNodeIdGenerationOptions;
import fr.inria.corese.core.next.data.io.serializer.LineEndingOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.option.AbstractNFamilyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.impl.io.serialization.base.AbstractLineBasedSerializer;

/**
 * Serializes a Corese {@link Model} into N-Triples format.
 * This class extends {@link AbstractLineBasedSerializer} to provide
 * N-Triples specific serialization behavior.
 */
public class NTriplesSerializer extends AbstractLineBasedSerializer {

    /**
     * Logger for this class, used for logging potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(NTriplesSerializer.class);

    /**
     * Constructs a new {@code NTriplesSerializer} instance with the specified model and default configuration.
     * The default configuration is obtained from {@link NTriplesSerializerOptions#defaultConfig()}.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NTriplesSerializer(Model model) {
        super(model, NTriplesSerializerOptions.defaultConfig());
    }

    /**
     * Constructs a new {@code NTriplesSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link NTriplesSerializerOptions} to use for serialization. Must not be null.
     *               This config object should be an instance of {@code NTriplesConfig} or a subclass thereof.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NTriplesSerializer(Model model, IOOptions config) {
        this(model);
        Objects.requireNonNull(config, "NTriplesConfig cannot be null");
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
     * Retrieves the RDF format supported by this serializer, which is N-TRIPLES.
     *
     * @return {@link RDFFormat#NTRIPLES}.
     */
    @Override
    public RDFFormat getFormat() {
        return RDFFormat.NTRIPLES;
    }

    /**
     * Writes the context (named graph) part of a statement.
     * For N-Triples, contexts are not supported, so this method logs a warning
     * if a context is present and does nothing.
     *
     * @param writer the {@link Writer} to which the context will be written.
     * @param stmt   the {@link Statement} whose context should be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void writeContext(Writer writer, Statement stmt) throws IOException {
        Resource context = stmt.getContext();

        if (context != null && logger.isWarnEnabled()) {
            logger.warn("N-Triples format does not support named graphs. Context '{}' will be ignored for statement: {}",
                    context.stringValue(), stmt);
        }
    }
}
