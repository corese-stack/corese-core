package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.StorageBenchmark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Native snapshot and resumable execution contracts, including partial consumption. */
class ProgressiveSelectTest {
    private static final String PREFIX = "PREFIX : <urn:bench:> ";
    private MemoryStorageManager storage;
    private Repository repository;
    private final AtomicInteger opened = new AtomicInteger();
    private final AtomicInteger released = new AtomicInteger();
    private final AtomicInteger read = new AtomicInteger();
    private final AtomicInteger snapshots = new AtomicInteger();
    private final AtomicInteger closedSnapshots = new AtomicInteger();
    private final AtomicReference<RuntimeException> openingFailure = new AtomicReference<>();
    private final AtomicReference<RuntimeException> closingFailure = new AtomicReference<>();
    private Consumer<Statement> visit = statement -> { };
    private final CountDownLatch viewClosed = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
        StorageManager observed = observe(storage);
        when(observed.openSession()).thenAnswer(call -> observe(storage.openSession()));
        repository = Repositories.create(observed);
    }

    private StorageManager observe(StorageManager source) {
        StorageManager observed = mock(StorageManager.class, delegatesTo(source));
        doAnswer(call -> {
            StorageManager snapshot = source.openReadSnapshot();
            snapshots.incrementAndGet();
            QueryOperations queries = mock(QueryOperations.class, delegatesTo(snapshot.queries()));
            doAnswer(find -> {
                if (openingFailure.get() != null) throw openingFailure.get();
                opened.incrementAndGet();
                return snapshot.queries().find(find.getArgument(0)).peek(statement -> {
                    read.incrementAndGet();
                    visit.accept(statement);
                }).onClose(() -> {
                    released.incrementAndGet();
                    if (closingFailure.get() != null) throw closingFailure.get();
                });
            }).when(queries).find(any());
            StorageManager view = mock(StorageManager.class, delegatesTo(snapshot));
            when(view.queries()).thenReturn(queries);
            doAnswer(close -> {
                snapshot.close();
                closedSnapshots.incrementAndGet();
                viewClosed.countDown();
                return null;
            }).when(view).close();
            return view;
        }).when(observed).openReadSnapshot();
        return observed;
    }

    @AfterEach
    void tearDown() {
        repository.close();
        assertEquals(opened.get(), released.get(), "Statement cursors must all be released");
        assertEquals(snapshots.get(), closedSnapshots.get(), "Read snapshots must all be released");
    }

    @Test
    void evaluateDoesNotReadAndHasNextBuffersExactlyOneRow() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluateWithDiagnostics();
            var before = execution.profile();
            assertEquals(0, read.get());
            assertEquals(0, opened.get());
            assertEquals(0, before.patterns().getFirst().candidateEdges());
            try (var rows = execution.result()) {
                assertTrue(rows.hasNext());
                assertTrue(rows.hasNext());
                assertEquals(1, read.get());
                var first = rows.next();
                assertEquals(1, read.get());
                assertTrue(rows.hasNext());
                assertEquals(2, read.get());
                assertEquals("urn:bench:s0", first.getValue("s").stringValue());
            }
            assertEquals(0, before.patterns().getFirst().candidateEdges(), "Profiles are immutable snapshots");
            assertEquals(2, execution.profile().patterns().getFirst().candidateEdges());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void consumerLimitReadsOnlyTheRequestedRows(int limit) {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            assertEquals(limit, rows.stream().limit(limit).count());
            assertEquals(limit, read.get());
            assertEquals(0, closedSnapshots.get());
        }
        assertEquals(1, closedSnapshots.get());
    }

    @Test
    void closingBeforeTheFirstRowNeverOpensAStatementCursor() {
        try (var connection = repository.getConnection()) {
            var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate();
            rows.close();
            rows.close();
            assertEquals(0, opened.get());
            assertEquals(1, closedSnapshots.get());
            assertThrows(IllegalStateException.class, rows::hasNext);
        }
    }

    @Test
    void sqlLimitReleasesWithoutAnExtraHasNextCall() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o } LIMIT 1").evaluate()) {
            assertNotNull(rows.next());
            assertEquals(1, closedSnapshots.get());
            assertEquals(1, released.get());
            assertFalse(rows.hasNext());
            assertThrows(NoSuchElementException.class, rows::next);
        }
    }

    @Test
    void exhaustionReleasesEveryCursorAndTheQuerySnapshot() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            assertEquals(1200, rows.stream().count());
            assertFalse(rows.hasNext());
            assertEquals(1, closedSnapshots.get());
            assertEquals(opened.get(), released.get());
        }
    }

    @Test
    void selectiveBgpStopsAtItsFirstJoinedRow() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s ?name { ?s :type :Person . ?s :name ?name }").evaluateWithDiagnostics();
            try (var rows = execution.result()) {
                assertEquals(0, read.get());
                assertEquals("name0", rows.next().getValue("name").stringValue());
                assertEquals(2, read.get());
            }
            assertEquals(List.of(1L, 1L), execution.profile().patterns().stream()
                    .map(pattern -> pattern.candidateEdges()).toList());
        }
    }

    @Test
    void theSnapshotCoversLookupsOpenedAfterMutations() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX
                     + "SELECT ?s ?name { ?s :type :Person . ?s :name ?name }").evaluate()) {
            storage.mutations().clear();
            assertEquals("name0", rows.next().getValue("name").stringValue());
            assertEquals(200, 1 + rows.stream().count());
            assertEquals(0, storage.queries().count(fr.inria.corese.core.next.storage.api.model.StatementPattern.matchAll()));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sourceTransactionCanFinishWhileItsReadSnapshotRemainsValid(boolean commit) {
        try (var connection = repository.getConnection()) {
            connection.begin();
            connection.add(Values.factory().createIRI("urn:private"), Values.factory().createIRI("urn:p"), Values.factory().createIRI("urn:o"));
            try (var rows = connection.prepareTupleQuery("SELECT ?s { ?s <urn:p> ?o }").evaluate()) {
                if (commit) connection.commit(); else connection.rollback();
                assertEquals("urn:private", rows.next().getValue("s").stringValue());
                assertFalse(rows.hasNext());
            }
        }
    }

    @Test
    void bindingsAndPreparedPlanFramesAreIndependentForOverlappingCursors() {
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery(PREFIX + "SELECT ?name { ?s :name ?name }");
            query.setBinding("s", Values.factory().createIRI("urn:bench:s0"));
            var first = query.evaluateWithDiagnostics();
            query.setBinding("s", Values.factory().createIRI("urn:bench:s1"));
            var second = query.evaluateWithDiagnostics();
            try (var left = first.result(); var right = second.result()) {
                assertTrue(second.profile().planReused());
                assertEquals(0, second.profile().planningNanos());
                assertEquals("name1", right.next().getValue("name").stringValue());
                assertEquals("name0", left.next().getValue("name").stringValue());
            }
        }
    }

    @Test
    void valuesBindFilterProjectionAndOffsetPreserveSolutions() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("""
                     SELECT (?n + 1 AS ?value) ?error {
                       VALUES ?n { 1 2 3 4 5 }
                       BIND(1 / 0 AS ?error)
                       FILTER(?n > 1)
                     } OFFSET 1 LIMIT 2
                     """).evaluate()) {
            assertEquals(List.of("4", "5"), rows.stream().map(row -> {
                assertFalse(row.hasBinding("error"));
                assertFalse(row.hasBinding("n"));
                return row.getValue("value").stringValue();
            }).toList());
            assertEquals(0, read.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT * {}", "SELECT ?x { VALUES ?x { 1 } }"})
    void emptyAndValuesOnlyPatternsProduceTheirSingleSolution(String sparql) {
        try (var connection = repository.getConnection(); var rows = connection.prepareTupleQuery(sparql).evaluate()) {
            assertNotNull(rows.next());
            assertFalse(rows.hasNext());
        }
    }

    @Test
    void distinctOffsetAndLimitAreAppliedAfterProjection() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT DISTINCT ?x { VALUES ?x { 1 1 2 2 3 } } OFFSET 1 LIMIT 1")
                     .evaluate()) {
            assertEquals("2", rows.next().getValue("x").stringValue());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT * { ?s ?p ?o } LIMIT 0", "SELECT * { <urn:missing> ?p ?o }"})
    void noResultsReleaseTheirReadSnapshot(String sparql) {
        try (var connection = repository.getConnection(); var rows = connection.prepareTupleQuery(sparql).evaluate()) {
            assertFalse(rows.hasNext());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @Test
    void unionAdvancesOnlyTheCurrentBranchAndPreservesBags() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + """
                     SELECT ?s { { ?s :type :Person } UNION { ?s :type :Person } }
                     """).evaluate()) {
            assertEquals(0, read.get());
            assertEquals("urn:bench:s0", rows.next().getValue("s").stringValue());
            assertEquals(1, read.get());
            assertEquals(400, 1 + rows.stream().count());
        }
    }

    @Test
    void namedGraphCursorRetainsTheCapturedRegistryAndStreamsItsRows() {
        storage.mutations().addAll(java.util.stream.IntStream.range(0, 200)
                .mapToObj(i -> Values.factory().createStatement(Values.factory().createIRI("urn:named:" + i),
                        Values.factory().createIRI("urn:bench:type"), Values.factory().createIRI("urn:bench:Person"),
                        Values.factory().createIRI("urn:graph"))).toList());
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + "SELECT ?g ?s { GRAPH ?g { ?s :type :Person } }")
                     .evaluate()) {
            assertEquals(0, read.get());
            storage.mutations().clear();
            assertNotNull(rows.next().getValue("g"));
            assertEquals(1, read.get());
            assertEquals(200, 1 + rows.stream().count());
        }
    }

    @Test
    void simpleOptionalStreamsAndKeepsEveryUnmatchedLeftSolution() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + """
                     SELECT ?s ?name ?missing {
                       ?s :type :Person OPTIONAL { ?s :name ?name } OPTIONAL { ?s :missing ?missing }
                     }
                     """).evaluate()) {
            assertEquals(0, read.get());
            var first = rows.next();
            assertEquals("name0", first.getValue("name").stringValue());
            assertFalse(first.hasBinding("missing"));
            assertEquals(2, read.get());
            assertEquals(200, 1 + rows.stream().count());
        }
    }

    @Test
    void groupedPureJoinsRemainProgressive() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX
                     + "SELECT ?s ?name { { ?s :type :Person } { ?s :name ?name } }").evaluate()) {
            assertEquals(0, read.get());
            assertEquals("name0", rows.next().getValue("name").stringValue());
            assertEquals(2, read.get());
        }
    }

    @Test
    void scopedExpressionsUseIndependentBindings() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + """
                     SELECT ?copy {
                       ?s :type :Person
                       { BIND(?s AS ?copy) } UNION { BIND(?s AS ?copy) }
                     } LIMIT 1
                     """).evaluate()) {
            assertEquals(1, snapshots.get());
            assertEquals(0, read.get());
            assertFalse(rows.next().hasBinding("copy"));
        }
    }

    @Test
    void localExpressionsInUnionGroupsAreProgressiveAndShareTheQueryTimestamp() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + """
                     SELECT ?s ?time (NOW() AS ?outerTime) {
                       { ?s :type :Person BIND(NOW() AS ?time) FILTER(BOUND(?s)) }
                       UNION { ?s :type :Person BIND(NOW() AS ?time) }
                     }
                     """).evaluate()) {
            assertEquals(0, read.get());
            var row = rows.next();
            assertEquals(row.getValue("time"), row.getValue("outerTime"));
            assertEquals(1, read.get());
        }
    }

    @Test
    void graphNameIsJoinedAfterItsLocalExpressionsHaveBeenEvaluated() {
        storage.mutations().createGraph(Values.factory().createIRI("urn:graph"));
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT ?g ?copy { GRAPH ?g { BIND(?g AS ?copy) } }").evaluate()) {
            assertEquals(1, snapshots.get());
            var row = rows.next();
            assertEquals("urn:graph", row.getValue("g").stringValue());
            assertFalse(row.hasBinding("copy"));
            assertFalse(rows.hasNext());
        }
    }

    @Test
    void scopedJoinsKeepTheirBufferedStrategyToAvoidQuadraticIndependentScans() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery(PREFIX + """
                     SELECT ?s ?name {
                       { ?s :type :Person } { ?s :name ?name FILTER(BOUND(?name)) }
                     }
                     """).evaluate()) {
            assertEquals(0, snapshots.get());
            assertEquals(200, rows.stream().count());
        }
    }

    @Test
    void orderingAndAggregatesKeepTheirBlockingSemantics() {
        try (var connection = repository.getConnection()) {
            try (var rows = connection.prepareTupleQuery("SELECT (COUNT(*) AS ?count) { ?s ?p ?o }").evaluate()) {
                assertEquals("1200", rows.next().getValue("count").stringValue());
            }
            try (var rows = connection.prepareTupleQuery(PREFIX + "SELECT ?name { ?s :name ?name } ORDER BY ?name LIMIT 1")
                    .evaluate()) {
                assertEquals("name0", rows.next().getValue("name").stringValue());
            }
            assertEquals(1, snapshots.get());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void traversalFailureKeepsItsCauseAndReleasesTheCompleteView(boolean duringTraversal) {
        StorageException cause = new StorageException(ErrorCode.QUERY_FAILED, "Read failed");
        if (duringTraversal) visit = statement -> { throw cause; };
        else openingFailure.set(cause);
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            var failure = assertThrows(QueryEvaluationException.class, rows::hasNext);
            assertSame(cause, failure.getCause());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @Test
    void fatalTraversalErrorsReleaseTheReadViewWithoutChangingTheirIdentity() {
        var cause = new AssertionError("Traversal failed");
        visit = statement -> { throw cause; };
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            assertSame(cause, assertThrows(AssertionError.class, rows::next));
            assertEquals(1, closedSnapshots.get());
        }
    }

    @Test
    void closeCallbackFailureIsSuppressedOnTheOriginalTraversalFailure() {
        StorageException cause = new StorageException(ErrorCode.QUERY_FAILED, "Read failed");
        closingFailure.set(new IllegalStateException("Close failed"));
        visit = statement -> { throw cause; };
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            var failure = assertThrows(QueryEvaluationException.class, rows::next);
            assertSame(cause, failure.getCause());
            assertEquals(List.of(closingFailure.get()), List.of(cause.getSuppressed()));
            assertEquals(1, closedSnapshots.get());
        }
    }

    @Test
    void interruptionBeforeAdvancingPreservesTheFlagAndReleasesTheSnapshot() {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT ?x { VALUES ?x { 1 2 3 } }").evaluate()) {
            Thread.currentThread().interrupt();
            assertThrows(QueryEvaluationException.class, rows::next);
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, closedSnapshots.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptionDuringTraversalReleasesResourcesAndPreservesTheInterrupt() {
        visit = statement -> Thread.currentThread().interrupt();
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            assertThrows(QueryEvaluationException.class, rows::hasNext);
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, closedSnapshots.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void activeTimeoutReleasesResourcesAndCannotInterruptTheFollowingQuery() {
        visit = statement -> {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }");
            query.setTimeout(Duration.ofMillis(150));
            try (var rows = query.evaluate()) {
                assertThrows(QueryTimeoutException.class, rows::hasNext);
                assertEquals(1, closedSnapshots.get());
                assertFalse(Thread.currentThread().isInterrupted());
            }
            visit = statement -> { };
            query.setTimeout(Duration.ZERO);
            try (var rows = query.evaluate()) {
                assertNotNull(rows.next());
                assertFalse(Thread.currentThread().isInterrupted());
            }
        }
    }

    @Test
    void idleTimeoutReleasesTheSnapshotWithoutInterruptingClientCode() throws InterruptedException {
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }");
            query.setTimeout(Duration.ofMillis(150));
            try (var rows = query.evaluate()) {
                assertTrue(viewClosed.await(5, TimeUnit.SECONDS));
                assertEquals(0, opened.get());
                assertFalse(Thread.currentThread().isInterrupted());
                assertThrows(QueryTimeoutException.class, rows::hasNext);
            }
        }
    }

    @Test
    void timeoutAfterHasNextDiscardsTheBufferedRowAndReleasesItsStatementCursor() throws InterruptedException {
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }");
            query.setTimeout(Duration.ofMillis(150));
            try (var rows = query.evaluate()) {
                assertTrue(rows.hasNext());
                assertTrue(viewClosed.await(5, TimeUnit.SECONDS));
                assertEquals(1, released.get());
                assertFalse(Thread.currentThread().isInterrupted());
                assertThrows(QueryTimeoutException.class, rows::next);
            }
        }
    }

    @Test
    void crossThreadCloseInterruptsAnActiveReadAndReleasesItsSnapshot() throws InterruptedException {
        var entered = new CountDownLatch(1);
        visit = statement -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            var cancellationFailed = new java.util.concurrent.atomic.AtomicBoolean();
            Thread closer = Thread.ofVirtual().start(() -> {
                try {
                    if (entered.await(5, TimeUnit.SECONDS)) rows.close();
                    else cancellationFailed.set(true);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    cancellationFailed.set(true);
                }
            });
            try {
                assertThrows(QueryEvaluationException.class, rows::next);
                assertTrue(Thread.currentThread().isInterrupted());
                assertEquals(1, closedSnapshots.get());
            } finally {
                Thread.interrupted();
                closer.join();
            }
            assertFalse(cancellationFailed.get());
        }
    }

    @Test
    void crossThreadCloseReleasesAnIdleCursorAndConsumptionRemainsConfined() throws InterruptedException {
        try (var connection = repository.getConnection();
             var rows = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }").evaluate()) {
            assertNotNull(rows.next());
            var closer = Thread.ofVirtual().start(rows::close);
            closer.join();
            assertEquals(1, closedSnapshots.get());
            assertThrows(IllegalStateException.class, rows::hasNext);
        }
    }
}
