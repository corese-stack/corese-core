package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecution;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.storage.api.StorageManager;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PreparedPlanCacheTest {
    private static final String PREFIX = "PREFIX : <urn:bench:> ";
    private static final String SELECT = PREFIX + "SELECT ?s { ?s :type :Person . ?s :name \"name42\" }";
    private MemoryStorageManager storage;
    private QueryOperations reads;
    private Repository repository;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
        reads = mock(QueryOperations.class, delegatesTo(storage.queries()));
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(reads);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        when(observed.openSession()).thenAnswer(call -> {
            StorageManager session = mock(StorageManager.class, delegatesTo(storage.openSession()));
            when(session.queries()).thenReturn(reads);
            doThrow(UnsupportedOperationException.class).when(session).openReadSnapshot();
            return session;
        });
        repository = Repositories.create(observed);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void warmEvaluationReusesThePlanWithoutCountsAndKeepsItsEstimates() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            var cold = query.evaluateWithDiagnostics();
            assertEquals(List.of("urn:bench:s42"), subjects(cold));
            assertFalse(cold.profile().planReused());
            assertTrue(cold.profile().compilationNanos() > 0);
            assertTrue(cold.profile().planningNanos() > 0);
            clearInvocations(reads);
            var warm = query.evaluateWithDiagnostics();
            assertEquals(List.of("urn:bench:s42"), subjects(warm));
            assertTrue(warm.profile().planReused());
            assertEquals(0, warm.profile().compilationNanos());
            assertEquals(0, warm.profile().planningNanos());
            assertTrue(warm.profile().executionSetupNanos() > 0);
            assertEquals(cold.profile().patterns(), warm.profile().patterns());
            verify(reads, never()).count(any());
            verify(reads, times(2)).find(any());
        }
    }

    @Test
    void normalAndDiagnosticCallsShareOnlyTheCompiledImage() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            try (var result = query.evaluate()) { assertEquals(1, result.stream().count()); }
            var warm = query.evaluateWithDiagnostics();
            assertTrue(warm.profile().planReused());
            assertEquals(1, subjects(warm).size());
            try (var result = query.evaluate()) { assertEquals(1, result.stream().count()); }
            assertEquals(1, warm.profile().patterns().getFirst().candidateEdges());
            verify(reads, times(2)).count(any());
        }
    }

    @Test
    void bindingAndTimeoutChangesDoNotRecompileTheUnboundPlan() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s { ?s :type :Person . ?s :name ?name }");
            subjects(query.evaluateWithDiagnostics());
            clearInvocations(reads);
            query.setBinding("s", iri("s42"));
            query.setTimeout(Duration.ofSeconds(5));
            var bound = query.evaluateWithDiagnostics();
            assertTrue(bound.profile().planReused());
            assertEquals(List.of("urn:bench:s42"), subjects(bound));
            query.setBinding("s", iri("s43"));
            var rebound = query.evaluateWithDiagnostics();
            assertTrue(rebound.profile().planReused());
            assertEquals(List.of("urn:bench:s43"), subjects(rebound));
            query.clearBindings();
            query.setTimeout(Duration.ZERO);
            var unbound = query.evaluateWithDiagnostics();
            assertTrue(unbound.profile().planReused());
            assertEquals(200, subjects(unbound).size());
            verify(reads, never()).count(any());
        }
    }

    @Test
    void equalSizeReplacementInvalidatesThePlan() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            subjects(query.evaluateWithDiagnostics());
            long size = connection.size();
            connection.remove(iri("s42"), iri("name"), Values.factory().createLiteral("name42"));
            connection.add(iri("s43"), iri("name"), Values.factory().createLiteral("name42"));
            assertEquals(size, connection.size());
            clearInvocations(reads);
            var updated = query.evaluateWithDiagnostics();
            assertFalse(updated.profile().planReused());
            assertEquals(List.of("urn:bench:s43"), subjects(updated));
            verify(reads, times(2)).count(any());
        }
    }

    @Test
    void invalidationReordersPatternsUsingTheNewCardinalities() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            var before = query.evaluateWithDiagnostics();
            subjects(before);
            assertTrue(before.profile().patterns().getFirst().pattern().contains("urn:bench:name"));
            var extra = java.util.stream.IntStream.range(0, 250).mapToObj(i ->
                    Values.factory().createStatement(iri("extra" + i), iri("name"),
                            Values.factory().createLiteral("name42"))).toList();
            connection.add(extra);
            var after = query.evaluateWithDiagnostics();
            assertFalse(after.profile().planReused());
            assertTrue(after.profile().patterns().getFirst().pattern().contains("urn:bench:type"));
            assertEquals(200, after.profile().patterns().getFirst().estimatedCardinality());
            assertEquals(251, after.profile().patterns().getLast().estimatedCardinality());
            assertEquals(List.of("urn:bench:s42"), subjects(after));
        }
    }

    @Test
    void duplicateAndAbsentMutationsDoNotInvalidate() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            subjects(query.evaluateWithDiagnostics());
            long version = reads.getDataVersion();
            connection.add(iri("s42"), iri("name"), Values.factory().createLiteral("name42"));
            connection.remove(iri("absent"), iri("absent"), iri("absent"));
            assertEquals(version, reads.getDataVersion());
            var warm = query.evaluateWithDiagnostics();
            assertTrue(warm.profile().planReused());
            assertEquals(1, subjects(warm).size());
        }
    }

    @Test
    void changedOrderedDatasetInvalidatesButAnEquivalentDatasetDoesNot() {
        try (var connection = repository.getConnection()) {
            connection.add(iri("x"), iri("name"), Values.factory().createLiteral("name42"), iri("g1"));
            connection.add(iri("x"), iri("type"), iri("Person"), iri("g1"));
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            subjects(query.evaluateWithDiagnostics());
            query.setDataset(Dataset.builder().defaultGraph(iri("g1")).defaultGraph(iri("g2")).build());
            var changed = query.evaluateWithDiagnostics();
            assertFalse(changed.profile().planReused());
            assertEquals(List.of("urn:bench:x"), subjects(changed));
            query.setDataset(Dataset.builder().defaultGraph(iri("g1")).defaultGraph(iri("g2")).build());
            var equivalent = query.evaluateWithDiagnostics();
            assertTrue(equivalent.profile().planReused());
            subjects(equivalent);
            query.setDataset(Dataset.builder().defaultGraph(iri("g2")).defaultGraph(iri("g1")).build());
            var reversed = query.evaluateWithDiagnostics();
            assertFalse(reversed.profile().planReused());
            subjects(reversed);
            query.setDataset(Dataset.empty());
            var empty = query.evaluateWithDiagnostics();
            assertFalse(empty.profile().planReused());
            assertTrue(subjects(empty).isEmpty());
            query.setDataset(null);
            var original = query.evaluateWithDiagnostics();
            assertFalse(original.profile().planReused());
            assertEquals(List.of("urn:bench:s42"), subjects(original));
        }
    }

    @Test
    void unavailableVersionNeverEnablesUnsafeReuse() {
        when(reads.getDataVersion()).thenReturn(-1L);
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            subjects(query.evaluateWithDiagnostics());
            var repeated = query.evaluateWithDiagnostics();
            assertFalse(repeated.profile().planReused());
            assertTrue(repeated.profile().compilationNanos() > 0);
            assertTrue(repeated.profile().planningNanos() > 0);
            assertEquals(1, subjects(repeated).size());
            verify(reads, times(4)).count(any());
        }
    }

    @Test
    void aMutationRacingWithCountsCannotPublishThePlanUnderANewVersion() {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            long count = storage.queries().count(call.getArgument(0));
            if (first.getAndSet(false)) {
                storage.mutations().add(Values.factory().createStatement(iri("new"), iri("type"), iri("Person")));
            }
            return count;
        }).when(reads).count(any());
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(SELECT);
            subjects(query.evaluateWithDiagnostics());
            var replanned = query.evaluateWithDiagnostics();
            assertFalse(replanned.profile().planReused());
            subjects(replanned);
            var stable = query.evaluateWithDiagnostics();
            assertTrue(stable.profile().planReused());
            subjects(stable);
            verify(reads, times(4)).count(any());
        }
    }

    @Test
    void transactionViewsInvalidateOnOwnWritesRollbackAndOtherCommits() {
        // Real sessions are needed here: the observed reads in the other tests target committed data.
        try (var real = Repositories.create(storage.openSession());
             var a = real.getConnection(); var b = real.getConnection()) {
            TupleQuery qa = a.prepareTupleQuery(SELECT);
            TupleQuery qb = b.prepareTupleQuery(SELECT);
            subjects(qa.evaluateWithDiagnostics());
            subjects(qb.evaluateWithDiagnostics());
            a.begin();
            var captured = qa.evaluateWithDiagnostics();
            assertTrue(captured.profile().planReused());
            subjects(captured);
            b.add(iri("other"), iri("name"), Values.factory().createLiteral("name42"));
            b.add(iri("other"), iri("type"), iri("Person"));
            var snapshot = qa.evaluateWithDiagnostics();
            assertTrue(snapshot.profile().planReused());
            assertEquals(List.of("urn:bench:s42"), subjects(snapshot));
            a.rollback();
            var visible = qa.evaluateWithDiagnostics();
            assertFalse(visible.profile().planReused());
            assertEquals(2, subjects(visible).size());
            // Bring B's plan up to date before testing invisible uncommitted writes.
            subjects(qb.evaluateWithDiagnostics());
            a.begin();
            a.remove(iri("other"), iri("name"), Values.factory().createLiteral("name42"));
            var ownWrite = qa.evaluateWithDiagnostics();
            assertFalse(ownWrite.profile().planReused());
            assertEquals(1, subjects(ownWrite).size());
            var outside = qb.evaluateWithDiagnostics();
            assertTrue(outside.profile().planReused());
            assertEquals(2, subjects(outside).size());
            a.commit();
            var committed = qb.evaluateWithDiagnostics();
            assertFalse(committed.profile().planReused());
            assertEquals(1, subjects(committed).size());
            var ownCommit = qa.evaluateWithDiagnostics();
            assertTrue(ownCommit.profile().planReused(), "Own commit publishes the same visible data");
            subjects(ownCommit);
            a.begin();
            a.remove(iri("s42"), iri("name"), Values.factory().createLiteral("name42"));
            assertTrue(subjects(qa.evaluateWithDiagnostics()).isEmpty());
            a.rollback();
            var rolledBack = qa.evaluateWithDiagnostics();
            assertFalse(rolledBack.profile().planReused());
            assertEquals(1, subjects(rolledBack).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT ?s { ?s ?p ?o } LIMIT 3",
            "SELECT ?s { ?s :type/:missing ?o }",
            "SELECT ?s { VALUES ?s { :s42 :s42 :s43 } ?s :type :Person OPTIONAL { ?s :missing ?x } }",
            "SELECT ?s { { SELECT ?s { ?s :type :Person . ?s :name \"name42\" } } FILTER EXISTS { ?s :type :Person } }",
            "SELECT ?s (COUNT(?name) AS ?n) { ?s :type :Person . ?s :name ?name } GROUP BY ?s ORDER BY ?s LIMIT 3",
            "SELECT ?s { ?s :type :Person MINUS { ?s :name \"name41\" } } LIMIT 3"
    })
    void mutableOperatorStateDoesNotLeakAcrossWarmEvaluations(String body) {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(PREFIX + body);
            List<String> cold = subjects(query.evaluateWithDiagnostics());
            var warm = query.evaluateWithDiagnostics();
            assertTrue(warm.profile().planReused());
            assertEquals(cold, subjects(warm));
            var repeated = query.evaluateWithDiagnostics();
            assertTrue(repeated.profile().planReused());
            assertEquals(cold, subjects(repeated));
        }
    }

    @Test
    void earlierResultRemainsReadableAfterAnotherEvaluation() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(PREFIX + "SELECT ?s { ?s :type :Person } LIMIT 3");
            var first = query.evaluateWithDiagnostics();
            var second = query.evaluateWithDiagnostics();
            assertTrue(second.profile().planReused());
            assertEquals(subjects(second), subjects(first));
        }
    }

    private static List<String> subjects(QueryExecution<TupleQueryResult> execution) {
        try (var result = execution.result()) {
            return result.stream().map(row -> row.getValue("s").stringValue()).sorted().toList();
        }
    }

    private static IRI iri(String local) {
        return Values.factory().createIRI("urn:bench:" + local);
    }
}
