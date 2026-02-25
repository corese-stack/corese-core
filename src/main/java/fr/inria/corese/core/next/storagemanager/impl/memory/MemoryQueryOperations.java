package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.storagemanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.storagemanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;

import java.util.stream.Stream;

/**
 * Query operations implementation for {@link MemoryStorageManager}.
 */
public class MemoryQueryOperations implements QueryOperations {

    private final MemoryAdapter adapter;

    /**
     * Constructs a new MemoryQueryOperations.
     *
     * @param adapter the MemoryAdapter for data access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public MemoryQueryOperations(MemoryAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("MemoryAdapter cannot be null");
        this.adapter = adapter;
    }

    /**
     * Queries for statements matching the given pattern.
     *
     * @param pattern the statement pattern to match (must not be null)
     * @return a stream of matching statements (must be closed)
     * @throws IllegalArgumentException if pattern is null
     * @throws StorageException         if the query fails
     */
    @Override
    public Stream<Statement> query(StatementPattern pattern) throws StorageException {
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");
        try {
            return adapter.find(
                    pattern.getSubject(),
                    pattern.getPredicate(),
                    pattern.getObject(),
                    pattern.getContexts()
            ).stream();
        } catch (Exception e) {
            throw new StorageException(ErrorCode.QUERY_FAILED, "Query failed", e);
        }
    }

    /**
     * Counts the number of statements matching the given pattern.
     *
     * @param pattern the statement pattern to match (must not be null)
     * @return the count of matching statements
     * @throws StorageException if the query fails
     */
    @Override
    public long count(StatementPattern pattern) throws StorageException {
        try (Stream<Statement> stream = query(pattern)) {
            return stream.count();
        }
    }

    /**
     * Checks if any statement matches the given pattern.
     *
     * @param pattern the statement pattern to match (must not be null)
     * @return true if at least one matching statement exists
     * @throws StorageException if the query fails
     */
    @Override
    public boolean exists(StatementPattern pattern) throws StorageException {
        try (Stream<Statement> stream = query(pattern)) {
            return stream.findAny().isPresent();
        }
    }

    /**
     * Filters statements and returns them as a new Model.
     *
     * @param pattern the statement pattern to match
     * @return never returns (always throws)
     * @throws UnsupportedOperationException always
     */
    @Override
    public Model filter(StatementPattern pattern) throws StorageException {
        throw new UnsupportedOperationException(
                "filter() not implemented for MemoryStorageManager. " +
                        "Use query() to get a stream of matching statements."
        );
    }
}
