package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Objects;

/**
 * Write operations implemented by an RDF storage backend.
 *
 * <p>Duplicate insertion and removal of absent statements are normal set
 * operations, not failures. Exceptional backend failures are reported with
 * {@link StorageException}.</p>
 */
public interface MutationOperations {

    /**
     * Inserts a single statement.
     *
     * @param statement Statement to insert
     * @return {@code true} if the storage changed
     * @throws StorageException     if operation fails
     * @throws NullPointerException if {@code statement} is {@code null}
     */
    boolean add(Statement statement) throws StorageException;

    /**
     * Deletes a single statement.
     *
     * @param statement Statement to delete
     * @return {@code true} if the storage changed
     * @throws StorageException     if operation fails
     * @throws NullPointerException if {@code statement} is {@code null}
     */
    boolean remove(Statement statement) throws StorageException;

    /**
     * Deletes every statement matching the given pattern.
     *
     * @param pattern pattern to remove
     * @return number of removed statements
     * @throws StorageException if the operation fails
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    long remove(StatementPattern pattern) throws StorageException;

    /**
     * Clears statements from the specified contexts.
     *
     * @param contexts contexts to clear; null or empty means every graph and an
     *                 explicit null element means the default graph
     * @return number of removed statements
     * @throws StorageException if operation fails
     */
    long clear(Resource... contexts) throws StorageException;

    /**
     * Adds statements, returning the number of new statements. Implementations
     * may override this method with a native batch operation.
     *
     * @param statements statements to add
     * @return number of new statements
     * @throws StorageException if the operation fails
     * @throws NullPointerException if the source or one of its statements is null
     */
    default long addAll(Iterable<? extends Statement> statements) throws StorageException {
        long changed = 0;
        for (Statement statement : Objects.requireNonNull(statements, "statements")) {
            if (add(Objects.requireNonNull(statement, "statement"))) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Removes statements, returning the number that existed. Implementations
     * may override this method with a native batch operation.
     *
     * @param statements statements to remove
     * @return number of removed statements
     * @throws StorageException if the operation fails
     * @throws NullPointerException if the source or one of its statements is null
     */
    default long removeAll(Iterable<? extends Statement> statements) throws StorageException {
        long changed = 0;
        for (Statement statement : Objects.requireNonNull(statements, "statements")) {
            if (remove(Objects.requireNonNull(statement, "statement"))) {
                changed++;
            }
        }
        return changed;
    }
}
