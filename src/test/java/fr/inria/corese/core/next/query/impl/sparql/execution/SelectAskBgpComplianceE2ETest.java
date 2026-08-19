package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compliance tests for the SELECT / ASK / BGP socle — cases identified as missing
 * after auditing the existing E2E suite against SPARQL 1.1 section 10 (Basic Graph
 * Patterns), section 17.1 (Query forms) and the W3C SPARQL 1.1 test suite manifests.
 *
 * <p>Each nested class documents the spec reference that motivates the test.</p>
 */
class SelectAskBgpComplianceE2ETest {

    private static final String EX    = "http://example.org/";
    private static final String XSD   = "http://www.w3.org/2001/XMLSchema#";
    private static final String KNOWS = EX + "knows";
    private static final String NAME  = EX + "name";
    private static final String BORN  = EX + "born";

    private CoreseValueFactory vf;
    private MemoryStorageManager storage;
    private NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        vf       = new CoreseValueFactory();
        storage  = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);
    }

    @Nested
    @DisplayName("PREFIX declarations")
    class PrefixDeclarations {

        @Test
        @DisplayName("PREFIX in SELECT query resolves abbreviated IRIs correctly")
        void prefixInSelectResolvesAbbreviatedIris() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

            try (TupleQueryResult result = executor.evaluateTuple("""
                    PREFIX ex: <http://example.org/>
                    SELECT ?o WHERE { ex:alice ex:knows ?o }
                    """)) {
                assertTrue(result.hasNext(), "PREFIX must be resolved — one result expected");
                assertEquals(EX + "bob", result.next().getValue("o").stringValue());
                assertFalse(result.hasNext());
            }
        }

        @Test
        @DisplayName("Multiple PREFIX declarations coexist in the same query")
        void multiplePrefixDeclarationsCoexist() {
            String foafName = "http://xmlns.com/foaf/0.1/name";
            insert(iri(EX + "alice"), iri(foafName), vf.createLiteral("Alice"));

            try (TupleQueryResult result = executor.evaluateTuple("""
                    PREFIX ex:   <http://example.org/>
                    PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                    SELECT ?name WHERE { ex:alice foaf:name ?name }
                    """)) {
                assertTrue(result.hasNext());
                assertEquals("Alice", result.next().getValue("name").stringValue());
                assertFalse(result.hasNext());
            }
        }

        @Test
        @DisplayName("PREFIX in ASK query resolves abbreviated IRIs correctly")
        void prefixInAskResolvesAbbreviatedIris() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

            assertTrue(executor.evaluateBoolean("""
                    PREFIX ex: <http://example.org/>
                    ASK { ex:alice ex:knows ex:bob }
                    """));
        }
    }

    @Nested
    @DisplayName("ASK edge cases")
    class AskEdgeCases {

        @Test
        @DisplayName("ASK {} with empty WHERE body returns true — one empty solution exists")
        void askEmptyBodyReturnsTrue() {
            // Per spec, {} matches exactly one empty solution regardless of store content.
            assertTrue(executor.evaluateBoolean("ASK {}"),
                    "ASK {} must return true: the empty pattern always matches once");
        }

        @Test
        @DisplayName("ASK {} returns true even when the store is empty")
        void askEmptyBodyReturnsTrueOnEmptyStore() {
            // Store is empty — no data at all.
            assertTrue(executor.evaluateBoolean("ASK {}"),
                    "ASK {} must return true on an empty store: the empty pattern always matches");
        }

        @Test
        @DisplayName("ASK with two-triple join returns true when both triples match")
        void askWithTwoTripleJoinReturnsTrueWhenBothMatch() {
            // alice --knows--> bob --knows--> carol
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));
            insert(iri(EX + "bob"),   iri(KNOWS), iri(EX + "carol"));

            assertTrue(executor.evaluateBoolean("""
                    ASK {
                      <http://example.org/alice> <http://example.org/knows> ?mid .
                      ?mid <http://example.org/knows> <http://example.org/carol>
                    }
                    """));
        }

        @Test
        @DisplayName("ASK with two-triple join returns false when the chain is broken")
        void askWithTwoTripleJoinReturnsFalseWhenChainBroken() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));
            // bob does NOT know carol

            assertFalse(executor.evaluateBoolean("""
                    ASK {
                      <http://example.org/alice> <http://example.org/knows> ?mid .
                      ?mid <http://example.org/knows> <http://example.org/carol>
                    }
                    """));
        }
    }

    @Nested
    @DisplayName("Empty WHERE body in SELECT")
    class EmptyWhereBody {

        @Test
        @DisplayName("SELECT * WHERE {} returns exactly one empty solution")
        void selectStarEmptyWhereReturnsOneEmptySolution() {
            // The empty pattern {} matches once, producing one solution with no bindings.
            try (TupleQueryResult result = executor.evaluateTuple("SELECT * WHERE {}")) {
                // Binding names: SELECT * on {} → no variables in scope → empty list
                assertTrue(result.getBindingNames().isEmpty(),
                        "SELECT * on an empty pattern must produce no columns");
                assertTrue(result.hasNext(), "One empty solution must be returned");
                BindingSet bs = result.next();
                assertTrue(bs.getBindingNames().isEmpty(),
                        "The single solution must have no bound variables");
                assertFalse(result.hasNext(), "Only one solution must be returned");
            }
        }

        @Test
        @DisplayName("SELECT * WHERE {} returns one solution even when the store has data")
        void selectStarEmptyWhereReturnsSolutionWhenStoreHasData() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

            try (TupleQueryResult result = executor.evaluateTuple("SELECT * WHERE {}")) {
                assertTrue(result.hasNext(),
                        "One empty solution must be returned regardless of store content");
                result.next();
                assertFalse(result.hasNext(), "Only one solution must be returned");
            }
        }
    }

    @Nested
    @DisplayName("Projected variable not visible in WHERE")
    class ProjectedVariableNotInWhere {

        @Test
        @DisplayName("SELECT ?x WHERE { ?s ?p ?o } throws when ?x is not in the body")
        void selectProjectedVarNotInWhereThrows() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

            assertThrows(QueryValidationException.class,
                    () -> {
                        try (var ignored = executor.evaluateTuple("SELECT ?x WHERE { ?s ?p ?o }")) {
                            fail("Should have thrown before returning a result");
                        }
                    },
                    "A variable projected in SELECT but absent from WHERE must cause an error");
        }

        @Test
        @DisplayName("Error message names the missing variable")
        void selectProjectedVarNotInWhereMessageNamesVariable() {
            insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

            QueryValidationException ex = assertThrows(QueryValidationException.class,
                    () -> {
                        try (var ignored = executor.evaluateTuple("SELECT ?missing WHERE { ?s ?p ?o }")) {
                            fail("Should have thrown before returning a result");
                        }
                    });
            assertTrue(ex.getMessage().contains("missing"),
                    "Exception message must name the absent variable — got: " + ex.getMessage());
        }
    }

    @Nested
    @DisplayName("xsd:date and xsd:dateTime literals in BGP")
    class DateLiterals {

        @Test
        @DisplayName("BGP matches and projects an xsd:date literal")
        void bgpMatchesDateLiteral() {
            insert(iri(EX + "alice"), iri(BORN),
                    vf.createLiteral("1990-01-15", vf.createIRI(XSD + "date")));

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT ?born WHERE { <" + EX + "alice> <" + BORN + "> ?born }")) {
                assertTrue(result.hasNext());
                assertEquals("1990-01-15", result.next().getValue("born").stringValue());
                assertFalse(result.hasNext());
            }
        }

        @Test
        @DisplayName("BGP with constant xsd:date in object position matches the right triple")
        void bgpWithConstantDateInObjectPosition() {
            insert(iri(EX + "alice"), iri(BORN),
                    vf.createLiteral("1990-01-15", vf.createIRI(XSD + "date")));
            insert(iri(EX + "bob"), iri(BORN),
                    vf.createLiteral("1985-06-20", vf.createIRI(XSD + "date")));

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT ?s WHERE { ?s <" + BORN + "> \"1990-01-15\"^^<" + XSD + "date> }")) {
                List<String> subjects = collectSingleColumn(result, "s");
                assertEquals(List.of(EX + "alice"), subjects);
            }
        }

        @Test
        @DisplayName("BGP matches and projects an xsd:dateTime literal")
        void bgpMatchesDateTimeLiteral() {
            insert(iri(EX + "event"), iri(BORN),
                    vf.createLiteral("2024-03-15T10:30:00", vf.createIRI(XSD + "dateTime")));

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT ?dt WHERE { <" + EX + "event> <" + BORN + "> ?dt }")) {
                assertTrue(result.hasNext());
                assertEquals("2024-03-15T10:30:00", result.next().getValue("dt").stringValue());
                assertFalse(result.hasNext());
            }
        }
    }

    // =========================================================================
    // 6. DISTINCT combined with ORDER BY
    // =========================================================================

    @Nested
    @DisplayName("DISTINCT combined with ORDER BY")
    class DistinctWithOrderBy {

        @Test
        @DisplayName("SELECT DISTINCT with ORDER BY deduplicates before sorting")
        void selectDistinctWithOrderByDeduplicatesAndSorts() {
            // alice and carol share the same name; bob is unique
            insert(iri(EX + "alice"), iri(NAME), vf.createLiteral("Alice"));
            insert(iri(EX + "bob"),   iri(NAME), vf.createLiteral("Bob"));
            insert(iri(EX + "carol"), iri(NAME), vf.createLiteral("Alice")); // duplicate value

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT DISTINCT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ?name")) {
                List<String> names = collectSingleColumn(result, "name");
                assertEquals(List.of("Alice", "Bob"), names,
                        "DISTINCT must collapse duplicates; ORDER BY must then sort");
            }
        }

        @Test
        @DisplayName("SELECT DISTINCT with ORDER BY DESC produces deduplicated descending results")
        void selectDistinctWithOrderByDescDeduplicatesAndSortsDesc() {
            insert(iri(EX + "a"), iri(NAME), vf.createLiteral("Charlie"));
            insert(iri(EX + "b"), iri(NAME), vf.createLiteral("Alice"));
            insert(iri(EX + "c"), iri(NAME), vf.createLiteral("Charlie")); // duplicate

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT DISTINCT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY DESC(?name)")) {
                List<String> names = collectSingleColumn(result, "name");
                assertEquals(List.of("Charlie", "Alice"), names);
            }
        }
    }

    @Nested
    @DisplayName("ORDER BY with mixed RDF term types")
    class OrderByMixedTypes {

        @Test
        @DisplayName("ORDER BY places IRIs before plain literals (SPARQL order)")
        void orderByPlacesIrisBeforeLiterals() {
            insert(iri(EX + "r1"), iri(KNOWS), vf.createLiteral("a literal"));
            insert(iri(EX + "r1"), iri(KNOWS), iri(EX + "anIri"));

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT ?o WHERE { <" + EX + "r1> <" + KNOWS + "> ?o } ORDER BY ?o")) {
                List<String> values = collectSingleColumn(result, "o");
                assertEquals(2, values.size(), "Both IRI and literal must be returned");
                // IRI must come before literal per SPARQL ordering
                assertEquals(EX + "anIri", values.get(0), "IRI must sort before literal");
                assertEquals("a literal",   values.get(1), "Literal must sort after IRI");
            }
        }

        @Test
        @DisplayName("ORDER BY literals of the same plain type sorts lexicographically")
        void orderByLiteralsOfSamePlainTypeSortsLexicographically() {
            insert(iri(EX + "r1"), iri(NAME), vf.createLiteral("Zara"));
            insert(iri(EX + "r2"), iri(NAME), vf.createLiteral("Alice"));
            insert(iri(EX + "r3"), iri(NAME), vf.createLiteral("Mike"));

            try (TupleQueryResult result = executor.evaluateTuple(
                    "SELECT ?name WHERE { ?s <" + NAME + "> ?name } ORDER BY ?name")) {
                List<String> names = collectSingleColumn(result, "name");
                assertEquals(List.of("Alice", "Mike", "Zara"), names);
            }
        }
    }

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
            Value v = result.next().getValue(varName);
            values.add(v == null ? null : v.stringValue());
        }
        return values;
    }
}
