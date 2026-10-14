package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CowStructuralCopyTest {
    @Test
    void mappedDenseCopyKeepsCollidingKeysHolesAndIndependentBuffers() throws ReflectiveOperationException {
        var source = new DenseTermTable<int[]>();
        for (int i = 0; i < 256; i++) {
            source.put(DenseTermTableTest.key(i, true), new int[] { i });
        }
        for (int i = 0; i < 256; i += 3) {
            source.remove(DenseTermTableTest.key(i, true));
        }
        var copy = new DenseTermTable<>(source, int[]::clone);
        assertEquals(new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
        var oldKeys = source.keySet().iterator();
        for (var entry : copy.entrySet()) {
            var key = entry.getKey();
            assertSame(oldKeys.next(), key);
            assertNotSame(source.get(key), entry.getValue());
            entry.getValue()[0] = -1;
            assertNotEquals(-1, source.get(key)[0]);
        }
        for (String name : List.of("keys", "data", "hashes", "positions")) {
            Field field = storageField(DenseTermTable.class, name);
            assertNotSame(field.get(source), field.get(copy));
        }
        var removed = copy.keySet().iterator().next();
        assertNotNull(copy.remove(removed));
        assertNotNull(source.get(removed));
        for (int i = 300; i < 500; i++) {
            copy.put(DenseTermTableTest.key(i, true), new int[] { i });
        }
        assertNull(source.get(DenseTermTableTest.key(300, true)));
        for (var entry : source.entrySet()) {
            assertNotNull(source.get(entry.getKey()));
        }
    }

    @Test
    void mappedCompactCopiesCoverSingletonPairsDenseAndRetainedDenseAfterChurn() throws ReflectiveOperationException {
        for (int count : new int[] { 0, 1, 2, 8, 9, 40 }) {
            assertCompactCopy(count);
        }
    }

    private static void assertCompactCopy(int count) throws ReflectiveOperationException {
        var source = new CompactTermMap<int[]>();
        for (int i = 0; i < count; i++) {
            source.add(DenseTermTableTest.key(i, true), new int[] { i });
        }
        if (count == 40) {
            for (int i = 0; i < 34; i++) {
                source.remove(DenseTermTableTest.key(i, true));
            }
        }
        List<TermKey> keys = new ArrayList<>();
        source.forEach((key, value) -> keys.add(key));
        var copy = new CompactTermMap<>(source, int[]::clone);
        List<TermKey> copiedKeys = new ArrayList<>();
        copy.forEach((key, value) -> copiedKeys.add(key));
        assertEquals(keys, copiedKeys);
        for (int i = 0; i < keys.size(); i++) {
            assertSame(keys.get(i), copiedKeys.get(i));
            copy.get(keys.get(i))[0] = -1;
            assertNotEquals(-1, source.get(keys.get(i))[0]);
        }
        Field entries = storageField(CompactTermMap.class, "entries");
        if (count > 1) {
            assertNotSame(entries.get(source), entries.get(copy));
        }
        copy.add(DenseTermTableTest.key(100, true), new int[] { 100 });
        for (var key : keys) {
            copy.remove(key);
        }
        assertEquals(keys.size(), source.size());
        assertNull(source.get(DenseTermTableTest.key(100, true)));
    }

    /**
     * Inspects private storage buffers so the tests can assert COW container
     * identity, which is deliberately unavailable through the production API.
     */
    @SuppressWarnings("java:S3011")
    private static Field storageField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test
    void writerAndThreeReadersStayIsolatedAcrossDenseBranchLeafChangesAndRollback() {
        var cfg = StorageConfig.memory();
        List<Statement> input = createDenseStatements();
        try (var root = Storages.create(cfg);
                var writer = root.openSession();
                var r1 = root.openSession();
                var r2 = root.openSession();
                var r3 = root.openSession()) {
            root.mutations().addAll(input);
            List<Statement> stored;
            try (var rows = root.queries().find(StatementPattern.matchAll())) {
                stored = rows.toList();
            }
            try (var t1 = r1.transactions().beginTransaction();
                    var t2 = r2.transactions().beginTransaction();
                    var t3 = r3.transactions().beginTransaction();
                    var tx = writer.transactions().beginTransaction()) {
                assertTrue(writer.mutations().remove(input.getFirst()));
                assertEquals(143, writer.mutations().remove(StatementPattern.of(new SimpleIRI("urn:s0"), null, null)));
                var fresh = Values.factory().createStatement(new SimpleIRI("urn:new"), new SimpleIRI("urn:new-p"),
                        new SimpleIRI("urn:new-o"));
                assertTrue(writer.mutations().add(fresh));
                for (var reader : List.of(root, r1, r2, r3)) {
                    assertReaderSnapshot(reader, input, stored);
                }
                MemoryStorageAssertions.verify((IndexedStatementStore) writer.queries());
                tx.rollback();
                try (var rows = writer.queries().find(StatementPattern.matchAll())) {
                    assertEquals(input, rows.toList());
                }
            }
        }
    }

    private static List<Statement> createDenseStatements() {
        List<Statement> input = new ArrayList<>();
        for (int s = 0; s < 12; s++) {
            for (int p = 0; p < 12; p++) {
                for (int o = 0; o < 12; o++) {
                    input.add(Values.factory().createStatement(new SimpleIRI("urn:s" + s), new SimpleIRI("urn:p" + p),
                            new SimpleIRI("urn:o" + o)));
                }
            }
        }
        return input;
    }

    private static void assertReaderSnapshot(StorageManager reader, List<Statement> expected, List<Statement> stored) {
        try (var rows = reader.queries().find(StatementPattern.matchAll())) {
            var actual = rows.toList();
            assertEquals(expected, actual);
            for (int i = 0; i < actual.size(); i++) {
                assertSame(stored.get(i), actual.get(i));
            }
        }
        MemoryStorageAssertions.verify((IndexedStatementStore) reader.queries());
    }
}
