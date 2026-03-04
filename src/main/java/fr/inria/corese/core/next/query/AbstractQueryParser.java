package fr.inria.corese.core.next.query;

/**
 * Base implementation of the {@link QueryParser} interface.
 *
 * <p>This abstract class provides common configuration handling logic
 * for concrete query parser implementations. It stores and manages
 * a {@link QueryOptions} instance that controls parsing behavior
 * (e.g., base IRI resolution, strict mode, error handling strategy).</p>
 *
 * <p>Subclasses are responsible for implementing the actual parsing
 * logic for the supported query language (e.g., SPARQL) and producing
 *
 * <p>This class centralizes configuration management so that all
 * parser implementations share consistent option handling behavior.</p>
 *
 * @see QueryParser
 * @see QueryOptions
 */
public abstract class AbstractQueryParser implements QueryParser {

    /**
     * The current configuration used by this parser.
     */
    private QueryOptions config;

    /**
     * Constructs a new parser with the given configuration.
     *
     * @param config the initial parsing configuration
     */
    protected AbstractQueryParser(QueryOptions config) {
        this.config = config;
    }

    /**
     * Sets the configuration options for this parser.
     *
     * @param options the parsing configuration
     */
    @Override
    public void setConfig(QueryOptions options) {
        this.config = options;
    }

    /**
     * Returns the current configuration options of this parser.
     *
     * @return the parsing configuration
     */
    @Override
    public QueryOptions getConfig() {
        return config;
    }
}
