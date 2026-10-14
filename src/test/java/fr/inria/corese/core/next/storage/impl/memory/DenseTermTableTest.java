package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DenseTermTableTest {
    static TermKey key(int i, boolean collision) {
        StringBuilder s = new StringBuilder("urn:");
        if (collision) {
            for (int b = 0; b < 9; b++) {
                s.append((i & (1 << b)) == 0 ? "Aa" : "BB");
            }
        } else {
            s.append("key:").append(i);
        }
        return new TermKey(new SimpleIRI(s.toString()));
    }

    @Test
    void randomChangesAndCompactionPreserveOrderAndCopies() {
        for (boolean collision : List.of(false, true)) {
            exerciseRandomChanges(collision);
        }
    }

    private static void exerciseRandomChanges(boolean collision) {
        var map = new DenseTermTable<String>();
        var oracle = new LinkedHashMap<TermKey, String>();
        var random = new Random(17689);
        for (int op = 0; op < 15000; op++) {
            TermKey k = key(random.nextInt(512), collision);
            String v = "v" + op;
            switch (random.nextInt(5)) {
            case 0 -> assertEquals(oracle.put(k, v), map.put(k, v));
            case 1 -> assertEquals(oracle.remove(k), map.remove(k));
            case 2 -> assertEquals(oracle.replace(k, v), map.replace(k, v));
            case 3 -> assertEquals(oracle.putIfAbsent(k, v), map.putIfAbsent(k, v));
            case 4 -> assertEquals(oracle.computeIfAbsent(k, x -> v), map.computeIfAbsent(k, x -> v));
            default -> throw new AssertionError("Unexpected random operation");
            }
            assertEquals(oracle.get(k), map.get(k));
            assertEquals(oracle.size(), map.size());
            if (op % 71 == 0) {
                assertEquals(new ArrayList<>(oracle.keySet()), new ArrayList<>(map.keySet()));
                assertEquals(new ArrayList<>(oracle.values()), new ArrayList<>(map.values()));
                var copy = new DenseTermTable<>(map);
                map.put(key(513, false), "extra");
                assertEquals(new ArrayList<>(oracle.values()), new ArrayList<>(copy.values()));
                map.remove(key(513, false));
            }
        }
        for (var k : new ArrayList<>(oracle.keySet())) {
            assertEquals(oracle.remove(k), map.remove(k));
        }
        assertTrue(map.isEmpty());
        map.put(key(0, collision), "reborn");
        assertEquals(List.of("reborn"), new ArrayList<>(map.values()));
    }

    @Test
    void duplicateFactoriesShortCircuitAndReinsertAppends() {
        var map = new DenseTermTable<String>();
        for (int i = 0; i < 400; i++) {
            map.put(key(i, false), "v" + i);
        }
        assertEquals("v3", map.computeIfAbsent(key(3, false), k -> {
            fail("existing key invokes factory");
            return null;
        }));
        for (int i = 0; i < 250; i++) {
            map.remove(key(i, false));
        }
        for (int i = 0; i < 250; i++) {
            map.put(key(i, false), "v" + i);
        }
        assertEquals("v250", map.values().iterator().next());
        int[] calls = { 0 };
        assertTrue(map.visit(v -> {
            calls[0]++;
            return true;
        }));
        assertEquals(1, calls[0]);
        assertEquals(400, map.sum(v -> 1));
        assertNull(map.computeIfAbsent(key(500, false), k -> null));
        assertEquals(400, map.size());
    }

    @Test
    void bulkDeletionAndReinsertionPreserveCanonicalKeysAndSnapshots() {
        var map = new DenseTermTable<String>();
        var keys = new TermKey[1024];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = key(i, false);
            map.put(keys[i], "v" + i);
        }
        var copy = new DenseTermTable<>(map);
        for (int i = 0; i < 768; i++) {
            assertEquals("v" + i, map.remove(keys[i]));
            assertNull(map.get(keys[i]));
        }
        var survivors = map.keySet().iterator();
        for (int i = 768; i < 1024; i++) {
            assertSame(keys[i], survivors.next());
        }
        assertFalse(survivors.hasNext());
        for (int i = 0; i < 768; i++) {
            map.put(keys[i], "v" + i);
        }
        var ordered = map.keySet().iterator();
        for (int i = 768; i < 1024; i++) {
            assertSame(keys[i], ordered.next());
        }
        for (int i = 0; i < 768; i++) {
            assertSame(keys[i], ordered.next());
        }
        assertFalse(ordered.hasNext());
        var snapshot = copy.keySet().iterator();
        for (var original : keys) {
            assertSame(original, snapshot.next());
        }
        assertFalse(snapshot.hasNext());
    }
}
