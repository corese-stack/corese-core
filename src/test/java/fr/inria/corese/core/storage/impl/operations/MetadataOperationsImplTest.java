package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.model.GraphStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetadataOperationsImpl.
 */
@DisplayName("MetadataOperationsImpl Tests")
class MetadataOperationsImplTest {

    private MetadataOperationsImpl metadataOperations;
    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        metadataOperations = new MetadataOperationsImpl(graph);
    }

    @Test
    @DisplayName("Constructor with null graph should throw IllegalArgumentException")
    void testConstructorWithNullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MetadataOperationsImpl(null));
    }

    @Test
    @DisplayName("Get all predicates on empty graph should return empty set")
    void testGetAllPredicatesOnEmptyGraph() throws DataManagerException {
        Set<Node> predicates = metadataOperations.getAllPredicates();

        assertNotNull(predicates);
        assertTrue(predicates.isEmpty());
    }


    @Test
    @DisplayName("Get all predicates should return all unique predicates")
    void testGetAllPredicates() throws DataManagerException {
        // Add test data with different predicates
        Node s = graph.addResource("http://example.org/subject");
        Node p1 = graph.addProperty("http://example.org/predicate1");
        Node p2 = graph.addProperty("http://example.org/predicate2");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p1, o);
        graph.addEdge(s, p2, o);

        Set<Node> predicates = metadataOperations.getAllPredicates();

        assertNotNull(predicates);
        assertTrue(predicates.size() >= 2);
        assertTrue(predicates.contains(p1));
        assertTrue(predicates.contains(p2));
    }

    @Test
    @DisplayName("Get predicates for specific context")
    void testGetPredicatesForContext() throws DataManagerException {
        Node context = graph.addGraph("http://example.org/graph1");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(context, s, p, o);

        Set<Node> predicates = metadataOperations.getPredicates(context);

        assertNotNull(predicates);
        assertFalse(predicates.isEmpty());
        assertTrue(predicates.contains(p));
    }

    @Test
    @DisplayName("Get all nodes on empty graph should return empty set")
    void testGetAllNodesOnEmptyGraph() throws DataManagerException {
        Set<Node> nodes = metadataOperations.getAllNodes();

        assertNotNull(nodes);
        assertTrue(nodes.isEmpty());
    }

    @Test
    @DisplayName("Get all nodes should return all subjects and objects")
    void testGetAllNodes() throws DataManagerException {
        Node s1 = graph.addResource("http://example.org/subject1");
        Node s2 = graph.addResource("http://example.org/subject2");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o1 = graph.addResource("http://example.org/object1");
        Node o2 = graph.addResource("http://example.org/object2");

        graph.addEdge(s1, p, o1);
        graph.addEdge(s2, p, o2);

        Set<Node> nodes = metadataOperations.getAllNodes();

        assertNotNull(nodes);
        assertTrue(nodes.size() >= 4);
        assertTrue(nodes.contains(s1));
        assertTrue(nodes.contains(s2));
        assertTrue(nodes.contains(o1));
        assertTrue(nodes.contains(o2));
    }

    @Test
    @DisplayName("Get nodes for specific context")
    void testGetNodesForContext() throws DataManagerException {
        Node context = graph.addGraph("http://example.org/graph1");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(context, s, p, o);

        Set<Node> nodes = metadataOperations.getNodes(context);

        assertNotNull(nodes);
        assertTrue(nodes.size() >= 2);
        assertTrue(nodes.contains(s));
        assertTrue(nodes.contains(o));
    }

    @Test
    @DisplayName("Get contexts on empty graph should return empty set")
    void testGetContextsOnEmptyGraph() throws DataManagerException {
        Set<Node> contexts = metadataOperations.getContexts();

        assertNotNull(contexts);
        // May contain default graph
        assertTrue(contexts.isEmpty() || contexts.size() == 1);
    }

    @Test
    @DisplayName("Get contexts should return all named graphs")
    void testGetContexts() throws DataManagerException {
        Node context1 = graph.addGraph("http://example.org/graph1");
        Node context2 = graph.addGraph("http://example.org/graph2");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(context1, s, p, o);
        graph.addEdge(context2, s, p, o);

        Set<Node> contexts = metadataOperations.getContexts();

        assertNotNull(contexts);
        assertTrue(contexts.size() >= 2);
        assertTrue(contexts.contains(context1));
        assertTrue(contexts.contains(context2));
    }

    @Test
    @DisplayName("Get statistics on empty graph should return zero counts")
    void testGetStatisticsOnEmptyGraph() throws DataManagerException {
        GraphStatistics stats = metadataOperations.getStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.getEdgeCount());
        assertEquals(0, stats.getNodeCount());
        assertEquals(0, stats.getPredicateCount());
        assertTrue(stats.isEmpty());
    }

    @Test
    @DisplayName("Get statistics should return correct counts")
    void testGetStatistics() throws DataManagerException {
        // Add test data
        Node s1 = graph.addResource("http://example.org/s1");
        Node s2 = graph.addResource("http://example.org/s2");
        Node p1 = graph.addProperty("http://example.org/p1");
        Node p2 = graph.addProperty("http://example.org/p2");
        Node o1 = graph.addResource("http://example.org/o1");
        Node o2 = graph.addResource("http://example.org/o2");

        graph.addEdge(s1, p1, o1);
        graph.addEdge(s1, p2, o2);
        graph.addEdge(s2, p1, o1);

        GraphStatistics stats = metadataOperations.getStatistics();

        assertNotNull(stats);
        assertEquals(3, stats.getEdgeCount());
        assertTrue(stats.getNodeCount() >= 4);
        assertTrue(stats.getPredicateCount() >= 2);
        assertFalse(stats.isEmpty());
        assertTrue(stats.hasData());
    }

    @Test
    @DisplayName("Get statistics should calculate density")
    void testGetStatisticsWithDensity() throws DataManagerException {
        // Add some edges
        for (int i = 0; i < 5; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            graph.addEdge(s, p, o);
        }

        GraphStatistics stats = metadataOperations.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.getEdgeCount() > 0);
        assertTrue(stats.getDensity() >= 0.0);
        assertTrue(stats.getDensity() <= 1.0);
    }

    @Test
    @DisplayName("Get statistics should calculate average degree")
    void testGetStatisticsWithAverageDegree() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");

        for (int i = 0; i < 3; i++) {
            Node o = graph.addResource("http://example.org/o" + i);
            graph.addEdge(s, p, o);
        }

        GraphStatistics stats = metadataOperations.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.getAverageDegree() > 0.0);
    }

    @Test
    @DisplayName("Get predicates with null context should return all predicates")
    void testGetPredicatesWithNullContext() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p, o);

        Set<Node> predicates = metadataOperations.getPredicates(null);

        assertNotNull(predicates);
        assertTrue(predicates.contains(p));
    }

    @Test
    @DisplayName("Get nodes with null context should return all nodes")
    void testGetNodesWithNullContext() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p, o);

        Set<Node> nodes = metadataOperations.getNodes(null);

        assertNotNull(nodes);
        assertTrue(nodes.contains(s));
        assertTrue(nodes.contains(o));
    }

    @Test
    @DisplayName("Multiple contexts should be tracked separately")
    void testMultipleContexts() throws DataManagerException {
        Node ctx1 = graph.addGraph("http://example.org/graph1");
        Node ctx2 = graph.addGraph("http://example.org/graph2");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(ctx1, s, p, o);
        graph.addEdge(ctx2, s, p, o);

        Set<Node> contexts = metadataOperations.getContexts();

        assertNotNull(contexts);
        assertTrue(contexts.contains(ctx1));
        assertTrue(contexts.contains(ctx2));
    }

    @Test
    @DisplayName("Statistics should track context count")
    void testStatisticsContextCount() throws DataManagerException {
        Node ctx1 = graph.addGraph("http://example.org/graph1");
        Node ctx2 = graph.addGraph("http://example.org/graph2");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(ctx1, s, p, o);
        graph.addEdge(ctx2, s, p, o);

        GraphStatistics stats = metadataOperations.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.getContextCount() >= 2);
    }

    @Test
    @DisplayName("Returned sets should be unmodifiable")
    void testReturnedSetsAreUnmodifiable() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p, o);

        Set<Node> predicates = metadataOperations.getAllPredicates();

        assertThrows(UnsupportedOperationException.class, () -> predicates.add(graph.addProperty("http://example.org/another")));
    }
}