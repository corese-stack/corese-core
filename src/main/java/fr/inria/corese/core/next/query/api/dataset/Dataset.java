package fr.inria.corese.core.next.query.api.dataset;

import fr.inria.corese.core.next.data.api.term.IRI;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable SPARQL dataset override.
 *
 * <p>Default graphs correspond to {@code FROM}; named graphs correspond to
 * {@code FROM NAMED}. Supplying a dataset to a query replaces all dataset
 * clauses from the query text, including when either set is empty.</p>
 *
 * <pre>{@code
 * Dataset dataset = Dataset.builder()
 *         .defaultGraph(valueFactory.createIRI("https://example.org/data"))
 *         .namedGraph(valueFactory.createIRI("https://example.org/named"))
 *         .build();
 * }</pre>
 */
public final class Dataset {

    private static final Dataset EMPTY = new Dataset(Set.of(), Set.of());

    private final Set<IRI> defaultGraphs;
    private final Set<IRI> namedGraphs;

    private Dataset(Set<IRI> defaultGraphs, Set<IRI> namedGraphs) {
        this.defaultGraphs = immutableCopy(defaultGraphs);
        this.namedGraphs = immutableCopy(namedGraphs);
    }

    /** Returns a dataset with no default or named graphs. */
    public static Dataset empty() {
        return EMPTY;
    }

    /** Returns a builder for an immutable dataset. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the ordered set of default graph IRIs. */
    public Set<IRI> getDefaultGraphs() {
        return defaultGraphs;
    }

    /** Returns the ordered set of named graph IRIs. */
    public Set<IRI> getNamedGraphs() {
        return namedGraphs;
    }

    /** Returns whether both graph sets are empty. */
    public boolean isEmpty() {
        return defaultGraphs.isEmpty() && namedGraphs.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Dataset other
                && defaultGraphs.equals(other.defaultGraphs)
                && namedGraphs.equals(other.namedGraphs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultGraphs, namedGraphs);
    }

    @Override
    public String toString() {
        return "Dataset{defaultGraphs=" + defaultGraphs + ", namedGraphs=" + namedGraphs + '}';
    }

    private static Set<IRI> immutableCopy(Set<IRI> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    /** Builder for {@link Dataset}. Duplicate IRIs are ignored. */
    public static final class Builder {

        private final Set<IRI> defaultGraphs = new LinkedHashSet<>();
        private final Set<IRI> namedGraphs = new LinkedHashSet<>();

        private Builder() {
        }

        /** Adds a default graph. */
        public Builder defaultGraph(IRI graph) {
            defaultGraphs.add(Objects.requireNonNull(graph, "graph"));
            return this;
        }

        /** Adds a named graph. */
        public Builder namedGraph(IRI graph) {
            namedGraphs.add(Objects.requireNonNull(graph, "graph"));
            return this;
        }

        /** Builds an immutable snapshot. */
        public Dataset build() {
            if (defaultGraphs.isEmpty() && namedGraphs.isEmpty()) {
                return Dataset.empty();
            }
            return new Dataset(defaultGraphs, namedGraphs);
        }
    }
}
