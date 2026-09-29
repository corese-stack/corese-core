package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CONSTRUCT blank-node handling.
 *
 * <p>SPARQL 1.1 §10.1.3: A blank-node label in a CONSTRUCT template is an
 * <em>existential</em> variable — each solution produces a fresh blank node,
 * but occurrences of the <em>same label</em> within one solution all map to
 * the same blank node.</p>
 */
@DisplayName("CONSTRUCT - blank-node per-solution semantics")
class ConstructBlankNodesTest extends PipelineTestSupport {

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        // Add a second triple so we get two solutions
        insert(iri(ALICE), iri(KNOWS), iri("http://example.org/carol"));
    }

    private List<Statement> collect(String sparql) {
        List<Statement> results = new ArrayList<>();
        try (GraphQueryResult r = executor.evaluateGraph(sparql)) {
            while (r.hasNext()) results.add(r.next());
        }
        return results;
    }

    @Test
    @DisplayName("different solutions produce different blank nodes for the same label")
    void differentSolutionsProduceDifferentBlankNodes() {
        // Two solutions (alice→bob, alice→carol): each solution gets its own _:b
        List<Statement> stmts = collect("""
                CONSTRUCT { _:b <http://example.org/knows> ?o }
                WHERE     { ?s <http://example.org/knows> ?o }
                """);

        assertEquals(2, stmts.size(), "Two solutions must produce two statements");

        BNode b1 = assertInstanceOf(BNode.class, stmts.get(0).getSubject());
        BNode b2 = assertInstanceOf(BNode.class, stmts.get(1).getSubject());
        assertNotEquals(b1.getLabel(), b2.getLabel(),
                "Blank nodes from different solutions must be distinct");
    }

    @Test
    @DisplayName("same blank-node label used twice in one template solution maps to one node")
    void sameLabelWithinOneSolutionMapsToSameBlankNode() {
        // One solution; _:b appears as both subject and object of different triples
        List<Statement> stmts = collect("""
                CONSTRUCT {
                  _:b <http://example.org/knows> ?o .
                  <http://example.org/alice>  <http://example.org/has> _:b
                }
                WHERE { <http://example.org/alice> <http://example.org/knows> ?o }
                LIMIT 1
                """);

        assertEquals(2, stmts.size(), "Template with two triples must produce two statements");

        // First triple: _:b is subject
        BNode bAsSubject = assertInstanceOf(BNode.class, stmts.get(0).getSubject());
        // Second triple: _:b is object
        BNode bAsObject = assertInstanceOf(BNode.class, stmts.get(1).getObject());

        assertEquals(bAsSubject.getLabel(), bAsObject.getLabel(),
                "The same blank-node label within one solution must resolve to the same blank node");
    }

    @Test
    @DisplayName("RDF collection in CONSTRUCT template produces fresh rdf:first/rdf:rest chains per solution")
    void collectionTemplateFreshChainsPerSolution() {
        // setUp inserts (alice, knows, bob); @BeforeEach adds (alice, knows, carol) → 2 solutions
        final String RDF_FIRST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#first";
        final String RDF_REST  = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest";
        final String RDF_NIL   = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";
        final String PROP      = "http://example.org/prop";

        List<Statement> stmts = collect("""
                PREFIX ex: <http://example.org/>
                CONSTRUCT { (?s ?o) ex:prop ?p }
                WHERE     { ?s ?p ?o }
                """);

        // 2 solutions × 5 triples (4 list-structure + 1 ex:prop) = 10
        assertEquals(10, stmts.size(), "2 solutions × 5 triples each = 10 total");

        // Extract the two list-head blank-node labels (subjects of ex:prop triples)
        List<String> headLabels = stmts.stream()
                .filter(s -> PROP.equals(s.getPredicate().stringValue()))
                .map(s -> assertInstanceOf(BNode.class, s.getSubject(),
                        "list-head subject must be a blank node").getLabel())
                .toList();

        assertEquals(2, headLabels.size());
        assertNotEquals(headLabels.get(0), headLabels.get(1),
                "Two solutions must produce distinct list-head blank nodes");

        // Each head must carry exactly one rdf:first and one rdf:rest
        for (String headLabel : headLabels) {
            long firstCount = stmts.stream()
                    .filter(s -> s.getSubject() instanceof BNode b && b.getLabel().equals(headLabel)
                              && RDF_FIRST.equals(s.getPredicate().stringValue()))
                    .count();
            long restCount = stmts.stream()
                    .filter(s -> s.getSubject() instanceof BNode b && b.getLabel().equals(headLabel)
                              && RDF_REST.equals(s.getPredicate().stringValue()))
                    .count();
            assertEquals(1, firstCount, "Each list-head must have exactly one rdf:first");
            assertEquals(1, restCount,  "Each list-head must have exactly one rdf:rest");
        }

        // Tails (rdf:rest targets from heads) must also be distinct blank nodes
        List<String> tailLabels = stmts.stream()
                .filter(s -> s.getSubject() instanceof BNode b && headLabels.contains(b.getLabel())
                          && RDF_REST.equals(s.getPredicate().stringValue()))
                .map(s -> assertInstanceOf(BNode.class, s.getObject(),
                        "rdf:rest must point to a blank node tail").getLabel())
                .toList();

        assertEquals(2, tailLabels.size());
        assertNotEquals(tailLabels.get(0), tailLabels.get(1),
                "Two solutions must produce distinct list-tail blank nodes");

        // Each tail must terminate the chain with rdf:rest = rdf:nil
        for (String tailLabel : tailLabels) {
            boolean terminatesWithNil = stmts.stream()
                    .anyMatch(s -> s.getSubject() instanceof BNode b && b.getLabel().equals(tailLabel)
                              && RDF_REST.equals(s.getPredicate().stringValue())
                              && RDF_NIL.equals(s.getObject().stringValue()));
            assertTrue(terminatesWithNil, "List tail must terminate with rdf:rest rdf:nil");
        }
    }

    @Test
    @DisplayName("CONSTRUCT with only variables produces concrete IRI statements")
    void constructWithVariablesProducesConcreteStatements() {
        List<Statement> stmts = collect("""
                CONSTRUCT { ?s <http://example.org/knows> ?o }
                WHERE     { ?s <http://example.org/knows> ?o }
                LIMIT 1
                """);

        assertEquals(1, stmts.size());
        assertNotNull(stmts.getFirst().getSubject());
        assertNotNull(stmts.getFirst().getObject());
    }

    @Test
    void generatedBlankNodesDoNotAliasDatasetNodes() {
        for (int i = 0; i < 32; i++) {
            insert(valueFactory.createBNode("cb" + i), iri(KNOWS), iri(BOB));
        }
        var statements = collect("CONSTRUCT { _:b <http://example.org/link> ?s } WHERE { ?s ?p ?o }");
        for (Statement statement : statements) {
            assertNotEquals(statement.getSubject(), statement.getObject());
        }
        assertEquals(statements.size(), statements.stream().map(Statement::getSubject).distinct().count());
    }

    @Test
    void expandsNestedCollections() {
        var statements = collect("CONSTRUCT { ((<http://example.org/a>)) <http://example.org/p> <http://example.org/o> } WHERE {}");
        assertEquals(5, statements.size());
        var firstValues = statements.stream()
                .filter(s -> s.getPredicate().stringValue().endsWith("#first"))
                .map(Statement::getObject).toList();
        assertEquals(2, firstValues.size());
        assertTrue(firstValues.contains(iri("http://example.org/a")));
        assertEquals(1, firstValues.stream().filter(BNode.class::isInstance).count());
    }

    @Test
    void expandsNestedPropertyListsInObjectPosition() {
        var statements = collect("CONSTRUCT { <http://example.org/s> <http://example.org/p> [ <http://example.org/q> [ <http://example.org/r> 1 ] ] } WHERE {}");
        assertEquals(3, statements.size());
        var outer = statements.stream().filter(s -> s.getSubject().equals(iri("http://example.org/s"))).findFirst().orElseThrow();
        var middle = statements.stream().filter(s -> s.getSubject().equals(outer.getObject())).findFirst().orElseThrow();
        assertInstanceOf(BNode.class, middle.getObject());
        assertTrue(statements.stream().anyMatch(s -> s.getSubject().equals(middle.getObject())));
    }

    @Test
    void decodesUnicodeIriInTemplateAndWhere() {
        var statements = collect("CONSTRUCT { ?s <http://example.org/\\u0041> ?o } WHERE { ?s <http://example.org/\\u006bnows> ?o }");
        assertEquals(2, statements.size());
        assertTrue(statements.stream().allMatch(s -> s.getPredicate().equals(iri("http://example.org/A"))));
    }

    @Test
    void preservesEscapedBackslashBeforeSurrogateText() {
        var statements = collect("CONSTRUCT { <http://example.org/s> <http://example.org/p> \"\\\\uD800\" } WHERE {}");
        assertEquals(1, statements.size());
        assertEquals("\\uD800", statements.getFirst().getObject().stringValue());
    }

    @Test
    void decodesPrologueBeforeResolvingRelativeIris() {
        for (String query : List.of(
                "BASE <\\u0068ttp://example.org/> CONSTRUCT { <s> <p> <o> } WHERE {}",
                "PREFIX ex: <\\u0068ttp://example.org/> CONSTRUCT { ex:s ex:p ex:o } WHERE {}",
                "BASE <http://example.org/\\u0041/> PREFIX ex: <../> CONSTRUCT { ex:s ex:p ex:o } WHERE {}")) {
            var statements = collect(query);
            assertEquals(1, statements.size(), query);
            assertEquals(iri("http://example.org/s"), statements.getFirst().getSubject(), query);
            assertEquals(iri("http://example.org/p"), statements.getFirst().getPredicate(), query);
            assertEquals(iri("http://example.org/o"), statements.getFirst().getObject(), query);
        }
    }
}
