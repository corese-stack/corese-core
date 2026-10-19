package fr.inria.corese.core.next.query.api.diagnostics;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Query result and access to immutable measurements of its progress. For a lazy
 * SELECT, profile() snapshots the work performed so far; consume or close the
 * result before requesting final measurements. Client idle time is excluded.
 * This wrapper owns no resources. The caller must close tuple and graph results.
 * Both result consumption and profile access belong to the evaluation thread.
 *
 * @param <T> query result type
 */
public final class QueryExecution<T> {
    private final T result;
    private final Supplier<QueryExecutionProfile> profiles;

    /**
     * Creates an execution with a non-null result and profile source.
     *
     * @param result evaluated result owned by the caller
     * @param profiles source of immutable progress measurements
     * @throws NullPointerException if either argument is null
     */
    public QueryExecution(T result, Supplier<QueryExecutionProfile> profiles) {
        this.result = Objects.requireNonNull(result, "result");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
    }

    /** @return the evaluated result, which must be closed when resource-bearing */
    public T result() {
        return result;
    }

    /** @return an immutable snapshot of the work completed so far */
    public QueryExecutionProfile profile() {
        return profiles.get();
    }
}
