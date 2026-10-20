package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.storage.ProducerDefault;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
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
        Query query = query(BROAD_THEN_SELECTIVE);
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
        Query query = query("?s :link ?v . ?u :unrelated ?z . ?s :seed ?x");
        query.complete(new StatisticsProducer(Map.of("urn:bench:link", 100L,
                "urn:bench:unrelated", 2L, "urn:bench:seed", 1L)));
        assertEquals(List.of("urn:bench:seed", "urn:bench:link", "urn:bench:unrelated"), predicates(query));
    }

    @Test
    void variablePredicatesParticipateInJoinConnectivity() {
        Query query = query("?s ?p :seed . ?u :unrelated ?z . ?p :link ?x");
        query.complete(new StatisticsProducer(Map.of("urn:bench:link", 100L, "urn:bench:unrelated", 2L)) {
            @Override
            public long estimateCardinality(Query scope, Node graph, Edge edge) {
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
        Query query = query("?s :large ?x . ?s :small ?y");
        query.complete(new StatisticsProducer(Map.of("urn:bench:large", Long.MAX_VALUE,
                "urn:bench:small", 3_000_000_000L)));
        assertEquals(List.of("urn:bench:small", "urn:bench:large"), predicates(query));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, QueryStatistics.UNKNOWN_CARDINALITY, 7})
    void equalOrUnavailableStatisticsKeepStableSourceOrder(long count) {
        Query query = query("?s :left ?x . ?s :right ?y");
        query.complete(new StatisticsProducer(Map.of("urn:bench:left", count, "urn:bench:right", count)));
        assertEquals(List.of("urn:bench:left", "urn:bench:right"), predicates(query));
    }

    @Test
    void producerWithoutStatisticsRetainsConnectedFallback() {
        Query query = query(BROAD_THEN_SELECTIVE);
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
        Query query = query(BROAD_THEN_SELECTIVE);
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
        Query query = query("?s :type :Person . ?s :knows/:knows ?friend . ?s :name \"name42\"");
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
        Query query = query("?s :name \"name42\"");
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
        Query query = query("GRAPH :g { ?s :type :Person . ?s :name \"name42\" } "
                + "?x :type :Person . ?x :name \"name42\"");
        List<Node> graphs = new ArrayList<>();
        StatisticsProducer producer = new StatisticsProducer(Map.of()) {
            @Override
            public long estimateCardinality(Query scope, Node graph, Edge edge) {
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
            Query query = query("?s :name \"name42\"");
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
        Query query = query(sparql);
        query.complete(new StorageManagerProducer(storage));
        assertEquals(List.of("urn:bench:hasMark", "urn:bench:type", "urn:bench:evaluatesTeachingUnit", "urn:bench:type"),
                predicates(query));
    }

    private static Query query(String body) {
        return new CoreseAstQueryBuilder().toNextQuery((SelectQueryAst) new SparqlParser()
                .parse(PREFIX + "SELECT * { " + body + " }"));
    }

    private static IRI iri(String local) {
        return Values.factory().createIRI("urn:bench:" + local);
    }

    private static List<String> predicates(Query query) {
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

    private static class StatisticsProducer extends ProducerDefault implements QueryStatistics {
        private final Map<String, Long> counts;

        private StatisticsProducer(Map<String, Long> counts) {
            this.counts = counts;
        }

        @Override
        public long estimateCardinality(Query query, Node graph, Edge edge) {
            return counts.getOrDefault(edge.getEdgeLabel(), UNKNOWN_CARDINALITY);
        }
    }
}
