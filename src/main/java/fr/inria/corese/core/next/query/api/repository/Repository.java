package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.Repositories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.InputStream;
import java.io.Reader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import java.util.Objects;

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
             TupleQueryResult raw = conn.prepareTupleQuery(sparqlSelect).evaluate()) {
            names = List.copyOf(raw.getBindingNames());
            while (raw.hasNext()) {
                bindings.add(raw.next());
            }
        }
        return MaterializedResults.tuple(names, bindings);
    }

    /**
     * Evaluates a SPARQL SELECT query directly against this repository in one call,
     * mapping each solution {@link BindingSet} using the provided mapper function.
     *
     * <p>The repository connection is automatically opened and closed within this call.
     * The mapper should extract needed terms eagerly rather than retaining references
     * to the row, because the underlying query cursor is closed upon return.</p>
     *
     * @param sparqlSelect the SPARQL SELECT query text
     * @param mapper mapping function converting each {@link BindingSet} into a result element
     * @param <T> the mapped result type
     * @return an unmodifiable list of mapped results
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default <T> List<T> select(String sparqlSelect, Function<? super BindingSet, ? extends T> mapper)
            throws QuerySyntaxException, RepositoryException {
        Objects.requireNonNull(mapper, "mapper");
        List<T> results = new ArrayList<>();
        try (RepositoryConnection conn = getConnection();
             TupleQueryResult raw = conn.prepareTupleQuery(sparqlSelect).evaluate()) {
            while (raw.hasNext()) {
                results.add(mapper.apply(raw.next()));
            }
        }
        return Collections.unmodifiableList(results);
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
            return conn.prepareBooleanQuery(sparqlAsk).evaluate();
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
     * @param sparqlGraph the SPARQL CONSTRUCT or DESCRIBE query text
     * @return a materialized {@link GraphQueryResult} containing output statements
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     */
    default GraphQueryResult graph(String sparqlGraph) throws QuerySyntaxException, RepositoryException {
        List<Statement> statements = new ArrayList<>();
        try (RepositoryConnection conn = getConnection();
             GraphQueryResult raw = conn.prepareGraphQuery(sparqlGraph).evaluate()) {
            while (raw.hasNext()) {
                statements.add(raw.next());
            }
        }
        return MaterializedResults.graph(statements);
    }

    /**
     * Evaluates a SPARQL CONSTRUCT or DESCRIBE query directly against this repository in one call.
     *
     * <p>This is a natural semantic alias for {@link #graph(String)} following SPARQL 1.1 terminology.</p>
     *
     * @param sparqlConstruct the SPARQL CONSTRUCT query text
     * @return a materialized {@link GraphQueryResult} containing output statements
     * @throws QuerySyntaxException if the query string is syntactically invalid
     * @throws RepositoryException if evaluation fails
     * @see #graph(String)
     */
    default GraphQueryResult construct(String sparqlConstruct) throws QuerySyntaxException, RepositoryException {
        return graph(sparqlConstruct);
    }

    /**
     * Tests whether at least one statement matching the specified pattern exists in this repository.
     * A {@code null} term is treated as a wildcard.
     *
     * @param subject statement subject to match, or {@code null} for wildcard
     * @param predicate statement predicate to match, or {@code null} for wildcard
     * @param object statement object to match, or {@code null} for wildcard
     * @param contexts contexts to match, or empty for every graph
     * @return {@code true} if a matching statement exists, {@code false} otherwise
     * @throws RepositoryException if the check fails
     */
    default boolean hasStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException {
        try (RepositoryConnection conn = getConnection()) {
            return conn.hasStatement(subject, predicate, object, contexts);
        }
    }

    /**
     * Returns the total number of statements in the designated graph contexts.
     * An empty {@code contexts} array counts across all graphs (default and named graphs).
     *
     * @param contexts contexts to count, or none for every graph
     * @return number of statements in the selected contexts
     * @throws RepositoryException if the count cannot be performed
     */
    default long size(Resource... contexts) throws RepositoryException {
        try (RepositoryConnection conn = getConnection()) {
            return conn.size(contexts);
        }
    }

    /**
     * Adds the specified statements to this repository in an auto-managed connection.
     *
     * @param statements statements to add
     * @throws RepositoryException if the addition fails
     * @throws NullPointerException if {@code statements} or any element is {@code null}
     */
    default void add(Statement... statements) throws RepositoryException {
        Objects.requireNonNull(statements, "statements");
        try (RepositoryConnection conn = getConnection()) {
            conn.add(Arrays.asList(statements));
        }
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
            conn.prepareUpdate(sparqlUpdate).execute();
        }
    }

    /**
     * Loads a file using its extension, owning the connection and import transaction.
     *
     * @param source RDF file
     * @see RepositoryConnection#load(Path)
     */
    default void load(Path source) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source);
        }
    }

    /**
     * Loads RDF through a connection owned and closed by this call.
     * Caller-owned streams and readers remain open.
     *
     * @param source RDF input
     * @param format explicit RDF format
     * @param contexts target graphs, or none to preserve source contexts
     * @see RepositoryConnection#load(Path, RDFFormat, Resource...)
     */
    default void load(Path source, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source, format, contexts);
        }
    }

    /**
     * Loads RDF through a connection owned and closed by this call.
     * Caller-owned streams and readers remain open.
     *
     * @param source RDF input
     * @param format explicit RDF format
     * @param contexts target graphs, or none to preserve source contexts
     * @see RepositoryConnection#load(InputStream, RDFFormat, Resource...)
     */
    default void load(InputStream source, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source, format, contexts);
        }
    }

    /**
     * Loads RDF with an external base IRI in a transaction owned by this call.
     *
     * @param source caller-owned input, never closed here
     * @param format explicit RDF format
     * @param baseIRI external base IRI, or null
     * @param contexts target graphs, or none to preserve source contexts
     * @see RepositoryConnection#load(InputStream, RDFFormat, String, Resource...)
     */
    default void load(InputStream source, RDFFormat format, String baseIRI, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source, format, baseIRI, contexts);
        }
    }

    /**
     * Loads RDF through a connection owned and closed by this call.
     * Caller-owned streams and readers remain open.
     *
     * @param source RDF input
     * @param format explicit RDF format
     * @param contexts target graphs, or none to preserve source contexts
     * @see RepositoryConnection#load(Reader, RDFFormat, Resource...)
     */
    default void load(Reader source, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source, format, contexts);
        }
    }

    /**
     * Loads RDF with an external base IRI in a transaction owned by this call.
     *
     * @param source caller-owned input, never closed here
     * @param format explicit RDF format
     * @param baseIRI external base IRI, or null
     * @param contexts target graphs, or none to preserve source contexts
     * @see RepositoryConnection#load(Reader, RDFFormat, String, Resource...)
     */
    default void load(Reader source, RDFFormat format, String baseIRI, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.load(source, format, baseIRI, contexts);
        }
    }

    /**
     * Exports RDF through a connection owned and closed by this call.
     * Caller-owned streams and writers remain open.
     *
     * @param destination RDF output
     * @param format RDF format
     * @param contexts selected graphs, or none for every graph
     * @see RepositoryConnection#export(Path, RDFFormat, Resource...)
     */
    default void export(Path destination, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.export(destination, format, contexts);
        }
    }

    /**
     * Exports RDF through a connection owned and closed by this call.
     * Caller-owned streams and writers remain open.
     *
     * @param destination RDF output
     * @param format RDF format
     * @param contexts selected graphs, or none for every graph
     * @see RepositoryConnection#export(OutputStream, RDFFormat, Resource...)
     */
    default void export(OutputStream destination, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.export(destination, format, contexts);
        }
    }

    /**
     * Exports RDF through a connection owned and closed by this call.
     * Caller-owned streams and writers remain open.
     *
     * @param destination RDF output
     * @param format RDF format
     * @param contexts selected graphs, or none for every graph
     * @see RepositoryConnection#export(Writer, RDFFormat, Resource...)
     */
    default void export(Writer destination, RDFFormat format, Resource... contexts) {
        try (RepositoryConnection connection = getConnection()) {
            connection.export(destination, format, contexts);
        }
    }

    /**
     * Runs work in an owned connection and transaction, then closes the connection.
     *
     * @param action work that must not manually end the transaction or close the connection
     * @see RepositoryConnection#inTransaction(Consumer)
     */
    default void inTransaction(Consumer<? super RepositoryConnection> action) {
        try (RepositoryConnection connection = getConnection()) {
            connection.inTransaction(action);
        }
    }

    /**
     * Runs work in an owned connection and transaction and returns its detached value.
     *
     * @param action work that must not manually end the transaction or close the connection
     * @param <T> return value type
     * @return the callback's value after commit and connection closure
     * @see RepositoryConnection#computeInTransaction(Function)
     */
    default <T> T computeInTransaction(Function<? super RepositoryConnection, ? extends T> action) {
        try (RepositoryConnection connection = getConnection()) {
            return connection.computeInTransaction(action);
        }
    }
}
