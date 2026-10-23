package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Objects;

/**
 * Write and update operations implemented by an RDF storage backend session.
 *
 * <h2>Threading &amp; Session Confinement</h2>
 * <p>Instances obtained from a {@link fr.inria.corese.core.next.storage.api.StorageManager} session
 * are not thread-safe. Mutations must be confined to the session's owning thread.</p>
 *
 * <h2>Transactions &amp; Visibility</h2>
 * <ul>
 *   <li>When operating outside an active transaction, mutations may auto-commit immediately
 *       or participate in an implicit session transaction depending on backend configuration.</li>
 *   <li>When an explicit transaction is active (via {@link fr.inria.corese.core.next.storage.api.transaction.TransactionManager}),
 *       uncommitted mutations are immediately visible to the current session but remain isolated
 *       from other concurrent sessions until commit.</li>
 *   <li>The default implementations of batch operations (such as {@link #addAll}) iterate sequentially;
 *       backends with transactional staging may override them to provide atomic batch publishing.</li>
 * </ul>
 *
 * <h2>Graph Semantics &amp; Invariants</h2>
 * <ul>
 *   <li>Inserting a statement with a {@code null} context targets the default graph.</li>
 *   <li>{@link #createGraph(Resource)} registers an empty named graph without requiring statements.
 *       Registered empty named graphs survive {@link #clear(Resource...)} but are removed by {@link #dropGraph(Resource)}.</li>
 *   <li>Duplicate insertion and removal of absent statements are idempotent set operations,
 *       returning {@code false} without raising exceptions.</li>
 * </ul>
 */
public interface MutationOperations {

    /**
     * Creates an empty named graph in the storage.
     *
     * @param graph the named graph identifier (must not be {@code null})
     * @return {@code true} if the graph was created, {@code false} if it already existed
     * @throws NullPointerException          if {@code graph} is {@code null}
     * @throws UnsupportedOperationException if named graph creation is not supported
     * @throws StorageException              if the operation fails
     */
    default boolean createGraph(Resource graph) {
        throw new UnsupportedOperationException("Named graph creation is not supported");
    }

    /**
     * Removes a named graph and all of its contained statements.
     *
     * @param graph the named graph identifier (must not be {@code null})
     * @return {@code true} if the graph was removed, {@code false} if it was absent
     * @throws NullPointerException          if {@code graph} is {@code null}
     * @throws UnsupportedOperationException if named graph removal is not supported
     * @throws StorageException              if the operation fails
     */
    default boolean dropGraph(Resource graph) {
        throw new UnsupportedOperationException("Named graph removal is not supported");
    }

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
