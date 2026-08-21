package fr.inria.corese.core.next.query.impl.sparql.parser;

/**
 * Internal base class that stores the configuration shared by query parsers.
 *
 * <p>This implementation detail lives beside the concrete parsers; consumers
 * pipeline code should use {@link QueryParser}, not extend this class.</p>
 */
abstract class AbstractQueryParser implements QueryParser {

    private SparqlParserOptions config;

    protected AbstractQueryParser(SparqlParserOptions config) {
        setConfig(config);
    }

    @Override
    public final void setConfig(SparqlParserOptions options) {
        this.config = options != null ? options : new SparqlParserOptions.Builder().build();
    }

    @Override
    public final SparqlParserOptions getConfig() {
        return config;
    }
}
