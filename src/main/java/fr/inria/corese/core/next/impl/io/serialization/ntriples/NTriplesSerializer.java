package fr.inria.corese.core.next.impl.io.serialization.ntriples;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.impl.io.serialization.base.AbstractLineBasedSerializer;

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
     * The default configuration is obtained from {@link NTriplesOption#defaultConfig()}.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NTriplesSerializer(Model model) {
        this(model, NTriplesOption.defaultConfig());
    }

    /**
     * Constructs a new {@code NTriplesSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link NTriplesOption} to use for serialization. Must not be null.
     *               This config object should be an instance of {@code NTriplesConfig} or a subclass thereof.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NTriplesSerializer(Model model, NTriplesOption config) {
        super(model, config);
        Objects.requireNonNull(config, "NTriplesConfig cannot be null");
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return "N-Triples"
     */
    @Override
    protected String getFormatName() {
        return "N-Triples";
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
