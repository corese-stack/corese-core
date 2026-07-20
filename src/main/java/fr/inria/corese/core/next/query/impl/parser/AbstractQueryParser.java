package fr.inria.corese.core.next.query.impl.parser;

/**
 * Internal base class that stores the configuration shared by query parsers.
 *
 * <p>This implementation detail lives beside the concrete parsers; consumers
 * pipeline code should use {@link QueryParser}, not extend this class.</p>
 */
abstract class AbstractQueryParser implements QueryParser {

    private QueryOptions config;

    protected AbstractQueryParser(QueryOptions config) {
        this.config = config;
    }

    @Override
    public final void setConfig(QueryOptions options) {
        this.config = options;
    }

    @Override
    public final QueryOptions getConfig() {
        return config;
    }
}
