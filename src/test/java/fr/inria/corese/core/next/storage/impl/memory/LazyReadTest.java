package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

class LazyReadTest {
    private static final Resource G = new SimpleIRI("urn:lazy:g");
    private static final Resource H = new SimpleIRI("urn:lazy:h");
    private static StorageManager storage() { return Storages.create(StorageConfig.memory()); }
    private static Statement row(int s, int p, int o, Resource g) {
        return Values.factory().createStatement(new SimpleIRI("urn:lazy:s" + s),
                new SimpleIRI("urn:lazy:p" + p), new SimpleIRI("urn:lazy:o" + o), g);
    }
    @SuppressWarnings("java:S3011")
    private static Object partition(StorageManager storage, Resource graph) throws ReflectiveOperationException {
        Object store = storage.queries();
        var method = store.getClass().getDeclaredMethod("state");
        method.setAccessible(true);
        Object state = method.invoke(store);
        Field field = state.getClass().getDeclaredField("graphs");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(state)).get(graph);
    }
    @SuppressWarnings("java:S3011")
    private static int references(Object partition) throws ReflectiveOperationException {
        Field field = partition.getClass().getDeclaredField("references");
        field.setAccessible(true);
        return field.getInt(partition);
    }
    private static List<Statement> all(StorageManager storage) {
        try (var stream = storage.queries().find(StatementPattern.matchAll())) { return stream.toList(); }
    }

    @Test void positionalCursorMatchesBulkTraversalAcrossAllMasksDenseLeavesAndHoles() {
        try (var storage = storage()) {
            List<Statement> surviving = createSurvivingDataset(storage);
            for (int mask = 0; mask < 8; mask++) {
                for (Resource[] graphs : new Resource[][]{{}, {null}, {H, G}, {G, H}}) {
                    var pattern = StatementPattern.of((mask & 4) == 0 ? null : new SimpleIRI("urn:lazy:s2"),
                            (mask & 2) == 0 ? null : new SimpleIRI("urn:lazy:p3"),
                            (mask & 1) == 0 ? null : new SimpleIRI("urn:lazy:o4"), graphs);
                    verifyPatternTraversal(storage, surviving, pattern, mask);
                }
            }
            MemoryStorageAssertions.verify((IndexedStatementStore) storage.queries());
        }
    }

    private static List<Statement> createSurvivingDataset(StorageManager storage) {
        List<Statement> input = new ArrayList<>();
        for (Resource g : new Resource[]{null, G, H}) {
            for (int s = 0; s < 12; s++) {
                for (int p = 0; p < 12; p++) {
                    for (int o = 0; o < 12; o++) {
                        input.add(row(s, p, o, g));
                    }
                }
            }
        }
        storage.mutations().addAll(input);
        for (int i = 0; i < input.size(); i += 7) {
            storage.mutations().remove(input.get(i));
        }
        return all(storage);
    }

    private static void verifyPatternTraversal(StorageManager storage, List<Statement> surviving,
            StatementPattern pattern, int mask) {
        List<Statement> bulk;
        List<Statement> cursor = new ArrayList<>();
        try (var stream = storage.queries().find(pattern)) { bulk = stream.toList(); }
        try (var stream = storage.queries().find(pattern)) {
            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                assertTrue(iterator.hasNext());
                cursor.add(iterator.next());
            }
        }
        assertEquals(bulk, cursor, "encounter order, mask " + mask);
        var oracle = surviving.stream().filter(s -> matchesPattern(s, pattern)).toList();
        assertEquals(new HashSet<>(oracle), new HashSet<>(cursor));
    }

    private static boolean matchesPattern(Statement s, StatementPattern query) {
        return (query.getSubject() == null || query.getSubject().equals(s.getSubject()))
                && (query.getPredicate() == null || query.getPredicate().equals(s.getPredicate()))
                && (query.getObject() == null || query.getObject().equals(s.getObject()))
                && (query.getContexts().length == 0 || java.util.Arrays.asList(query.getContexts()).contains(s.getContext()));
    }

    @Test void cursorsCaptureAtFindAndSurviveConcurrentMutationClearAndShutdown() {
        var root = storage();
        try {
            List<Statement> input = new ArrayList<>();
            for (int i = 0; i < 40; i++) input.add(row(0, 0, i, null));
            root.mutations().addAll(input); List<Statement> original = all(root);
            try (var unopened = root.queries().find(StatementPattern.matchAll());
                 var partial = root.queries().find(StatementPattern.matchAll())) {
                var iterator = partial.iterator(); assertSame(original.getFirst(), iterator.next());
                CompletableFuture.runAsync(() -> {
                    root.mutations().remove(input.get(5)); root.mutations().add(row(2, 3, 4, null));
                    root.mutations().clear(); root.close();
                }).join();
                assertEquals(original, unopened.toList());
                List<Statement> remaining = new ArrayList<>(); iterator.forEachRemaining(remaining::add);
                assertEquals(original.subList(1, original.size()), remaining);
                for (int i = 0; i < remaining.size(); i++) assertSame(original.get(i + 1), remaining.get(i));
            }
        } finally { root.close(); }
    }

    @Test void closeExhaustionAndConsumerFailureReleaseExactlyOnce() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, 0, 0, null)); Object partition = partition(root, null);
            var a = root.queries().find(StatementPattern.matchAll());
            var b = root.queries().find(StatementPattern.matchAll()); assertEquals(3, references(partition));
            a.close(); a.close(); assertEquals(2, references(partition));
            b.close(); assertEquals(1, references(partition));
            try (var stream = root.queries().find(StatementPattern.matchAll())) { assertEquals(1, stream.count()); }
            assertEquals(1, references(partition));
            assertEquals(1, root.queries().find(StatementPattern.matchAll()).parallel().mapToLong(s -> 1).sum());
            assertEquals(1, references(partition));
            try (var stream = root.queries().find(StatementPattern.matchAll())) {
                assertThrows(IllegalArgumentException.class, () -> stream.forEach(s -> { throw new IllegalArgumentException("consumer"); }));
                assertEquals(1, references(partition));
            }
            try (var stream = root.queries().find(StatementPattern.matchAll())) {
                assertThrows(IllegalArgumentException.class, () -> stream.limit(10).forEach(s -> { throw new IllegalArgumentException("partial consumer"); }));
                assertEquals(1, references(partition));
            }
            try (var stream = root.queries().find(StatementPattern.of(new SimpleIRI("urn:absent"), null, null))) {
                assertTrue(stream.findFirst().isEmpty()); assertEquals(1, references(partition));
            }
        }
    }

    @Test void partialStreamsCloseWithoutKeepingOldPartitionBuffers() throws Exception {
        try (var root = storage()) {
            for (int i = 0; i < 20; i++) root.mutations().add(row(0, 0, i, null));
            Object original = partition(root, null);
            var stream = root.queries().find(StatementPattern.matchAll());
            var cursor = stream.spliterator(); assertTrue(cursor.tryAdvance(s -> {}));
            root.mutations().add(row(1, 1, 1, null)); assertNotSame(original, partition(root, null));
            assertEquals(1, references(original)); stream.close(); assertEquals(0, references(original));
            assertFalse(cursor.tryAdvance(s -> fail("closed cursor")));
            Object writer = partition(root, null); root.mutations().add(row(1, 1, 2, null));
            assertSame(writer, partition(root, null));
        }
    }

    @Test void explicitContextsDoNotShareUnselectedGraphsAndKeepRegistryOrder() throws Exception {
        try (var root = storage()) {
            root.mutations().addAll(List.of(row(0, 0, 0, G), row(0, 0, 1, H), row(0, 0, 2, null)));
            Object unselected = partition(root, H);
            try (var cursor = root.queries().find(StatementPattern.of(null, null, null, G, G))) {
                root.mutations().add(row(0, 0, 3, H)); assertSame(unselected, partition(root, H));
                assertEquals(List.of(row(0, 0, 0, G)), cursor.toList());
            }
            try (var cursor = root.queries().find(StatementPattern.of(null, null, null, null, G))) {
                assertEquals(List.of(row(0, 0, 0, G), row(0, 0, 2, null)), cursor.toList());
            }
        }
    }

    @Test void cursorOwnsItsVersionAcrossTransactionCommitAndRollback() {
        try (var root = storage(); var session = root.openSession()) {
            root.mutations().add(row(0, 0, 0, null));
            try (var transaction = session.transactions().beginTransaction();
                 var cursor = session.queries().find(StatementPattern.matchAll())) {
                session.mutations().add(row(0, 0, 1, null)); transaction.commit();
                assertEquals(List.of(row(0, 0, 0, null)), cursor.toList()); assertEquals(2, all(root).size());
            }
            try (var transaction = session.transactions().beginTransaction()) {
                session.mutations().add(row(0, 0, 2, null));
                try (var cursor = session.queries().find(StatementPattern.matchAll())) {
                    transaction.rollback(); assertEquals(3, cursor.toList().size()); assertEquals(2, all(root).size());
                }
            }
        }
    }

    @Test void threeIndependentReadersAndWriterCanProgressConcurrently() {
        try (var root = storage()) {
            List<Statement> input = new ArrayList<>();
            for (int s = 0; s < 120; s++) {
                for (int p = 0; p < 12; p++) {
                    for (int o = 0; o < 12; o++) {
                        input.add(row(s, p, o, null));
                    }
                }
            }
            root.mutations().addAll(input);
            List<Statement> expected = all(root);
            var start = new CountDownLatch(1);
            List<CompletableFuture<Void>> tasks = new ArrayList<>();
            for (int r = 0; r < 3; r++) {
                tasks.add(launchConcurrentReader(root, start, expected));
            }
            tasks.add(launchConcurrentWriter(root, start, input));
            start.countDown();
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
            MemoryStorageAssertions.verify((IndexedStatementStore) root.queries());
        }
    }

    private static CompletableFuture<Void> launchConcurrentReader(StorageManager root,
            CountDownLatch start, List<Statement> expected) {
        var stream = root.queries().find(StatementPattern.matchAll());
        return CompletableFuture.runAsync(() -> {
            try (stream) {
                try {
                    start.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
                List<Statement> actual = new ArrayList<>();
                var iterator = stream.iterator();
                while (iterator.hasNext()) {
                    actual.add(iterator.next());
                    if (actual.size() % 100 == 0) {
                        Thread.yield();
                    }
                }
                assertEquals(expected, actual);
            }
        });
    }

    private static CompletableFuture<Void> launchConcurrentWriter(StorageManager root,
            CountDownLatch start, List<Statement> input) {
        return CompletableFuture.runAsync(() -> {
            try {
                start.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
            for (int i = 0; i < 50; i++) {
                root.mutations().remove(input.get(i));
                root.mutations().add(row(500, 0, i, null));
            }
        });
    }
}
