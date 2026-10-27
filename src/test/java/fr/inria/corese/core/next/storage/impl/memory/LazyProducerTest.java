package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.eval.RdfTermMatcher;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlKgramEvaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LazyProducerTest {
    private static final String PREFIX = "PREFIX : <urn:bench:> ";

    private static final class Fixture implements AutoCloseable {
        final StorageManager root = Storages.create(StorageConfig.memory());
        final StorageManager observed = mock(StorageManager.class);
        final QueryOperations queries = mock(QueryOperations.class);
        final AtomicInteger opened = new AtomicInteger();
        final AtomicInteger read = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        Consumer<Statement> callback = s -> { };
        final SparqlQueryExecutor executor;

        Fixture() {
            root.mutations().addAll(StorageBenchmark.data(20, 0, false));
            when(observed.queries()).thenReturn(queries);
            when(observed.openReadSnapshot()).thenThrow(UnsupportedOperationException.class);
            when(observed.metadata()).thenReturn(root.metadata());
            when(queries.find(any())).thenAnswer(call -> {
                opened.incrementAndGet();
                return root.queries().find(call.getArgument(0)).peek(s -> {
                    read.incrementAndGet(); callback.accept(s);
                }).onClose(closed::incrementAndGet);
            });
            executor = new SparqlQueryExecutor(observed);
        }

        @SuppressWarnings("java:S3011")
        Object partition() throws ReflectiveOperationException {
            Object store = root.queries();
            var method = store.getClass().getDeclaredMethod("state");
            method.setAccessible(true);
            Object state = method.invoke(store);
            var field = state.getClass().getDeclaredField("graphs");
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(state)).get(null);
        }

        @SuppressWarnings("java:S3011")
        int references() throws ReflectiveOperationException {
            Object partition = partition();
            var field = partition.getClass().getDeclaredField("references");
            field.setAccessible(true);
            return field.getInt(partition);
        }
        void assertReleased() throws Exception {
            assertEquals(opened.get(), closed.get(), "all opened streams closed exactly once");
            assertEquals(1, references(), "only the live storage state owns the partition");
            Object partition = partition();
            root.mutations().add(Values.factory().createStatement(new SimpleIRI("urn:new:s"),
                    new SimpleIRI("urn:new:p"), new SimpleIRI("urn:new:o")));
            assertSame(partition, partition(), "no ghost snapshot forces the next mutation to copy");
        }
        @Override public void close() { root.close(); }
    }

    @Test void askAndLimitsReadOnlyTheRequestedPrefixAndReleaseSnapshots() throws Exception {
        for (int limit : new int[]{0, 1, 10}) try (var f = new Fixture()) {
            if (limit == 0) assertTrue(f.executor.evaluateBoolean("ASK { ?s ?p ?o }"));
            else try (var result = f.executor.evaluateTuple("SELECT * { ?s ?p ?o } LIMIT " + limit)) {
                assertEquals(limit, result.stream().count());
            }
            assertEquals(limit == 0 ? 1 : limit, f.read.get());
            f.assertReleased();
        }
    }

    @Test void selectiveBgpPushesBindingsWithoutBuildingAllFirstEdgeSolutions() throws Exception {
        try (var f = new Fixture(); var result = f.executor.evaluateTuple(PREFIX
                + "SELECT ?s ?name { ?s :type :Person . ?s :name ?name } LIMIT 1")) {
            assertTrue(result.hasNext());
            var row = result.next(); assertEquals("urn:bench:s0", row.getValue("s").stringValue());
            assertEquals("name0", row.getValue("name").stringValue()); assertFalse(result.hasNext());
            assertEquals(2, f.read.get()); f.assertReleased();
        }
    }

    @Test void defaultGraphDeduplicatesWhileNamedGraphsKeepTheirOwnSolutions() throws Exception {
        try (var f = new Fixture()) {
            var duplicate = Values.factory().createStatement(new SimpleIRI("urn:bench:s0"),
                    new SimpleIRI("urn:bench:name"), Values.factory().createLiteral("name0"), new SimpleIRI("urn:graph"));
            f.root.mutations().add(duplicate);
            try (var result = f.executor.evaluateTuple(PREFIX + "SELECT ?s ?name { ?s :name ?name }")) {
                assertEquals(20, result.stream().count());
            }
            try (var result = f.executor.evaluateTuple(PREFIX + "SELECT ?g ?s { GRAPH ?g { ?s :name ?name } }")) {
                assertEquals(1, result.stream().count());
            }
            f.assertReleased();
        }
    }

    @Test void fullScansEmptyMatchesOffsetDistinctAndOrderingPreserveSemantics() throws Exception {
        try (var f = new Fixture()) {
            try (var result = f.executor.evaluateTuple("SELECT * { ?s ?p ?o }")) {
                assertEquals(120, result.stream().count());
            }
            assertFalse(f.executor.evaluateBoolean("ASK { <urn:absent> ?p ?o }"));
            try (var result = f.executor.evaluateTuple(PREFIX
                    + "SELECT DISTINCT ?g { ?s :group ?g } ORDER BY DESC(?g) OFFSET 1 LIMIT 2")) {
                assertEquals(List.of("urn:bench:g8", "urn:bench:g7"), result.stream()
                        .map(b -> b.getValue("g").stringValue()).toList());
            }
            f.assertReleased();
        }
    }

    @Test void askRespectsOffsetLimitZeroAndOrdering() throws Exception {
        try (var f = new Fixture()) {
            assertTrue(f.executor.evaluateBoolean("ASK { ?s ?p ?o } OFFSET 119"));
            assertFalse(f.executor.evaluateBoolean("ASK { ?s ?p ?o } OFFSET 120"));
            assertFalse(f.executor.evaluateBoolean("ASK { ?s ?p ?o } LIMIT 0"));
            assertTrue(f.executor.evaluateBoolean("ASK { ?s ?p ?o } ORDER BY DESC(?s) OFFSET 119"));
            f.assertReleased();
        }
    }

    @Test void storageTraversalFailureReleasesSnapshot() throws Exception {
        try (var f = new Fixture()) {
            f.callback = s -> { throw new IllegalStateException("storage traversal failed"); };
            assertThrows(IllegalStateException.class, () -> f.executor.evaluateBoolean("ASK { ?s ?p ?o }"));
            f.assertReleased();
        }
    }

    @Test void matcherFailureClosesTheCurrentlySuspendedEdgeCursor() throws Exception {
        try (var f = new Fixture()) {
            var producer = new StorageManagerProducer(f.observed);
            Matcher matcher = mock(Matcher.class);
            when(matcher.match(any(Edge.class), any(Edge.class), any(Environment.class)))
                    .thenThrow(new IllegalStateException("matcher failed"));
            var eval = Eval.create(producer, new SparqlKgramEvaluator(), matcher);
            var query = new CoreseAstQueryBuilder().toNextQuery((SelectQueryAst) new SparqlParser().parse("SELECT * { ?s ?p ?o }"));
            assertThrows(IllegalStateException.class, () -> eval.query(query));
            assertEquals(1, f.read.get()); f.assertReleased();
        }
    }

    @Test void iterablesAreLazyRepeatableAndCloseIdempotently() throws Exception {
        try (var f = new Fixture()) {
            var producer = new StorageManagerProducer(f.observed);
            var query = new CoreseAstQueryBuilder().toNextQuery((SelectQueryAst) new SparqlParser().parse("SELECT * { ?s ?p ?o }"));
            Edge edge = query.getBody().first().first().getEdge();
            Environment environment = mock(Environment.class); when(environment.getQuery()).thenReturn(query);
            var iterable = producer.getEdges(null, List.of(), edge, environment);
            assertEquals(0, f.opened.get());
            var iterator = iterable.iterator(); assertEquals(2, f.references());
            assertTrue(iterator.hasNext()); assertTrue(iterator.hasNext()); assertEquals(1, f.read.get());
            assertNotNull(iterator.next()); producer.closeEdges(iterable); producer.closeEdges(iterable);
            assertFalse(iterator.hasNext());
            List<Edge> rows = new ArrayList<>(); iterable.forEach(rows::add); assertEquals(120, rows.size());
            producer.finish(query); producer.close(); producer.close(); f.assertReleased();
        }
    }

    @Test void interruptionDuringTraversalClosesTheCursorAndPreservesInterruptStatus() throws Exception {
        try (var f = new Fixture()) {
            f.callback = s -> Thread.currentThread().interrupt();
            try {
                assertThrows(QueryEvaluationException.class, () -> f.executor.evaluateTuple("SELECT * { ?s ?p ?o }"));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
            f.assertReleased();
        }
    }

    @Test void timeoutClosesAnActuallyOpenSnapshotBeforeReturningToTheCaller() throws Exception {
        try (var f = new Fixture()) {
            // Warm the parser/bridge before installing a deterministically blocking source.
            assertTrue(f.executor.evaluateBoolean("ASK { ?s ?p ?o }"));
            CountDownLatch entered = new CountDownLatch(1);
            f.callback = s -> {
                entered.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new QueryEvaluationException("interrupted source", failure);
                }
            };
            var result = CompletableFuture.runAsync(() -> assertThrows(QueryTimeoutException.class,
                    () -> f.executor.evaluateTuple("SELECT * { ?s ?p ?o }", null, null, 500)));
            assertTrue(entered.await(5, TimeUnit.SECONDS), "cursor opened before timeout");
            result.get(5, TimeUnit.SECONDS); f.assertReleased();
        }
    }
}
