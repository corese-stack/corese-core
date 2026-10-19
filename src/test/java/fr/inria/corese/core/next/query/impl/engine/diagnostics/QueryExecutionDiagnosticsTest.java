package fr.inria.corese.core.next.query.impl.engine.diagnostics;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.StorageBenchmark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryExecutionDiagnosticsTest {
    private static final String PREFIX = "PREFIX : <urn:bench:> ";
    private static final String SELECTIVE = "SELECT ?s { ?s :type :Person . ?s :name \"name42\" }";
    @SuppressWarnings("java:S1450") // Test fixture shared between setup and cursor lifecycle tests
    private QueryOperations realQueries;
    private QueryOperations operations;
    private Repository repository;
    private final AtomicInteger opened = new AtomicInteger();
    private final AtomicInteger closed = new AtomicInteger();

    @BeforeEach
    void setUp() {
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
        realQueries = storage.queries();
        operations = mock(QueryOperations.class, delegatesTo(realQueries));
        doAnswer(call -> {
            opened.incrementAndGet();
            return realQueries.find(call.getArgument(0)).onClose(closed::incrementAndGet);
        }).when(operations).find(any());
        StorageManager observed = mock(StorageManager.class, delegatesTo(storage));
        when(observed.queries()).thenReturn(operations);
        doThrow(UnsupportedOperationException.class).when(observed).openReadSnapshot();
        when(observed.openSession()).thenAnswer(call -> {
            StorageManager session = mock(StorageManager.class, delegatesTo(storage.openSession()));
            when(session.queries()).thenReturn(operations);
            doThrow(UnsupportedOperationException.class).when(session).openReadSnapshot();
            return session;
        });
        repository = Repositories.create(observed);
    }

    @AfterEach
    void tearDown() {
        repository.close();
        assertEquals(opened.get(), closed.get(), "Every storage cursor must be released");
    }

    @Test
    void reportsChosenOrderEstimatesAndCandidatesWithoutExtraCounts() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX + SELECTIVE).evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals("urn:bench:s42", result.next().getValue("s").stringValue());
                assertFalse(result.hasNext());
            }
            var profile = execution.profile();
            assertEquals(2, profile.patterns().size());
            var first = profile.patterns().getFirst();
            assertTrue(first.pattern().contains("urn:bench:name"));
            assertEquals(1, first.estimatedCardinality());
            assertEquals(1, first.iteratorOpenings());
            assertEquals(1, first.candidateEdges());
            var second = profile.patterns().getLast();
            assertTrue(second.pattern().contains("urn:bench:type"));
            assertEquals(200, second.estimatedCardinality());
            assertEquals(1, second.candidateEdges());
            assertFalse(first.auxiliary());
            assertNotEquals(first.location(), second.location());
            assertTrue(profile.compilationNanos() > 0);
            assertTrue(profile.planningNanos() > 0);
            assertTrue(profile.evaluationNanos() > 0);
            assertTrue(profile.format().contains("estimate=200; iterations=1; candidates=1"));
            assertThrows(UnsupportedOperationException.class, () -> profile.patterns().clear());
            verify(operations, times(2)).count(any());
            verify(operations, times(2)).find(any());
            assertEquals(opened.get(), closed.get(), "Evaluation releases snapshots before returning");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void limitsCountOnlyConsumedCandidatesAndDoNotRequestSingletonEstimates(int limit) {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery("SELECT * { ?s ?p ?o } LIMIT " + limit)
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(limit, result.stream().count());
            }
            var pattern = execution.profile().patterns().getFirst();
            assertEquals(-1, pattern.estimatedCardinality());
            assertEquals(1, pattern.iteratorOpenings());
            assertEquals(limit, pattern.candidateEdges());
            verify(operations, never()).count(any());
        }
    }

    @Test
    void askStopsAtItsFirstCandidateAndReleasesTheSnapshot() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareBooleanQuery("ASK { ?s ?p ?o }").evaluateWithDiagnostics();
            assertTrue(execution.result());
            assertEquals(1, execution.profile().patterns().getFirst().candidateEdges());
            assertEquals(opened.get(), closed.get());
            verify(operations, never()).count(any());
        }
    }

    @Test
    void repeatedJoinInvocationsAreSummedRatherThanReportedAsDistinctRows() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s { ?s :type :Person . ?s :name ?name }").evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(200, result.stream().count());
            }
            var patterns = execution.profile().patterns();
            assertEquals(200, patterns.getFirst().candidateEdges());
            assertEquals(1, patterns.getFirst().iteratorOpenings());
            assertEquals(200, patterns.getLast().candidateEdges());
            assertEquals(200, patterns.getLast().iteratorOpenings());
        }
    }

    @Test
    void unvisitedPatternsRetainTheirPlanPositionAndZeroCounters() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT * { ?s :type :Missing . ?s :name ?name }").evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertFalse(result.hasNext());
            }
            assertEquals(0, execution.profile().patterns().getFirst().estimatedCardinality());
            assertEquals(0, execution.profile().patterns().getLast().iteratorOpenings());
            assertEquals(0, execution.profile().patterns().getLast().candidateEdges());
        }
    }

    @Test
    void candidateCountsPrecedeRdfTermMatching() {
        try (var connection = repository.getConnection()) {
            connection.add(iri("a"), iri("self"), iri("a"));
            connection.add(iri("a"), iri("self"), iri("b"));
            var execution = connection.prepareTupleQuery(PREFIX + "SELECT ?s { ?s :self ?s }")
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            assertEquals(2, execution.profile().patterns().getFirst().candidateEdges());
        }
    }

    @Test
    void orderByLimitStillReportsTheFullScanRequiredForSorting() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?name { ?s :name ?name } ORDER BY ?name LIMIT 10").evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(10, result.stream().count());
            }
            assertEquals(200, execution.profile().patterns().getFirst().candidateEdges());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT ?s { ?s :name \"name42\" OPTIONAL { ?s :missing ?o } }",
            "SELECT ?s { ?s :name \"name42\" MINUS { ?s :name \"name41\" } }",
            "SELECT ?s ?v { ?s :name \"name42\" BIND(?s AS ?v) FILTER(?s = :s42) }",
            "SELECT (COUNT(?s) AS ?n) { ?s :type :Person }"
    })
    void instrumentationPreservesSolutionsAcrossSemanticOperators(String sparql) {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(PREFIX + sparql);
            java.util.List<String> ordinary;
            try (var result = query.evaluate()) {
                ordinary = result.stream().map(row -> row.getBindingNames().stream().sorted()
                        .map(name -> name + "=" + row.getValue(name)).toList().toString()).sorted().toList();
            }
            var execution = query.evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(ordinary, result.stream().map(row -> row.getBindingNames().stream().sorted()
                        .map(name -> name + "=" + row.getValue(name)).toList().toString()).sorted().toList());
            }
        }
    }

    @Test
    void unknownCountsDoNotPreventExecution() {
        doThrow(new UnsupportedOperationException("No estimates")).when(operations).count(any());
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX + SELECTIVE).evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            assertTrue(execution.profile().patterns().stream().allMatch(p -> p.estimatedCardinality() == -1));
            assertTrue(execution.profile().format().contains("estimate=unknown"));
        }
    }

    @Test
    void repeatedEvaluationsUseFreshCountersAndCurrentDataDatasetAndBindings() {
        try (var connection = repository.getConnection()) {
            TupleQuery query = connection.prepareTupleQuery(PREFIX + SELECTIVE);
            var before = query.evaluateWithDiagnostics();
            try (var result = before.result()) {
                assertEquals(1, result.stream().count());
            }
            connection.add(iri("other"), iri("name"), Values.factory().createLiteral("name42"), iri("g"));
            connection.add(iri("other"), iri("type"), iri("Person"), iri("g"));
            query.setDataset(Dataset.builder().defaultGraph(iri("g")).build());
            query.setBinding("s", iri("other"));
            var after = query.evaluateWithDiagnostics();
            try (var result = after.result()) {
                assertEquals("urn:bench:other", result.next().getValue("s").stringValue());
                assertFalse(result.hasNext());
            }
            assertEquals(200, before.profile().patterns().getLast().estimatedCardinality());
            assertTrue(after.profile().patterns().stream().allMatch(p -> p.estimatedCardinality() == 1));
            assertTrue(after.profile().patterns().stream().allMatch(p -> p.candidateEdges() == 1));
            // An ordinary evaluation after profiling must not update a prior report.
            try (var result = query.evaluate()) {
                assertEquals(1, result.stream().count());
            }
            assertEquals(1, after.profile().patterns().getFirst().candidateEdges());
        }
    }

    @Test
    void unionBranchesKeepSeparatePatternIdentitiesAndSolutionBags() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s { { ?s :name \"name42\" } UNION { ?s :name \"name42\" } }")
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(2, result.stream().count());
            }
            var patterns = execution.profile().patterns();
            assertEquals(2, patterns.size());
            assertTrue(patterns.stream().allMatch(p -> !p.auxiliary() && p.candidateEdges() == 1));
            assertNotEquals(patterns.getFirst().location(), patterns.getLast().location());
        }
    }

    @Test
    void namedGraphScopeIsVisibleAndPreserved() {
        try (var connection = repository.getConnection()) {
            connection.add(iri("s"), iri("p"), iri("o"), iri("g"));
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT * { GRAPH :g { ?s :p ?o } }").evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            assertTrue(execution.profile().patterns().getFirst().pattern().contains("GRAPH"));
            assertTrue(execution.profile().patterns().getFirst().pattern().contains("urn:bench:g"));
        }
    }

    @Test
    void producerCountersCountAfterInterGraphDeduplication() {
        try (var connection = repository.getConnection()) {
            for (String graph : new String[]{"g1", "g2"}) {
                connection.add(iri("s42"), iri("name"), Values.factory().createLiteral("name42"), iri(graph));
                connection.add(iri("s42"), iri("type"), iri("Person"), iri(graph));
            }
            var execution = connection.prepareTupleQuery(PREFIX + SELECTIVE)
                    .setDataset(Dataset.builder().defaultGraph(iri("g1")).defaultGraph(iri("g2")).build())
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            assertEquals(2, execution.profile().patterns().getFirst().estimatedCardinality());
            assertEquals(1, execution.profile().patterns().getFirst().candidateEdges());
        }
    }

    @Test
    void subqueryPatternsKeepTheirScope() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s { { SELECT ?s { ?s :type :Person . ?s :name \"name42\" } } ?s :group ?g }")
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            assertEquals(3, execution.profile().patterns().size());
            assertTrue(execution.profile().patterns().stream().noneMatch(QueryExecutionProfile.Pattern::auxiliary));
            assertTrue(execution.profile().patterns().stream().allMatch(p -> p.candidateEdges() == 1));
        }
    }

    @Test
    void pathsIncludeLogicalCandidatesAndExplicitAuxiliaryLookups() {
        try (var connection = repository.getConnection()) {
            connection.add(iri("a"), iri("link"), iri("b"));
            connection.add(iri("b"), iri("link"), iri("c"));
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?o { :a :link/:link ?o }").evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals("urn:bench:c", result.next().getValue("o").stringValue());
                assertFalse(result.hasNext());
            }
            var patterns = execution.profile().patterns();
            assertFalse(patterns.getFirst().auxiliary());
            assertEquals(1, patterns.getFirst().candidateEdges());
            assertTrue(patterns.stream().anyMatch(QueryExecutionProfile.Pattern::auxiliary));
        }
    }

    @Test
    void existsReportsOnlyActuallyExecutedAuxiliaryPatterns() {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareTupleQuery(PREFIX
                    + "SELECT ?s { ?s :name \"name42\" FILTER EXISTS { ?s :type :Person } }")
                    .evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(1, result.stream().count());
            }
            var patterns = execution.profile().patterns();
            assertEquals(2, patterns.size(), "Do not reconstruct unexecuted EXISTS trees for display");
            assertTrue(patterns.getLast().auxiliary());
            assertEquals(1, patterns.getLast().candidateEdges());
        }
    }

    @Test
    void auxiliaryDescriptionsRetainLiteralDatatypesAndDoNotMergeDifferentTerms() {
        try (var connection = repository.getConnection()) {
            connection.add(iri("s42"), iri("mark"), Values.factory().createLiteral("1"));
            connection.add(iri("s42"), iri("mark"), Values.factory().createLiteral("1", Values.factory().createIRI(
                    "http://www.w3.org/2001/XMLSchema#integer")));
            var execution = connection.prepareTupleQuery(PREFIX + """
                    SELECT ?s {
                      { ?s :name "name42" FILTER EXISTS { ?s :mark "1" } }
                      UNION
                      { ?s :name "name42" FILTER EXISTS { ?s :mark 1 } }
                    }
                    """).evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertEquals(2, result.stream().count());
            }
            var auxiliary = execution.profile().patterns().stream()
                    .filter(QueryExecutionProfile.Pattern::auxiliary).toList();
            assertEquals(2, auxiliary.size());
            assertNotEquals(auxiliary.getFirst().pattern(), auxiliary.getLast().pattern());
            assertTrue(auxiliary.stream().anyMatch(p -> p.pattern().contains("^^<")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONSTRUCT { ?s <urn:out> ?o } WHERE { ?s ?p ?o } LIMIT 1", "DESCRIBE <urn:bench:s42>"})
    void graphQueriesReturnTheUsualOwnedResultWithAProfile(String sparql) {
        try (var connection = repository.getConnection()) {
            var execution = connection.prepareGraphQuery(sparql).evaluateWithDiagnostics();
            try (var result = execution.result()) {
                assertTrue(result.hasNext());
            }
            assertFalse(execution.profile().patterns().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void storageFailuresKeepTheirCauseAndReleaseCursors(boolean duringTraversal) {
        StorageException cause = new StorageException(ErrorCode.QUERY_FAILED, "Backend failure");
        if (duringTraversal) {
            doAnswer(call -> {
                opened.incrementAndGet();
                return realQueries.find(call.getArgument(0))
                        .peek(statement -> { throw cause; }).onClose(closed::incrementAndGet);
            }).when(operations).find(any());
        } else {
            doThrow(cause).when(operations).find(any());
        }
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }");
            var failure = assertThrows(QueryEvaluationException.class, query::evaluateWithDiagnostics);
            assertSame(cause, failure.getCause());
            assertEquals(opened.get(), closed.get());
        }
    }

    @Test
    void timeoutReleasesItsCursorsAndDoesNotContaminateTheFollowingQuery() {
        doAnswer(call -> {
            opened.incrementAndGet();
            return realQueries.find(call.getArgument(0)).peek(statement -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new QueryEvaluationException("Interrupted", interrupted);
                }
            }).onClose(closed::incrementAndGet);
        }).when(operations).find(any());
        try (var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT * { ?s ?p ?o }");
            query.setTimeout(Duration.ofMillis(30));
            assertThrows(QueryTimeoutException.class, query::evaluateWithDiagnostics);
            assertFalse(Thread.currentThread().isInterrupted());
            assertEquals(opened.get(), closed.get());
        }
        doAnswer(call -> {
            opened.incrementAndGet();
            return realQueries.find(call.getArgument(0)).onClose(closed::incrementAndGet);
        }).when(operations).find(any());
        try (var connection = repository.getConnection()) {
            assertTrue(connection.prepareBooleanQuery("ASK { ?s ?p ?o }").evaluateWithDiagnostics().result());
        }
    }

    private static IRI iri(String local) {
        return Values.factory().createIRI("urn:bench:" + local);
    }
}
