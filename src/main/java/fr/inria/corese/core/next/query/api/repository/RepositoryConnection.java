package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.StatementResult;

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
 * <p>A connection is a mutable session and is not safe for concurrent use.</p>
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

    // --- RDF data access ---

    /**
     * Adds a statement to the repository. Adding an existing statement has no
     * effect.
     *
     * @param statement statement to add
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void add(Statement statement) throws RepositoryException;

    /**
     * Adds every statement supplied by an iterable. This method consumes the
     * iterable progressively and does not retain it in memory.
     *
     * @param statements statements to add
     * @throws NullPointerException if the iterable or one of its statements is {@code null}
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void add(Iterable<? extends Statement> statements) throws RepositoryException;

    /**
     * Adds a statement assembled from RDF terms. With no context, the statement
     * is added to the default graph. With several contexts, one statement is
     * added to each graph; a {@code null} context denotes the default graph.
     *
     * @param subject statement subject
     * @param predicate statement predicate
     * @param object statement object
     * @param contexts target contexts, or none for the default graph
     * @throws NullPointerException if a required RDF term is {@code null}
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void add(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException;

    /**
     * Removes a statement if it is present.
     *
     * @param statement statement to remove
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void remove(Statement statement) throws RepositoryException;

    /**
     * Removes every supplied statement that is present.
     *
     * @param statements statements to remove
     * @throws NullPointerException if the iterable or one of its statements is {@code null}
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void remove(Iterable<? extends Statement> statements) throws RepositoryException;

    /**
     * Removes statements matching an RDF pattern. A {@code null} subject,
     * predicate, or object is a wildcard. No contexts means every graph; an
     * explicit {@code null} context selects only the default graph.
     *
     * @param subject subject to match, or {@code null}
     * @param predicate predicate to match, or {@code null}
     * @param object object to match, or {@code null}
     * @param contexts contexts to match, or none for every graph
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException;

    /**
     * Removes all statements from the selected contexts. No contexts clears the
     * whole repository; an explicit {@code null} context clears only the default
     * graph.
     *
     * @param contexts contexts to clear, or none to clear every graph
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    void clear(Resource... contexts) throws RepositoryException;

    /**
     * Tests whether at least one statement matches an RDF pattern. A
     * {@code null} term is a wildcard. No contexts means every graph; an explicit
     * {@code null} context selects only the default graph.
     *
     * @param subject subject to match, or {@code null}
     * @param predicate predicate to match, or {@code null}
     * @param object object to match, or {@code null}
     * @param contexts contexts to match, or none for every graph
     * @return whether a matching statement exists
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    boolean hasStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException;

    /**
     * Returns the number of statements in the selected contexts. No contexts
     * means every graph; an explicit {@code null} context selects only the
     * default graph.
     *
     * @param contexts contexts to count, or none for every graph
     * @return number of matching statements
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    long size(Resource... contexts) throws RepositoryException;

    /**
     * Returns a progressive result containing statements matching an RDF
     * pattern. A {@code null} term is a wildcard. No contexts means every graph;
     * an explicit {@code null} context selects only the default graph.
     *
     * <p>The result depends on this connection and must be closed before the
     * connection is closed.</p>
     *
     * @param subject subject to match, or {@code null}
     * @param predicate predicate to match, or {@code null}
     * @param object object to match, or {@code null}
     * @param contexts contexts to match, or none for every graph
     * @return a closeable, single-use statement result
     * @throws RepositoryException if the operation fails or the connection is closed
     */
    StatementResult getStatements(
            Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException;

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
