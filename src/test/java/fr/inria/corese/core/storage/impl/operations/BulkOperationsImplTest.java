package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BulkOperationsImpl.
 */
@DisplayName("BulkOperationsImpl Tests")
class BulkOperationsImplTest {

    private BulkOperationsImpl bulkOperations;
    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        bulkOperations = new BulkOperationsImpl(graph);
    }

    @Test
    @DisplayName("Constructor with null graph should throw IllegalArgumentException")
    void testConstructorWithNullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> new BulkOperationsImpl(null));
    }

    @Test
    @DisplayName("Insert batch of 5 edges should succeed")
    void testInsertBatchSuccess() throws DataManagerException {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            edges.add(graph.create(graph.getDefaultGraphNode(), s, p, o));
        }

        MutationResult result = bulkOperations.insertBatch(edges);

        assertTrue(result.isBulk());
        assertEquals(5, result.getTotalAttempted());
        assertEquals(5, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(5, graph.size());
    }

    @Test
    @DisplayName("Insert batch with empty list should throw IllegalArgumentException")
    void testInsertBatchWithEmptyListThrows() {
        assertThrows(IllegalArgumentException.class, () -> bulkOperations.insertBatch(new ArrayList<>()));
    }

    @Test
    @DisplayName("Insert batch with null list should throw IllegalArgumentException")
    void testInsertBatchWithNullListThrows() {
        assertThrows(IllegalArgumentException.class, () -> bulkOperations.insertBatch(null));
    }

    @Test
    @DisplayName("Delete batch of 3 edges should succeed")
    void testDeleteBatchSuccess() throws DataManagerException {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            edges.add(graph.addEdge(s, p, o));
        }

        MutationResult result = bulkOperations.deleteBatch(edges);

        assertTrue(result.isBulk());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, graph.size());
    }


    @Test
    @DisplayName("Clear all should remove all edges from graph")
    void testClearAll() throws DataManagerException {
        // Add some data
        for (int i = 0; i < 10; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            graph.addEdge(s, p, o);
        }

        MutationResult result = bulkOperations.clearAll();

        assertTrue(result.isBulk());
        assertEquals(0, graph.size());
    }

    @Test
    @DisplayName("Declare context should succeed")
    void testDeclareContext() throws DataManagerException {
        Node context = graph.addResource("http://example.org/context");

        MutationResult result = bulkOperations.declareContext(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Declare context with null should throw IllegalArgumentException")
    void testDeclareContextWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> bulkOperations.declareContext(null));
    }

    @Test
    @DisplayName("Undeclare context should remove context and its edges")
    void testUndeclareContext() throws DataManagerException {
        Node context = graph.addResource("http://example.org/context");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p, o, context);

        MutationResult result = bulkOperations.undeclareContext(context);

        assertTrue(result.isBulk());
    }

    @Test
    @DisplayName("Insert batch with partial failure should track failures")
    void testInsertBatchPartialFailure() throws DataManagerException {
        List<Edge> edges = new ArrayList<>();

        // Valid edge
        Node s1 = graph.addResource("http://example.org/s1");
        Node p = graph.addProperty("http://example.org/p");
        Node o1 = graph.addResource("http://example.org/o1");
        edges.add(graph.create(graph.getDefaultGraphNode(), s1, p, o1));

        graph.addEdge(s1, p, o1);

        MutationResult result = bulkOperations.insertBatch(edges);

        assertEquals(1, result.getTotalAttempted());
    }

    @Test
    @DisplayName("Success rate should be correctly calculated")
    void testGetSuccessRate() throws DataManagerException {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            edges.add(graph.create(graph.getDefaultGraphNode(), s, p, o));
        }

        MutationResult result = bulkOperations.insertBatch(edges);

        assertEquals(1.0, result.getSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("Large batch insert (100 edges) should succeed")
    void testLargeBatchInsert() throws DataManagerException {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            edges.add(graph.create(graph.getDefaultGraphNode(), s, p, o));
        }

        MutationResult result = bulkOperations.insertBatch(edges);

        assertTrue(result.isCompleteSuccess());
        assertEquals(100, result.getSuccessCount());
        assertEquals(100, graph.size());
    }


    @Test
    @DisplayName("Clear all on empty graph should succeed with no deletions")
    void testClearAllOnEmptyGraph() throws DataManagerException {
        MutationResult result = bulkOperations.clearAll();

        assertTrue(result.isBulk());
        assertEquals(0, graph.size());
    }

    @Test
    @DisplayName("Multiple batch operations should all succeed")
    void testMultipleBatchOperations() throws DataManagerException {
        // First batch
        List<Edge> batch1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p1");
            Node o = graph.addResource("http://example.org/o" + i);
            batch1.add(graph.create(graph.getDefaultGraphNode(), s, p, o));
        }
        bulkOperations.insertBatch(batch1);

        assertEquals(5, graph.size());

        // Second batch
        List<Edge> batch2 = new ArrayList<>();
        for (int i = 5; i < 10; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p2");
            Node o = graph.addResource("http://example.org/o" + i);
            batch2.add(graph.create(graph.getDefaultGraphNode(), s, p, o));
        }
        bulkOperations.insertBatch(batch2);

        assertEquals(10, graph.size());
    }
}