package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleBNode;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.data.api.term.SimpleTriple;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MemoryStorageTest {
    private static final IRI P = new SimpleIRI("urn:p");
    private static final Resource S = new SimpleIRI("urn:s");
    private static final Value O = new SimpleIRI("urn:o");
    private static final Resource G = new SimpleIRI("urn:g");
    private static final StorageConfig CONFIG = StorageConfig.memory();

    private static Statement statement(int i, Resource graph) {
        return Values.factory().createStatement(new SimpleIRI("urn:s" + i), P, new SimpleIRI("urn:o" + i), graph);
    }
    private static IndexedStatementStore store() {
        IndexedStatementStore store = new IndexedStatementStore();
        store.lifecycle().initialize(CONFIG);
        return store;
    }

    @Test void densePrefixesSurviveRemovalRollbackAndReinsertion() {
        var store = store();
        List<Statement> statements = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            String label = switch (i) {
                case 0 -> "Aa";
                case 1 -> "BB";
                default -> "value" + i;
            };
            statements.add(Values.factory().createStatement(S, P, new SimpleLiteral(label)));
        }
        assertEquals(40, store.addAll(statements));
        StatementPattern prefix = StatementPattern.of(S, P, null, (Resource) null, null);
        assertEquals(40, store.count(prefix));
        try (var transaction = store.beginTransaction()) {
            for (int i = 0; i < 39; i++) assertTrue(store.remove(statements.get(i)));
            assertEquals(1, store.count(prefix));
            assertEquals(List.of(statements.getLast()), store.find(prefix).toList());
            transaction.rollback();
        }
        assertEquals(40, store.count(prefix));
        for (int i = 0; i < 39; i++) assertTrue(store.remove(statements.get(i)));
        assertEquals(1, store.count(prefix));
        assertTrue(store.add(statements.getFirst()));
        assertFalse(store.add(statements.getFirst()));
        assertEquals(2, store.count(prefix));
        assertTrue(store.remove(statements.getLast()));
        assertTrue(store.remove(statements.getFirst()));
        assertEquals(0, store.count(prefix));
        assertTrue(store.add(statements.get(1)));
        assertEquals(1, store.count(prefix));
        store.closeSession();
    }

    @Test void sharedKeysSurviveCrossRoleAliasesChurnAndBulkMerges() {
        var store = store();
        List<Statement> triples = List.of(
                Values.factory().createStatement(S, (IRI) S, S),
                Values.factory().createStatement(P, (IRI) S, O),
                Values.factory().createStatement((Resource) O, P, S));
        for (Statement triple : triples) { assertTrue(store.add(triple)); MemoryStorageAssertions.verify(store); }
        try (var snapshot = store.beginTransaction()) {
            for (Statement triple : triples) { assertTrue(store.remove(triple)); MemoryStorageAssertions.verify(store); }
            assertEquals(0, store.count(StatementPattern.matchAll()));
            snapshot.rollback();
        }
        MemoryStorageAssertions.verify(store);
        assertEquals(3, store.count(StatementPattern.matchAll()));
        for (Statement triple : triples) { assertTrue(store.remove(triple)); MemoryStorageAssertions.verify(store); }
        for (int iteration = 0; iteration < 20; iteration++) {
            Statement singleton = statement(iteration, null);
            assertEquals(1, store.addAll(List.of(singleton)));
            MemoryStorageAssertions.verify(store);
            assertEquals(3, store.addAll(triples));
            MemoryStorageAssertions.verify(store);
            assertTrue(store.remove(singleton));
            MemoryStorageAssertions.verify(store);
            for (Statement triple : triples) { assertTrue(store.remove(triple)); MemoryStorageAssertions.verify(store); }
        }
        store.closeSession();
    }

    @Test void pluginIsExplicitAndDefaultRemainsMemory() {
        try (var indexed = Storages.create(CONFIG); var memory = Storages.create()) {
            assertInstanceOf(MemoryStorageManager.class, indexed);
            assertInstanceOf(fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager.class, memory);
            assertTrue(indexed.isOpen());
            assertEquals(Set.of(IsolationLevel.SERIALIZABLE), indexed.transactions().getSupportedIsolationLevels());
        }
    }

    @Test void nativeValuesAreSharedForUnitAndBulkWithoutMergingLexicalVariants() {
        for (boolean bulk : List.of(false, true)) {
            try (var storage = Storages.create(CONFIG)) {
                var rows = List.of(
                        Values.factory().createStatement(new SimpleIRI("urn:shared"), new SimpleIRI("urn:p1"), new SimpleLiteral("01")),
                        Values.factory().createStatement(new SimpleIRI("urn:shared"), new SimpleIRI("urn:p2"), new SimpleLiteral("01")),
                        Values.factory().createStatement(new SimpleIRI("urn:shared"), new SimpleIRI("urn:p3"), new SimpleLiteral("1")));
                if (bulk) assertEquals(3, storage.mutations().addAll(rows));
                else rows.forEach(row -> assertTrue(storage.mutations().add(row)));
                List<Statement> actual = storage.queries().find(StatementPattern.matchAll()).toList();
                assertEquals(new HashSet<>(rows), new HashSet<>(actual));
                assertSame(actual.get(0).getSubject(), actual.get(1).getSubject());
                assertSame(actual.get(0).getObject(), actual.get(1).getObject());
                assertNotSame(actual.get(0).getObject(), actual.get(2).getObject());
                MemoryStorageAssertions.verify((IndexedStatementStore) storage.queries());
            }
        }
    }

    @Test void copyOnWritePreservesMultipleReadersAcrossWritesBatchesAndGraphDrops() {
        try (var root = Storages.create(CONFIG); var a = root.openSession(); var b = root.openSession()) {
            List<Statement> original = List.of(statement(1, null), statement(2, G));
            root.mutations().addAll(original);
            try (var txA = a.transactions().beginTransaction(); var txB = b.transactions().beginTransaction()) {
                assertTrue(root.mutations().remove(original.getFirst()));
                root.mutations().addAll(List.of(statement(3, null), statement(4, G)));
                assertTrue(root.mutations().dropGraph(G));
                assertEquals(new HashSet<>(original), new HashSet<>(a.queries().find(StatementPattern.matchAll()).toList()));
                assertEquals(new HashSet<>(original), new HashSet<>(b.queries().find(StatementPattern.matchAll()).toList()));
                assertTrue(a.mutations().remove(original.getLast()));
                assertEquals(List.of(original.getFirst()), a.queries().find(StatementPattern.matchAll()).toList());
                assertEquals(2, b.queries().count(StatementPattern.matchAll()));
                assertThrows(StorageException.class, txA::commit);
                txA.rollback();
                txB.rollback();
            }
            try (var tx = a.transactions().beginTransaction()) {
                assertEquals(1, a.mutations().addAll(List.of(statement(5, G))));
                tx.commit();
            }
            assertEquals(2, root.queries().count(StatementPattern.matchAll()));
            for (var session : List.of(root, a, b)) MemoryStorageAssertions.verify((IndexedStatementStore) session.queries());
        }
    }

    @Test void graphContextsAreSharedAcrossUnitsBatchesAndReaderSnapshots() {
        for (boolean bulk : List.of(false, true)) {
            for (boolean blank : List.of(false, true)) {
                testContextSharingScenario(bulk, blank);
            }
        }
    }

    private void testContextSharingScenario(boolean bulk, boolean blank) {
        try (var root = Storages.create(CONFIG); var reader = root.openSession()) {
            Resource canonical = blank ? new SimpleBNode("context") : new SimpleIRI("urn:context");
            assertTrue(root.mutations().createGraph(canonical));
            for (int round = 0; round < 3; round++) {
                List<Statement> rows = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    rows.add(statement(round * 12 + i, blank ? new SimpleBNode("context") : new SimpleIRI("urn:context")));
                }
                if (bulk) {
                    assertEquals(12, root.mutations().addAll(rows));
                } else {
                    rows.forEach(row -> assertTrue(root.mutations().add(row)));
                }
            }
            List<Statement> original = root.queries().find(StatementPattern.matchAll()).toList();
            original.forEach(row -> assertSame(canonical, row.getContext()));
            try (var tx = reader.transactions().beginTransaction()) {
                assertEquals(36, root.mutations().clear(canonical));
                assertEquals(1, root.mutations().addAll(List.of(statement(100,
                        blank ? new SimpleBNode("context") : new SimpleIRI("urn:context")))));
                assertSame(canonical, root.queries().find(StatementPattern.matchAll()).findFirst().orElseThrow().getContext());
                assertTrue(root.mutations().dropGraph(canonical));
                assertEquals(original, reader.queries().find(StatementPattern.matchAll()).toList());
                tx.rollback();
            }
            MemoryStorageAssertions.verify((IndexedStatementStore) root.queries());
        }
    }

    @Test void contextSharingPreservesDifferentIRIViewsForTheSameGraph() {
        for (boolean bulk : List.of(false, true)) {
            try (var root = Storages.create(CONFIG)) {
                IRI first = new SimpleIRI("urn:", "context:graph");
                IRI second = new SimpleIRI("urn:context:", "graph");
                List<Statement> input = List.of(statement(1, first), statement(2, second));
                if (bulk) root.mutations().addAll(input);
                else input.forEach(root.mutations()::add);
                List<Statement> actual = root.queries().find(StatementPattern.matchAll()).toList();
                assertEquals(1, root.metadata().getContexts().size());
                assertEquals(input, actual);
                assertEquals("urn:", ((IRI) actual.get(0).getContext()).getNamespace());
                assertEquals("urn:context:", ((IRI) actual.get(1).getContext()).getNamespace());
            }
        }
    }

    @Test void inlineLeafTransitionsPreserveLexicalIdentityAndCopiedPermutations() {
        for (TripleIndex.Order order : TripleIndex.Order.values()) {
            TripleIndex index = new TripleIndex(order);
            List<Statement> rows = new ArrayList<>();
            for (String suffix : List.of("Aa", "BB", "01", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
                rows.add(switch (order) {
                    case SPO -> Values.factory().createStatement(S, P, new SimpleLiteral(suffix));
                    case POS -> Values.factory().createStatement(new SimpleIRI("urn:" + suffix), P, O);
                    case OSP -> Values.factory().createStatement(S, new SimpleIRI("urn:" + suffix), O);
                });
            }
            for (Statement row : rows) { assertTrue(MemoryStorageAssertions.add(index, row)); assertFalse(MemoryStorageAssertions.add(index, row)); }
            TripleIndex copy = index.copy();
            for (int i = 0; i < rows.size() - 1; i++) assertTrue(MemoryStorageAssertions.remove(index, rows.get(i)));
            assertEquals(1, index.size());
            assertTrue(MemoryStorageAssertions.add(index, rows.getFirst()));
            assertEquals(2, index.size());
            assertEquals(rows.size(), copy.size());
            for (Statement row : rows) {
                StatementPattern pattern = StatementPattern.of(row.getSubject(), row.getPredicate(), row.getObject());
                assertEquals(1, copy.count(pattern));
                assertEquals(row == rows.getLast() || row == rows.getFirst() ? 1 : 0, index.count(pattern));
            }
            assertTrue(MemoryStorageAssertions.remove(index, rows.getFirst()));
            assertTrue(MemoryStorageAssertions.remove(index, rows.getLast()));
            assertEquals(0, index.size());
            assertTrue(MemoryStorageAssertions.add(index, rows.get(1)));
        }
    }

    @Test void poolingPreservesIRIViewsAndExplicitLiteralMetadata() {
        for (boolean bulk : List.of(false, true)) {
            try (var storage = Storages.create(CONFIG)) {
                IRI first = new SimpleIRI("urn:", "view:item");
                IRI second = new SimpleIRI("urn:view:", "item");
                assertEquals(first.stringValue(), second.stringValue());
                IRI datatype = new SimpleIRI("urn:custom-datatype");
                SimpleLiteral text = new SimpleLiteral("10", datatype,
                        fr.inria.corese.core.next.data.api.literal.XSDDatatype.STRING);
                SimpleLiteral number = new SimpleLiteral("10", datatype,
                        fr.inria.corese.core.next.data.api.literal.XSDDatatype.INTEGER);
                SimpleLiteral firstDatatypeView = new SimpleLiteral("20", new SimpleIRI("urn:", "datatype:x"));
                SimpleLiteral secondDatatypeView = new SimpleLiteral("20", new SimpleIRI("urn:datatype:", "x"));
                List<Statement> input = List.of(
                        Values.factory().createStatement(first, new SimpleIRI("urn:p1"), text),
                        Values.factory().createStatement(second, new SimpleIRI("urn:p2"), number),
                        Values.factory().createStatement(first, new SimpleIRI("urn:p3"), firstDatatypeView),
                        Values.factory().createStatement(first, new SimpleIRI("urn:p4"), secondDatatypeView));
                if (bulk) assertEquals(4, storage.mutations().addAll(input));
                else input.forEach(row -> assertTrue(storage.mutations().add(row)));
                List<Statement> actual = storage.queries().find(StatementPattern.matchAll()).toList();
                assertEquals(input, actual);
                assertEquals(first.getNamespace(), ((IRI) actual.get(0).getSubject()).getNamespace());
                assertEquals(second.getNamespace(), ((IRI) actual.get(1).getSubject()).getNamespace());
                assertSame(text.getCoreDatatype(), ((SimpleLiteral) actual.get(0).getObject()).getCoreDatatype());
                assertSame(number.getCoreDatatype(), ((SimpleLiteral) actual.get(1).getObject()).getCoreDatatype());
                assertEquals(firstDatatypeView.getDatatype().getNamespace(), ((SimpleLiteral) actual.get(2).getObject()).getDatatype().getNamespace());
                assertEquals(secondDatatypeView.getDatatype().getNamespace(), ((SimpleLiteral) actual.get(3).getObject()).getDatatype().getNamespace());
                MemoryStorageAssertions.verify((IndexedStatementStore) storage.queries());
            }
        }
    }

    @SuppressWarnings("java:S2245")
    @Test void allMasksAndContextSelectionsMatchIndependentQuadOracleAfterRandomMutations() {
        var store = store();
        Random random = new Random(614);
        Set<Statement> oracle = new HashSet<>();
        Set<Resource> registeredGraphs = new java.util.LinkedHashSet<>();
        List<Resource> contexts = Arrays.asList(null, G, new SimpleBNode("g"));
        List<Statement> pool = new ArrayList<>();
        for (Resource context : contexts) {
            for (int i = 0; i < 12; i++) {
                pool.add(Values.factory().createStatement(new SimpleIRI("urn:s" + i % 3),
                        new SimpleIRI("urn:p" + i % 2), new SimpleLiteral("v" + i % 4), context));
            }
        }
        for (int operation = 0; operation < 120; operation++) {
            Statement s = applyRandomMutation(store, oracle, pool, random);
            MemoryStorageAssertions.verify(store);
            oracle.stream().map(Statement::getContext).filter(java.util.Objects::nonNull).forEach(registeredGraphs::add);
            assertEquals(registeredGraphs, store.getContexts());
            Set<Resource> subjects = oracle.stream().map(Statement::getSubject).collect(Collectors.toSet());
            Set<IRI> predicates = oracle.stream().map(Statement::getPredicate).collect(Collectors.toSet());
            Set<Value> objects = oracle.stream().map(Statement::getObject).collect(Collectors.toSet());
            assertEquals(subjects, store.getSubjects());
            assertEquals(predicates, store.getPredicates());
            assertEquals(objects, store.getObjects());
            assertEquals(new fr.inria.corese.core.next.storage.api.model.StorageStatistics(
                    oracle.size(), subjects.size(), predicates.size(), objects.size(), registeredGraphs.size()),
                    store.getStatistics());
            verifyMasksAndContexts(store, oracle, s);
        }
    }

    private static Statement applyRandomMutation(IndexedStatementStore store, Set<Statement> oracle,
            List<Statement> pool, Random random) {
        Statement s = pool.get(random.nextInt(pool.size()));
        switch (random.nextInt(4)) {
            case 0 -> assertEquals(oracle.add(s), store.add(s));
            case 1 -> assertEquals(oracle.remove(s), store.remove(s));
            case 2 -> {
                List<Statement> batch = List.of(s, pool.get(random.nextInt(pool.size())), s);
                long added = batch.stream().filter(oracle::add).count();
                assertEquals(added, store.addAll(batch));
            }
            default -> {
                StatementPattern pattern = StatementPattern.of(s.getSubject(), null, null, s.getContext());
                long removed = oracle.stream().filter(t -> matches(t, pattern)).count();
                oracle.removeIf(t -> matches(t, pattern));
                assertEquals(removed, store.remove(pattern));
            }
        }
        return s;
    }

    private static void verifyMasksAndContexts(IndexedStatementStore store, Set<Statement> oracle, Statement s) {
        for (int mask = 0; mask < 8; mask++) {
            for (Resource[] selected : List.of(new Resource[0], new Resource[]{null}, new Resource[]{G},
                    new Resource[]{null, G, G}, new Resource[]{new SimpleIRI("urn:absent")})) {
                StatementPattern p = StatementPattern.of((mask & 1) != 0 ? s.getSubject() : null,
                        (mask & 2) != 0 ? s.getPredicate() : null, (mask & 4) != 0 ? s.getObject() : null, selected);
                Set<Statement> expected = oracle.stream().filter(t -> matches(t, p)).collect(Collectors.toSet());
                try (var found = store.find(p)) { assertEquals(expected, found.collect(Collectors.toSet())); }
                assertEquals(expected.size(), store.count(p));
                assertEquals(!expected.isEmpty(), store.contains(p));
            }
        }
    }
    private static boolean matches(Statement s, StatementPattern p) {
        return (p.getSubject() == null || p.getSubject().equals(s.getSubject()))
                && (p.getPredicate() == null || p.getPredicate().equals(s.getPredicate()))
                && (p.getObject() == null || p.getObject().equals(s.getObject()))
                && (p.isContextWildcard() || Arrays.asList(p.getContexts()).contains(s.getContext()));
    }

    @Test void everyPermutationVisitsOnlyItsMatchingLeaves() {
        for (TripleIndex.Order order : TripleIndex.Order.values()) {
            TripleIndex index = new TripleIndex(order);
            for (int i = 0; i < 1000; i++) MemoryStorageAssertions.add(index, statement(i, null));
            Statement s = statement(42, null);
            StatementPattern p = switch (order) {
                case SPO -> StatementPattern.of(s.getSubject(), P, null);
                case POS -> StatementPattern.of(null, P, s.getObject());
                case OSP -> StatementPattern.of(s.getSubject(), null, s.getObject());
            };
            AtomicInteger visits = new AtomicInteger();
            index.visit(p, candidate -> { visits.incrementAndGet(); assertEquals(s, candidate); return false; });
            assertEquals(1, visits.get());
        }
    }

    @Test void multiGraphSearchesKeepRdfIdentityAndSnapshotsAcrossBoundRemoval() {
        IRI integer = new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer");
        List<Statement> input = new ArrayList<>();
        List<Resource> graphs = new ArrayList<>();
        graphs.add(null);
        for (int i = 0; i < 128; i++) graphs.add(new SimpleIRI("urn:search:graph:" + i));
        Resource left = new SimpleIRI("urn:", "Aa");
        Resource right = new SimpleIRI("urn:BB");
        // Colliding subject hashes, lexical numeric variants and nested triple objects.
        for (Resource graph : graphs) {
            input.add(Values.factory().createStatement(left, P, new SimpleLiteral("01", integer), graph));
            input.add(Values.factory().createStatement(left, P, new SimpleLiteral("1", integer), graph));
            input.add(Values.factory().createStatement(right, P, new SimpleLiteral("01", integer), graph));
            input.add(Values.factory().createStatement(new SimpleBNode("Aa"), new SimpleIRI("urn:alt"),
                    new SimpleTriple(left, P, right), graph));
        }
        try (var root = Storages.create(CONFIG); var reader = root.openSession()) {
            root.mutations().addAll(input);
            StatementPattern remove = StatementPattern.of(new SimpleIRI("urn:Aa"), P, new SimpleLiteral("01", integer));
            try (var transaction = reader.transactions().beginTransaction()) {
                assertEquals(graphs.size(), root.mutations().remove(remove));
                for (var session : List.of(root, reader)) {
                    List<Statement> expectedRows = session == root
                            ? input.stream().filter(s -> !matches(s, remove)).toList() : input;
                    verifySessionPatternSearches(session, expectedRows, input.subList(0, 4), graphs);
                    MemoryStorageAssertions.verify((IndexedStatementStore) session.queries());
                }
                transaction.rollback();
            }
            assertEquals(0, reader.queries().count(remove));
            assertEquals(graphs.size(), root.mutations().addAll(input));
            assertEquals(graphs.size(), reader.queries().count(remove));
        }
    }

    private static void verifySessionPatternSearches(StorageManager session, List<Statement> expectedRows,
            List<Statement> sampleTargets, List<Resource> graphs) {
        for (Statement target : sampleTargets) {
            for (int mask = 0; mask < 8; mask++) {
                verifyMaskAcrossContexts(session, expectedRows, target, mask, graphs);
            }
        }
    }

    private static void verifyMaskAcrossContexts(StorageManager session, List<Statement> expectedRows,
            Statement target, int mask, List<Resource> graphs) {
        Resource s = (mask & 1) != 0 ? target.getSubject() : null;
        IRI p = (mask & 2) != 0 ? target.getPredicate() : null;
        Value o = (mask & 4) != 0 ? target.getObject() : null;
        for (Resource[] contexts : List.of(new Resource[0], new Resource[]{null},
                new Resource[]{graphs.getLast(), graphs.get(1), null, graphs.get(1)},
                new Resource[]{new SimpleIRI("urn:missing-graph")})) {
            StatementPattern pattern = StatementPattern.of(s, p, o, contexts);
            Set<Statement> expected = expectedRows.stream().filter(row -> matches(row, pattern))
                    .collect(Collectors.toSet());
            assertEquals(expected.size(), session.queries().count(pattern));
            assertEquals(!expected.isEmpty(), session.queries().contains(pattern));
            try (var found = session.queries().find(pattern)) {
                assertEquals(expected, new HashSet<>(found.toList()));
            }
        }
    }

    @Test void graphNamesSurviveClearAndLastRemovalButNotDrop() {
        var store = store();
        assertTrue(store.createGraph(G));
        assertFalse(store.createGraph(G));
        assertEquals(Set.of(G), store.getContexts());
        store.add(statement(1, G));
        store.add(statement(1, null));
        assertEquals(1, store.clear((Resource) null));
        assertEquals(1, store.clear(G));
        assertEquals(Set.of(G), store.getContexts());
        store.add(statement(1, G));
        assertTrue(store.remove(statement(1, G)));
        assertFalse(store.remove(statement(1, G)));
        assertEquals(Set.of(G), store.getContexts());
        assertTrue(store.dropGraph(G));
        assertFalse(store.dropGraph(G));
        assertEquals(0, store.getStatistics().contextCount());
        MemoryStorageAssertions.verify(store);
        assertThrows(UnsupportedOperationException.class, () -> store.getContexts().add(G));
    }

    @Test void termIdentityAndNestedTriplesArePreserved() {
        var store = store();
        IRI integer = new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer");
        List<Value> objects = List.of(new SimpleLiteral("01", integer), new SimpleLiteral("1", integer),
                new SimpleIRI("urn:same"), new SimpleBNode("urn:same"),
                new SimpleLiteral("word", "en"), new SimpleLiteral("word", "fr"), new SimpleTriple(S, P, O));
        for (Value o : objects) assertTrue(store.add(Values.factory().createStatement(S, P, o)));
        assertFalse(store.add(Values.factory().createStatement(S, P, new SimpleLiteral("word", "EN"))));
        assertEquals(objects.size(), store.count(StatementPattern.matchAll()));
        for (Value o : objects) assertEquals(1, store.count(StatementPattern.of(S, P, o)));
        MemoryStorageAssertions.verify(store);
    }

    @Test void streamsSnapshotMatchesAndContextFilterOrderDoesNotAffectTraversal() {
        var store = store();
        store.add(statement(1, G)); store.add(statement(2, null));
        List<Statement> first;
        try (var stream = store.find(StatementPattern.of(null, null, null, G, null))) { first = stream.toList(); }
        try (var stream = store.find(StatementPattern.of(null, null, null, null, G))) { assertEquals(first, stream.toList()); }
        try (var stream = store.find(StatementPattern.matchAll())) {
            store.clear();
            assertEquals(first, stream.toList());
        }
    }

    @Test void batchIsSingleUseAndCountsOnlyNewQuads() {
        var store = store();
        store.add(statement(1, G));
        AtomicInteger iterators = new AtomicInteger();
        Iterable<Statement> once = () -> {
            assertEquals(1, iterators.incrementAndGet());
            return List.of(statement(1, G), statement(2, G), statement(2, G), statement(2, null)).iterator();
        };
        assertEquals(2, store.addAll(once));
        assertEquals(3, store.count(StatementPattern.matchAll()));
        assertEquals(0, store.addAll(List.of()));
        MemoryStorageAssertions.verify(store);
    }

    @Test void sourceFailuresNeverPublishAPrefixOrNewGraph() {
        var store = store();
        for (String phase : List.of("iterator", "hasNext", "next", "null")) {
            Iterable<Statement> bad = createFailingIterable(phase);
            Class<? extends RuntimeException> expected = "null".equals(phase) ? NullPointerException.class : StorageException.class;
            assertThrows(expected, () -> store.addAll(bad));
            assertEquals(0, store.count(StatementPattern.matchAll()));
            assertTrue(store.getContexts().isEmpty());
            MemoryStorageAssertions.verify(store);
        }
        assertTrue(store.add(statement(2, null)));
    }

    private static Iterable<Statement> createFailingIterable(String phase) {
        if ("iterator".equals(phase)) {
            return () -> { throw new IllegalArgumentException("source failure"); };
        }
        return () -> new Iterator<>() {
            int position;
            @Override public boolean hasNext() {
                if (position == 1 && "hasNext".equals(phase)) {
                    throw new IllegalArgumentException("source failure");
                }
                return position < 2;
            }
            @Override public Statement next() {
                if (position++ == 0) return statement(1, G);
                if ("null".equals(phase)) return null;
                throw new IllegalArgumentException("source failure");
            }
        };
    }

    @Test void sessionsOnSameThreadHaveIndependentSnapshotsAndGlobalConflicts() {
        try (StorageManager root = Storages.create(CONFIG)) {
            StorageManager a = root.openSession();
            StorageManager b = root.openSession();
            try (var txA = a.transactions().beginTransaction(); var txB = b.transactions().beginTransaction()) {
                a.mutations().add(statement(1, G));
                b.mutations().createGraph(new SimpleIRI("urn:empty"));
                assertEquals(0, b.queries().count(StatementPattern.matchAll()));
                assertTrue(root.metadata().getContexts().isEmpty());
                txA.commit();
                assertEquals(0, b.queries().count(StatementPattern.matchAll()));
                assertThrows(StorageException.class, txB::commit);
                assertTrue(txB.isActive());
                txB.rollback();
            }
            assertEquals(Set.of(G), root.metadata().getContexts());
            assertEquals(1, b.queries().count(StatementPattern.matchAll()));
            a.close();
            assertTrue(root.isOpen());
            assertFalse(a.isOpen());
        }
    }

    @Test void otherThreadsCannotAccidentallyAutocommitIntoAnActiveSession() {
        try (var root = Storages.create(CONFIG); var session = root.openSession(); var tx = session.transactions().beginTransaction()) {
            CompletableFuture.runAsync(() -> {
                assertThrows(IllegalStateException.class, () -> session.mutations().add(statement(1, null)));
                assertThrows(IllegalStateException.class, () -> session.queries().count(StatementPattern.matchAll()));
                assertThrows(IllegalStateException.class, tx::commit);
            }).join();
            CompletableFuture.runAsync(() -> {
                try (var other = root.openSession()) { other.mutations().add(statement(2, G)); }
            }).join();
            assertEquals(0, session.queries().count(StatementPattern.matchAll()));
            assertThrows(StorageException.class, tx::commit);
        }
    }

    @Test void batchFailurePreservesEarlierTransactionWritesAndRollbackDiscardsGraphs() {
        try (var root = Storages.create(CONFIG); var session = root.openSession()) {
            try (var tx = session.transactions().beginTransaction()) {
                session.mutations().add(statement(1, null));
                assertThrows(NullPointerException.class, () -> session.mutations().addAll(Arrays.asList(statement(2, G), null)));
                assertEquals(1, session.queries().count(StatementPattern.matchAll()));
                assertTrue(session.metadata().getContexts().isEmpty());
                assertEquals(1, session.mutations().addAll(List.of(statement(2, G))));
                assertEquals(0, root.queries().count(StatementPattern.matchAll()));
                tx.rollback();
            }
            assertEquals(0, root.queries().count(StatementPattern.matchAll()));
            assertTrue(root.metadata().getContexts().isEmpty());
        }
    }

    @Test void batchSourceCanCommitAnotherSessionWithoutLosingItsChanges() {
        try (var root = Storages.create(CONFIG); var a = root.openSession(); var b = root.openSession()) {
            Iterable<Statement> source = () -> {
                assertThrows(IllegalStateException.class, () -> a.mutations().add(statement(9, null)));
                b.mutations().add(statement(1, G));
                return List.of(statement(2, null)).iterator();
            };
            assertEquals(1, a.mutations().addAll(source));
            assertEquals(2, root.queries().count(StatementPattern.matchAll()));
        }
    }

    @Test void publicQueriesUpdatesBindingsAndEmptyGraphsUseTheNewBackend() {
        try (var repository = Repositories.create(CONFIG); var a = repository.getConnection(); var b = repository.getConnection()) {
            a.add(List.of(Values.factory().createStatement(S, P, O),
                    Values.factory().createStatement((Resource) O, new SimpleIRI("urn:name"), new SimpleLiteral("Bob"))));
            assertTrue(a.prepareBooleanQuery("ASK { <urn:s> <urn:p> ?friend . ?friend <urn:name> \"Bob\" }").evaluate());
            assertFalse(a.prepareBooleanQuery("ASK { ?same <urn:p> ?same }").evaluate());
            a.begin();
            a.prepareUpdate("CREATE GRAPH <urn:empty> ; INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> <urn:o> } }").execute();
            assertTrue(a.prepareBooleanQuery("ASK { GRAPH <urn:empty> {} }").evaluate());
            assertFalse(b.prepareBooleanQuery("ASK { GRAPH <urn:g> { ?s ?p ?o } }").evaluate());
            a.commit();
            assertTrue(b.prepareBooleanQuery("ASK { GRAPH <urn:g> { ?s ?p ?o } }").evaluate());
            assertFalse(b.prepareBooleanQuery("ASK { <urn:o> <urn:p> ?o }").evaluate());
        }
    }
    @Test void numericAndTemporalValuesKeepTheirEvaluationCapabilities() {
        try (var repository = Repositories.create(CONFIG); var connection = repository.getConnection()) {
            connection.add(List.of(Values.factory().createStatement(S, P, Values.factory().createLiteral(30)),
                    Values.factory().createStatement(S, new SimpleIRI("urn:date"), Values.factory().createLiteral(
                            "2026-10-02T12:30:00Z", new SimpleIRI("http://www.w3.org/2001/XMLSchema#dateTime")))));
            assertTrue(connection.prepareBooleanQuery("ASK { ?s <urn:p> ?age FILTER(?age + 1 = 31) }").evaluate());
            assertTrue(connection.prepareBooleanQuery("ASK { ?s <urn:date> ?date FILTER(HOURS(?date) = 12) }").evaluate());
        }
    }

    @Test void foreignTermImplementationsAreCapturedAndSearchKeysAreNormalized() {
        var store = store();
        var external = new fr.inria.corese.core.next.data.spi.term.AbstractIRI("urn:s") {
            @Override public boolean equals(Object o) { return super.equals(o); }
            @Override public int hashCode() { return stringValue().hashCode(); }
        };
        store.add(Values.factory().createStatement(external, P, O));
        assertTrue(store.contains(StatementPattern.of(S, P, O)));
        assertTrue(store.contains(StatementPattern.of(external, P, O)));
        assertEquals(1, store.remove(StatementPattern.of(external, null, null)));
        MemoryStorageAssertions.verify(store);
    }

    @Test void collidingHashesStillDistinguishRdfTermsAndLiteralLexicalForms() {
        var store = store();
        List<Value> objects = List.of(new SimpleIRI("urn:Aa"), new SimpleIRI("urn:BB"),
                new SimpleBNode("urn:Aa"), new SimpleLiteral("Aa"), new SimpleLiteral("BB"),
                Values.factory().createLiteral("01", new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer")),
                Values.factory().createLiteral("1", new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer")));
        List<Statement> statements = objects.stream().map(v -> Values.factory().createStatement(S, P, v)).toList();
        assertEquals(statements.size(), store.addAll(statements));
        for (Value value : objects) assertEquals(1, store.count(StatementPattern.of(null, null, value)));
        try (var tx = store.beginTransaction()) {
            assertEquals(statements.size(), store.count(StatementPattern.matchAll()));
            assertTrue(store.remove(statements.getFirst()));
            tx.rollback();
        }
        assertEquals(statements.size(), store.count(StatementPattern.matchAll()));
        MemoryStorageAssertions.verify(store);
    }

    @Test void publicJoinPushesBindingsToTheIndexedSession() {
        try (var root = Storages.create(CONFIG)) {
            root.mutations().addAll(List.of(Values.factory().createStatement(S, P, O),
                    Values.factory().createStatement((Resource) O, new SimpleIRI("urn:name"), new SimpleLiteral("name"))));
            var session = org.mockito.Mockito.spy(root.openSession());
            var queries = org.mockito.Mockito.spy(session.queries());
            List<StatementPattern> patterns = new ArrayList<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                patterns.add(invocation.getArgument(0));
                return invocation.callRealMethod();
            }).when(queries).find(org.mockito.ArgumentMatchers.any());
            org.mockito.Mockito.doReturn(queries).when(session).queries();
            var owner = org.mockito.Mockito.spy(root);
            org.mockito.Mockito.doReturn(session).when(owner).openSession();
            try (var repository = Repositories.create(owner); var connection = repository.getConnection()) {
                assertTrue(connection.prepareBooleanQuery(
                        "ASK { VALUES ?s { <urn:s> } ?s <urn:p> ?friend . ?friend <urn:name> ?name }").evaluate());
            }
            assertTrue(patterns.stream().anyMatch(p -> S.equals(p.getSubject()) && P.equals(p.getPredicate())));
            // The optimizer may reverse the join: either subject or object carries its binding.
            assertTrue(patterns.stream().anyMatch(p ->
                    (O.equals(p.getSubject()) && new SimpleIRI("urn:name").equals(p.getPredicate()))
                            || (S.equals(p.getSubject()) && P.equals(p.getPredicate()) && O.equals(p.getObject()))));
        }
    }

    @Test void concurrentOwnerCloseIsIdempotentAndExistingStreamsRemainUsable() {
        var root = Storages.create(CONFIG);
        root.mutations().add(statement(1, null));
        try (var snapshot = root.queries().find(StatementPattern.matchAll())) {
            var gate = new java.util.concurrent.CyclicBarrier(2);
            Runnable close = () -> {
                try {
                    gate.await();
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
                root.close();
            };
            CompletableFuture.allOf(CompletableFuture.runAsync(close), CompletableFuture.runAsync(close)).join();
            assertFalse(root.isOpen());
            assertEquals(1, snapshot.count());
            assertThrows(IllegalStateException.class, () -> root.queries().count(StatementPattern.matchAll()));
            root.close();
        }
    }

    @Test void literalsCaptureForeignDatatypeIdentityEvenInsideBuiltinLiteral() {
        var datatype = new java.util.concurrent.atomic.AtomicReference<>("urn:original");
        var external = new fr.inria.corese.core.next.data.spi.term.AbstractIRI("urn:original") {
            @Override public String stringValue() { return datatype.get(); }
        };
        var store = store();
        store.add(Values.factory().createStatement(S, P, new SimpleLiteral("value", external)));
        datatype.set("urn:changed");
        assertTrue(store.contains(StatementPattern.of(S, P, new SimpleLiteral("value", new SimpleIRI("urn:original")))));
        assertFalse(store.contains(StatementPattern.of(S, P, new SimpleLiteral("value", new SimpleIRI("urn:changed")))));
        MemoryStorageAssertions.verify(store);
    }

}
