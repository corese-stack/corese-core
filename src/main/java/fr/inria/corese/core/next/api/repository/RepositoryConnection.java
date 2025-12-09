package fr.inria.corese.core.next.api.repository;

import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.dataset.Dataset;
import fr.inria.corese.core.next.api.query.*;

/**
 * A connection to a Corese {@link Repository}, providing access to query
 * evaluation and data updates.
 * <p>
 * A {@code RepositoryConnection} represents a lightweight session object:
 * it encapsulates the state needed to evaluate SPARQL queries, prepare updates,
 * manage datasets (FROM / FROM NAMED), and optionally participate in transactions.
 * </p>
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
     * Creates a prepared SPARQL {@code SELECT} query.
     *
     * @param queryLanguage the query language
     * @param queryString   the textual query form
     * @return an executable {@link TupleQuery}
     */
    TupleQuery prepareTupleQuery(QueryLanguage queryLanguage, String queryString);

    /**
     * Creates a prepared SPARQL {@code CONSTRUCT} or {@code DESCRIBE} query.
     *
     * @param queryLanguage the query language (typically SPARQL)
     * @param queryString   the textual query form
     * @return an executable {@link GraphQuery}
     */
    GraphQuery prepareGraphQuery(QueryLanguage queryLanguage, String queryString);

    /**
     * Creates a prepared SPARQL {@code ASK} query.
     *
     * @param queryLanguage the query language (typically SPARQL)
     * @param queryString   the textual query form
     * @return an executable {@link BooleanQuery}
     */
    BooleanQuery prepareBooleanQuery(QueryLanguage queryLanguage, String queryString);

    /**
     * Creates a prepared SPARQL 1.1 UPDATE.
     *
     * @param queryLanguage the update language (typically SPARQL)
     * @param updateString  the textual update form
     * @return an executable {@link Update}
     */
    Update prepareUpdate(QueryLanguage queryLanguage, String updateString);

    // --- Dataset for this connection (FROM / FROM NAMED) ---

    /**
     * Sets a connection-scoped dataset used for all queries prepared
     * by this connection. This overrides any dataset declared inside
     * the query itself.
     *
     * @param dataset the dataset to enforce
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

    /**
     * Begins a new transaction, if supported by the backend. If transactions
     * are not supported, this method may throw an exception.
     */
    void begin();

    /**
     * Commits the active transaction, making all changes permanent.
     */
    void commit();

    /**
     * Rolls back the active transaction, discarding all uncommitted changes.
     */
    void rollback();

    /**
     * Closes this connection and releases any underlying resources.
     * After calling {@code close()}, the connection becomes unusable.
     */
    @Override
    void close();
}