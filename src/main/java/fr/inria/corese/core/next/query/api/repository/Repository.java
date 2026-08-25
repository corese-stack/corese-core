package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.Repositories;

import java.util.ArrayList;
import java.util.List;

/**
 * A Corese repository that contains RDF data that can be queried and updated.
 *
 * <p>Repositories are created open through {@link Repositories}. Common query
 * methods manage their own connections and return materialized, connection-safe
 * results. Use {@link #getConnection()} for prepared operations and progressive
 * result consumption.</p>
 *
 * @see RepositoryConnection
 * @see Repositories
 */
public interface Repository extends AutoCloseable {

    /**
     * Returns whether this repository is open and can create connections.
     * Repositories returned by the public factory are open immediately.
     *
     * @return {@code true} until {@link #close()} is called
     */
    boolean isOpen();

    /**
     * Closes this repository and the storage backend it owns.
     *
     * <p>Closing a repository is idempotent. Existing connections become closed
     * and no new connection can be created.</p>
     *
     * @throws RepositoryException if the underlying storage cannot be closed
     */
    @Override
    void close() throws RepositoryException;

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
     * @throws RepositoryException if the repository is closed
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
