package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryDeletionTest {
    private static final StorageConfig CONFIG = StorageConfig.memory();
    private static final SimpleIRI S = new SimpleIRI("urn:delete:s");
    private static final SimpleIRI P = new SimpleIRI("urn:delete:p");
    private static final SimpleIRI G = new SimpleIRI("urn:delete:g");

    private static Statement statement(int i, Resource graph) {
        return Values.factory().createStatement(S, P, new SimpleIRI("urn:delete:o" + i), graph);
    }

    private static List<Statement> rows(StorageManager storage) {
        try (var stream = storage.queries().find(StatementPattern.matchAll())) {
            return stream.toList();
        }
    }

    private static void verify(StorageManager storage) {
        MemoryStorageAssertions.verify((IndexedStatementStore) storage.queries());
    }

    @Test
    void clearRetainsGraphOrderCanonicalContextsAndThreeConcurrentSnapshots()
            throws InterruptedException, ExecutionException, TimeoutException {
        try (var root = Storages.create(CONFIG)) {
            var later = new SimpleIRI("urn:delete:later");
            root.mutations().add(statement(0, G));
            root.mutations().add(statement(1, null));
            root.mutations().add(statement(2, later));
            List<Statement> original = rows(root);
            var ready = new CountDownLatch(3);
            var cleared = new CountDownLatch(1);
            List<CompletableFuture<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                tasks.add(startSnapshotReader(root, original, ready, cleared));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
                // Duplicate selectors, reversed order, default and absent graph.
                assertEquals(2, root.mutations().clear(null, G, G, new SimpleIRI("urn:absent")));
                assertEquals(Set.of(G, later), root.metadata().getContexts());
                assertEquals(0, root.mutations().clear(null, G));
                // Reinsert in reverse order: graph registry order must still win.
                root.mutations().add(statement(3, null));
                root.mutations().add(statement(4, new SimpleIRI(G.stringValue())));
                List<Statement> current = rows(root);
                assertEquals(List.of(statement(4, G), statement(3, null), statement(2, later)), current);
                assertSame(original.getFirst().getContext(), current.getFirst().getContext());
                verify(root);
            } finally {
                cleared.countDown();
            }
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
            assertEquals(3, root.mutations().remove(StatementPattern.matchAll()));
            assertEquals(Set.of(G, later), root.metadata().getContexts());
            assertTrue(root.mutations().dropGraph(G));
            assertEquals(Set.of(later), root.metadata().getContexts());
            verify(root);
        }
    }

    private static CompletableFuture<Void> startSnapshotReader(StorageManager root, List<Statement> original,
            CountDownLatch ready, CountDownLatch cleared) {
        return CompletableFuture.runAsync(() -> {
            try (var reader = root.openSession(); var tx = reader.transactions().beginTransaction()) {
                ready.countDown();
                assertTrue(cleared.await(10, TimeUnit.SECONDS));
                assertSameStatements(original, rows(reader));
                verify(reader);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
    }

    private static void assertSameStatements(List<Statement> expected, List<Statement> actual) {
        assertEquals(expected, actual);
        for (int i = 0; i < expected.size(); i++) {
            assertSame(expected.get(i), actual.get(i));
        }
    }

    @Test
    void clearRollbackCommitConflictsAndNoOpVersionsPreserveOwners() {
        try (var root = Storages.create(CONFIG); var writer = root.openSession(); var reader = root.openSession()) {
            var input = List.of(statement(0, G), statement(1, null));
            root.mutations().addAll(input);
            try (var snapshot = reader.transactions().beginTransaction()) {
                try (var tx = writer.transactions().beginTransaction()) {
                    assertEquals(2, writer.mutations().clear());
                    assertEquals(input, rows(root));
                    assertEquals(input, rows(reader));
                    tx.rollback();
                }
                assertEquals(input, rows(writer));
                try (var tx = writer.transactions().beginTransaction()) {
                    assertEquals(2, writer.mutations().clear());
                    writer.mutations().add(statement(2, G));
                    tx.commit();
                }
                assertEquals(input, rows(reader));
                assertEquals(List.of(statement(2, G)), rows(root));
                assertThrows(StorageException.class, snapshot::commit);
                verify(reader);
                verify(root);
            }
            try (var tx = writer.transactions().beginTransaction()) {
                assertEquals(0, root.mutations().clear((Resource) null));
                assertEquals(0, root.mutations().clear(new SimpleIRI("urn:absent")));
                writer.mutations().add(statement(3, G));
                tx.commit(); // Empty clears must not create conflicts or missing graphs.
            }
            assertFalse(root.metadata().getContexts().contains(new SimpleIRI("urn:absent")));
            verify(root);
        }
    }

    @Test
    void batchFilteringMatchesOrderedOracleForAllMasksAndPermutations() {
        List<Statement> input = createIndexStatements();
        for (TripleIndex.Order order : TripleIndex.Order.values()) {
            for (int mask = 0; mask < 8; mask++) {
                assertOrderedFiltering(input, order, mask);
            }
        }
    }

    private static List<Statement> createIndexStatements() {
        List<Statement> input = new ArrayList<>();
        for (int s = 0; s < 16; s++) {
            for (int p = 0; p < 4; p++) {
                for (int o = 0; o < 16; o++) {
                    input.add(Values.factory().createStatement(new SimpleIRI("urn:s" + s), new SimpleIRI("urn:p" + p),
                            new SimpleIRI("urn:o" + o)));
                }
            }
        }
        return input;
    }

    private static void assertOrderedFiltering(List<Statement> input, TripleIndex.Order order, int mask) {
        TripleIndex index = new TripleIndex(order);
        for (Statement s : input) {
            MemoryStorageAssertions.add(index, s);
        }
        index.finishStaging();
        TripleIndex snapshot = index.copy();
        var subject = (mask & 4) == 0 ? null : input.getFirst().getSubject();
        var predicate = (mask & 2) == 0 ? null : input.getFirst().getPredicate();
        var object = (mask & 1) == 0 ? null : input.getFirst().getObject();
        List<Statement> survivors = input.stream().filter(s -> !matches(s, subject, predicate, object)).toList();
        List<Statement> before = new ArrayList<>();
        snapshot.visit(StatementPattern.matchAll(), s -> {
            before.add(s);
            return false;
        });
        assertEquals((long) input.size() - survivors.size(),
                index.removeMatches(TermKey.nullable(subject), TermKey.nullable(predicate), TermKey.nullable(object)));
        List<Statement> actual = new ArrayList<>();
        index.visit(StatementPattern.matchAll(), s -> {
            actual.add(s);
            return false;
        });
        assertEquals(before.stream().filter(survivors::contains).toList(), actual);
        assertEquals(survivors.size(), index.size());
        assertEquals(0,
                index.removeMatches(TermKey.nullable(subject), TermKey.nullable(predicate), TermKey.nullable(object)));
        List<Statement> unchanged = new ArrayList<>();
        snapshot.visit(StatementPattern.matchAll(), s -> {
            unchanged.add(s);
            return false;
        });
        assertEquals(before, unchanged);
        restoreRemovedStatements(index, input, survivors);
        index.finishStaging();
        assertEquals(input.size(), index.size());
        for (Statement s : input) {
            assertEquals(1, index.count(StatementPattern.of(s.getSubject(), s.getPredicate(), s.getObject())));
        }
    }

    private static boolean matches(Statement statement, Resource subject, IRI predicate, Value object) {
        return (subject == null || subject.equals(statement.getSubject()))
                && (predicate == null || predicate.equals(statement.getPredicate()))
                && (object == null || object.equals(statement.getObject()));
    }

    private static void restoreRemovedStatements(TripleIndex index, List<Statement> input, List<Statement> survivors) {
        for (Statement statement : input) {
            if (!survivors.contains(statement)) {
                assertTrue(MemoryStorageAssertions.add(index, statement));
            }
        }
    }

    @Test
    void broadPatternsAcrossGraphsRetainTermsAndReaderVersions() {
        try (var root = Storages.create(CONFIG); var reader = root.openSession()) {
            List<Statement> input = createGraphStatements();
            root.mutations().addAll(input);
            List<Statement> before = rows(root);
            try (var tx = reader.transactions().beginTransaction()) {
                assertEquals(512, root.mutations().remove(StatementPattern.of(null, P, null, G, null)));
                var actual = rows(root);
                var expected = before.stream().filter(s -> !s.getPredicate().equals(P)).toList();
                assertSameStatements(expected, actual);
                assertEquals(before, rows(reader));
                verify(root);
                verify(reader);
                // Bound pattern covers an entire remaining partition: same fast clear contract.
                assertEquals(768,
                        root.mutations().remove(StatementPattern.of(null, new SimpleIRI("urn:keep"), null, G)));
                assertEquals(Set.of(G), root.metadata().getContexts());
                assertEquals(before, rows(reader));
                root.mutations().addAll(input);
                verify(root);
                verify(reader);
            }
        }
    }

    private static List<Statement> createGraphStatements() {
        List<Statement> input = new ArrayList<>();
        for (Resource graph : new Resource[] { null, G }) {
            for (int i = 0; i < 1024; i++) {
                input.add(Values.factory().createStatement(new SimpleIRI("urn:s" + (i / 8)),
                        i % 4 == 0 ? P : new SimpleIRI("urn:keep"), new SimpleIRI("urn:o" + i), graph));
            }
        }
        return input;
    }

    @Test
    void denseBatchFilteringHandlesHolesCollisionsNoOpsAndReinsertion() {
        for (boolean collision : List.of(false, true)) {
            assertDenseBatchFiltering(collision);
        }
    }

    private static void assertDenseBatchFiltering(boolean collision) {
        var table = new DenseTermTable<Integer>();
        var oracle = new LinkedHashMap<TermKey, Integer>();
        for (int i = 0; i < 512; i++) {
            var key = DenseTermTableTest.key(i, collision);
            table.put(key, i);
            oracle.put(key, i);
        }
        for (int i = 0; i < 50; i++) {
            var key = DenseTermTableTest.key(i, collision);
            table.remove(key);
            oracle.remove(key);
        }
        var copy = new DenseTermTable<>(table);
        assertEquals(0, table.removeIf(i -> false));
        for (var entry : oracle.entrySet()) {
            assertEquals(entry.getValue(), table.get(entry.getKey()));
        }
        int size = oracle.size();
        oracle.values().removeIf(i -> i % 3 != 0);
        assertEquals(size - oracle.size(), table.removeIf(i -> i % 3 != 0));
        assertEquals(new ArrayList<>(oracle.values()), new ArrayList<>(table.values()));
        var keys = table.keySet().iterator();
        for (var entry : oracle.entrySet()) {
            assertSame(entry.getKey(), keys.next());
            assertEquals(entry.getValue(), table.get(entry.getKey()));
        }
        assertEquals(size, copy.size());
        assertEquals(table.size(), table.removeIf(i -> true));
        assertEquals(0, table.removeIf(i -> true));
        table.put(DenseTermTableTest.key(0, collision), 7);
        assertEquals(7, table.get(DenseTermTableTest.key(0, collision)));
    }
}
