package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.Query;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.dataset.CoreseDataset;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the public SPARQL API exposed by {@link CoreseRepositoryConnection}.
 *
 * <p>Uses a real {@link MemoryStorageManager} to exercise the full pipeline
 * (parser → AST → bridge → KGRAM) through the API without touching any
 * internal classes directly.</p>
 */
class CoreseRepositoryConnectionTest {

    private static final String ALICE = "http://example.org/alice";
    private static final String BOB = "http://example.org/bob";
    private static final String KNOWS = "http://example.org/knows";

    private ValueFactory vf;
    private MemoryStorageManager storage;
    private CoreseRepository repository;

    @BeforeEach
    void setUp() throws RepositoryException {
        vf = new CoreseValueFactory();
        storage = MemoryStorageManager.builder().build();
        repository = new CoreseRepository(storage);
        repository.init();

        // Insert one triple: alice knows bob
        storage.getMutationOperations().insertStatement(
                vf.createStatement(iri(ALICE), iri(KNOWS), iri(BOB)));
    }

    // -------------------------------------------------------------------------
    // TupleQuery (SELECT)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("prepareTupleQuery")
    class PrepareTupleQuery {

        @Test
        @DisplayName("Returns TupleQuery for a SELECT query")
        void returnsTupleQueryForSelect() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                assertNotNull(q);
                assertEquals(Query.QueryType.TUPLE, q.getQueryType());
            }
        }

        @Test
        @DisplayName("evaluate() returns the matching triples")
        void evaluateReturnsMatchingTriples() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                TupleQueryResult result = q.evaluate();

                assertEquals(List.of("s", "p", "o"), result.getBindingNames());
                assertTrue(result.hasNext());
                BindingSet bs = result.next();
                assertEquals(ALICE, bs.getValue("s").stringValue());
                assertEquals(KNOWS, bs.getValue("p").stringValue());
                assertEquals(BOB, bs.getValue("o").stringValue());
                assertFalse(result.hasNext());
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-SELECT query string")
        void throwsSyntaxExceptionForAsk() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareTupleQuery(QueryLanguage.SPARQL, "ASK WHERE { ?s ?p ?o }"));
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for invalid SPARQL")
        void throwsSyntaxExceptionForInvalidSparql() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareTupleQuery(QueryLanguage.SPARQL, "THIS IS NOT SPARQL"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // BooleanQuery (ASK)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("prepareBooleanQuery")
    class PrepareBooleanQuery {

        @Test
        @DisplayName("Returns BooleanQuery for an ASK query")
        void returnsBooleanQueryForAsk() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK WHERE { ?s ?p ?o }");
                assertNotNull(q);
                assertEquals(Query.QueryType.BOOLEAN, q.getQueryType());
            }
        }

        @Test
        @DisplayName("evaluate() returns true when data matches")
        void evaluateReturnsTrueWhenDataExists() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK WHERE { ?s ?p ?o }");
                assertTrue(q.evaluate());
            }
        }

        @Test
        @DisplayName("evaluate() returns false when no data matches")
        void evaluateReturnsFalseWhenNoMatch() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                        "ASK WHERE { <http://example.org/nobody> ?p ?o }");
                assertFalse(q.evaluate());
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-ASK query string")
        void throwsSyntaxExceptionForSelect() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareBooleanQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // GraphQuery (CONSTRUCT / DESCRIBE)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("prepareGraphQuery")
    class PrepareGraphQuery {

        @Test
        @DisplayName("Returns GraphQuery for a CONSTRUCT query")
        void returnsGraphQueryForConstruct() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
                assertNotNull(q);
                assertEquals(Query.QueryType.GRAPH, q.getQueryType());
            }
        }

        @Test
        @DisplayName("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } returns the stored triple")
        void constructSpoReturnsStoredTriple() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
                GraphQueryResult result = q.evaluate();

                assertTrue(result.hasNext(), "Expected at least one constructed statement");
                Statement stmt = result.next();
                assertEquals(ALICE, stmt.getSubject().stringValue());
                assertEquals(KNOWS, stmt.getPredicate().stringValue());
                assertEquals(BOB,   stmt.getObject().stringValue());
                assertFalse(result.hasNext(), "Expected exactly one statement");
            }
        }

        @Test
        @DisplayName("CONSTRUCT returns empty result when WHERE clause matches nothing")
        void constructReturnsEmptyWhenNoMatch() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s <http://example.org/missing> ?o }");
                GraphQueryResult result = q.evaluate();
                assertFalse(result.hasNext(), "Expected no statements when WHERE has no match");
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-CONSTRUCT/DESCRIBE query string")
        void throwsSyntaxExceptionForSelect() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareGraphQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Update (SPARQL UPDATE)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("prepareUpdate")
    class PrepareUpdate {

        @Test
        @DisplayName("Returns Update for a valid INSERT DATA")
        void returnsUpdateForInsertData() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(QueryLanguage.SPARQL,
                        "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");
                assertNotNull(u);
            }
        }

        @Test
        @DisplayName("INSERT DATA inserts a triple into the store")
        void insertDataInsertsTriple() throws Exception {
            String carol = "http://example.org/carol";
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(QueryLanguage.SPARQL,
                        "INSERT DATA { <" + ALICE + "> <" + KNOWS + "> <" + carol + "> }");
                u.execute();
            }
            // verify via SELECT
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL,
                        "SELECT ?o WHERE { <" + ALICE + "> <" + KNOWS + "> ?o }");
                TupleQueryResult result = q.evaluate();
                boolean foundCarol = false;
                while (result.hasNext()) {
                    if (carol.equals(result.next().getValue("o").stringValue())) {
                        foundCarol = true;
                    }
                }
                assertTrue(foundCarol, "INSERT DATA should have added alice→knows→carol");
            }
        }

        @Test
        @DisplayName("DELETE DATA removes a triple from the store")
        void deleteDataRemovesTriple() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(QueryLanguage.SPARQL,
                        "DELETE DATA { <" + ALICE + "> <" + KNOWS + "> <" + BOB + "> }");
                u.execute();
            }
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                        "ASK WHERE { <" + ALICE + "> <" + KNOWS + "> <" + BOB + "> }");
                assertFalse(q.evaluate(), "DELETE DATA should have removed alice→knows→bob");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Operation API (bindings, dataset, timeout, inferredStatements)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Operation API")
    class OperationApi {

        @Test
        @DisplayName("setBinding / getBindings round-trip")
        void bindingsRoundTrip() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                Value v = vf.createIRI("http://example.org/x");

                q.setBinding("x", v);

                assertTrue(q.getBindings().hasBinding("x"));
                assertEquals(v, q.getBindings().getValue("x"));
            }
        }

        @Test
        @DisplayName("removeBinding removes the binding")
        void removeBindingWorks() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                Value v = vf.createIRI("http://example.org/x");

                q.setBinding("x", v);
                q.removeBinding("x");

                assertFalse(q.getBindings().hasBinding("x"));
            }
        }

        @Test
        @DisplayName("clearBindings removes all bindings")
        void clearBindingsWorks() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");

                q.setBinding("x", vf.createIRI("http://example.org/x"));
                q.setBinding("y", vf.createIRI("http://example.org/y"));
                q.clearBindings();

                assertFalse(q.getBindings().hasBinding("x"));
                assertFalse(q.getBindings().hasBinding("y"));
            }
        }

        @Test
        @DisplayName("setDataset / getDataset round-trip")
        void datasetRoundTrip() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                Dataset ds = new CoreseDataset().addDefaultGraph("http://example.org/g");

                q.setDataset(ds);

                assertSame(ds, q.getDataset());
            }
        }

        @Test
        @DisplayName("setMaxExecutionTime / getMaxExecutionTime round-trip")
        void timeoutRoundTrip() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");

                q.setMaxExecutionTime(30);

                assertEquals(30, q.getMaxExecutionTime());
            }
        }

        @Test
        @DisplayName("setIncludeInferred defaults to true")
        void includeInferredDefaultsToTrue() throws Exception {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
                assertTrue(q.getIncludeInferred());
            }
        }
    }


    @Nested
    @DisplayName("Connection lifecycle")
    class ConnectionLifecycle {

        @Test
        @DisplayName("Connection is open after creation")
        void openAfterCreation() throws RepositoryException {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertTrue(conn.isOpen());
            }
        }

        @Test
        @DisplayName("Connection is closed after close()")
        void closedAfterClose() throws RepositoryException {
            RepositoryConnection conn = repository.getConnection();
            conn.close();
            assertFalse(conn.isOpen());
        }

        @Test
        @DisplayName("prepareTupleQuery on closed connection throws RepositoryException")
        void prepareOnClosedConnectionThrows() throws RepositoryException {
            RepositoryConnection conn = repository.getConnection();
            conn.close();
            assertThrows(RepositoryException.class, () ->
                    conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }"));
        }

        @Test
        @DisplayName("Dataset scoped at connection level")
        void connectionDatasetRoundTrip() throws RepositoryException {
            try (RepositoryConnection conn = repository.getConnection()) {
                Dataset ds = new CoreseDataset().addNamedGraph("http://example.org/g");
                conn.setDataset(ds);
                assertSame(ds, conn.getDataset());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private IRI iri(String iri) {
        return vf.createIRI(iri);
    }
}
