package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.SimpleBNode;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.data.api.term.SimpleTriple;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class IngestionAllocationTest {
    private static final StorageConfig CONFIG = StorageConfig.memory();

    @Test void rawLookupMatchesKeyLookupForKindsSplitsLexicalFormsAndCollisions() {
        var table = new DenseTermTable<Integer>();
        var datatype = new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer");
        List<Value> values = new ArrayList<>(List.of(new SimpleIRI("urn:same"), new SimpleBNode("urn:same"),
                new SimpleLiteral("01", datatype), new SimpleLiteral("1", datatype),
                new SimpleLiteral("word", "en"), new SimpleLiteral("word", "fr"),
                new SimpleIRI("http://example/a", "bc"),
                new SimpleTriple(new SimpleIRI("urn:s"), new SimpleIRI("urn:p"), new SimpleLiteral("x"))));
        for (int i = 0; i < 512; i++) values.add(DenseTermTableTest.key(i, true).value());
        for (int i = 0; i < values.size(); i++) table.put(new TermKey(values.get(i)), i);
        for (Value value : values) assertEquals(table.get(new TermKey(value)), table.get(value, TermKey.structuralHash(value)));
        assertEquals(6, table.get(new SimpleIRI("http://example/ab", "c"), TermKey.structuralHash(values.get(6))));
        var miss = new SimpleIRI("urn:absent");
        assertNull(table.get(miss, TermKey.structuralHash(miss)));
        assertNull(table.get(values.getFirst(), TermKey.structuralHash(values.getFirst()) ^ 1));
        for (int i = 0; i < 100; i++) table.remove(new TermKey(values.get(i)));
        for (Value value : values) assertEquals(table.get(new TermKey(value)), table.get(value, TermKey.structuralHash(value)));
    }

    @Test void stagingProbeNeverBecomesAKeyAndCopiesKeepCanonicalIdentity() {
        var terms = new StagingTerms(4);
        var value = new SimpleIRI("http://example/a", "bc");
        var key = new TermKey(value);
        terms.put(key, key);
        assertSame(key, terms.intern(new SimpleIRI("http://example/ab", "c")));
        assertNull(terms.find(new SimpleBNode("http://example/abc"), key.hashCode()));
        var copy = new StagingTerms(terms);
        for (int i = 0; i < 512; i++) {
            TermKey collision = DenseTermTableTest.key(i, true);
            terms.put(collision, collision);
            assertSame(collision, terms.intern(collision.value()));
        }
        assertEquals(513, terms.size()); assertEquals(1, copy.size());
        assertNull(copy.find(DenseTermTableTest.key(100, true).value(), DenseTermTableTest.key(100, true).hashCode()));
        var added = new SimpleIRI("urn:copy-only");
        copy.intern(added);
        assertNull(terms.find(added, TermKey.structuralHash(added)));
        assertSame(key, terms.find(value, key.hashCode()));
        terms.forEach((stored, canonical) -> { assertEquals(TermKey.class, stored.getClass()); assertSame(stored, canonical); });
    }

    @Test void sizedStreamingCollectionCreatesMultipleGraphsWithSnapshotIsolation() {
        List<Statement> input = new ArrayList<>();
        var predicate = new SimpleIRI("urn:p");
        for (int i = 0; i < 1024; i++) input.add(Values.factory().createStatement(new SimpleIRI("urn:s" + i),
                predicate, new SimpleIRI("urn:o" + i), new SimpleIRI("urn:g" + i % 128)));
        var iterations = new AtomicInteger();
        var sizes = new AtomicInteger();
        var source = new AbstractCollection<Statement>() {
            @Override public int size() { sizes.incrementAndGet(); return input.size(); }
            @Override public Iterator<Statement> iterator() {
                assertEquals(1, iterations.incrementAndGet()); return input.iterator();
            }
        };
        try (var root = Storages.create(CONFIG); var reader = root.openSession();
             var snapshot = reader.transactions().beginTransaction()) {
            assertEquals(1024, root.mutations().addAll(source));
            assertEquals(128, root.metadata().getContexts().size());
            assertEquals(0, reader.queries().count(StatementPattern.matchAll()));
            assertEquals(1024, root.queries().count(StatementPattern.matchAll()));
            MemoryStorageAssertions.verify((IndexedStatementStore) root.queries());
        }
        assertEquals(1, iterations.get()); assertEquals(1, sizes.get());
    }

    @Test void collectionWithUnavailableSizeIsConsumedOnceAndPublishedAtomically() {
        var iri = new SimpleIRI("urn:shared-role");
        List<Statement> input = new ArrayList<>();
        for (int i = 0; i < 1024; i++) input.add(Values.factory().createStatement(iri, iri, new SimpleIRI("urn:o" + i)));
        var iterations = new AtomicInteger();
        var source = new AbstractCollection<Statement>() {
            @Override public int size() { throw new UnsupportedOperationException("streaming collection"); }
            @Override public Iterator<Statement> iterator() {
                assertEquals(1, iterations.incrementAndGet()); return input.iterator();
            }
        };
        try (var root = Storages.create(CONFIG); var reader = root.openSession()) {
            root.mutations().add(input.getFirst());
            try (var tx = reader.transactions().beginTransaction()) {
                assertEquals(1023, root.mutations().addAll(source));
                assertEquals(1, reader.queries().count(StatementPattern.matchAll()));
                assertEquals(1024, root.queries().count(StatementPattern.matchAll()));
                MemoryStorageAssertions.verify((IndexedStatementStore) root.queries());
                MemoryStorageAssertions.verify((IndexedStatementStore) reader.queries());
                try (var rows = root.queries().find(StatementPattern.matchAll())) { assertEquals(input, rows.toList()); }
            }
        }
        assertEquals(1, iterations.get());
    }

    @Test void failedStreamingBatchPreservesPriorWritesAndOtherSnapshots() {
        var s = new SimpleIRI("urn:s");
        var p = new SimpleIRI("urn:p");
        var old = Values.factory().createStatement(s, p, new SimpleIRI("urn:old"));
        var prior = Values.factory().createStatement(s, p, new SimpleIRI("urn:prior"));
        try (var root = Storages.create(CONFIG); var writer = root.openSession(); var reader = root.openSession()) {
            root.mutations().add(old);
            try (var snapshot = reader.transactions().beginTransaction(); var tx = writer.transactions().beginTransaction()) {
                writer.mutations().add(prior);
                var iterations = new AtomicInteger();
                Iterable<Statement> failing = () -> {
                    assertEquals(1, iterations.incrementAndGet());
                    return new Iterator<>() {
                        int index;
                        public boolean hasNext() {
                            if (index == 256) {
                                throw new IllegalArgumentException("input failure");
                            }
                            return true;
                        }
                        public Statement next() { return Values.factory().createStatement(s, p, new SimpleIRI("urn:new" + index++)); }
                    };
                };
                var failure = assertThrows(StorageException.class, () -> writer.mutations().addAll(failing));
                assertInstanceOf(IllegalArgumentException.class, failure.getCause());
                try (var rows = writer.queries().find(StatementPattern.matchAll())) { assertEquals(List.of(old, prior), rows.toList()); }
                assertEquals(1, root.queries().count(StatementPattern.matchAll()));
                assertEquals(1, reader.queries().count(StatementPattern.matchAll()));
                MemoryStorageAssertions.verify((IndexedStatementStore) writer.queries());
                tx.commit();
                assertEquals(2, root.queries().count(StatementPattern.matchAll()));
                assertEquals(1, reader.queries().count(StatementPattern.matchAll()));
                assertEquals(1, iterations.get());
            }
        }
    }
}
