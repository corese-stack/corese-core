package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Online selection owns the complete read snapshot until scan and ranking finish. */
class TopKLifecycleTest {
    private MemoryStorageManager storage;
    private StorageManager backend;
    private SparqlQueryExecutor executor;
    private final AtomicInteger read = new AtomicInteger();
    private final AtomicInteger opened = new AtomicInteger();
    private final AtomicInteger released = new AtomicInteger();
    private final AtomicInteger snapshots = new AtomicInteger();
    private final AtomicInteger closedSnapshots = new AtomicInteger();
    private Consumer<Statement> visit = statement -> { };
    private final AtomicReference<RuntimeException> openingFailure = new AtomicReference<>();
    private final AtomicReference<RuntimeException> closingFailure = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
        backend = mock(StorageManager.class, delegatesTo(storage));
        doAnswer(call -> observeSnapshot()).when(backend).openReadSnapshot();
        executor = new SparqlQueryExecutor(backend);
    }

    private StorageManager observeSnapshot() {
        StorageManager snapshot = storage.openReadSnapshot();
        snapshots.incrementAndGet();
        QueryOperations reads = mock(QueryOperations.class, delegatesTo(snapshot.queries()));
        doAnswer(call -> {
            if (openingFailure.get() != null) throw openingFailure.get();
            opened.incrementAndGet();
            return snapshot.queries().find(call.getArgument(0)).peek(statement -> {
                read.incrementAndGet();
                visit.accept(statement);
            }).onClose(() -> {
                released.incrementAndGet();
                if (closingFailure.get() != null) throw closingFailure.get();
            });
        }).when(reads).find(any());
        StorageManager view = mock(StorageManager.class, delegatesTo(snapshot));
        when(view.queries()).thenReturn(reads);
        doAnswer(call -> {
            snapshot.close();
            closedSnapshots.incrementAndGet();
            return null;
        }).when(view).close();
        return view;
    }

    @AfterEach
    void tearDown() {
        storage.close();
        assertEquals(opened.get(), released.get(), "All statement cursors must be released");
        assertEquals(snapshots.get(), closedSnapshots.get(), "All read snapshots must be released");
    }

    @Test
    void evaluatesEveryCandidateAndReleasesTheSnapshotBeforeReturningTheSmallResult() {
        try (var rows = executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10")) {
            assertEquals(1200, read.get());
            assertEquals(1, closedSnapshots.get());
            assertEquals(10, rows.stream().count());
        }
    }

    @Test
    void zeroLimitDoesNotReadInputEvenWithOffset() {
        try (var rows = executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s OFFSET 100 LIMIT 0")) {
            assertFalse(rows.hasNext());
            assertEquals(0, read.get());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @Test
    void clearingTheSourceDuringScanningPreservesBothTheScanAndLaterJoinLookups() {
        visit = statement -> {
            if (read.get() == 1) storage.mutations().clear();
        };
        try (var rows = executor.evaluateTuple("""
                SELECT ?s ?name { ?s <urn:bench:type> ?type . ?s <urn:bench:name> ?name }
                ORDER BY ?name LIMIT 3
                """)) {
            assertEquals(List.of("name0", "name1", "name10"),
                    rows.stream().map(row -> row.getValue("name").stringValue()).toList());
            assertEquals(0, storage.queries().count(StatementPattern.matchAll()));
            assertEquals(400, read.get());
            assertEquals(1, closedSnapshots.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void storageFailuresPreserveTheirCauseAndReleaseTheCompleteView(boolean traversing) {
        var cause = new StorageException(ErrorCode.QUERY_FAILED, "Read failed");
        if (traversing) visit = statement -> { throw cause; };
        else openingFailure.set(cause);
        QueryEvaluationException failure = assertThrows(QueryEvaluationException.class,
                () -> executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10"));
        assertSame(cause, failure.getCause());
        assertEquals(1, closedSnapshots.get());
    }

    @Test
    void fatalTraversalErrorsAreRethrownUnchangedAfterCleanup() {
        var cause = new AssertionError("Traversal failed");
        visit = statement -> { throw cause; };
        assertSame(cause, assertThrows(AssertionError.class,
                () -> executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10")));
        assertEquals(1, closedSnapshots.get());
    }

    @Test
    void aFailingCloseCallbackDoesNotHideTheTraversalFailure() {
        var cause = new StorageException(ErrorCode.QUERY_FAILED, "Read failed");
        closingFailure.set(new IllegalStateException("Close failed"));
        visit = statement -> { throw cause; };
        var failure = assertThrows(QueryEvaluationException.class,
                () -> executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10"));
        assertSame(cause, failure.getCause());
        assertEquals(1, cause.getSuppressed().length);
        assertSame(closingFailure.get(), cause.getSuppressed()[0]);
        assertEquals(1, closedSnapshots.get());
    }

    @Test
    void anInterruptedScanReleasesTheSnapshot() {
        visit = statement -> Thread.currentThread().interrupt();
        try {
            assertThrows(QueryEvaluationException.class,
                    () -> executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10"));
            assertEquals(1, closedSnapshots.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void deadlineCancelsTheOnlineScanAndDoesNotContaminateTheFollowingQuery() {
        visit = statement -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new QueryEvaluationException("Read interrupted", failure);
            }
        };
        assertTimeout(Duration.ofSeconds(3), () -> assertThrows(QueryTimeoutException.class,
                () -> executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 10", null, null, 100)));
        assertEquals(1, closedSnapshots.get());
        assertFalse(Thread.currentThread().isInterrupted());
        visit = statement -> { };
        try (var rows = executor.evaluateTuple("SELECT * { ?s ?p ?o } ORDER BY ?s LIMIT 1")) {
            assertTrue(rows.hasNext());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * { ?s ?p ?o } ORDER BY ?s",
            "SELECT ?p (COUNT(*) AS ?n) { ?s ?p ?o } GROUP BY ?p ORDER BY ?n LIMIT 2",
            "SELECT * { { SELECT ?s { ?s ?p ?o } } } ORDER BY ?s LIMIT 2",
            "SELECT ?s { ?s <urn:bench:link>+ ?o } ORDER BY ?s LIMIT 2",
            "SELECT ?s { ?s ?p ?o MINUS { ?s <urn:absent> ?x } } ORDER BY ?s LIMIT 2",
            "SELECT * { ?s ?p ?o } ORDER BY ?s OFFSET 2147483647 LIMIT 10"
    })
    void inadmissibleSelectionRetainsTheClassicCompleteEvaluator(String query) {
        try (TupleQueryResult rows = executor.evaluateTuple(query)) {
            assertNotNull(rows);
            assertEquals(0, snapshots.get());
            verify(backend, never()).openReadSnapshot();
        }
    }
}
