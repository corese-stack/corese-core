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
}
