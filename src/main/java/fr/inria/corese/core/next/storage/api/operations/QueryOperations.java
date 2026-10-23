package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.stream.Stream;

/**
 * Read and pattern-matching operations implemented by an RDF storage backend session.
 *
 * <h2>Threading &amp; Session Confinement</h2>
 * <p>Instances obtained from a {@link fr.inria.corese.core.next.storage.api.StorageManager} session
 * are not thread-safe. Each thread must operate on its own session obtained via
 * {@link fr.inria.corese.core.next.storage.api.StorageManager#openSession()} or an independent
 * snapshot via {@link fr.inria.corese.core.next.storage.api.StorageManager#openReadSnapshot()}.</p>
 *
 * <h2>Stream &amp; Resource Lifecycle</h2>
 * <p>Streams returned by {@link #find(StatementPattern)} are lazy, single-use, and may hold low-level
 * cursor and snapshot partition resources. <strong>Callers must always close returned streams</strong>,
 * preferably using a {@code try-with-resources} block. This requirement applies even when using
 * short-circuiting terminal operations such as {@link Stream#findAny()} or {@link Stream#limit(long)},
 * as the terminal operation alone does not automatically close the underlying stream. Failure to close
 * streams can lead to partition retention and increased memory footprint.</p>
 *
 * <h2>Graph Matching Semantics</h2>
 * <p>Pattern matching respects the dataset semantics defined by {@link StatementPattern}:</p>
 * <ul>
 *   <li>A {@code null} or empty contexts array matches statements across all graphs (default graph and named graphs).</li>
 *   <li>An explicit {@code null} array element matches only statements in the default graph.</li>
 *   <li>Specific resource IRIs or blank nodes match only statements in those designated named graphs.</li>
 * </ul>
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
