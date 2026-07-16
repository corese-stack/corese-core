package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the minimal autonomous SELECT/ASK execution path.
 *
 * <p>These tests intentionally use {@link MemoryStorageManager} instead of
 * mocks, so they exercise the parser, AST-to-KGRAM bridge, KGRAM evaluation,
 * {@code StorageManagerProducer}, and result adaptation together.</p>
 */
class NextSparqlPipelineExecutorTest {

    private static final String ALICE = "http://example.org/alice";
    private static final String BOB = "http://example.org/bob";
    private static final String KNOWS = "http://example.org/knows";
    private static final String NAME = "http://example.org/name";

    private ValueFactory valueFactory;
    private MemoryStorageManager storage;
    private NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        valueFactory = new CoreseAdaptedValueFactory();
        storage = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);

        insert(iri(ALICE), iri(KNOWS), iri(BOB));
    }

    @Test
    @DisplayName("SELECT * WHERE { ?s ?p ?o } runs through the next pipeline")
    void selectSpoRunsEndToEnd() {
        TupleQueryResult result = executor.evaluateTuple("SELECT * WHERE { ?s ?p ?o }");

        assertEquals(List.of("s", "p", "o"), result.getBindingNames());
        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("s").stringValue());
        assertEquals(KNOWS, binding.getValue("p").stringValue());
        assertEquals(BOB, binding.getValue("o").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("SELECT with repeated variables joins BGP patterns end-to-end")
    void selectJoinRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?s ?name WHERE {
                  ?s <http://example.org/knows> ?friend .
                  ?friend <http://example.org/name> ?name .
                }
                """);

        assertEquals(List.of("s", "name"), result.getBindingNames());
        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("s").stringValue());
        assertEquals("Bob", binding.getValue("name").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("ASK WHERE { ?s ?p ?o } returns true when data exists")
    void askSpoReturnsTrueWhenDataExists() {
        assertTrue(executor.evaluateBoolean("ASK WHERE { ?s ?p ?o }"));
    }

    @Test
    @DisplayName("ASK WHERE { <s> <p> <missing> } returns false")
    void askSpoReturnsFalseWhenNoDataExists() {
        assertFalse(executor.evaluateBoolean("""
                ASK WHERE {
                  <http://example.org/alice> <http://example.org/knows> <http://example.org/missing>
                }
                """));
    }

    @Test
    @DisplayName("Tuple evaluation rejects non-SELECT queries")
    void tupleEvaluationRejectsNonSelectQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.evaluateTuple("ASK WHERE { ?s ?p ?o }"));
    }

    @Test
    @DisplayName("Boolean evaluation rejects non-ASK queries")
    void booleanEvaluationRejectsNonAskQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.evaluateBoolean("SELECT * WHERE { ?s ?p ?o }"));
    }

    private void insert(Resource subject, IRI predicate, Value object) {
        storage.getMutationOperations().insertStatement(valueFactory.createStatement(subject, predicate, object));
    }

    private IRI iri(String iri) {
        return valueFactory.createIRI(iri);
    }
}
