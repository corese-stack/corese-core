package fr.inria.corese.core.storage;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.config.DataManagerConfig;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.model.EdgePattern;
import fr.inria.corese.core.storage.api.dataManager.support.model.GraphStatistics;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CoresGraphDataManager.
 *
 */
@DisplayName("CoreseGraphDataManager Integration Tests")
class CoreseGraphDataManagerIntegrationTest {

    private CoreseGraphDataManager dataManager;

    @BeforeEach
    void setUp() throws DataManagerException {
        dataManager = new CoreseGraphDataManagerBuilder().build();

        // Initialize with transaction support enabled
        DataManagerConfig config = DataManagerConfig.builder()
                .debug(false)
                .transactionSupport(true)
                .build();
        dataManager.getLifecycle().initialize(config);
    }

    @Test
    @DisplayName("Bulk insert of 100 edges should succeed")
    void testBulkOperations() throws DataManagerException {
        java.util.List<Edge> edges = new java.util.ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Node s = dataManager.getGraph().addResource("http://example.org/s" + i);
            Node p = dataManager.getGraph().addProperty("http://example.org/p");
            Node o = dataManager.getGraph().addResource("http://example.org/o" + i);
            edges.add(dataManager.getGraph().create(dataManager.getGraph().getDefaultGraphNode(), s, p, o));
        }

        MutationResult result = dataManager.getBulkOperations().insertBatch(edges);

        assertTrue(result.isCompleteSuccess());
        assertEquals(100, result.getSuccessCount());
        assertEquals(100, dataManager.getGraph().size());
    }

    @Test
    @DisplayName("Metadata operations should return correct statistics")
    void testMetadataOperations() throws DataManagerException {
        // Add test data
        Node s = dataManager.getGraph().addResource("http://example.org/subject");
        Node p1 = dataManager.getGraph().addProperty("http://example.org/p1");
        Node p2 = dataManager.getGraph().addProperty("http://example.org/p2");
        Node o = dataManager.getGraph().addResource("http://example.org/object");

        Edge edge1 = dataManager.getGraph().create(dataManager.getGraph().getDefaultGraphNode(), s, p1, o);
        Edge edge2 = dataManager.getGraph().create(dataManager.getGraph().getDefaultGraphNode(), s, p2, o);

        dataManager.getMutationOperations().insertEdge(edge1);
        dataManager.getMutationOperations().insertEdge(edge2);

        // Get metadata
        Set<Node> predicates = dataManager.getMetadataOperations().getAllPredicates();
        assertTrue(predicates.size() >= 2);

        GraphStatistics stats = dataManager.getMetadataOperations().getStatistics();
        assertEquals(2, stats.getEdgeCount());
        assertTrue(stats.getNodeCount() >= 2);
        assertTrue(stats.getPredicateCount() >= 2);
    }

    @Test
    @DisplayName("Query with predicate pattern should find matching edges")
    void testQueryWithPattern() throws DataManagerException {
        // Add test data
        Node s = dataManager.getGraph().addResource("http://example.org/Alice");
        Node p = dataManager.getGraph().addProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Node o = dataManager.getGraph().addResource("http://example.org/Person");
        Edge edge = dataManager.getGraph().create(dataManager.getGraph().getDefaultGraphNode(), s, p, o);

        dataManager.getMutationOperations().insertEdge(edge);

        // Query with pattern
        EdgePattern pattern = EdgePattern.builder()
                .predicate(p)
                .build();

        try (Stream<Edge> edges = dataManager.getQueryOperations().query(pattern)) {
            long count = edges.count();
            assertEquals(1, count);
        }
    }

    @Test
    @DisplayName("Clear all data should empty the graph")
    void testClearAllData() throws DataManagerException {
        // Add data
        for (int i = 0; i < 10; i++) {
            Node s = dataManager.getGraph().addResource("http://example.org/s" + i);
            Node p = dataManager.getGraph().addProperty("http://example.org/p");
            Node o = dataManager.getGraph().addResource("http://example.org/o" + i);
            dataManager.getMutationOperations().insertEdge(s, p, o, null);
        }

        assertTrue(dataManager.getGraph().size() > 0);

        // Clear all
        MutationResult result = dataManager.getBulkOperations().clearAll();
        // For bulk operations, check if there were deletions
        assertTrue(result.getTotalAttempted() > 0 || dataManager.getGraph().size() == 0);

        assertEquals(0, dataManager.getGraph().size());
    }

    @Test
    @DisplayName("Lifecycle shutdown should properly clean up resources")
    void testLifecycleShutdown() throws DataManagerException {
        assertTrue(dataManager.getLifecycle().isInitialized());

        dataManager.getLifecycle().shutdown();

        assertFalse(dataManager.getLifecycle().isInitialized());
    }
}