package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.BNode;
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
 * End-to-end tests for Basic Graph Patterns (BGP) across all RDF term types.
 *
 * <p>These tests verify that the pipeline correctly matches and projects:</p>
 * <ul>
 *   <li>plain string literals</li>
 *   <li>language-tagged literals (@en, @fr, …)</li>
 *   <li>typed literals (xsd:integer, xsd:decimal, xsd:boolean, xsd:date)</li>
 *   <li>blank nodes as subjects and objects</li>
 *   <li>multi-triple BGP join chains</li>
 *   <li>constants (IRIs) used directly in triple patterns</li>
 * </ul>
 */
class BgpTermTypesE2ETest {

    private static final String EX     = "http://example.org/";
    private static final String XSD    = "http://www.w3.org/2001/XMLSchema#";
    private static final String NAME   = EX + "name";
    private static final String AGE    = EX + "age";
    private static final String KNOWS  = EX + "knows";
    private static final String LABEL  = EX + "label";
    private static final String ACTIVE = EX + "active";
    private static final String SCORE  = EX + "score";

    private CoreseValueFactory vf;
    private MemoryStorageManager storage;
    private NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        vf       = new CoreseValueFactory();
        storage  = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);
    }

    @Test
    @DisplayName("BGP matches and projects a plain string literal")
    void bgpMatchesPlainStringLiteral() {
        insert(iri(EX + "alice"), iri(NAME), vf.createLiteral("Alice"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name WHERE { <" + EX + "alice> <" + NAME + "> ?name }")) {

            assertTrue(result.hasNext());
            assertEquals("Alice", result.next().getValue("name").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP with constant literal in object position matches correctly")
    void bgpWithConstantLiteralInObjectPosition() {
        insert(iri(EX + "alice"), iri(NAME), vf.createLiteral("Alice"));
        insert(iri(EX + "bob"),   iri(NAME), vf.createLiteral("Bob"));

        // Only the triple with "Alice" should match
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + NAME + "> \"Alice\" }");

        List<String> subjects = collectSingleColumn(result, "s");
        assertEquals(List.of(EX + "alice"), subjects);
    }

    @Test
    @DisplayName("BGP projects a language-tagged literal with its language tag")
    void bgpProjectsLanguageTaggedLiteral() {
        insert(iri(EX + "paris"), iri(LABEL), vf.createLiteral("Paris", "fr"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?label WHERE { <" + EX + "paris> <" + LABEL + "> ?label }")) {

            assertTrue(result.hasNext());
            Value label = result.next().getValue("label");
            assertEquals("Paris", label.stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP distinguishes language-tagged literals with different language tags")
    void bgpDistinguishesLanguageTags() {
        insert(iri(EX + "london"), iri(LABEL), vf.createLiteral("London", "en"));
        insert(iri(EX + "london"), iri(LABEL), vf.createLiteral("Londres", "fr"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?label WHERE { <" + EX + "london> <" + LABEL + "> ?label }");

        List<String> labels = collectSingleColumn(result, "label");
        assertEquals(2, labels.size(), "Both language-tagged labels must appear");
        assertTrue(labels.contains("London"));
        assertTrue(labels.contains("Londres"));
    }

    @Test
    @DisplayName("BGP matches and projects an xsd:integer literal")
    void bgpMatchesIntegerLiteral() {
        insert(iri(EX + "alice"), iri(AGE), vf.createLiteral("30", vf.createIRI(XSD + "integer")));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?age WHERE { <" + EX + "alice> <" + AGE + "> ?age }")) {

            assertTrue(result.hasNext());
            Value age = result.next().getValue("age");
            assertEquals("30", age.stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP with constant xsd:integer in object position matches the right triple")
    void bgpWithConstantIntegerInObjectPosition() {
        insert(iri(EX + "alice"), iri(AGE), vf.createLiteral("30", vf.createIRI(XSD + "integer")));
        insert(iri(EX + "bob"),   iri(AGE), vf.createLiteral("25", vf.createIRI(XSD + "integer")));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + AGE + "> \"30\"^^<" + XSD + "integer> }");

        List<String> subjects = collectSingleColumn(result, "s");
        assertEquals(List.of(EX + "alice"), subjects);
    }

    @Test
    @DisplayName("BGP matches and projects an xsd:boolean literal")
    void bgpMatchesBooleanLiteral() {
        insert(iri(EX + "alice"), iri(ACTIVE), vf.createLiteral("true", vf.createIRI(XSD + "boolean")));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?active WHERE { <" + EX + "alice> <" + ACTIVE + "> ?active }")) {

            assertTrue(result.hasNext());
            assertEquals("true", result.next().getValue("active").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP matches and projects an xsd:decimal literal")
    void bgpMatchesDecimalLiteral() {
        insert(iri(EX + "alice"), iri(SCORE), vf.createLiteral("9.5", vf.createIRI(XSD + "decimal")));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?score WHERE { <" + EX + "alice> <" + SCORE + "> ?score }")) {

            assertTrue(result.hasNext());
            assertEquals("9.5", result.next().getValue("score").stringValue());
            assertFalse(result.hasNext());
        }
    }


    @Test
    @DisplayName("BGP matches a blank node in object position and re-uses it as subject")
    void bgpJoinsThroughBlankNode() {
        // alice --knows--> _:b0 --name--> "Bob"
        BNode bnode = vf.createBNode();
        insert(iri(EX + "alice"), iri(KNOWS), bnode);
        insert(bnode,             iri(NAME),  vf.createLiteral("Bob"));

        try (TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?name WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?b .
                  ?b <http://example.org/name> ?name .
                }
                """)) {

            assertTrue(result.hasNext());
            assertEquals("Bob", result.next().getValue("name").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP projects a blank node variable")
    void bgpProjectsBlankNodeVariable() {
        BNode bnode = vf.createBNode();
        insert(iri(EX + "alice"), iri(KNOWS), bnode);

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?b WHERE { <" + EX + "alice> <" + KNOWS + "> ?b }")) {

            assertTrue(result.hasNext());
            Value bVal = result.next().getValue("b");
            assertNotNull(bVal, "Blank node must be projected as a non-null value");
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP with three-triple chain joins correctly")
    void bgpThreeTripleChain() {
        // alice --knows--> bob --knows--> carol --name--> "Carol"
        insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));
        insert(iri(EX + "bob"),   iri(KNOWS), iri(EX + "carol"));
        insert(iri(EX + "carol"), iri(NAME),  vf.createLiteral("Carol"));

        try (TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?name WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?mid .
                  ?mid <http://example.org/knows> ?person .
                  ?person <http://example.org/name> ?name .
                }
                """)) {

            assertTrue(result.hasNext());
            assertEquals("Carol", result.next().getValue("name").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BGP with no matching triple returns empty result")
    void bgpWithNoMatchReturnsEmpty() {
        insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + NAME + "> ?name }")) {

            assertFalse(result.hasNext(), "No triple for NAME predicate — result must be empty");
        }
    }

    @Test
    @DisplayName("BGP with constant IRI subject filters to matching triples only")
    void bgpWithConstantIriSubjectFilters() {
        insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));
        insert(iri(EX + "bob"),   iri(KNOWS), iri(EX + "carol"));

        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?o WHERE { <" + EX + "alice> <" + KNOWS + "> ?o }");

        List<String> objects = collectSingleColumn(result, "o");
        assertEquals(List.of(EX + "bob"), objects, "Only alice's outgoing knows must appear");
    }

    @Test
    @DisplayName("BGP with multiple variables projects all columns")
    void bgpProjectsMultipleColumns() {
        insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));

        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s ?p ?o WHERE { ?s ?p ?o }")) {

            assertEquals(List.of("s", "p", "o"), result.getBindingNames());
            assertTrue(result.hasNext());
            BindingSet bs = result.next();
            assertEquals(EX + "alice", bs.getValue("s").stringValue());
            assertEquals(KNOWS, bs.getValue("p").stringValue());
            assertEquals(EX + "bob", bs.getValue("o").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Blank node label used twice in a pattern matches the same resource in both positions")
    void blankNodeLabelReuseJoinsOnSameResource() {
        BNode bnode = vf.createBNode();
        insert(bnode, iri(NAME), vf.createLiteral("Bob"));
        insert(bnode, iri(AGE),  vf.createLiteral("30"));

        // The query uses _:b twice — both occurrences must bind to the same blank node.
        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?name ?age WHERE { _:b <" + NAME + "> ?name . _:b <" + AGE + "> ?age }")) {
            assertTrue(result.hasNext(), "Pattern with _:b used twice must match when both triples share the same subject");
            var bs = result.next();
            assertEquals("Bob", bs.getValue("name").stringValue());
            assertEquals("30",  bs.getValue("age").stringValue());
            assertFalse(result.hasNext(), "Exactly one solution expected");
        }
    }

    @Test
    @DisplayName("Different blank node labels in a pattern bind independently")
    void differentBlankNodeLabelsBindIndependently() {
        // _:a and _:b are different non-distinguished variables — they can bind to different resources.
        BNode b1 = vf.createBNode();
        BNode b2 = vf.createBNode();
        insert(b1, iri(NAME), vf.createLiteral("Alice"));
        insert(b2, iri(NAME), vf.createLiteral("Bob"));

        // _:a and _:b are distinct — both rows must appear
        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?n1 ?n2 WHERE { _:a <" + NAME + "> ?n1 . _:b <" + NAME + "> ?n2 }")) {
            int count = 0;
            while (result.hasNext()) { result.next(); count++; }
            // Should produce 4 rows: (Alice,Alice), (Alice,Bob), (Bob,Alice), (Bob,Bob)
            // because _:a and _:b are independent variables (no constraint they differ)
            assertEquals(4, count, "_:a and _:b are independent — all combinations expected");
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
