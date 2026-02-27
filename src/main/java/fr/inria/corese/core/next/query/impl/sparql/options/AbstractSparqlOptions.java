package fr.inria.corese.core.next.query.impl.sparql.options;

import fr.inria.corese.core.next.query.QueryOptions;

/**
 * Abstract base for SPARQL options using Builder pattern.
 */
public abstract class AbstractSparqlOptions implements QueryOptions {
    /**
     * Builder class for constructing instances of QueryOptions.
     */
    public abstract static class Builder<T extends QueryOptions> {
        protected Builder() {}
        public abstract T build();
    }
}
