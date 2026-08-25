package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;

/**
 * A connection to a Corese {@link Repository}, providing access to query
 * evaluation and data updates.
 * <p>
 * A {@code RepositoryConnection} represents a lightweight session object:
 * it encapsulates the state needed to evaluate SPARQL queries, prepare updates,
 * manage datasets (FROM / FROM NAMED), and optionally participate in transactions.
 * </p>
 *
 * <p>Connections and their prepared operations are invalidated when either the
 * connection or their repository is closed. Closing a connection is idempotent
 * and never closes its repository.</p>
 *
 *
 * @see Repository
 * @see TupleQuery
 * @see GraphQuery
 * @see BooleanQuery
 * @see Update
 */
public interface RepositoryConnection extends AutoCloseable {

    /**
     * Returns the repository this connection is associated with.
     *
     * @return the parent {@link Repository}
     */
    Repository getRepository();

    /**
     * Returns the {@link ValueFactory} associated with this connection,
     * used to create IRIs, literals, blank nodes, and RDF triples.
     *
     * @return the value factory
     */
    ValueFactory getValueFactory();

    /**
     * Indicates whether this connection is still open and usable.
     *
     * @return {@code true} if the connection is open, {@code false} otherwise
     */
    boolean isOpen();

    // --- SPARQL query preparation ---

    /**
     * Creates a prepared SPARQL SELECT query.
     *
     * @param queryString the textual query form
     * @return an executable {@link TupleQuery}
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if the connection is closed
     */
    TupleQuery prepareTupleQuery(String queryString)
            throws QuerySyntaxException, RepositoryException;

    /**
     * Creates a prepared SPARQL CONSTRUCT or DESCRIBE query.
     *
     * @param queryString the textual query form
     * @return an executable {@link GraphQuery}
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if the connection is closed
     */
    GraphQuery prepareGraphQuery(String queryString)
            throws QuerySyntaxException, RepositoryException;

    /**
     * Creates a prepared SPARQL ASK query.
     *
     * @param queryString the textual query form
     * @return an executable {@link BooleanQuery}
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if the connection is closed
     */
    BooleanQuery prepareBooleanQuery(String queryString)
            throws QuerySyntaxException, RepositoryException;

    /**
     * Creates a prepared SPARQL update request.
     *
     * @param updateString the textual update form
     * @return an executable {@link Update}
     * @throws QuerySyntaxException if the update string is syntactically invalid
     * @throws RepositoryException if the connection is closed
     */
    Update prepareUpdate(String updateString)
            throws QuerySyntaxException, RepositoryException;


    // --- Dataset for this connection (FROM / FROM NAMED) ---

    /**
     * Sets a connection-scoped dataset used for all queries prepared
     * by this connection. This overrides any dataset declared inside
     * the query itself.
     *
     * @param dataset the dataset to enforce, or {@code null} to clear it
     */
    void setDataset(Dataset dataset);

    /**
     * Returns the dataset associated with this connection, or {@code null}
     * if no explicit dataset has been set.
     *
     * @return the active {@link Dataset}, or {@code null}
     */
    Dataset getDataset();

    // --- Transactions (optional, depending on backend) ---

    /** Returns whether the backing storage supports transactions. */
    boolean supportsTransactions();

    /** Returns whether this connection currently owns an active transaction. */
    boolean isActive();

    /**
     * Begins a new transaction, if supported by the backend.
     *
     * @throws RepositoryException if transactions are not supported, if a transaction is already active,
     *                             or if the connection is closed
     */
    void begin() throws RepositoryException;

    /**
     * Commits the active transaction, making all changes permanent.
     *
     * @throws RepositoryException if commit fails, no transaction is active,
     *                             or the connection is closed
     */
    void commit() throws RepositoryException;

    /**
     * Rolls back the active transaction, discarding all uncommitted changes.
     *
     * @throws RepositoryException if rollback fails, no transaction is active,
     *                             or the connection is closed
     */
    void rollback() throws RepositoryException;

    /**
     * Closes this connection and releases any resources it holds. Repeated calls
     * have no effect. An active transaction is rolled back before closing.
     *
     * @throws RepositoryException if closing the connection fails
     */
    @Override
    void close() throws RepositoryException;
}
