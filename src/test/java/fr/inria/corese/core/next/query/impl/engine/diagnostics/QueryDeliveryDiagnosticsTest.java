package fr.inria.corese.core.next.query.impl.engine.diagnostics;

import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ExecutionMode;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FallbackReason;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FirstRowStatus;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.impl.engine.eval.SelectCursor;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.StorageBenchmark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryDeliveryDiagnosticsTest {
    private static final String PREFIX = "PREFIX : <urn:bench:> ";
    private MemoryStorageManager storage;
    private Repository repository;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(20, 0, false));
        repository = Repositories.create(storage);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void reportsActualStreamingAndFreezesTheTimeWhenTheFirstRowBecomesReady() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluateWithDiagnostics();
            var before = execution.profile();
            assertEquals(ExecutionMode.STREAMING, before.delivery().mode());
            assertEquals(FallbackReason.NONE, before.delivery().fallbackReason());
            assertEquals(FirstRowStatus.UNOBSERVED, before.delivery().firstRowStatus());
            assertEquals(-1, before.delivery().firstRowNanos());
            assertEquals(0, before.patterns().getFirst().candidateEdges());
            try (var rows = execution.result()) {
                assertTrue(rows.hasNext());
                var first = execution.profile().delivery();
                assertEquals(FirstRowStatus.AVAILABLE, first.firstRowStatus());
                assertTrue(first.firstRowNanos() > 0);
                assertEquals(1, execution.profile().patterns().getFirst().candidateEdges());
                assertNotNull(rows.next());
                assertEquals(119, rows.stream().count());
                assertEquals(first, execution.profile().delivery());
            }
            assertEquals(FirstRowStatus.UNOBSERVED, before.delivery().firstRowStatus());
            assertTrue(execution.profile().format().contains("Delivery: STREAMING; fallback: NONE; first row: AVAILABLE"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * { <urn:missing> ?p ?o }",
            "SELECT * { ?s ?p ?o } LIMIT 0",
            "SELECT * { ?s ?p ?o } OFFSET 120"
    })
    void successfulExhaustionDistinguishesEmptyResultsFromUnobservedResults(String query) {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(query).evaluateWithDiagnostics();
            assertEquals(FirstRowStatus.UNOBSERVED, execution.profile().delivery().firstRowStatus());
            try (var rows = execution.result()) {
                assertFalse(rows.hasNext());
            }
            assertEquals(FirstRowStatus.EMPTY, execution.profile().delivery().firstRowStatus());
            assertEquals(-1, execution.profile().delivery().firstRowNanos());
        }
    }

    @Test
    void closeBeforeTheFirstAdvanceDoesNotInventAnEmptyResult() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluateWithDiagnostics();
            execution.result().close();
            assertEquals(FirstRowStatus.UNOBSERVED, execution.profile().delivery().firstRowStatus());
            assertEquals(-1, execution.profile().delivery().firstRowNanos());
            assertEquals(0, execution.profile().patterns().getFirst().iteratorOpenings());
        }
    }

    @Test
    void anAvailableFirstRowRemainsRecordedAfterEarlyClose() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluateWithDiagnostics();
            long first;
            try (var rows = execution.result()) {
                assertNotNull(rows.next());
                first = execution.profile().delivery().firstRowNanos();
            }
            assertEquals(FirstRowStatus.AVAILABLE, execution.profile().delivery().firstRowStatus());
            assertEquals(first, execution.profile().delivery().firstRowNanos());
        }
    }

    @Test
    void aReusedPlanRetainsTheDeliveryObservationWithoutRecompiling() {
        try (var connection = repository.getConnection()) {
            var prepared = connection.prepareTupleQuery("SELECT * { ?s ?p ?o } LIMIT 1");
            try (var first = prepared.evaluate()) {
                assertTrue(first.hasNext());
            }
            var execution = prepared.evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                assertTrue(execution.profile().planReused());
                assertEquals(0, execution.profile().compilationNanos());
                assertEquals(0, execution.profile().planningNanos());
                assertEquals(FirstRowStatus.UNOBSERVED, execution.profile().delivery().firstRowStatus());
                assertTrue(rows.hasNext());
                assertEquals(ExecutionMode.STREAMING, execution.profile().delivery().mode());
                assertTrue(execution.profile().delivery().firstRowNanos() > 0);
            }
        }
    }

    @Test
    void actualCursorTimingIncludesSnapshotSetupAndTraversalButExcludesClientPauses() {
        var time = new AtomicLong();
        var diagnostics = new ExecutionDiagnostics(time::get);
        StorageManager backend = mock(StorageManager.class, delegatesTo(storage));
        var snapshot = storage.openReadSnapshot();
        StorageManager view = mock(StorageManager.class, delegatesTo(snapshot));
        QueryOperations reads = mock(QueryOperations.class, delegatesTo(snapshot.queries()));
        when(view.queries()).thenReturn(reads);
        doAnswer(invocation -> {
            time.addAndGet(11);
            return view;
        }).when(backend).openReadSnapshot();
        doAnswer(invocation -> {
            time.addAndGet(7);
            return snapshot.queries().find(invocation.getArgument(0)).peek(statement -> time.addAndGet(5));
        }).when(reads).find(any());
        try (var rows = new SparqlQueryExecutor(backend).withDiagnostics(diagnostics).evaluateTuple("SELECT * { ?s ?p ?o }")) {
            time.addAndGet(10_000);
            assertTrue(rows.hasNext());
            assertEquals(23, diagnostics.snapshot().delivery().firstRowNanos());
            time.addAndGet(20_000);
            assertNotNull(rows.next());
            assertTrue(rows.hasNext());
            assertEquals(23, diagnostics.snapshot().delivery().firstRowNanos());
        }
        verify(view, times(1)).close();
    }

    static Stream<Arguments> materializedQueries() {
        return Stream.of(
                Arguments.of("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 1", FallbackReason.ORDER_BY),
                Arguments.of("SELECT (COUNT(*) AS ?n) { ?s ?p ?o }", FallbackReason.AGGREGATION),
                Arguments.of("SELECT ?p { ?s ?p ?o } GROUP BY ?p", FallbackReason.AGGREGATION),
                Arguments.of("SELECT * { { SELECT ?s { ?s ?p ?o } LIMIT 1 } }", FallbackReason.SUBQUERY),
                Arguments.of(PREFIX + "SELECT ?o { :s0 :link+ ?o }", FallbackReason.PROPERTY_PATH),
                Arguments.of("SELECT ?s { ?s ?p ?o MINUS { ?s <urn:absent> ?x } }", FallbackReason.MINUS),
                Arguments.of("SELECT ?s ?copy { ?s ?p ?o OPTIONAL { BIND(?s AS ?copy) } } LIMIT 1",
                        FallbackReason.COMPLEX_OPTIONAL),
                Arguments.of(PREFIX + "SELECT ?s ?name { { ?s :type :Person } { ?s :name ?name FILTER(BOUND(?name)) } }",
                        FallbackReason.SCOPED_JOIN),
                Arguments.of("SELECT * { ?s ?p ?o GRAPH ?g { ?a ?b ?c FILTER(BOUND(?a)) } }",
                        FallbackReason.REPEATED_SCOPE)
        );
    }

    @ParameterizedTest
    @MethodSource("materializedQueries")
    void explainsTheActualMaterializationGuard(String query, FallbackReason expected) {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(query).evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                var delivery = execution.profile().delivery();
                assertEquals(ExecutionMode.MATERIALIZED, delivery.mode());
                assertEquals(expected, delivery.fallbackReason());
                assertEquals(rows.hasNext() ? FirstRowStatus.AVAILABLE : FirstRowStatus.EMPTY, delivery.firstRowStatus());
                if (rows.hasNext()) assertTrue(delivery.firstRowNanos() > 0);
                else assertEquals(-1, delivery.firstRowNanos());
            }
        }
    }

    @Test
    void aMaterializedEmptySelectIsKnownBeforeTheCallerStartsReading() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { <urn:missing> ?p ?o } ORDER BY ?p")
                    .evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                assertEquals(FirstRowStatus.EMPTY, execution.profile().delivery().firstRowStatus());
                assertEquals(-1, execution.profile().delivery().firstRowNanos());
                assertFalse(rows.hasNext());
            }
        }
    }

    @Test
    void aggregateOverEmptyDataStillProducesARow() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT (COUNT(*) AS ?n) { <urn:missing> ?p ?o }")
                    .evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                assertEquals(FirstRowStatus.AVAILABLE, execution.profile().delivery().firstRowStatus());
                assertEquals("0", rows.next().getValue("n").stringValue());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASK { ?s ?p ?o }", "ASK { <urn:missing> ?p ?o }"})
    void askDoesNotConfuseABooleanResultWithASelectRow(String query) {
        try (var connection = repository.getConnection()) {
            var delivery = connection.prepareBooleanQuery(query).evaluateWithDiagnostics().profile().delivery();
            assertEquals(ExecutionMode.MATERIALIZED, delivery.mode());
            assertEquals(FallbackReason.QUERY_FORM, delivery.fallbackReason());
            assertEquals(FirstRowStatus.NOT_APPLICABLE, delivery.firstRowStatus());
            assertEquals(-1, delivery.firstRowNanos());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONSTRUCT { ?s <urn:out> ?o } { ?s ?p ?o } LIMIT 1", "DESCRIBE <urn:missing>"})
    void graphResultsDoNotExposeASelectTiming(String query) {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareGraphQuery(query).evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                assertNotNull(rows);
                assertEquals(FirstRowStatus.NOT_APPLICABLE, execution.profile().delivery().firstRowStatus());
                assertEquals(FallbackReason.QUERY_FORM, execution.profile().delivery().fallbackReason());
            }
        }
    }

    @Test
    void backendWithoutIndependentSnapshotsReportsItsActualFallback() {
        StorageManager backend = mock(StorageManager.class, delegatesTo(storage));
        doThrow(UnsupportedOperationException.class).when(backend).openReadSnapshot();
        var diagnostics = new ExecutionDiagnostics();
        var executor = new SparqlQueryExecutor(backend).withDiagnostics(diagnostics);
        try (var rows = executor.evaluateTuple("SELECT * { ?s ?p ?o }")) {
            assertTrue(rows.hasNext());
            var delivery = diagnostics.snapshot().delivery();
            assertEquals(ExecutionMode.MATERIALIZED, delivery.mode());
            assertEquals(FallbackReason.SNAPSHOT_UNAVAILABLE, delivery.fallbackReason());
            assertEquals(FirstRowStatus.AVAILABLE, delivery.firstRowStatus());
        }
    }

    @Test
    void queryGuardPrecedesBackendCapabilityWithoutExtraCountsOrSnapshotAttempts() {
        StorageManager backend = mock(StorageManager.class, delegatesTo(storage));
        QueryOperations reads = mock(QueryOperations.class, delegatesTo(storage.queries()));
        when(backend.queries()).thenReturn(reads);
        var diagnostics = new ExecutionDiagnostics();
        var executor = new SparqlQueryExecutor(backend).withDiagnostics(diagnostics);
        try (var rows = executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s")) {
            assertTrue(rows.hasNext());
            assertEquals(FallbackReason.ORDER_BY, diagnostics.snapshot().delivery().fallbackReason());
            verify(backend, never()).openReadSnapshot();
            verify(reads, never()).count(any());
            verify(reads, times(1)).find(any());
        }
    }

    @Test
    void aTraversalFailureBeforeAnyRowDoesNotClaimAnEmptyResult() {
        StorageManager backend = mock(StorageManager.class, delegatesTo(storage));
        StorageManager snapshot = mock(StorageManager.class, delegatesTo(storage.openReadSnapshot()));
        QueryOperations reads = mock(QueryOperations.class);
        doReturn(snapshot).when(backend).openReadSnapshot();
        when(snapshot.queries()).thenReturn(reads);
        when(reads.find(any())).thenThrow(new QueryEvaluationException("Read failed"));
        var diagnostics = new ExecutionDiagnostics();
        try (var rows = new SparqlQueryExecutor(backend).withDiagnostics(diagnostics).evaluateTuple("SELECT * { ?s ?p ?o }")) {
            assertThrows(QueryEvaluationException.class, rows::next);
            assertEquals(FirstRowStatus.UNOBSERVED, diagnostics.snapshot().delivery().firstRowStatus());
            assertEquals(-1, diagnostics.snapshot().delivery().firstRowNanos());
            verify(snapshot, times(1)).close();
        }
    }

    @Test
    void guardClassificationDoesNotExecuteAnUnsupportedServiceOrExtension() {
        assertEquals(FallbackReason.SERVICE, SelectCursor.fallbackReason(Query.create(Exp.create(Type.SERVICE))));
        var functional = Exp.create(Type.BIND);
        functional.setFunctional(true);
        assertEquals(FallbackReason.UNSUPPORTED_OPERATOR, SelectCursor.fallbackReason(Query.create(functional)));
    }
}
