package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ExecutionMode;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FallbackReason;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.StorageBenchmark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Differential checks retain the established stable sort as the independent reference. */
class TopKQueryTest {
    private MemoryStorageManager storage;
    private SparqlQueryExecutor executor;
    private SparqlQueryExecutor fullSort;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        storage.mutations().addAll(StorageBenchmark.data(200, 0, false));
        executor = new SparqlQueryExecutor(storage);
        StorageManager classic = mock(StorageManager.class, delegatesTo(storage));
        doThrow(UnsupportedOperationException.class).when(classic).openReadSnapshot();
        fullSort = new SparqlQueryExecutor(classic);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ASC(?x) LIMIT 3",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY DESC(?x) LIMIT 3",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ?x OFFSET 2 LIMIT 3",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY DESC(?x) OFFSET 2 LIMIT 3",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ?x LIMIT 1000",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ?x LIMIT 2147483646",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ?x OFFSET 50 LIMIT 3",
            "SELECT ?x { VALUES ?x { 9 1 4 2 7 3 } } ORDER BY ?x OFFSET 2 LIMIT 0",
            "SELECT ?x { VALUES ?x {} } ORDER BY ?x LIMIT 10",
            "SELECT ?x ?id { VALUES (?x ?id) { (2 0) (2 1) (1 2) (2 3) (1 4) (2 5) (1 6) } } ORDER BY ?x LIMIT 5",
            "SELECT ?x ?id { VALUES (?x ?id) { (2 0) (2 1) (1 2) (2 3) (1 4) (2 5) (1 6) } } ORDER BY DESC(?x) OFFSET 1 LIMIT 4",
            "SELECT ?x ?id { VALUES (?x ?id) { (2 0) (2 1) (1 2) (2 3) (1 4) (2 5) (1 6) } } ORDER BY ?x DESC(?id) OFFSET 1 LIMIT 4",
            "SELECT DISTINCT ?x { VALUES ?x { 9 1 1 4 2 2 7 3 3 } } ORDER BY ?x OFFSET 1 LIMIT 3",
            "SELECT DISTINCT ?x { VALUES (?x ?rank) { (1 8) (1 0) (2 5) (3 2) (3 1) } } ORDER BY ?rank LIMIT 2",
            "SELECT DISTINCT (?x + 1 AS ?value) { VALUES ?x { 3 3 2 1 1 0 } } ORDER BY DESC(?value) OFFSET 1 LIMIT 2",
            "SELECT ?x ?id { VALUES (?x ?id) { (UNDEF 0) (2 1) (UNDEF 2) (1 3) } } ORDER BY ?x LIMIT 3",
            "SELECT ?x ?id { VALUES (?x ?id) { (UNDEF 0) (2 1) (UNDEF 2) (1 3) } } ORDER BY DESC(?x) LIMIT 3",
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?x { VALUES ?x { 1 1.0 1E0 \"01\"^^xsd:integer 0 2E0 } } ORDER BY ?x LIMIT 4",
            "SELECT ?x { VALUES ?x { 2 \"two\" <urn:a> true UNDEF \"one\" 1 } } ORDER BY ?x LIMIT 5",
            "SELECT ?s ?o { ?s ?p ?o } ORDER BY DESC(?s) ?o LIMIT 10 OFFSET 7",
            "SELECT ?s { { ?s <urn:bench:type> ?o } UNION { ?s <urn:bench:type> ?o } } ORDER BY ?s LIMIT 10 OFFSET 3",
            "SELECT ?s ?missing { ?s <urn:bench:type> ?o OPTIONAL { ?s <urn:missing> ?missing } } ORDER BY DESC(?s) LIMIT 10"
    })
    void agreesExactlyWithTheExistingComparatorAndStableFullSort(String query) {
        assertEquals(rows(fullSort, query), rows(executor, query));
    }

    @Test
    void offsetIsAppliedAfterSelectingTheBestLimitPlusOffsetCandidates() {
        try (var result = executor.evaluateTuple("SELECT ?x { VALUES ?x { 8 7 6 5 4 3 2 1 } } ORDER BY ?x OFFSET 2 LIMIT 3")) {
            assertEquals(List.of("3", "4", "5"), result.stream().map(row -> row.getValue("x").stringValue()).toList());
        }
    }

    @Test
    void genericTypedNumericStorageValuesSortIdenticallyWithTopKAndTheClassicEvaluator() {
        String query = "SELECT ?s ?score { ?s <urn:bench:score> ?score } ORDER BY DESC(?score) ?s LIMIT 10";
        assertEquals(rows(fullSort, query), rows(executor, query));
    }

    @Test
    void tiesRemainInArrivalOrderWhenLaterBetterCandidatesEvictHeapMembers() {
        try (var result = executor.evaluateTuple("""
                SELECT ?id { VALUES (?key ?id) { (2 0) (2 1) (2 2) (1 3) (2 4) (1 5) (2 6) } }
                ORDER BY ?key OFFSET 1 LIMIT 3
                """)) {
            assertEquals(List.of("5", "0", "1"), result.stream().map(row -> row.getValue("id").stringValue()).toList());
        }
    }

    @Test
    void randomizedRepeatedKeysMatchFullSortingAcrossSelectionBoundaries() {
        var random = new Random(617);
        var values = new StringBuilder();
        for (int i = 0; i < 300; i++) values.append('(').append(random.nextInt(11)).append(' ').append(i).append(") ");
        String body = "SELECT ?key ?id { VALUES (?key ?id) { " + values + " } } ORDER BY ";
        for (String order : List.of("?key", "DESC(?key)", "?key DESC(?id)")) {
            for (int offset : List.of(0, 1, 17, 299, 400)) {
                String query = body + order + " OFFSET " + offset + " LIMIT 13";
                assertEquals(rows(fullSort, query), rows(executor, query), query);
            }
        }
    }

    @Test
    void preparedSelectionReusesItsPlanAndKeepsFreshParametersAndStorageVersions() {
        try (Repository repository = Repositories.create(storage); var connection = repository.getConnection()) {
            var prepared = connection.prepareTupleQuery("SELECT ?s { ?s ?p ?o } ORDER BY ?s LIMIT 2");
            prepared.setBinding("p", Values.factory().createIRI("urn:bench:type"));
            try (var first = prepared.evaluate()) {
                assertEquals(2, first.stream().count());
            }
            prepared.setBinding("p", Values.factory().createIRI("urn:bench:name"));
            var warm = prepared.evaluateWithDiagnostics();
            try (var result = warm.result()) {
                assertEquals(2, result.stream().count());
                assertTrue(warm.profile().planReused());
                assertEquals(0, warm.profile().compilationNanos());
                assertEquals(ExecutionMode.MATERIALIZED, warm.profile().delivery().mode());
                assertEquals(FallbackReason.ORDER_BY, warm.profile().delivery().fallbackReason());
            }
            connection.clear();
            var changed = prepared.evaluateWithDiagnostics();
            try (var result = changed.result()) {
                assertFalse(result.hasNext());
                assertFalse(changed.profile().planReused());
            }
        }
    }

    private static List<Map<String, Value>> rows(SparqlQueryExecutor engine, String query) {
        try (TupleQueryResult result = engine.evaluateTuple(query)) {
            var rows = new ArrayList<Map<String, Value>>();
            result.forEach(row -> {
                var bindings = new LinkedHashMap<String, Value>();
                for (String name : result.getBindingNames()) {
                    if (row.hasBinding(name)) bindings.put(name, row.getValue(name));
                }
                rows.add(bindings);
            });
            return rows;
        }
    }
}
