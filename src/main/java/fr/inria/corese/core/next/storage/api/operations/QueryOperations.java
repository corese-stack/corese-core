package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.stream.Stream;

/**
 * Read operations implemented by an RDF storage backend.
 */
public interface QueryOperations {

    /**
     * Returns a token identifying the data currently visible to this session.
     * A supported token is stable across reads and changes after effective content
     * or graph-registry mutations. A snapshot transaction keeps its captured token
     * despite other sessions' commits, and changes it after its own writes. After
     * rollback the token identifies the restored visible state. Tokens are opaque:
     * clients compare equality, never infer statement counts or commit order.
     *
     * <p>Backends unable to provide reliable invalidation return {@code -1}; query
     * planning then remains uncached. Implementations must validate session access
     * and must not create a read cursor or transaction to obtain this token.</p>
     *
     * @return non-negative session-visible data token, or {@code -1} if unavailable
     * @throws StorageException if the visible version cannot be read
     */
    default long getDataVersion() {
        return -1;
    }

    /**
     * Queries statements matching the given pattern.
     *
     * @param pattern Statement pattern to match
     * @return Stream of matching statements (must be closed)
     * @throws StorageException     if query fails
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    Stream<Statement> find(StatementPattern pattern) throws StorageException;

    /**
     * Counts statements matching the given pattern.
     * Implementations should avoid materializing matching statements when possible.
     *
     * @param pattern Statement pattern to match
     * @return Number of matching statements
     * @throws StorageException     if count fails
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    long count(StatementPattern pattern) throws StorageException;

    /**
     * Checks if at least one statement matches the given pattern.
     * More efficient than query().findAny() as it can stop after first match.
     *
     * @param pattern Statement pattern to match
     * @return true if at least one statement matches
     * @throws StorageException     if check fails
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    boolean contains(StatementPattern pattern) throws StorageException;
}
