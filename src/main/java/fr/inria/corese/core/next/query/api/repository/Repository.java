package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A Corese repository that contains RDF data that can be queried and updated.
 *
 * @see RepositoryConnection
 */
public interface Repository {

    /**
     * Initializes the repository, making it ready for use.
     *
     * @throws RepositoryException if initialization fails (e.g., I/O errors, corrupt data)
     *                             or if the repository is already initialized
     */
    void init() throws RepositoryException;

    /**
     * Checks whether the repository has been successfully initialized.
     *
     * @return {@code true} if initialized and ready for use, {@code false} otherwise
     */
    boolean isInitialized();

    /**
     * Shuts down the repository and releases all resources.
     *
     * @throws RepositoryException if shutdown fails (e.g., unable to flush data)
     */
    void shutDown() throws RepositoryException;

    /**
     * Checks whether the repository supports write operations.
     *
     * @return {@code true} if write operations are supported, {@code false} for read-only
     */
    boolean isWritable();

    /**
     * Opens a new connection to this repository.
     *
     * @return a new {@link RepositoryConnection}
     * @throws RepositoryException if the repository is not initialized or closed
     */
    RepositoryConnection getConnection() throws RepositoryException;

    /**
     * Returns the value factory associated with this repository.
     *
     * @return the {@link ValueFactory}
     */
    ValueFactory getValueFactory();

    // -------------------------------------------------------------------------
    // Level 1: Convenience 1-liner Shortcuts (Materialized, Connection-Safe)
    // -------------------------------------------------------------------------

    /**
     * Evaluates a SPARQL SELECT query directly against this repository in one call,
     * returning a fully materialized, connection-safe {@link TupleQueryResult}.
     *
     * <p>This convenience method reads every solution into memory before returning.
     * Its memory usage therefore grows with the size of the result. For large
     * results or progressive consumption, use a {@link RepositoryConnection} and
     * keep it open while consuming the result returned by the prepared tuple query.</p>
     *
     * @param sparqlSelect the SPARQL SELECT query text
     * @return a materialized {@link TupleQueryResult} containing all solutions
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     */
    default TupleQueryResult select(String sparqlSelect) throws QuerySyntaxException, RepositoryException {
        List<String> names;
        List<BindingSet> bindings = new ArrayList<>();
        try (RepositoryConnection conn = getConnection();
             TupleQueryResult raw = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlSelect).evaluate()) {
            names = List.copyOf(raw.getBindingNames());
            while (raw.hasNext()) {
                bindings.add(raw.next());
            }
        }
        return MaterializedResults.tuple(names, bindings);
    }

    /**
     * Evaluates a SPARQL ASK query directly against this repository in one call.
     *
     * @param sparqlAsk the SPARQL ASK query text
     * @return {@code true} if at least one solution exists, {@code false} otherwise
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     */
    default boolean ask(String sparqlAsk) throws QuerySyntaxException, RepositoryException {
        try (RepositoryConnection conn = getConnection()) {
            return conn.prepareBooleanQuery(QueryLanguage.SPARQL, sparqlAsk).evaluate();
        }
    }

    /**
     * Evaluates a SPARQL CONSTRUCT or DESCRIBE query directly against this repository in one call,
     * returning a fully materialized, connection-safe {@link GraphQueryResult}.
     *
     * <p>This convenience method reads every output statement into memory before
     * returning. Its memory usage therefore grows with the size of the result. For
     * large results or progressive consumption, use a {@link RepositoryConnection}
     * and keep it open while consuming the result returned by the prepared graph
     * query.</p>
     *
     * @param sparqlConstruct the SPARQL CONSTRUCT or DESCRIBE query text
     * @return a materialized {@link GraphQueryResult} containing output statements
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     */
    default GraphQueryResult construct(String sparqlConstruct) throws QuerySyntaxException, RepositoryException {
        List<Statement> statements = new ArrayList<>();
        try (RepositoryConnection conn = getConnection();
             GraphQueryResult raw = conn.prepareGraphQuery(QueryLanguage.SPARQL, sparqlConstruct).evaluate()) {
            while (raw.hasNext()) {
                statements.add(raw.next());
            }
        }
        return MaterializedResults.graph(statements);
    }

    /**
     * Executes a SPARQL UPDATE request directly against this repository in one call.
     *
     * @param sparqlUpdate the SPARQL UPDATE request text
     * @throws QuerySyntaxException if the update string is syntactically invalid
     * @throws RepositoryException if execution fails
     */
    default void update(String sparqlUpdate) throws QuerySyntaxException, RepositoryException {
        try (RepositoryConnection conn = getConnection()) {
            conn.prepareUpdate(QueryLanguage.SPARQL, sparqlUpdate).execute();
        }
    }
}
