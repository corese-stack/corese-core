package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.storage.ProducerDefault;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.StorageBenchmark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardinalityQueryPlannerTest {

    private static final String PREFIX = "PREFIX : <urn:bench:> ";
    private static final String BROAD_THEN_SELECTIVE = "?s :type :Person . ?s :name \"name42\"";
    private MemoryStorageManager storage;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
    }

    @AfterEach
    void close() {
        storage.close();
    }

    @Test
    void choosesSelectiveFirstPatternWithoutOpeningCursorsOrTransactions() {
        QueryPlan query = query(BROAD_THEN_SELECTIVE);
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        StorageManager observed = mock(StorageManager.class);
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();

        query.complete(new StorageManagerProducer(observed));

        assertEquals(List.of("urn:bench:name", "urn:bench:type"), predicates(query));
        verify(operations, times(2)).count(any());
        verify(operations, never()).find(any());
        verify(observed, never()).transactions();
        verify(observed, never()).openSession();
    }

    @Test
    void executionStartsWithSelectivePatternAndReadsOnlyTwoStatements() {
        List<StatementPattern> scans = new ArrayList<>();
        int[] visited = {0};
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        doAnswer(call -> {
            StatementPattern pattern = call.getArgument(0);
            scans.add(pattern);
            return storage.queries().find(pattern).peek(statement -> visited[0]++);
        }).when(operations).find(any());
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();

        try (var rows = new SparqlQueryExecutor(observed).evaluateTuple(
                PREFIX + "SELECT ?s { " + BROAD_THEN_SELECTIVE + " }")) {
            assertEquals("urn:bench:s42", rows.next().getValue("s").stringValue());
            assertFalse(rows.hasNext());
        }
        assertEquals(List.of(iri("name"), iri("type")), scans.stream()
                .map(StatementPattern::getPredicate).toList());
        assertEquals(2, visited[0]);
    }

    @Test
    void connectedJoinsPrecedeDisconnectedPatternsAfterSelectiveSeed() {
        QueryPlan query = query("?s :link ?v . ?u :unrelated ?z . ?s :seed ?x");
        query.complete(new StatisticsProducer(Map.of("urn:bench:link", 100L,
                "urn:bench:unrelated", 2L, "urn:bench:seed", 1L)));
        assertEquals(List.of("urn:bench:seed", "urn:bench:link", "urn:bench:unrelated"), predicates(query));
    }

    @Test
    void variablePredicatesParticipateInJoinConnectivity() {
        QueryPlan query = query("?s ?p :seed . ?u :unrelated ?z . ?p :link ?x");
        query.complete(new StatisticsProducer(Map.of("urn:bench:link", 100L, "urn:bench:unrelated", 2L)) {
            @Override
            public long estimateCardinality(QueryPlan scope, Node graph, Edge edge) {
                return edge.getEdgeVariable() == null ? super.estimateCardinality(scope, graph, edge) : 1;
            }
        });
        List<Edge> plan = edges(query.getBody());
        assertEquals("p", plan.getFirst().getEdgeVariable().getLabel());
        assertEquals("urn:bench:link", plan.get(1).getEdgeLabel());
        assertEquals("urn:bench:unrelated", plan.get(2).getEdgeLabel());
    }

    @Test
    void cardinalitiesRetainLongPrecision() {
        QueryPlan query = query("?s :large ?x . ?s :small ?y");
        query.complete(new StatisticsProducer(Map.of("urn:bench:large", Long.MAX_VALUE,
                "urn:bench:small", 3_000_000_000L)));
        assertEquals(List.of("urn:bench:small", "urn:bench:large"), predicates(query));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, QueryStatistics.UNKNOWN_CARDINALITY, 7})
    void equalOrUnavailableStatisticsKeepStableSourceOrder(long count) {
        QueryPlan query = query("?s :left ?x . ?s :right ?y");
        query.complete(new StatisticsProducer(Map.of("urn:bench:left", count, "urn:bench:right", count)));
        assertEquals(List.of("urn:bench:left", "urn:bench:right"), predicates(query));
    }

    @Test
    void producerWithoutStatisticsRetainsConnectedFallback() {
        QueryPlan query = query(BROAD_THEN_SELECTIVE);
        query.complete(new ProducerDefault());
        assertEquals(List.of("urn:bench:type", "urn:bench:name"), predicates(query));
    }

    @Test
    void backendWithoutCountSupportCanStillExecuteQueries() {
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        doThrow(new UnsupportedOperationException("Counts unavailable")).when(operations).count(any());
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        try (var rows = new SparqlQueryExecutor(observed).evaluateTuple(
                PREFIX + "SELECT ?s { " + BROAD_THEN_SELECTIVE + " }")) {
            assertEquals("urn:bench:s42", rows.next().getValue("s").stringValue());
            assertFalse(rows.hasNext());
        }
    }

    @Test
    void singlePatternAskDoesNotRequestStatistics() {
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        assertTrue(new SparqlQueryExecutor(observed).evaluateBoolean("ASK { ?s ?p ?o }"));
        verify(operations, never()).count(any());
    }

    @Test
    void emptyStorageProducesStablePlansAndEmptyResults() {
        storage.mutations().clear();
        QueryPlan query = query(BROAD_THEN_SELECTIVE);
        query.complete(new StorageManagerProducer(storage));
        assertEquals(List.of("urn:bench:type", "urn:bench:name"), predicates(query));
        try (var result = new SparqlQueryExecutor(storage).evaluateTuple(
                PREFIX + "SELECT * { " + BROAD_THEN_SELECTIVE + " }")) {
            assertFalse(result.hasNext());
        }
    }

    @Test
    void bindKeepsItsOriginalScopeBeforeLaterVariablesBecomeBound() {
        try (var rows = new SparqlQueryExecutor(storage).evaluateTuple(PREFIX
                + "SELECT ?s ?x { ?s :type :Person BIND(?later AS ?x) ?s :name ?later }")) {
            int count = 0;
            while (rows.hasNext()) {
                assertFalse(rows.next().hasBinding("x"));
                count++;
            }
            assertEquals(200, count);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?s :type :Person OPTIONAL { ?s :missing ?x } ?s :name \"name42\"",
            "?s :type :Person MINUS { ?s :name \"name41\" } ?s :name \"name42\"",
            "?s :type :Person . ?s :name ?name FILTER(?name = \"name42\")",
            "{ SELECT ?s { ?s :type :Person . ?s :name \"name42\" } } ?s :group ?g"
    })
    void semanticOperatorsPreserveSolutions(String body) {
        try (var rows = new SparqlQueryExecutor(storage).evaluateTuple(PREFIX + "SELECT ?s { " + body + " }")) {
            assertEquals("urn:bench:s42", rows.next().getValue("s").stringValue());
            assertFalse(rows.hasNext());
        }
    }

    @Test
    void valuesAndUnionPreserveSolutionBags() {
        try (var rows = new SparqlQueryExecutor(storage).evaluateTuple(PREFIX
                + "SELECT ?s { VALUES ?s { :s42 :s42 } " + BROAD_THEN_SELECTIVE + " }")) {
            assertEquals(2, rows.stream().count());
        }
        try (var rows = new SparqlQueryExecutor(storage).evaluateTuple(PREFIX
                + "SELECT ?s { { " + BROAD_THEN_SELECTIVE + " } UNION { " + BROAD_THEN_SELECTIVE + " } }")) {
            assertEquals(2, rows.stream().count());
        }
    }

    @Test
    void pathsAreBarriersAndDoNotReceiveOrdinaryTripleCounts() {
        QueryPlan query = query("?s :type :Person . ?s :knows/:knows ?friend . ?s :name \"name42\"");
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        StorageManager observed = mock(StorageManager.class);
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        query.complete(new StorageManagerProducer(observed));
        assertInstanceOf(PropertyPathEdge.class, edges(query.getBody()).get(1));
        verify(operations, never()).count(any());
    }

    @Test
    void estimatesHonorDefaultNamedAndExplicitEmptyDatasets() {
        storage.mutations().add(Values.factory().createStatement(iri("s42"), iri("name"),
                Values.factory().createLiteral("name42"), iri("g")));
        StorageManagerProducer producer = new StorageManagerProducer(storage);
        QueryPlan query = query("?s :name \"name42\"");
        Edge edge = edges(query.getBody()).getFirst();
        assertEquals(1, producer.estimateCardinality(query, null, edge));

        query.setDatasetSpecified(true);
        query.setNamed(List.of(NodeImpl.forValue(iri("g"))));
        assertEquals(0, producer.estimateCardinality(query, null, edge));
        assertEquals(1, producer.estimateCardinality(query, NodeImpl.forValue(iri("g")), edge));
        assertEquals(0, producer.estimateCardinality(query, NodeImpl.forValue(iri("other")), edge));
        assertEquals(QueryStatistics.UNKNOWN_CARDINALITY,
                producer.estimateCardinality(query, NodeImpl.forVariable("g"), edge));
        query.setFrom(List.of(NodeImpl.forValue(iri("g"))));
        assertEquals(1, producer.estimateCardinality(query, null, edge));
    }

    @Test
    void graphScopeIsRestoredBeforePlanningDefaultGraphPatterns() {
        QueryPlan query = query("GRAPH :g { ?s :type :Person . ?s :name \"name42\" } "
                + "?x :type :Person . ?x :name \"name42\"");
        List<Node> graphs = new ArrayList<>();
        StatisticsProducer producer = new StatisticsProducer(Map.of()) {
            @Override
            public long estimateCardinality(QueryPlan scope, Node graph, Edge edge) {
                graphs.add(graph);
                return 1;
            }
        };
        query.complete(producer);
        assertEquals(2, graphs.stream().filter(g -> g != null && g.getLabel().equals("urn:bench:g")).count());
        assertEquals(2, graphs.stream().filter(g -> g == null).count());
    }

    @Test
    void statisticsUseTheExistingSessionSnapshot() {
        try (var reader = storage.openSession(); var transaction = reader.transactions().beginTransaction()) {
            QueryPlan query = query("?s :name \"name42\"");
            Edge edge = edges(query.getBody()).getFirst();
            StorageManagerProducer producer = new StorageManagerProducer(reader);
            assertEquals(1, producer.estimateCardinality(query, null, edge));
            storage.mutations().clear();
            assertEquals(1, producer.estimateCardinality(query, null, edge));
            transaction.rollback();
            assertEquals(0, producer.estimateCardinality(query, null, edge));
        }
    }

    @Test
    void countFailuresRemainQueryDomainErrorsWithOriginalCause() {
        StorageException failure = new StorageException(ErrorCode.QUERY_FAILED, "Statistics failed");
        QueryOperations operations = mock(QueryOperations.class, delegatesTo(storage.queries()));
        doThrow(failure).when(operations).count(any());
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        var exception = assertThrows(QueryEvaluationException.class, () -> new SparqlQueryExecutor(observed)
                .evaluateBoolean(PREFIX + "ASK { " + BROAD_THEN_SELECTIVE + " }"));
        assertSame(failure, exception.getCause());
        verify(operations, never()).find(any());
    }

    @Test
    void selectiveFilterPushdownSeedsComparisonEdgeFirst() {
        String sparql = "?x :type :Teaching_Unit . ?e :type :Evaluation . ?e :evaluatesTeachingUnit ?x . ?e :hasMark ?y . FILTER (?y < 3)";
        QueryPlan query = query(sparql);
        query.complete(new StorageManagerProducer(storage));
        assertEquals(List.of("urn:bench:hasMark", "urn:bench:type", "urn:bench:evaluatesTeachingUnit", "urn:bench:type"),
                predicates(query));
    }


    @Test
    void subjectBindingBeatsSmallerObjectBoundPattern() {
        assertPlan("?x :object ?s . ?s :subject ?y . ?s :seed ?o",
                Map.of("object", 500L, "subject", 5000L, "seed", 1L),
                List.of("seed", "subject", "object"));
    }

    @Test
    void fullyBoundTripleIsAProbe() {
        assertPlan("?s :scan ?x . ?s :probe ?o . ?s :seed ?o",
                Map.of("scan", 5000L, "probe", 1000000L, "seed", 1L),
                List.of("seed", "probe", "scan"));
    }

    @Test
    void costsAreRecomputedAfterEachSelectedEdge() {
        assertPlan("?y :type :Professor . ?s :wide ?z . ?s :link ?y . ?s :seed ?o",
                Map.of("type", 100000L, "wide", 100000L, "link", 5000L, "seed", 1L),
                List.of("seed", "link", "type", "wide"));
    }

    @Test
    void constantsDoNotReceiveASecondReduction() {
        assertPlan("?s :name \"literal\" . ?a :rare ?b",
                Map.of("name", 20L, "rare", 10L), List.of("rare", "name"));
    }

    @Test
    void equalEffectiveCostsPreserveSourceOrder() {
        assertPlan("?x :object ?s . ?s :subject ?y . ?s :seed ?o",
                Map.of("object", 500L, "subject", 50000L, "seed", 1L),
                List.of("seed", "object", "subject"));
    }

    @Test
    void scaledCostsRetainAdjacentLongPrecision() {
        assertPlan("?s :large ?x . ?s :small ?y . ?s :seed ?o",
                Map.of("large", Long.MAX_VALUE, "small", Long.MAX_VALUE - 1, "seed", 1L),
                List.of("seed", "small", "large"));
    }

    @Test
    void variablePredicateDoesNotBecomeSingleTripleProbe() {
        QueryPlan query = query("?s ?p ?o . ?s :scan ?x . ?s :seed ?o");
        query.complete(new StatisticsProducer(Map.of("urn:bench:scan", 5000L, "urn:bench:seed", 1L)) {
            @Override
            public long estimateCardinality(QueryPlan scope, Node graph, Edge edge) {
                return edge.getEdgeVariable() == null ? super.estimateCardinality(scope, graph, edge) : 1000000;
            }
        });
        List<Edge> plan = edges(query.getBody());
        assertEquals("urn:bench:seed", plan.get(0).getEdgeLabel());
        assertEquals("urn:bench:scan", plan.get(1).getEdgeLabel());
        assertEquals("p", plan.get(2).getEdgeVariable().getLabel());
    }

    @Test
    void enclosingScopeVariablesReduceLookupCost() {
        QueryPlan query = query("?x :object ?s . ?s :subject ?y");
        CardinalityPatternOrdering.sort(query, null, query.getBody(), List.of("s"), List.of(),
                new StatisticsProducer(Map.of("urn:bench:object", 500L, "urn:bench:subject", 5000L)));
        assertEquals(List.of("urn:bench:subject", "urn:bench:object"), predicates(query));
    }

    @Test
    void zeroCardinalityRemainsCheaperThanFullyBoundProbe() {
        QueryPlan query = query("?s :probe ?o . ?s :empty ?o");
        CardinalityPatternOrdering.sort(query, null, query.getBody(), List.of("s", "o"), List.of(),
                new StatisticsProducer(Map.of("urn:bench:probe", 1L, "urn:bench:empty", 0L)));
        assertEquals(List.of("urn:bench:empty", "urn:bench:probe"), predicates(query));
    }

    // ── isCheapestOrTied: selective filter seed is demoted when a cheaper pattern exists ──

    @Test
    void selectiveFilterSeedDemotedWhenCheaperPatternExists() {
        // ?seed has a FILTER binding hint but count=50, while :cheaper has count=1.
        // Before the isCheapestOrTied fix, the seed was always placed first regardless.
        // After the fix, :cheaper wins the first slot.
        assertPlan("?s :seed ?y . ?s :cheaper ?z . FILTER(?y < 3)",
                Map.of("seed", 50L, "cheaper", 1L),
                List.of("cheaper", "seed"));
    }

    @Test
    void selectiveFilterSeedKeptFirstWhenItIsCheapest() {
        // Seed has FILTER binding and is also the cheapest — should remain first.
        assertPlan("?s :seed ?y . ?s :expensive ?z . FILTER(?y < 3)",
                Map.of("seed", 1L, "expensive", 50L),
                List.of("seed", "expensive"));
    }

    @Test
    void selectiveFilterSeedKeptFirstWhenTiedWithOtherPattern() {
        // Seed has FILTER binding and is tied — should remain first (tie-keep behaviour).
        assertPlan("?s :seed ?y . ?s :tied ?z . FILTER(?y < 3)",
                Map.of("seed", 5L, "tied", 5L),
                List.of("seed", "tied"));
    }

    @Test
    void selectiveFilterSeedDemotedWhenEmptyPatternExistsEvenIfSeedIsUnknown() {
        // ?seed has unknown cardinality (-1), but :empty has count=0.
        // With :empty in source position 0 and :seed as filter seed at position 1,
        // the unknown seed must not be promoted ahead of the known empty pattern.
        assertPlan("?s :empty ?z . ?s :seed ?y . FILTER(?y < 3)",
                Map.of("empty", 0L),
                List.of("empty", "seed"));
    }

    @Test
    void selectiveFilterSeedKeptWhenOtherPatternIsUnknownAndSeedIsKnownCheapest() {
        // Seed has count=0, while :unknown has no statistics (-1).
        // The known cheapest seed should remain first.
        assertPlan("?s :seed ?y . ?s :unknown ?z . FILTER(?y < 3)",
                Map.of("seed", 0L),
                List.of("seed", "unknown"));
    }

    @Test
    void selectiveFilterSeedKeptWhenAllPatternsHaveUnknownCardinalities() {
        // No statistics are available for any pattern: heuristic filter pushdown is preserved.
        assertPlan("?s :seed ?y . ?s :other ?z . FILTER(?y < 3)",
                Map.of(),
                List.of("seed", "other"));
    }

    private static void assertPlan(String body, Map<String, Long> counts, List<String> expected) {
        Map<String, Long> qualified = new java.util.HashMap<>();
        counts.forEach((key, value) -> qualified.put("urn:bench:" + key, value));
        QueryPlan query = query(body);
        query.complete(new StatisticsProducer(qualified));
        assertEquals(expected.stream().map(name -> "urn:bench:" + name).toList(), predicates(query));
    }

    private static QueryPlan query(String body) {
        return new CoreseAstQueryBuilder().toNextQuery((SelectQueryAst) new SparqlParser()
                .parse(PREFIX + "SELECT * { " + body + " }"));
    }

    private static IRI iri(String local) {
        return Values.factory().createIRI("urn:bench:" + local);
    }

    private static List<String> predicates(QueryPlan query) {
        return edges(query.getBody()).stream().map(edge -> edge.getEdgeLabel()).toList();
    }

    private static List<Edge> edges(Exp expression) {
        List<Edge> result = new ArrayList<>();
        if (expression.isEdge()) {
            result.add(expression.getEdge());
        }
        for (Exp child : expression) {
            result.addAll(edges(child));
        }
        return result;
    }

    @Test
    void joinOrderingUsesPredicateStatisticsWhenAvailable() {
        IRI seedIri = iri("seed");
        IRI highDistinctIri = iri("highDistinct");
        IRI lowDistinctIri = iri("lowDistinct");

        Map<String, Long> counts = Map.of(
                seedIri.stringValue(), 1L,
                lowDistinctIri.stringValue(), 1000L,
                highDistinctIri.stringValue(), 1000L);

        Map<String, PredicateStatistics> pStats = Map.of(
                lowDistinctIri.stringValue(), new PredicateStatistics(lowDistinctIri, 1000, 2, 100),
                highDistinctIri.stringValue(), new PredicateStatistics(highDistinctIri, 1000, 500, 100));

        QueryPlan query = query("?s :seed 1 . ?s :lowDistinct ?o1 . ?s :highDistinct ?o2");
        query.complete(new StatisticsProducer(counts, pStats));

        List<String> plan = predicates(query);
        assertEquals(seedIri.stringValue(), plan.get(0));
        assertEquals(highDistinctIri.stringValue(), plan.get(1));
        assertEquals(lowDistinctIri.stringValue(), plan.get(2));
    }

    @Test
    void joinOrderingUsesPredicateStatisticsWhenObjectIsBound() {
        IRI seedIri = iri("seed");
        IRI highDistinctIri = iri("highDistinctObj");
        IRI lowDistinctIri = iri("lowDistinctObj");

        Map<String, Long> counts = Map.of(
                seedIri.stringValue(), 1L,
                lowDistinctIri.stringValue(), 1000L,
                highDistinctIri.stringValue(), 1000L);

        Map<String, PredicateStatistics> pStats = Map.of(
                lowDistinctIri.stringValue(), new PredicateStatistics(lowDistinctIri, 1000, 100, 2),
                highDistinctIri.stringValue(), new PredicateStatistics(highDistinctIri, 1000, 100, 500));

        QueryPlan query = query("?x :seed ?shared . ?s1 :lowDistinctObj ?shared . ?s2 :highDistinctObj ?shared");
        query.complete(new StatisticsProducer(counts, pStats));

        List<String> plan = predicates(query);
        assertEquals(seedIri.stringValue(), plan.get(0));
        assertEquals(highDistinctIri.stringValue(), plan.get(1));
        assertEquals(lowDistinctIri.stringValue(), plan.get(2));
    }

    @Test
    void memoryStorageManagerProvidesExactPredicateStatistics() {
        IRI type = iri("type");
        var stats = storage.metadata().getPredicateStatistics(type);
        org.junit.jupiter.api.Assertions.assertNotNull(stats);
        assertEquals(type, stats.predicate());
        assertTrue(stats.count() > 0);
        assertTrue(stats.distinctSubjects() > 0);
        assertTrue(stats.distinctObjects() > 0);
    }

    @Test
    void joinOrderingWithIndexStatisticsAvoidsIntermediateCartesianExplosion() {
        var benchStorage = MemoryStorageManager.builder().build();
        IRI seed = iri("seed");
        IRI target = iri("target");
        IRI lowSel = iri("lowSel");
        IRI highSel = iri("highSel");

        var stmts = new ArrayList<fr.inria.corese.core.next.data.api.model.Statement>();
        stmts.add(Values.factory().createStatement(iri("s0"), seed, target));

        // lowSel: 10,000 statements all pointing to the same target object (1 distinct object)
        for (int i = 0; i < 10000; i++) {
            stmts.add(Values.factory().createStatement(iri("s_low_" + i), lowSel, target));
        }
        // highSel: 10,000 statements each pointing to a distinct object (10,000 distinct objects)
        stmts.add(Values.factory().createStatement(iri("s_high_0"), highSel, target));
        for (int i = 1; i < 10000; i++) {
            stmts.add(Values.factory().createStatement(iri("s_high_" + i), highSel, iri("other_" + i)));
        }
        benchStorage.mutations().addAll(stmts);

        var statsLow = benchStorage.metadata().getPredicateStatistics(lowSel);
        var statsHigh = benchStorage.metadata().getPredicateStatistics(highSel);

        assertEquals(1, statsLow.distinctObjects());
        assertEquals(10000, statsHigh.distinctObjects());

        // High selectivity (high distinct objects = small match count per bound object) is placed first
        QueryPlan plannedQuery = query("?x :seed ?t . ?s1 :lowSel ?t . ?s2 :highSel ?t");
        plannedQuery.complete(new StorageManagerProducer(benchStorage));
        List<String> optimalPlan = predicates(plannedQuery);
        assertEquals(List.of(seed.stringValue(), highSel.stringValue(), lowSel.stringValue()), optimalPlan);

        // Verify query produces the exact expected count
        String sparql = "PREFIX : <urn:bench:> SELECT * WHERE { ?x :seed ?t . ?s1 :lowSel ?t . ?s2 :highSel ?t }";
        int countCbo = 0;
        try (var res = new SparqlQueryExecutor(benchStorage).evaluateTuple(sparql)) {
            while (res.hasNext()) {
                res.next();
                countCbo++;
            }
        }
        assertEquals(10000, countCbo);
    }

    private static class StatisticsProducer extends ProducerDefault implements QueryStatistics {
        private final Map<String, Long> counts;
        private final Map<String, PredicateStatistics> predicateStats;

        private StatisticsProducer(Map<String, Long> counts) {
            this(counts, Map.of());
        }

        private StatisticsProducer(Map<String, Long> counts, Map<String, PredicateStatistics> predicateStats) {
            this.counts = counts;
            this.predicateStats = predicateStats;
        }

        @Override
        public long estimateCardinality(QueryPlan query, Node graph, Edge edge) {
            return counts.getOrDefault(edge.getEdgeLabel(), UNKNOWN_CARDINALITY);
        }

        @Override
        public PredicateStatistics getPredicateStatistics(QueryPlan query, Node graphNode, IRI predicate) {
            return predicateStats.get(predicate.stringValue());
        }
    }
}
