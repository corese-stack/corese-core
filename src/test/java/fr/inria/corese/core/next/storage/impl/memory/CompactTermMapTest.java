package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompactTermMapTest {
    private static TermKey key(int number) {
        StringBuilder text = new StringBuilder("urn:");
        for (int i = 0; i < 5; i++) text.append((number & (1 << i)) == 0 ? "Aa" : "BB");
        return new TermKey(new SimpleIRI(text.toString()));
    }

    private static List<String> entries(CompactTermMap<String> table) {
        List<String> result = new ArrayList<>();
        table.forEach((key, value) -> result.add(key.value().stringValue() + "=" + value));
        return result;
    }

    @Test void collisionsAndRandomChangesMatchOrderedMapOracle() {
        assertEquals(key(0).hashCode(), key(31).hashCode());
        var table = new CompactTermMap<String>();
        var oracle = new LinkedHashMap<TermKey, String>();
        Random random = new Random(614615);
        for (int operation = 0; operation < 3_000; operation++) {
            TermKey key = key(random.nextInt(32));
            if (random.nextBoolean()) {
                String value = "value" + operation;
                assertEquals(oracle.putIfAbsent(key, value) == null, table.add(key, value));
            } else assertEquals(oracle.remove(key) != null, table.remove(key));
            assertEquals(oracle.size(), table.size());
            for (int i = 0; i < 32; i++) assertEquals(oracle.get(key(i)), table.get(key(i)));
            assertEquals(new ArrayList<>(oracle.values()), entries(table).stream().map(s -> s.substring(s.indexOf('=') + 1)).toList());
            assertEquals(oracle.size(), table.sum(value -> 1));
        }
    }

    @Test void copiesRemainIndependentThroughPromotionDemotionAndEmptying() {
        var table = new CompactTermMap<String>();
        for (int i = 0; i < 24; i++) {
            table.add(key(i), "v" + i);
            var snapshot = new CompactTermMap<>(table);
            List<String> expected = entries(snapshot);
            table.add(key(31), "extra");
            assertEquals(expected, entries(snapshot));
            table.remove(key(31));
        }
        var full = new CompactTermMap<>(table);
        for (int i = 0; i < 24; i++) {
            assertTrue(table.remove(key(i)));
            var snapshot = new CompactTermMap<>(table);
            List<String> expected = entries(snapshot);
            table.add(key(31), "extra");
            assertEquals(expected, entries(snapshot));
            table.remove(key(31));
        }
        assertTrue(table.isEmpty());
        assertEquals(24, full.size());
    }

    @Test void shortCircuitVisitsOnlyTheFirstMatchingValue() {
        var table = new CompactTermMap<String>();
        for (int i = 0; i < 24; i++) {
            table.add(key(i), "v" + i);
            AtomicInteger calls = new AtomicInteger();
            assertTrue(table.visitMatching(null, value -> { calls.incrementAndGet(); return true; }));
            assertEquals(1, calls.get());
            assertTrue(table.visitMatching(key(i), value -> value.equals("v" + (table.size() - 1))));
        }
    }
}
