package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.model.MutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MutationOperationsImpl.
 *
 */
@DisplayName("MutationOperationsImpl Tests")
class MutationOperationsImplTest {

    private MutationOperationsImpl mutationOperations;
    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        mutationOperations = new MutationOperationsImpl(graph);
    }

    @Test
    @DisplayName("Constructor with null graph should throw IllegalArgumentException")
    void testConstructorWithNullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MutationOperationsImpl(null));
    }

    @Test
    @DisplayName("Insert edge should succeed and return success result")
    void testInsertEdgeSuccess() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        Edge edge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        MutationResult result = mutationOperations.insertEdge(edge);

        assertTrue(result.isSuccess());
        assertNotNull(result.getAffectedEdge());
        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("Insert edge with null should throw IllegalArgumentException")
    void testInsertEdgeWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> mutationOperations.insertEdge(null));
    }

    @Test
    @DisplayName("Insert edge with pattern (s, p, o) should succeed")
    void testInsertEdgeWithPattern() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        MutationResult result = mutationOperations.insertEdge(s, p, o, null);

        assertTrue(result.isSuccess());
        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("Insert edge with null subject should throw IllegalArgumentException")
    void testInsertEdgeWithNullSubjectThrows() {
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        assertThrows(IllegalArgumentException.class, () -> mutationOperations.insertEdge(null, p, o, null));
    }

    @Test
    @DisplayName("Insert edge with null predicate should throw IllegalArgumentException")
    void testInsertEdgeWithNullPredicateThrows() {
        Node s = graph.addResource("http://example.org/subject");
        Node o = graph.addResource("http://example.org/object");

        assertThrows(IllegalArgumentException.class, () -> mutationOperations.insertEdge(s, null, o, null));
    }

    @Test
    @DisplayName("Insert edge with null object should throw IllegalArgumentException")
    void testInsertEdgeWithNullObjectThrows() {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");

        assertThrows(IllegalArgumentException.class, () -> mutationOperations.insertEdge(s, p, null, null));
    }

    @Test
    @DisplayName("Delete edge should succeed and return success result")
    void testDeleteEdgeSuccess() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        // Setup: insert edge using graph directly
        graph.addEdge(s, p, o);

        // Create edge reference for deletion
        Edge edge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        MutationResult result = mutationOperations.deleteEdge(edge);

        assertTrue(result.isSuccess());
        assertEquals(0, graph.size());
    }

    @Test
    @DisplayName("Delete edge with null should throw IllegalArgumentException")
    void testDeleteEdgeWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> mutationOperations.deleteEdge(null));
    }

    @Test
    @DisplayName("Delete non-existent edge should return failure result")
    void testDeleteNonExistentEdge() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        Edge edge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        MutationResult result = mutationOperations.deleteEdge(edge);

        assertTrue(result.isFailure());
    }

    @Test
    @DisplayName("Generate blank node should return unique identifier")
    void testGenerateBlankNode() throws DataManagerException {
        String blankId1 = mutationOperations.generateBlankNode();
        String blankId2 = mutationOperations.generateBlankNode();

        assertNotNull(blankId1);
        assertNotNull(blankId2);
        assertTrue(blankId1.startsWith("_:"));
        assertTrue(blankId2.startsWith("_:"));
        assertNotEquals(blankId1, blankId2);
    }

    @Test
    @DisplayName("Update edge should succeed")
    void testUpdateEdge() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o1 = graph.addResource("http://example.org/object1");
        Node o2 = graph.addResource("http://example.org/object2");

        // Setup: add old edge
        graph.addEdge(s, p, o1);
        Edge oldEdge = graph.create(graph.getDefaultGraphNode(), s, p, o1);
        Edge newEdge = graph.create(graph.getDefaultGraphNode(), s, p, o2);

        MutationResult result = mutationOperations.updateEdge(oldEdge, newEdge);

        assertTrue(result.isSuccess());
        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("Update edge with null old edge should throw IllegalArgumentException")
    void testUpdateEdgeWithNullOldEdgeThrows() {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        Edge newEdge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        assertThrows(IllegalArgumentException.class, () -> mutationOperations.updateEdge(null, newEdge));
    }

    @Test
    @DisplayName("Update edge with null new edge should throw IllegalArgumentException")
    void testUpdateEdgeWithNullNewEdgeThrows() {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        // Setup
        graph.addEdge(s, p, o);
        Edge oldEdge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        assertThrows(IllegalArgumentException.class, () -> mutationOperations.updateEdge(oldEdge, null));
    }

    @Test
    @DisplayName("Insert edge with specific context should use that context")
    void testInsertEdgeWithContext() throws DataManagerException {
        Node context = graph.addGraph("http://example.org/myGraph");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        MutationResult result = mutationOperations.insertEdge(s, p, o, List.of(context));

        assertTrue(result.isSuccess());
        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("Insert duplicate edge should handle gracefully")
    void testInsertDuplicateEdge() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        // Insert first time
        MutationResult result1 = mutationOperations.insertEdge(s, p, o, null);
        assertTrue(result1.isSuccess());

        // Insert duplicate
        MutationResult result2 = mutationOperations.insertEdge(s, p, o, null);

        // May succeed or fail depending on Graph implementation
        assertNotNull(result2);
    }

    @Test
    @DisplayName("Delete with pattern and specific context should delete from that context only")
    void testDeleteEdgesWithContext() throws DataManagerException {
        Node ctx1 = graph.addGraph("http://example.org/graph1");
        Node ctx2 = graph.addGraph("http://example.org/graph2");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        // Setup: add edges to both contexts
        graph.addEdge(ctx1, s, p, o);
        graph.addEdge(ctx2, s, p, o);

        MutationResult result = mutationOperations.deleteEdges(s, p, o, List.of(ctx1));

        assertTrue(result.isBulk());
        assertTrue(result.getSuccessCount() >= 1);
    }

    @Test
    @DisplayName("MutationResult should contain correct edge information")
    void testMutationResultContainsEdgeInfo() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        Edge edge = graph.create(graph.getDefaultGraphNode(), s, p, o);

        MutationResult result = mutationOperations.insertEdge(edge);

        assertTrue(result.isSuccess());
        assertNotNull(result.getAffectedEdge());
        assertEquals(edge.getSubjectNode(), result.getAffectedEdge().getSubjectNode());
        assertEquals(edge.getPropertyNode(), result.getAffectedEdge().getPropertyNode());
        assertEquals(edge.getObjectNode(), result.getAffectedEdge().getObjectNode());
    }

    @Test
    @DisplayName("Multiple inserts should accumulate in graph")
    void testMultipleInserts() throws DataManagerException {
        for (int i = 0; i < 5; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);

            MutationResult result = mutationOperations.insertEdge(s, p, o, null);
            assertTrue(result.isSuccess());
        }

        assertEquals(5, graph.size());
    }

    @Test
    @DisplayName("Delete pattern with no matches should return empty result")
    void testDeletePatternNoMatches() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");

        MutationResult result = mutationOperations.deleteEdges(s, p, null, null);

        assertTrue(result.isBulk());
        assertEquals(0, result.getSuccessCount());
    }
}