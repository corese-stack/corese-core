package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Query operations implementation for {@link MemoryStorageManager}.
 */
final class MemoryQueryOperations implements QueryOperations {

    private final InMemoryStatementStore adapter;

    /**
     * Constructs a new MemoryQueryOperations.
     *
     * @param adapter the InMemoryStatementStore for data access (must not be null)
     * @throws NullPointerException if adapter is null
     */
    public MemoryQueryOperations(InMemoryStatementStore adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /**
     * Queries for statements matching the given pattern.
     *
     * @param pattern the statement pattern to match (must not be null)
     * @return a stream of matching statements (must be closed)
     * @throws NullPointerException if pattern is null
     * @throws StorageException         if the query fails
     */
    @Override
    public Stream<Statement> find(StatementPattern pattern) throws StorageException {
        Objects.requireNonNull(pattern, "pattern");
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
        try (Stream<Statement> stream = find(pattern)) {
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
    public boolean contains(StatementPattern pattern) throws StorageException {
        try (Stream<Statement> stream = find(pattern)) {
            return stream.findAny().isPresent();
        }
    }
}
