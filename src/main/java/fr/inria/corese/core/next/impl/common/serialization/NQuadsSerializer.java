package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.base.AbstractLineBasedSerializer;
import fr.inria.corese.core.next.impl.common.serialization.config.SerializerConfig;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

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
     * The default configuration is obtained from {@link SerializerConfig#nquadsConfig()}.
     *
     * @param model the {@link Model} to be serialized. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public NQuadsSerializer(Model model) {
        this(model, SerializerConfig.nquadsConfig());
    }

    /**
     * Constructs a new {@code NQuadsSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link SerializationConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or config is null.
     */
    public NQuadsSerializer(Model model, SerializationConfig config) {
        super(model, (SerializerConfig) config);
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return "N-Quads"
     */
    @Override
    protected String getFormatName() {
        return "N-Quads";
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