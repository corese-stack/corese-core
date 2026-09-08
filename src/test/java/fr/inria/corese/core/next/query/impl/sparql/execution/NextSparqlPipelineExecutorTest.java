package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        valueFactory = Values.factory();
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
    @DisplayName("FILTER evaluates native numeric expressions")
    void filterRunsThroughNativeExpressionEvaluator() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(42));
        insert(iri(BOB), iri(age), valueFactory.createLiteral(18));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/age> ?age .
                  FILTER(?age >= 21)
                }
                ORDER BY ?person
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("BIND evaluates native arithmetic and exposes the result")
    void bindRunsThroughNativeExpressionEvaluator() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(41));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextAge WHERE {
                  <http://example.org/alice> <http://example.org/age> ?age .
                  BIND(?age + 1 AS ?nextAge)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(42, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("nextAge")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("ORDER BY uses numeric value order rather than lexical order")
    void orderByUsesNativeRdfValueOrder() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?rank WHERE { ?person <http://example.org/rank> ?rank }
                ORDER BY ?rank
                """);

        assertEquals(2, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("rank")).intValue());
        assertEquals(10, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("rank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER numeric comparison does not reuse RDF term tie-breakers")
    void filterUsesValueComparisonInsteadOfTotalTermOrder() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(1));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?rank WHERE {
                  <http://example.org/alice> <http://example.org/rank> ?rank .
                  FILTER(?rank < 1.0)
                }
                """);

        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("STRLEN counts Unicode code points through the native evaluator")
    void stringLengthUsesUnicodeCodePoints() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("A🙂"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?length WHERE {
                  <http://example.org/alice> <http://example.org/name> ?name .
                  BIND(STRLEN(?name) AS ?length)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(2, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("length")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("SUBSTR uses Unicode code-point positions and preserves the language tag")
    void substringUsesUnicodeCodePointPositions() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("A🙂B", "fr"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?part WHERE {
                  <http://example.org/alice> <http://example.org/name> ?name .
                  BIND(SUBSTR(?name, 2, 1) AS ?part)
                }
                """);

        Literal part = (Literal) result.next().getValue("part");
        assertEquals("🙂", part.getLabel());
        assertEquals("fr", part.getLanguage().orElseThrow());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("NOW returns one stable value throughout a query evaluation")
    void nowIsStableWithinOneQuery() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?first ?second WHERE {
                  ?s ?p ?o .
                  BIND(NOW() AS ?first)
                  BIND(NOW() AS ?second)
                }
                """);

        var binding = result.next();
        assertTrue(binding.getValue("first").sameTerm(binding.getValue("second")));
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Logical operators apply the SPARQL error truth table")
    void logicalOperatorsApplySparqlErrorTruthTable() {
        assertFalse(executor.evaluateTuple("""
                SELECT ?s WHERE { ?s ?p ?o . FILTER((1 / 0) && false) }
                """).hasNext());
        assertTrue(executor.evaluateTuple("""
                SELECT ?s WHERE { ?s ?p ?o . FILTER((1 / 0) || true) }
                """).hasNext());
    }

    @Test
    @DisplayName("Arithmetic preserves SPARQL floating-point type promotion")
    void arithmeticPreservesFloatingPointPromotion() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?sum WHERE { ?s ?p ?o . BIND(1e0 + 1 AS ?sum) }
                """);

        Literal sum = (Literal) result.next().getValue("sum");
        assertEquals(XSDDatatype.DOUBLE, sum.getCoreDatatype());
        assertEquals(2.0d, sum.doubleValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER REGEX evaluates without the historical expression interpreter")
    void regexFilterRunsThroughNativeExpressionEvaluator() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/name> ?name .
                  FILTER(REGEX(?name, "^ali", "i"))
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER EXISTS evaluates as a correlated native graph pattern")
    void correlatedExistsFilterRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/knows> ?friend .
                  FILTER EXISTS { ?friend <http://example.org/name> ?name }
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER NOT EXISTS rejects solutions with a correlated match")
    void correlatedNotExistsFilterRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/knows> ?friend .
                  FILTER NOT EXISTS { ?friend <http://example.org/name> ?name }
                }
                """);

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

    // -------------------------------------------------------------------------
    // Initial bindings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SELECT with initial binding filters results to bound variable value")
    void selectWithInitialBindingFiltersResults() {
        // bob also knows carol — without binding both alice→bob and bob→carol would match ?s
        insert(iri(BOB), iri(KNOWS), iri("http://example.org/carol"));

        BindingSet bindings = singleBinding("s", iri(ALICE));
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?friend WHERE { ?s <" + KNOWS + "> ?friend }",
                bindings, null, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("friend").stringValue());
        assertFalse(result.hasNext(), "Only alice's friends should be returned");
    }

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
    @DisplayName("SELECT with dataset FROM restricts results to the named graph")
    void selectWithDatasetFromRestrictsToNamedGraph() {
        String graph1 = "http://example.org/graph1";
        String graph2 = "http://example.org/graph2";
        String carol = "http://example.org/carol";

        // alice→knows→bob in graph1, alice→knows→carol in graph2
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri(graph1));
        insertInGraph(iri(ALICE), iri(KNOWS), iri(carol), iri(graph2));

        Dataset dataset = Dataset.builder().defaultGraph(iri(graph1)).build();
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?o WHERE { <" + ALICE + "> <" + KNOWS + "> ?o }",
                null, dataset, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("o").stringValue());
        assertFalse(result.hasNext(), "Only data from graph1 should be visible");
    }

    @Test
    @DisplayName("SELECT with dataset FROM returns nothing when named graph is empty")
    void selectWithDatasetFromReturnsNothingForEmptyNamedGraph() {
        String emptyGraph = "http://example.org/empty";

        Dataset dataset = Dataset.builder().defaultGraph(iri(emptyGraph)).build();
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, dataset, 0L);

        assertFalse(result.hasNext(), "No results expected for an empty named graph");
    }

    @Test
    @DisplayName("An explicit empty dataset hides repository data")
    void explicitEmptyDatasetHasNoDefaultGraph() {
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, Dataset.empty(), 0L);

        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("A named-only dataset has an empty default graph")
    void namedOnlyDatasetHasNoDefaultGraph() {
        String graph = "http://example.org/graph";
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri(graph));
        Dataset dataset = Dataset.builder().namedGraph(iri(graph)).build();

        TupleQueryResult defaultResult = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }", null, dataset, 0L);

        assertFalse(defaultResult.hasNext());
    }

    // -------------------------------------------------------------------------
    // CONSTRUCT / graph evaluation
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

    @Test
    @DisplayName("Graph evaluation rejects non-CONSTRUCT/DESCRIBE queries")
    void graphEvaluationRejectsSelectQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.evaluateGraph("SELECT * WHERE { ?s ?p ?o }"));
    }

    // -------------------------------------------------------------------------
    // Initial bindings — literal and blank-node values
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SELECT with literal initial binding filters results by literal value")
    void selectWithLiteralInitialBindingFiltersResults() {
        insert(iri(BOB),   iri(NAME), valueFactory.createLiteral("Bob"));
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));

        BindingSet bindings = singleBinding("name", valueFactory.createLiteral("Bob"));
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + NAME + "> ?name }",
                bindings, null, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("s").stringValue());
        assertFalse(result.hasNext(), "Only the triple with literal 'Bob' should match");
    }

    // -------------------------------------------------------------------------
    // Timeout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Query with generous timeout completes normally")
    void queryWithGenerousTimeoutCompletesNormally() {
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, 10_000L); // 10 seconds — far more than needed

        assertTrue(result.hasNext(), "Result should be non-empty");
    }

    @Test
    @DisplayName("Query with zero timeout runs without timeout enforcement")
    void queryWithZeroTimeoutRunsWithoutEnforcement() {
        // timeout = 0 means disabled; must complete normally
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, 0L);

        assertTrue(result.hasNext(), "Result should be non-empty");
    }

    @Test
    @DisplayName("QueryTimeoutException is thrown when evaluation exceeds deadline")
    void queryTimeoutExceptionThrownWhenDeadlineExceeded() {
        // Insert many triples to create a cross-product query that takes longer than 1 ms.
        for (int i = 0; i < 200; i++) {
            insert(iri("http://example.org/s" + i), iri(KNOWS), iri("http://example.org/o" + i));
        }

        // Cross-product of 201 triples × 201 triples = 40 401 combinations — expensive enough
        // to reliably exceed a 1 ms timeout on any machine.
        assertThrows(QueryTimeoutException.class, this::evaluateAndConsumeTimeoutQuery);
    }

    private void evaluateAndConsumeTimeoutQuery() {
        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 }",
                null, null, 1L)) {
            result.stream().count();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void insert(Resource subject, IRI predicate, Value object) {
        storage.mutations().add(valueFactory.createStatement(subject, predicate, object));
    }

    private void insertInGraph(Resource subject, IRI predicate, Value object, Resource context) {
        storage.mutations().add(
                valueFactory.createStatement(subject, predicate, object, context));
    }

    private IRI iri(String iri) {
        return valueFactory.createIRI(iri);
    }

    /**
     * Creates a one-entry {@link BindingSet} binding {@code varName} to {@code value}.
     */
    private BindingSet singleBinding(String varName, Value value) {
        Binding b = new Binding() {
            @Override public String name()  { return varName; }
            @Override public Value value() { return value; }
        };
        return new BindingSet() {
            @Override public Set<String>      getBindingNames()         { return Set.of(varName); }
            @Override public boolean          hasBinding(String name)   { return varName.equals(name); }
            @Override public Value            getValue(String name)     { return varName.equals(name) ? value : null; }
            @Override public Iterator<Binding> iterator()               { return List.of(b).iterator(); }
        };
    }
}
