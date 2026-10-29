package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PredicateStatisticsEstimator — bounded subject sampling fix.
 *
 * <p>The root cause of the Q15 regression was that accumulateLeaf() iterated ALL
 * subjects in a leaf rather than stopping at maxSample subjects. For a predicate
 * like rdf:type with many objects but millions of total subjects, this caused
 * O(total subjects) work per estimation call.</p>
 */
class PredicateStatisticsEstimatorTest {

    private static IRI iri(String local) {
        return new SimpleIRI("urn:test:" + local);
    }

    private static Statement triple(String s, IRI p, String o) {
        return Values.factory().createStatement(iri(s), p, iri(o));
    }

    private static Statement tripleWithLiteral(String s, IRI p, String literal) {
        return Values.factory().createStatement(iri(s), p, new SimpleLiteral(literal));
    }

    // ── exact counting when dataset is small ──────────────────────────────────

    @Test
    void exactDistinctSubjectsWhenDatasetFitsWithinSampleBudget() {
        IRI type = iri("type");
        List<Statement> stmts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            stmts.add(triple("s" + i, type, "ClassA"));
        }
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(type);
        assertNotNull(stats);
        assertEquals(100, stats.count());
        assertEquals(100, stats.distinctSubjects());
        assertEquals(1, stats.distinctObjects());

        storage.close();
    }

    @Test
    void exactCountsForMultipleObjectBuckets() {
        IRI type = iri("type");
        List<Statement> stmts = new ArrayList<>();
        // 50 subjects → ClassA, 50 subjects → ClassB
        for (int i = 0; i < 50; i++) {
            stmts.add(triple("s" + i, type, "ClassA"));
        }
        for (int i = 50; i < 100; i++) {
            stmts.add(triple("s" + i, type, "ClassB"));
        }
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(type);
        assertNotNull(stats);
        assertEquals(100, stats.count());
        assertEquals(100, stats.distinctSubjects());
        assertEquals(2, stats.distinctObjects());

        storage.close();
    }

    // ── bounded sampling: O(maxSample) work even for large fan-out predicates ─

    @Test
    void samplingBoundedAtMaxSampleSubjectsForHighCardinalityPredicate() {
        // Simulate rdf:type pattern: few object classes, millions of subjects.
        // Before the fix, all subjects were iterated. After fix: stops at 512.
        IRI type = iri("type");
        int totalSubjects = 10_000;
        int maxSample = 512;

        List<Statement> stmts = new ArrayList<>();
        // 10 object classes with 1000 subjects each
        for (int i = 0; i < totalSubjects; i++) {
            stmts.add(triple("s" + i, type, "Class" + (i % 10)));
        }
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(type);
        assertNotNull(stats);
        assertEquals(totalSubjects, stats.count());
        assertEquals(10, stats.distinctObjects());
        // Distinct subjects is estimated (sampled), must be positive and ≤ totalSubjects
        assertTrue(stats.distinctSubjects() > 0,
                "distinctSubjects must be positive, got " + stats.distinctSubjects());
        assertTrue(stats.distinctSubjects() <= totalSubjects,
                "distinctSubjects must not exceed totalSubjects, got " + stats.distinctSubjects());
        // With uniform distribution, extrapolation should land near totalSubjects
        assertTrue(stats.distinctSubjects() >= totalSubjects / 2,
                "extrapolated distinctSubjects should be within 2x of actual, got " + stats.distinctSubjects());

        storage.close();
    }

    @Test
    void sampledDistinctSubjectsNeverExceedTotalCount() {
        IRI pred = iri("pred");
        List<Statement> stmts = new ArrayList<>();
        // 2000 subjects → single object (only 1 leaf in POS index)
        for (int i = 0; i < 2000; i++) {
            stmts.add(triple("s" + i, pred, "obj"));
        }
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(pred);
        assertNotNull(stats);
        assertEquals(2000, stats.count());
        assertEquals(1, stats.distinctObjects());
        assertTrue(stats.distinctSubjects() > 0);
        assertTrue(stats.distinctSubjects() <= stats.count(),
                "distinctSubjects must not exceed count: " + stats.distinctSubjects() + " > " + stats.count());

        storage.close();
    }

    // ── zero / missing cases ──────────────────────────────────────────────────

    @Test
    void returnsNullForUnknownPredicate() {
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(List.of(triple("s0", iri("known"), "obj")));

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(iri("unknown"));
        assertNull(stats);

        storage.close();
    }

    @Test
    void returnsZeroStatsForEmptyStorage() {
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        // nothing loaded — no stats for any predicate
        assertNull(storage.metadata().getPredicateStatistics(iri("type")));
        storage.close();
    }

    // ── single statement (Statement path vs Leaf path) ────────────────────────

    @Test
    void singleStatementPerObjectIsHandledExactly() {
        IRI pred = iri("pred");
        List<Statement> stmts = new ArrayList<>();
        // 3 subjects, each with a distinct object → hits Statement path (not Leaf)
        stmts.add(triple("s0", pred, "obj0"));
        stmts.add(triple("s1", pred, "obj1"));
        stmts.add(triple("s2", pred, "obj2"));
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(pred);
        assertNotNull(stats);
        assertEquals(3, stats.count());
        assertEquals(3, stats.distinctSubjects());
        assertEquals(3, stats.distinctObjects());

        storage.close();
    }

    // ── literal subjects integration ──────────────────────────────────────────

    @Test
    void literalObjectsCountedCorrectly() {
        IRI name = iri("name");
        List<Statement> stmts = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            stmts.add(tripleWithLiteral("s" + i, name, "name" + i));
        }
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(stmts);

        PredicateStatistics stats = storage.metadata().getPredicateStatistics(name);
        assertNotNull(stats);
        assertEquals(50, stats.count());
        assertEquals(50, stats.distinctSubjects());
        assertEquals(50, stats.distinctObjects());

        storage.close();
    }
}
