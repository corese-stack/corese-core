package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for SELECT solution modifiers: DISTINCT, ORDER BY, LIMIT, OFFSET.
 *
 * <p>All tests exercise the full pipeline: parser → AST → bridge → KGRAM → result adapter.
 * The storage backend is always an in-memory store to keep tests self-contained.</p>
 */
class SelectModifiersE2ETest {

    private static final String EX    = "http://example.org/";
    private static final String TYPE  = EX + "type";
    private static final String NAME  = EX + "name";

    private CoreseValueFactory vf;
    private MemoryStorageManager storage;
    private NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        vf      = new CoreseValueFactory();
        storage = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);
    }

    @Test
    @DisplayName("SELECT DISTINCT on multi-column result removes duplicate solution mappings")
    void selectDistinctMultiColumnRemovesDuplicateSolutionMappings() {
        String AGE = EX + "age";
        insert(iri(EX + "alice"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "alice"), iri(AGE),  vf.createLiteral("30"));
        insert(iri(EX + "carol"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "carol"), iri(AGE),  vf.createLiteral("30"));
        insert(iri(EX + "bob"),   iri(NAME), vf.createLiteral("Bob"));
        insert(iri(EX + "bob"),   iri(AGE),  vf.createLiteral("25"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT DISTINCT ?name ?age WHERE { ?s <" + NAME + "> ?name ; <" + AGE + "> ?age }")) {
            List<List<String>> rows = new ArrayList<>();
            while (result.hasNext()) {
                var bs = result.next();
                rows.add(List.of(
                        bs.getValue("name").stringValue(),
                        bs.getValue("age").stringValue()
                ));
            }
            assertEquals(2, rows.size(),
                    "DISTINCT must collapse (Alice,30) duplicate — expected 2 distinct rows, got: " + rows);
            long aliceCount = rows.stream().filter(r -> r.getFirst().equals("Alice")).count();
            assertEquals(1, aliceCount, "(Alice,30) must appear exactly once after DISTINCT");
        }
    }

    @Test
    @DisplayName("SELECT DISTINCT preserves rows with same first column but different second column")
    void selectDistinctMultiColumnPreservesPartiallyDifferentRows() {
        // (Alice, 30) and (Alice, 31) differ in age → both must be kept
        String AGE = EX + "age";
        insert(iri(EX + "alice1"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "alice1"), iri(AGE),  vf.createLiteral("30"));
        insert(iri(EX + "alice2"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "alice2"), iri(AGE),  vf.createLiteral("31"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT DISTINCT ?name ?age WHERE { ?s <" + NAME + "> ?name ; <" + AGE + "> ?age }")) {
            int count = 0;
            while (result.hasNext()) { result.next(); count++; }
            assertEquals(2, count, "(Alice,30) and (Alice,31) differ in ?age — both must survive DISTINCT");
        }
    }

    @Test
    @DisplayName("SELECT DISTINCT removes duplicate values")
    void selectDistinctRemovesDuplicates() {
        // alice and bob both have the same type → would produce two rows for ?t without DISTINCT
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "bob"),   iri(TYPE), iri(EX + "Person"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT DISTINCT ?t WHERE { ?s <" + TYPE + "> ?t }");

        List<String> types = collectSingleColumn(result, "t");
        assertEquals(1, types.size(), "DISTINCT should collapse two identical ?t values to one");
        assertEquals(EX + "Person", types.getFirst());
    }

    @Test
    @DisplayName("SELECT DISTINCT preserves all rows when values differ")
    void selectDistinctKeepsDistinctRows() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Agent"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT DISTINCT ?t WHERE { <" + EX + "alice> <" + TYPE + "> ?t }");

        List<String> types = collectSingleColumn(result, "t");
        assertEquals(2, types.size(), "Two distinct type values must both appear");
        assertTrue(types.contains(EX + "Person"));
        assertTrue(types.contains(EX + "Agent"));
    }

    @Test
    @DisplayName("SELECT without DISTINCT returns duplicates")
    void selectWithoutDistinctReturnsDuplicates() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "bob"),   iri(TYPE), iri(EX + "Person"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?t WHERE { ?s <" + TYPE + "> ?t }");

        List<String> types = collectSingleColumn(result, "t");
        assertEquals(2, types.size(), "Without DISTINCT both rows should appear");
    }


    @Test
    @DisplayName("ORDER BY ASC sorts string literals in ascending lexicographic order")
    void orderByAscSortsLexicographically() {
        insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie"));
        insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ASC(?name)");

        List<String> names = collectSingleColumn(result, "name");
        assertEquals(List.of("Alice", "Bob", "Charlie"), names);
    }

    @Test
    @DisplayName("ORDER BY DESC sorts string literals in descending lexicographic order")
    void orderByDescSortsLexicographically() {
        insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie"));
        insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY DESC(?name)");

        List<String> names = collectSingleColumn(result, "name");
        assertEquals(List.of("Charlie", "Bob", "Alice"), names);
    }

    @Test
    @DisplayName("ORDER BY without ASC/DESC defaults to ascending order")
    void orderByDefaultIsAscending() {
        insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie"));
        insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ?name");

        List<String> names = collectSingleColumn(result, "name");
        assertEquals(List.of("Alice", "Bob", "Charlie"), names);
    }

    @Test
    @DisplayName("LIMIT 1 returns at most one result")
    void limitOneReturnsOneRow() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "bob"),   iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "carol"), iri(TYPE), iri(EX + "Person"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + TYPE + "> <" + EX + "Person> } LIMIT 1");

        List<String> subjects = collectSingleColumn(result, "s");
        assertEquals(1, subjects.size(), "LIMIT 1 must return exactly one row");
    }

    @Test
    @DisplayName("LIMIT larger than result set returns all results")
    void limitLargerThanResultSetReturnsAll() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));
        insert(iri(EX + "bob"),   iri(TYPE), iri(EX + "Person"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + TYPE + "> <" + EX + "Person> } LIMIT 100");

        List<String> subjects = collectSingleColumn(result, "s");
        assertEquals(2, subjects.size(), "LIMIT larger than actual results must return all rows");
    }

    @Test
    @DisplayName("LIMIT 0 returns no results")
    void limitZeroReturnsNoResults() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + TYPE + "> <" + EX + "Person> } LIMIT 0")) {

            assertFalse(result.hasNext(), "LIMIT 0 must return an empty result set");
        }
    }

    @Test
    @DisplayName("ORDER BY + OFFSET skips the first N results")
    void orderByWithOffsetSkipsLeadingRows() {
        insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Bob"));
        insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ?name OFFSET 1");

        List<String> names = collectSingleColumn(result, "name");
        assertEquals(List.of("Bob", "Charlie"), names, "OFFSET 1 must skip the first row");
    }

    @Test
    @DisplayName("ORDER BY + LIMIT + OFFSET returns a sub-page of results")
    void orderByLimitOffsetReturnsPaginatedResults() {
        insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Bob"));
        insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie"));
        insert(iri(EX + "d"), iri(NAME), vf.createLiteral("Diana"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ?name LIMIT 2 OFFSET 1");

        List<String> names = collectSingleColumn(result, "name");
        assertEquals(List.of("Bob", "Charlie"), names,
                "LIMIT 2 OFFSET 1 must return the second and third rows of the ordered set");
    }

    @Test
    @DisplayName("OFFSET beyond result set returns empty result")
    void offsetBeyondResultSetReturnsEmpty() {
        insert(iri(EX + "alice"), iri(TYPE), iri(EX + "Person"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + TYPE + "> ?o } ORDER BY ?s OFFSET 10")) {

            assertFalse(result.hasNext(), "OFFSET beyond result count must return an empty result set");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void insert(Resource subject, IRI predicate, Value object) {
        storage.getMutationOperations()
                .insertStatement(vf.createStatement(subject, predicate, object));
    }

    private IRI iri(String value) {
        return vf.createIRI(value);
    }

    private List<String> collectSingleColumn(TupleQueryResult result, String varName) {
        List<String> values = new ArrayList<>();
        while (result.hasNext()) {
            BindingSet bs = result.next();
            Value v = bs.getValue(varName);
            values.add(v == null ? null : v.stringValue());
        }
        return values;
    }
}
