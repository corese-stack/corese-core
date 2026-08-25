package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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
    private CoreseRepository repository;

    @BeforeEach
    void setUp() throws RepositoryException {
        vf = new CoreseValueFactory();
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        repository = new CoreseRepository(storage);

        // Insert one triple: alice knows bob
        storage.getMutationOperations().insertStatement(
                vf.createStatement(iri(ALICE), iri(KNOWS), iri(BOB)));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    // -------------------------------------------------------------------------
    // TupleQuery (SELECT)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("prepareTupleQuery")
    class PrepareTupleQuery {

        @Test
        @DisplayName("Returns TupleQuery for a SELECT query")
        void returnsTupleQueryForSelect() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                assertNotNull(q);
            }
        }

        @Test
        @DisplayName("evaluate() returns the matching triples")
        void evaluateReturnsMatchingTriples() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                try (TupleQueryResult result = q.evaluate()) {
                    assertEquals(List.of("s", "p", "o"), result.getBindingNames());
                    assertTrue(result.hasNext());
                    BindingSet bs = result.next();
                    assertEquals(ALICE, bs.getValue("s").stringValue());
                    assertEquals(KNOWS, bs.getValue("p").stringValue());
                    assertEquals(BOB, bs.getValue("o").stringValue());
                    assertFalse(result.hasNext());
                }
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-SELECT query string")
        void throwsSyntaxExceptionForAsk() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareTupleQuery("ASK WHERE { ?s ?p ?o }"));
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for invalid SPARQL")
        void throwsSyntaxExceptionForInvalidSparql() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareTupleQuery("THIS IS NOT SPARQL"));
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
        void returnsBooleanQueryForAsk() {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery("ASK WHERE { ?s ?p ?o }");
                assertNotNull(q);
            }
        }

        @Test
        @DisplayName("evaluate() returns true when data matches")
        void evaluateReturnsTrueWhenDataExists() {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery("ASK WHERE { ?s ?p ?o }");
                assertTrue(q.evaluate());
            }
        }

        @Test
        @DisplayName("evaluate() returns false when no data matches")
        void evaluateReturnsFalseWhenNoMatch() {
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(
                        "ASK WHERE { <http://example.org/nobody> ?p ?o }");
                assertFalse(q.evaluate());
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-ASK query string")
        void throwsSyntaxExceptionForSelect() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareBooleanQuery("SELECT * WHERE { ?s ?p ?o }"));
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
        void returnsGraphQueryForConstruct() {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
                assertNotNull(q);
            }
        }

        @Test
        @DisplayName("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } returns the stored triple")
        void constructSpoReturnsStoredTriple() {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
                try (GraphQueryResult result = q.evaluate()) {
                    assertTrue(result.hasNext(), "Expected at least one constructed statement");
                    Statement stmt = result.next();
                    assertEquals(ALICE, stmt.getSubject().stringValue());
                    assertEquals(KNOWS, stmt.getPredicate().stringValue());
                    assertEquals(BOB,   stmt.getObject().stringValue());
                    assertFalse(result.hasNext(), "Expected exactly one statement");
                }
            }
        }

        @Test
        @DisplayName("CONSTRUCT returns empty result when WHERE clause matches nothing")
        void constructReturnsEmptyWhenNoMatch() {
            try (RepositoryConnection conn = repository.getConnection()) {
                GraphQuery q = conn.prepareGraphQuery(
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s <http://example.org/missing> ?o }");
                try (GraphQueryResult result = q.evaluate()) {
                    assertFalse(result.hasNext(), "Expected no statements when WHERE has no match");
                }
            }
        }

        @Test
        @DisplayName("Throws QuerySyntaxException for a non-CONSTRUCT/DESCRIBE query string")
        void throwsSyntaxExceptionForSelect() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertThrows(QuerySyntaxException.class, () ->
                        conn.prepareGraphQuery("SELECT * WHERE { ?s ?p ?o }"));
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
        void returnsUpdateForInsertData() {
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(
                        "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");
                assertNotNull(u);
            }
        }

        @Test
        @DisplayName("INSERT DATA inserts a triple into the store")
        void insertDataInsertsTriple() {
            String carol = "http://example.org/carol";
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(
                        "INSERT DATA { <" + ALICE + "> <" + KNOWS + "> <" + carol + "> }");
                u.execute();
            }
            // verify via SELECT
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery(
                        "SELECT ?o WHERE { <" + ALICE + "> <" + KNOWS + "> ?o }");
                boolean foundCarol = false;
                try (TupleQueryResult result = q.evaluate()) {
                    while (result.hasNext()) {
                        if (carol.equals(result.next().getValue("o").stringValue())) {
                            foundCarol = true;
                        }
                    }
                }
                assertTrue(foundCarol, "INSERT DATA should have added alice→knows→carol");
            }
        }

        @Test
        @DisplayName("DELETE DATA removes a triple from the store")
        void deleteDataRemovesTriple() {
            try (RepositoryConnection conn = repository.getConnection()) {
                Update u = conn.prepareUpdate(
                        "DELETE DATA { <" + ALICE + "> <" + KNOWS + "> <" + BOB + "> }");
                u.execute();
            }
            try (RepositoryConnection conn = repository.getConnection()) {
                BooleanQuery q = conn.prepareBooleanQuery(
                        "ASK WHERE { <" + ALICE + "> <" + KNOWS + "> <" + BOB + "> }");
                assertFalse(q.evaluate(), "DELETE DATA should have removed alice→knows→bob");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Query options (bindings, dataset, timeout)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Query options")
    class QueryOptions {

        @Test
        @DisplayName("setBinding / getBindings round-trip")
        void bindingsRoundTrip() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                Value v = vf.createIRI("http://example.org/x");

                q.setBinding("x", v);

                assertTrue(q.getBindings().hasBinding("x"));
                assertEquals(v, q.getBindings().getValue("x"));
            }
        }

        @Test
        @DisplayName("removeBinding removes the binding")
        void removeBindingWorks() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                Value v = vf.createIRI("http://example.org/x");

                q.setBinding("x", v);
                q.removeBinding("x");

                assertFalse(q.getBindings().hasBinding("x"));
            }
        }

        @Test
        @DisplayName("clearBindings removes all bindings")
        void clearBindingsWorks() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");

                q.setBinding("x", vf.createIRI("http://example.org/x"));
                q.setBinding("y", vf.createIRI("http://example.org/y"));
                q.clearBindings();

                assertFalse(q.getBindings().hasBinding("x"));
                assertFalse(q.getBindings().hasBinding("y"));
            }
        }

        @Test
        @DisplayName("setDataset / getDataset round-trip")
        void datasetRoundTrip() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                Dataset ds = Dataset.builder().defaultGraph(iri("http://example.org/g")).build();

                q.setDataset(ds);

                assertSame(ds, q.getDataset());
            }
        }

        @Test
        @DisplayName("setTimeout / getTimeout round-trip")
        void timeoutRoundTrip() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");

                q.setTimeout(Duration.ofSeconds(30));

                assertEquals(Duration.ofSeconds(30), q.getTimeout());
            }
        }

        @Test
        @DisplayName("Negative timeouts are rejected")
        void negativeTimeoutRejected() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                Duration negativeTimeout = Duration.ofMillis(-1);
                assertThrows(IllegalArgumentException.class,
                        () -> q.setTimeout(negativeTimeout));
            }
        }

        @Test
        @DisplayName("Binding names do not include the SPARQL variable prefix")
        void bindingPrefixRejected() {
            try (RepositoryConnection conn = repository.getConnection()) {
                TupleQuery q = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
                Value value = vf.createIRI("http://example.org/x");

                assertThrows(IllegalArgumentException.class, () -> q.setBinding("?s", value));
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
                    conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }"));
        }

        @Test
        @DisplayName("Prepared operations are invalidated when their connection closes")
        void preparedOperationInvalidatedAfterClose() {
            RepositoryConnection conn = repository.getConnection();
            TupleQuery query = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o }");
            conn.close();

            assertThrows(RepositoryException.class, query::evaluate);
        }

        @Test
        @DisplayName("Dataset scoped at connection level")
        void connectionDatasetRoundTrip() throws RepositoryException {
            try (RepositoryConnection conn = repository.getConnection()) {
                Dataset ds = Dataset.builder().namedGraph(iri("http://example.org/g")).build();
                conn.setDataset(ds);
                assertSame(ds, conn.getDataset());
            }
        }

        @Test
        @DisplayName("Memory repositories report that transactions are unsupported")
        void transactionsAreReportedAsUnsupported() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertFalse(conn.supportsTransactions());
                assertFalse(conn.isActive());
                assertThrows(RepositoryException.class, conn::begin);
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
