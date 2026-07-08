package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextSparqlPipelineExecutorTest {

    private static final String ALICE = "http://example.org/alice";
    private static final String BOB = "http://example.org/bob";
    private static final String KNOWS = "http://example.org/knows";

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
    @DisplayName("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } materializes next statements")
    void constructSpoRunsEndToEnd() {
        GraphQueryResult result = executor.evaluateGraph("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");

        List<Statement> statements = result.asList();
        assertEquals(1, statements.size());
        assertEquals(ALICE, statements.getFirst().getSubject().stringValue());
        assertEquals(KNOWS, statements.getFirst().getPredicate().stringValue());
        assertEquals(BOB, statements.getFirst().getObject().stringValue());
    }

    @Test
    @DisplayName("CONSTRUCT templates with blank nodes fail explicitly")
    void constructBlankNodeTemplateFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.evaluateGraph("""
                        CONSTRUCT { _:b <http://example.org/knows> ?o }
                        WHERE { ?s <http://example.org/knows> ?o }
                        """));

        assertTrue(error.getMessage().contains("Blank nodes in CONSTRUCT"));
    }

    @Test
    @DisplayName("DESCRIBE ?s WHERE { ?s ?p ?o } follows the minimal outgoing/incoming strategy")
    void describeSpoRunsEndToEnd() {
        GraphQueryResult result = executor.evaluateGraph("DESCRIBE ?s WHERE { ?s ?p ?o }");

        List<Statement> statements = result.asList();
        assertEquals(1, statements.size());
        assertEquals(ALICE, statements.getFirst().getSubject().stringValue());
        assertEquals(KNOWS, statements.getFirst().getPredicate().stringValue());
        assertEquals(BOB, statements.getFirst().getObject().stringValue());
    }

    @Test
    @DisplayName("INSERT DATA writes through StorageManager")
    void insertDataWritesThroughStorageManager() {
        executor.executeUpdate("""
                INSERT DATA {
                  <http://example.org/bob> <http://example.org/knows> <http://example.org/alice>
                }
                """);

        assertTrue(storage.getQueryOperations().exists(StatementPattern.of(iri(BOB), iri(KNOWS), iri(ALICE))));
    }

    @Test
    @DisplayName("INSERT DATA with prefixed names fails explicitly")
    void insertDataPrefixedNameFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("""
                        PREFIX ex: <http://example.org/>
                        INSERT DATA { ex:bob ex:knows ex:alice }
                        """));

        assertTrue(error.getMessage().contains("prefixed names are not resolved here"));
    }

    @Test
    @DisplayName("INSERT DATA with relative IRIs fails explicitly")
    void insertDataRelativeIriFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("""
                        INSERT DATA {
                          <bob> <http://example.org/knows> <http://example.org/alice>
                        }
                        """));

        assertTrue(error.getMessage().contains("must be absolute"));
    }

    @Test
    @DisplayName("INSERT DATA with prefixed literal datatype fails explicitly")
    void insertDataPrefixedDatatypeFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("""
                        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                        INSERT DATA {
                          <http://example.org/bob> <http://example.org/age> "42"^^xsd:integer
                        }
                        """));

        assertTrue(error.getMessage().contains("prefixed datatypes are not resolved here"));
    }

    @Test
    @DisplayName("INSERT DATA with escaped string literal fails explicitly")
    void insertDataEscapedLiteralFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("""
                        INSERT DATA {
                          <http://example.org/bob> <http://example.org/name> "Bo\\nb"
                        }
                        """));

        assertTrue(error.getMessage().contains("Escaped string literals are not supported yet"));
    }

    @Test
    @DisplayName("INSERT DATA with GRAPH blocks fails explicitly")
    void insertDataGraphBlockFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("""
                        INSERT DATA {
                          GRAPH <http://example.org/g> {
                            <http://example.org/bob> <http://example.org/knows> <http://example.org/alice>
                          }
                        }
                        """));

        assertTrue(error.getMessage().contains("GRAPH blocks"));
    }

    @Test
    @DisplayName("DELETE DATA deletes through StorageManager")
    void deleteDataDeletesThroughStorageManager() {
        executor.executeUpdate("""
                DELETE DATA {
                  <http://example.org/alice> <http://example.org/knows> <http://example.org/bob>
                }
                """);

        assertFalse(storage.getQueryOperations().exists(StatementPattern.of(iri(ALICE), iri(KNOWS), iri(BOB))));
    }

    @Test
    @DisplayName("Unsupported update operations fail explicitly")
    void unsupportedUpdateOperationFailsExplicitly() {
        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> executor.executeUpdate("CLEAR DEFAULT"));

        assertTrue(error.getMessage().contains("not supported yet"));
    }

    private void insert(Resource subject, IRI predicate, Value object) {
        storage.getMutationOperations().insertStatement(valueFactory.createStatement(subject, predicate, object));
    }

    private IRI iri(String iri) {
        return valueFactory.createIRI(iri);
    }
}
