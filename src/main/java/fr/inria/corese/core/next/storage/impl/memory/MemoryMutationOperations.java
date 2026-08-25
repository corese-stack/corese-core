package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Objects;

/**
 * Mutation operations implementation for {@link MemoryStorageManager}.
 */
final class MemoryMutationOperations implements MutationOperations {

    private final InMemoryStatementStore adapter;

    /**
     * Constructs a new MemoryMutationOperations.
     *
     * @param adapter the InMemoryStatementStore for data access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public MemoryMutationOperations(InMemoryStatementStore adapter) {
        if (adapter == null) throw new IllegalArgumentException("InMemoryStatementStore cannot be null");
        this.adapter = adapter;
    }

    /**
     * Inserts a statement into the store.
     *
     * @param statement the statement to insert (must not be null)
     * @return whether the store changed
     * @throws NullPointerException     if statement is null
     * @throws StorageException         if the insert operation fails
     */
    @Override
    public boolean add(Statement statement) throws StorageException {
        Objects.requireNonNull(statement, "statement");
        try {
            return adapter.add(statement);
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Insert failed", e);
        }
    }

    /**
     * Deletes a statement from the store.
     *
     * @param statement the statement to delete (must not be null)
     * @return whether the store changed
     * @throws NullPointerException     if statement is null
     * @throws StorageException         if the delete operation fails
     */
    @Override
    public boolean remove(Statement statement) throws StorageException {
        Objects.requireNonNull(statement, "statement");
        try {
            return adapter.remove(statement);
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Delete failed", e);
        }
    }

    /**
     * Deletes all statements matching the given pattern.
     *
     * @param pattern statement pattern
     * @return number of deleted statements
     * @throws StorageException if the operation fails
     */
    @Override
    public long remove(StatementPattern pattern) throws StorageException {
        Objects.requireNonNull(pattern, "pattern");
        try {
            var toRemove = adapter.find(
                    pattern.getSubject(),
                    pattern.getPredicate(),
                    pattern.getObject(),
                    pattern.getContexts());
            long count = toRemove.size();
            toRemove.forEach(adapter::remove);
            return count;
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Delete failed", e);
        }
    }

    /**
     * Clears statements from specified contexts.
     *
     * @param contexts contexts to clear; null/empty clears every graph and an
     *                 explicit null element clears the default graph
     * @return number of deleted statements
     * @throws StorageException if the clear operation fails
     */
    @Override
    public long clear(Resource... contexts) throws StorageException {
        try {
            if (contexts != null && contexts.length > 0) {
                return remove(StatementPattern.of(null, null, null, contexts));
            }
            int before = adapter.size();
            adapter.clear();
            return (long) before - adapter.size();
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Clear failed", e);
        }
    }
}
