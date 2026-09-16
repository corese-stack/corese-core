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
    @DisplayName("BIND in a UNION branch leaves references outside its group unbound")
    void bindInUnionBranchDoesNotReceiveOuterBindings() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(1));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextAge WHERE {
                  ?person <http://example.org/age> ?age .
                  { BIND(?age + 1 AS ?nextAge) } UNION { BIND(?age + 2 AS ?nextAge) }
                }
                """);

        var rows = result.stream().toList();
        assertEquals(2, rows.size());
        assertTrue(rows.stream().noneMatch(binding -> binding.hasBinding("nextAge")));
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
    @DisplayName("Inline VALUES preserves row order, duplicates, and UNDEF bindings")
    void inlineValuesPreservesBagSemanticsAndUndef() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?rank WHERE {
                  VALUES (?person ?rank) {
                    (<http://example.org/alice> 1)
                    (<http://example.org/alice> 1)
                    (<http://example.org/bob> UNDEF)
                  }
                }
                """);

        assertEquals(List.of("person", "rank"), result.getBindingNames());
        var first = result.next();
        assertEquals(ALICE, first.getValue("person").stringValue());
        assertEquals(1, ((Literal) first.getValue("rank")).intValue());
        var duplicate = result.next();
        assertEquals(ALICE, duplicate.getValue("person").stringValue());
        assertEquals(1, ((Literal) duplicate.getValue("rank")).intValue());
        var unbound = result.next();
        assertEquals(BOB, unbound.getValue("person").stringValue());
        assertFalse(unbound.hasBinding("rank"));
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("VALUES joins graph patterns at its position and rejects incompatible mappings")
    void inlineValuesJoinsGraphPatterns() {
        insert(iri(BOB), iri(KNOWS), iri(ALICE));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?friend WHERE {
                  VALUES ?person { <http://example.org/alice> <http://example.org/missing> }
                  ?person <http://example.org/knows> ?friend .
                }
                """);

        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("person").stringValue());
        assertEquals(BOB, binding.getValue("friend").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Query-level VALUES constrains the completed WHERE result")
    void queryLevelValuesConstrainWhereResults() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?friend WHERE {
                  ?person <http://example.org/knows> ?friend .
                }
                VALUES ?person { <http://example.org/alice> }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Query-level VALUES constrains a semicolon-expanded basic graph pattern")
    void queryLevelValuesConstrainSemicolonExpandedPattern() {
        String title = "http://purl.org/dc/elements/1.1/title";
        String price = "http://example.org/ns#price";
        String secondBook = "http://example.org/book/book2";
        insert(iri(ALICE), iri(title), valueFactory.createLiteral("first"));
        insert(iri(ALICE), iri(price), valueFactory.createLiteral(1));
        insert(iri(secondBook), iri(title), valueFactory.createLiteral("second"));
        insert(iri(secondBook), iri(price), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                PREFIX : <http://example.org/book/>
                PREFIX ns: <http://example.org/ns#>
                SELECT ?book ?title ?price {
                  ?book dc:title ?title ; ns:price ?price .
                }
                VALUES ?book { <http://example.org/alice> }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("book").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("BIND chains after VALUES and remains visible to following graph patterns")
    void bindAfterValuesIsVisibleToFollowingPatterns() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?nextRank WHERE {
                  VALUES ?person { <http://example.org/alice> }
                  ?person <http://example.org/rank> ?rank .
                  BIND(?rank + 1 AS ?nextRank)
                  VALUES ?nextRank { 3 }
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(3, ((Literal) result.next().getValue("nextRank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("VALUES UNDEF leaves an incoming binding available to following operations")
    void valuesUndefDoesNotClearIncomingBinding() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextRank WHERE {
                  ?person <http://example.org/rank> ?rank .
                  VALUES (?person ?rank) { (<http://example.org/alice> UNDEF) }
                  BIND(?rank + 1 AS ?nextRank)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(3, ((Literal) result.next().getValue("nextRank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("BIND expression errors keep the input solution with an unbound target")
    void bindExpressionErrorLeavesTargetUnbound() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?value WHERE {
                  VALUES ?person { <http://example.org/alice> }
                  BIND(1 / 0 AS ?value)
                }
                """);

        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("person").stringValue());
        assertFalse(binding.hasBinding("value"));
        assertFalse(result.hasNext());
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
    @DisplayName("temporalProximity01: NOT EXISTS excludes only earlier examinations")
    void temporalProximity01() {
        String ex = "http://www.w3.org/2009/sparql/docs/tests/data-sparql11/negation#";
        IRI type = iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        IRI date = iri("http://purl.org/dc/elements/1.1/date");
        for (String exam : List.of("examination1", "examination2")) {
            insert(iri(ex + exam), type, iri(ex + "PhysicalExamination"));
            insert(iri(ex + exam), iri(ex + "precedes"), iri(ex + "operation1"));
        }
        insert(iri(ex + "examination1"), date,
                valueFactory.createLiteral("2010-01-10", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "examination2"), date,
                valueFactory.createLiteral("2010-01-02", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "examination1"), iri(ex + "follows"), iri(ex + "examination2"));
        insert(iri(ex + "examination2"), iri(ex + "precedes"), iri(ex + "examination1"));
        insert(iri(ex + "operation1"), type, iri(ex + "SurgicalProcedure"));
        insert(iri(ex + "operation1"), date,
                valueFactory.createLiteral("2010-01-15", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "operation1"), iri(ex + "follows"), iri(ex + "examination1"));
        insert(iri(ex + "operation1"), iri(ex + "follows"), iri(ex + "examination2"));

        try (var result = executor.evaluateTuple("""
                PREFIX ex: <http://www.w3.org/2009/sparql/docs/tests/data-sparql11/negation#>
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                SELECT ?exam ?date {
                  ?exam a ex:PhysicalExamination; dc:date ?date; ex:precedes ex:operation1 .
                  ?op a ex:SurgicalProcedure; dc:date ?opDT .
                  FILTER NOT EXISTS {
                    ?otherExam a ex:PhysicalExamination;
                               ex:follows ?exam;
                               ex:precedes ex:operation1
                  }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(1, rows.size());
            assertEquals(ex + "examination1", rows.getFirst().getValue("exam").stringValue());
            assertEquals(valueFactory.createLiteral("2010-01-10", XSDDatatype.DATE.getIRI()),
                    rows.getFirst().getValue("date"));
        }
    }

    @Test
    @DisplayName("exists-graph-variable: EXISTS uses the outer graph binding")
    void existsGraphVariable() {
        String ex = "http://www.example.org/";
        IRI graph = iri(ex + "exists-graph-variable.ttl");
        // The W3C fixture is loaded as both default data and a named graph;
        // its relative <> resolves to the fixture's graph IRI.
        insert(iri(ex + "s1"), iri(ex + "p"), graph);
        insert(iri(ex + "s2"), iri(ex + "p"), iri(ex + "o2"));
        insertInGraph(iri(ex + "s1"), iri(ex + "p"), graph, graph);
        insertInGraph(iri(ex + "s2"), iri(ex + "p"), iri(ex + "o2"), graph);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://www.example.org/>
                SELECT ?s WHERE {
                  ?s :p ?g .
                  FILTER EXISTS { GRAPH ?g { ?s2 :p ?o2 } }
                }
                """)) {
            assertEquals(List.of(ex + "s1"),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
    }

    @Test
    @DisplayName("EXISTS waits for its GRAPH variable even after an unrelated BIND")
    void existsGraphVariableWaitsForOuterBinding() {
        String ex = "http://www.example.org/";
        IRI graph = iri(ex + "g");
        insert(iri(ex + "s1"), iri(ex + "p"), graph);
        insert(iri(ex + "s2"), iri(ex + "p"), iri(ex + "missing"));
        insertInGraph(iri(ex + "a"), iri(ex + "q"), iri(ex + "b"), graph);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://www.example.org/>
                SELECT ?s {
                  BIND(1 AS ?unrelated)
                  ?s :p ?g .
                  FILTER EXISTS { GRAPH ?g { ?s2 :q ?o2 } }
                }
                """)) {
            assertEquals(List.of(ex + "s1"),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
    }

    @Test
    @DisplayName("Default graph merge removes duplicate triples, preserving projected and named-graph rows")
    void defaultGraphMergePreservesSolutionMultiplicity() {
        IRI firstGraph = iri("http://example.org/g1");
        IRI secondGraph = iri("http://example.org/g2");
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), firstGraph);
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), secondGraph);
        insertInGraph(iri(ALICE), iri(KNOWS), iri("http://example.org/carol"), secondGraph);
        Dataset dataset = Dataset.builder().defaultGraph(firstGraph).defaultGraph(secondGraph)
                .namedGraph(firstGraph).namedGraph(secondGraph).build();
        try (var result = executor.evaluateTuple(
                "SELECT ?s { ?s <http://example.org/knows> ?o }", null, dataset, 0L)) {
            assertEquals(List.of(ALICE, ALICE),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
        try (var result = executor.evaluateTuple(
                "SELECT ?g { GRAPH ?g { ?s <http://example.org/knows> ?o } }", null, dataset, 0L)) {
            assertEquals(3, result.stream().count());
        }
    }

    @Test
    @DisplayName("EXISTS inside OPTIONAL filters within optional branch rather than being postponed")
    void existsInsideOptionalFiltersLocally() {
        IRI a = iri("http://example.org/a");
        IRI b = iri("http://example.org/b");
        IRI e = iri("http://example.org/e");
        IRI d = iri("http://example.org/d");
        IRI p = iri("http://example.org/p");
        IRI r = iri("http://example.org/r");
        insert(a, p, b);
        insert(a, p, e);
        insert(b, r, d);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://example.org/>
                SELECT ?s ?o {
                  VALUES ?s { :a }
                  OPTIONAL {
                    ?s :p ?o
                    FILTER EXISTS { ?o :r ?z }
                  }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(1, rows.size());
            assertEquals("http://example.org/b", rows.getFirst().getValue("o").stringValue());
        }
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
    @Test
    void valuesInsideGraphPreservesUnboundGraphRows() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g2"));
        try (var result = executor.evaluateTuple("""
                SELECT ?g ?t WHERE {
                  GRAPH ?g {
                    VALUES (?g ?t) { (UNDEF "foo") (<http://example.org/g1> "bar") }
                  }
                }
                """)) {
            assertEquals(List.of("http://example.org/g1:bar", "http://example.org/g1:foo", "http://example.org/g2:foo"),
                    result.stream().map(row -> row.getValue("g").stringValue() + ":" + row.getValue("t").stringValue())
                            .sorted().toList());
        }
    }

    @Test
    void trailingValuesDoesNotSupplyBindingsToWhere() {
        try (var result = executor.evaluateTuple("SELECT ?x ?y { BIND(?x AS ?y) } VALUES ?x { 1 }")) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("x").stringValue());
            assertFalse(row.hasBinding("y"));
            assertEquals(Set.of("x"), row.getBindingNames());
            var bindings = row.iterator();
            assertTrue(bindings.hasNext());
            assertEquals("x", bindings.next().name());
            assertFalse(bindings.hasNext());
            assertFalse(result.hasNext());
        }
    }

    @Test
    void unionPreservesRightBranchWhenLeftIsEmpty() {
        try (var result = executor.evaluateTuple("SELECT ?x { { VALUES ?x {} } UNION { VALUES ?x { 1 1 } } }")) {
            assertEquals(2, result.stream().count());
        }
    }

    @Test
    void selectStarDoesNotExposeExistentialBlankNodeVariables() {
        try (var result = executor.evaluateTuple("SELECT * { [] <http://example.org/knows> ?friend }")) {
            assertEquals(List.of("friend"), result.getBindingNames());
            assertTrue(result.hasNext());
            assertEquals(Set.of("friend"), result.next().getBindingNames());
        }
    }

    @Test
    void incompatibleValuesRowAfterUndefDoesNotPopIncomingBindings() {
        try (var result = executor.evaluateTuple("""
                SELECT ?x ?y ?z {
                  VALUES (?x ?y ?z) { (1 2 3) }
                  VALUES (?x ?y ?z) { (UNDEF 2 4) (UNDEF 2 3) }
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("x").stringValue());
            assertEquals("2", row.getValue("y").stringValue());
            assertEquals("3", row.getValue("z").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    void graphNameIsNotAnInputBindingForItsInnerBind() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        try (var result = executor.evaluateTuple("SELECT ?g ?copy { GRAPH ?g { BIND(?g AS ?copy) } }")) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("http://example.org/g1", row.getValue("g").stringValue());
            assertFalse(row.hasBinding("copy"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    void nestedGroupFilterDoesNotSeeOuterBind() {
        try (var result = executor.evaluateTuple("SELECT ?x { BIND(1 AS ?x) { FILTER(?x = 1) } }")) {
            assertFalse(result.hasNext());
        }
    }

    @Test
    void graphWithValuesRequiresAnExistingNamedGraph() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        assertTrue(executor.evaluateBoolean("ASK { GRAPH <http://example.org/g1> { VALUES ?x { 1 } } }"));
        assertFalse(executor.evaluateBoolean("ASK { GRAPH <http://example.org/missing> { VALUES ?x { 1 } } }"));
        assertFalse(executor.evaluateBoolean("""
                ASK FROM NAMED <http://example.org/other>
                { GRAPH <http://example.org/g1> { VALUES ?x { 1 } } }
                """));
    }

    @Test
    void valuesDistinguishesEmptyTableFromEmptySolution() {
        assertFalse(executor.evaluateBoolean("ASK {} VALUES ?x {}"));
        assertFalse(executor.evaluateBoolean("ASK { VALUES () {} }"));
        assertTrue(executor.evaluateBoolean("ASK { VALUES () { () } }"));
        try (var result = executor.evaluateTuple("SELECT * {} VALUES ?x {}")) {
            assertEquals(List.of("x"), result.getBindingNames());
            assertFalse(result.hasNext());
        }
        try (var result = executor.evaluateTuple("SELECT * { VALUES () { () () } }")) {
            assertEquals(2, result.stream().count());
        }
    }

    @Test
    @DisplayName("GROUP BY and COUNT aggregate count solutions per group")
    void groupByAndCountAggregate() {
        String charlie = "http://example.org/charlie";
        insert(iri(ALICE), iri(KNOWS), iri(charlie));
        insert(iri(BOB), iri(KNOWS), iri(charlie));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?o) AS ?cnt) WHERE {
                    ?s <http://example.org/knows> ?o .
                }
                GROUP BY ?s
                ORDER BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row1 = result.next();
            assertEquals(ALICE, row1.getValue("s").stringValue());
            assertEquals("2", row1.getValue("cnt").stringValue());

            assertTrue(result.hasNext());
            var row2 = result.next();
            assertEquals(BOB, row2.getValue("s").stringValue());
            assertEquals("1", row2.getValue("cnt").stringValue());

            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("COUNT(*) and COUNT(DISTINCT) evaluate properly over whole solution")
    void countStarAndDistinct() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("java"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("sparql"));
        insert(iri(BOB), iri(tag), valueFactory.createLiteral("java"));

        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(*) AS ?total) (COUNT(DISTINCT ?val) AS ?uniqueTags) WHERE {
                    ?s <http://example.org/tag> ?val .
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("3", row.getValue("total").stringValue());
            assertEquals("2", row.getValue("uniqueTags").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("SUM and AVG aggregates promote types and calculate correct totals")
    void sumAndAvgAggregates() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(20));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?score) AS ?total) (AVG(?score) AS ?average) WHERE {
                    ?s <http://example.org/score> ?score .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("30", row.getValue("total").stringValue());
            assertEquals("15", row.getValue("average").stringValue());
            assertEquals(XSDDatatype.DECIMAL.getIRI(), ((Literal) row.getValue("average")).getDatatype());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("MIN, MAX, and SAMPLE aggregates select extreme and sample values")
    void minMaxSampleAggregates() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(5));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(25));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(15));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (MIN(?score) AS ?minVal) (MAX(?score) AS ?maxVal) (SAMPLE(?score) AS ?sampleVal) WHERE {
                    ?s <http://example.org/score> ?score .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("5", row.getValue("minVal").stringValue());
            assertEquals("25", row.getValue("maxVal").stringValue());
            assertTrue(row.hasBinding("sampleVal"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP_CONCAT aggregates strings with custom separator")
    void groupConcatAggregate() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("A"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("B"));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (GROUP_CONCAT(?tag; separator=";") AS ?tags) WHERE {
                    ?s <http://example.org/tag> ?tag .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            String tags = row.getValue("tags").stringValue();
            assertTrue("A;B".equals(tags) || "B;A".equals(tags));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY with expression alias binds alias for projection")
    void groupByExpressionAlias() {
        String val = "http://example.org/val";
        insert(iri(ALICE), iri(val), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(val), valueFactory.createLiteral(10));

        try (var result = executor.evaluateTuple("""
                SELECT ?next (COUNT(?s) AS ?cnt) WHERE {
                    ?s <http://example.org/val> ?v .
                }
                GROUP BY (?v + 1 AS ?next)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("11", row.getValue("next").stringValue());
            assertEquals("2", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("HAVING filters groups based on aggregate condition")
    void havingWithAggregateCondition() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("t1"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("t2"));
        insert(iri(BOB), iri(tag), valueFactory.createLiteral("t1"));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?tag) AS ?cnt) WHERE {
                    ?s <http://example.org/tag> ?tag .
                }
                GROUP BY ?s
                HAVING (COUNT(?tag) > 1)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("2", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Implicit aggregate on empty dataset returns exactly 1 row")
    void implicitAggregateOnEmptyDatasetReturnsOneRow() {
        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(?o) AS ?count) (SUM(?score) AS ?sum) (AVG(?score) AS ?avg) (GROUP_CONCAT(?o) AS ?gc) WHERE {
                    ?s <http://example.org/nonexistent> ?o .
                    OPTIONAL { ?s <http://example.org/score> ?score }
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("0", row.getValue("count").stringValue());
            assertEquals("0", row.getValue("sum").stringValue());
            assertFalse(row.hasBinding("avg"));
            assertEquals("", row.getValue("gc").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY on empty dataset returns 0 rows")
    void groupByOnEmptyDatasetReturnsZeroRows() {
        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?o) AS ?count) WHERE {
                    ?s <http://example.org/nonexistent> ?o .
                }
                GROUP BY ?s
                """)) {
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Projection expression with aggregate arithmetic")
    void projectionExpressionWithAggregateArithmetic() {
        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(?s) AS ?c) ((COUNT(?s) + 5) AS ?cPlus) WHERE {
                    ?s <http://example.org/knows> ?o .
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("c").stringValue());
            assertEquals("6", row.getValue("cPlus").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("ORDER BY with aggregate expression sorts groups")
    void orderByAggregateExpression() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(20));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(30));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?val) AS ?total) WHERE {
                    ?s <http://example.org/score> ?val .
                }
                GROUP BY ?s
                ORDER BY DESC(?total)
                """)) {
            assertTrue(result.hasNext());
            var row1 = result.next();
            assertEquals(BOB, row1.getValue("s").stringValue());
            assertEquals("50", row1.getValue("total").stringValue());

            assertTrue(result.hasNext());
            var row2 = result.next();
            assertEquals(ALICE, row2.getValue("s").stringValue());
            assertEquals("10", row2.getValue("total").stringValue());

            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP_CONCAT concatenates blank nodes preserving their string representations")
    void groupConcatBlankNodes() {
        String edge = "http://example.org/item";
        var b1 = valueFactory.createBNode("b1");
        var b2 = valueFactory.createBNode("b2");
        insert(iri(ALICE), iri(edge), b1);
        insert(iri(ALICE), iri(edge), b2);

        try (var result = executor.evaluateTuple("""
                SELECT ?s (GROUP_CONCAT(?o ; SEPARATOR=",") AS ?concat) WHERE {
                    ?s <http://example.org/item> ?o .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            String concat = row.getValue("concat").stringValue();
            assertTrue(concat.contains(b1.stringValue()) && concat.contains(b2.stringValue()),
                    "GROUP_CONCAT must include blank node string representations");
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY with multiple identical expression aliases binds each alias")
    void groupByMultipleIdenticalExpressionAliases() {
        String num = "http://example.org/num";
        insert(iri(ALICE), iri(num), valueFactory.createLiteral(5));

        try (var result = executor.evaluateTuple("""
                SELECT ?y ?z (COUNT(*) AS ?cnt) WHERE {
                    ?s <http://example.org/num> ?val .
                }
                GROUP BY (?val + 1 AS ?y) (?val + 1 AS ?z)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("6", row.getValue("y").stringValue());
            assertEquals("6", row.getValue("z").stringValue());
            assertEquals("1", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Combined query with GROUP BY, Aggregates, HAVING and ORDER BY")
    void combinedGroupByAggregatesHavingOrderBy() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(5));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(20));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(30));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?val) AS ?total) (COUNT(?val) AS ?count) WHERE {
                    ?s <http://example.org/score> ?val .
                }
                GROUP BY ?s
                HAVING (SUM(?val) > 20)
                ORDER BY DESC(?total)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(BOB, row.getValue("s").stringValue());
            assertEquals("50", row.getValue("total").stringValue());
            assertEquals("2", row.getValue("count").stringValue());
            assertFalse(result.hasNext());
        }
    }

}
