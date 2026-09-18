package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end queryform pipeline tests. */
class NextSparqlPipelineQueryFormExecutorTest extends PipelineTestSupport {

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

    // -------------------------------------------------------------------------
    // Initial bindings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ASK with initial binding returns true when bound triple exists")
    void askWithInitialBindingReturnsTrueWhenMatch() {
        BindingSet bindings = singleBinding("s", iri(ALICE));
        assertTrue(executor.evaluateBoolean(
                "ASK { ?s <" + KNOWS + "> <" + BOB + "> }",
                bindings, null, 0L));
    }

    @Test
    @DisplayName("ASK with initial binding returns false when bound triple absent")
    void askWithInitialBindingReturnsFalseWhenNoMatch() {
        BindingSet bindings = singleBinding("s", iri(BOB)); // bob knows nobody
        assertFalse(executor.evaluateBoolean(
                "ASK { ?s <" + KNOWS + "> ?o }",
                bindings, null, 0L));
    }

    // -------------------------------------------------------------------------
    // Dataset restriction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CONSTRUCT query materialises triples from WHERE bindings")
    void constructQueryMaterialisesTriples() {
        GraphQueryResult result = executor.evaluateGraph("""
                CONSTRUCT { ?s <http://example.org/knows> ?o }
                WHERE     { ?s <http://example.org/knows> ?o }
                """);

        assertTrue(result.hasNext());
        Statement stmt = result.next();
        assertEquals(ALICE, stmt.getSubject().stringValue());
        assertEquals(KNOWS, stmt.getPredicate().stringValue());
        assertEquals(BOB,   stmt.getObject().stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("CONSTRUCT with no matching WHERE returns empty graph result")
    void constructWithNoMatchReturnsEmptyResult() {
        GraphQueryResult result = executor.evaluateGraph("""
                CONSTRUCT { ?s <http://example.org/knows> ?o }
                WHERE     { ?s <http://example.org/likes> ?o }
                """);

        assertFalse(result.hasNext());
    }

}
