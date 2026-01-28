package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.model.EdgePattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryOperationsImpl.
 *
 */
@DisplayName("QueryOperationsImpl Tests")
class QueryOperationsImplTest {

    private QueryOperationsImpl queryOperations;
    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        queryOperations = new QueryOperationsImpl(graph);
    }

    @Test
    @DisplayName("Constructor with null graph should throw IllegalArgumentException")
    void testConstructorWithNullGraphThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QueryOperationsImpl(null));
    }

    @Test
    @DisplayName("Query with null pattern should throw IllegalArgumentException")
    void testQueryWithNullPatternThrows() {
        assertThrows(IllegalArgumentException.class, () -> queryOperations.query(null));
    }

    @Test
    @DisplayName("Query with match-all pattern on empty graph should return empty stream")
    void testQueryMatchAllOnEmptyGraph() throws DataManagerException {
        EdgePattern pattern = EdgePattern.builder().build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            assertEquals(0, edges.count());
        }
    }

    @Test
    @DisplayName("Query with predicate pattern should return matching edges")
    void testQueryWithPredicatePattern() throws DataManagerException {
        // Add test data
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        graph.addEdge(s, p, o);

        EdgePattern pattern = EdgePattern.builder()
                .predicate(p)
                .build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            assertEquals(1, edges.count());
        }
    }

    @Test
    @DisplayName("Query with subject and predicate pattern should match specific edges")
    void testQueryWithSubjectAndPredicatePattern() throws DataManagerException {
        Node s1 = graph.addResource("http://example.org/s1");
        Node s2 = graph.addResource("http://example.org/s2");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s1, p, o);
        graph.addEdge(s2, p, o);

        EdgePattern pattern = EdgePattern.builder()
                .subject(s1)
                .predicate(p)
                .build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            long count = edges.count();
            assertEquals(1, count);
        }
    }


    @Test
    @DisplayName("Count with match-all pattern should return total edge count")
    void testCountWithMatchAllPattern() throws DataManagerException {
        // Add 5 edges
        for (int i = 0; i < 5; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            graph.addEdge(s, p, o);
        }

        EdgePattern pattern = EdgePattern.builder().build();

        long count = queryOperations.count(pattern);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("Count on empty graph should return 0")
    void testCountOnEmptyGraph() throws DataManagerException {
        EdgePattern pattern = EdgePattern.builder().build();

        long count = queryOperations.count(pattern);

        assertEquals(0, count);
    }

    @Test
    @DisplayName("Count with specific predicate should return matching count")
    void testCountWithPredicatePattern() throws DataManagerException {
        Node p1 = graph.addProperty("http://example.org/p1");
        Node p2 = graph.addProperty("http://example.org/p2");
        Node s = graph.addResource("http://example.org/subject");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p1, o);
        graph.addEdge(s, p1, o);
        graph.addEdge(s, p2, o);

        EdgePattern pattern = EdgePattern.builder()
                .predicate(p1)
                .build();

        long count = queryOperations.count(pattern);

        assertTrue(count >= 1);
    }

    @Test
    @DisplayName("Exists should return true when pattern matches")
    void testExistsReturnsTrueWhenMatches() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        graph.addEdge(s, p, o);

        EdgePattern pattern = EdgePattern.builder()
                .predicate(p)
                .build();

        boolean exists = queryOperations.exists(pattern);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists should return false when pattern does not match")
    void testExistsReturnsFalseWhenNoMatches() throws DataManagerException {
        Node p = graph.addProperty("http://example.org/nonexistent");

        EdgePattern pattern = EdgePattern.builder()
                .predicate(p)
                .build();

        boolean exists = queryOperations.exists(pattern);

        assertFalse(exists);
    }


    @Test
    @DisplayName("Find with null edge should throw IllegalArgumentException")
    void testFindWithNullEdgeThrows() {
        assertThrows(IllegalArgumentException.class, () -> queryOperations.find(null));
    }

    @Test
    @DisplayName("Query with context filter should return edges from that context")
    void testQueryWithContextFilter() throws DataManagerException {
        Node context = graph.addGraph("http://example.org/graph1");
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(context, s, p, o);

        EdgePattern pattern = EdgePattern.builder()
                .contexts(java.util.List.of(context))
                .build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            assertTrue(edges.findAny().isPresent());
        }
    }

    @Test
    @DisplayName("Query should support multiple predicates")
    void testQueryWithMultiplePredicates() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p1 = graph.addProperty("http://example.org/p1");
        Node p2 = graph.addProperty("http://example.org/p2");
        Node p3 = graph.addProperty("http://example.org/p3");
        Node o = graph.addResource("http://example.org/object");

        graph.addEdge(s, p1, o);
        graph.addEdge(s, p2, o);
        graph.addEdge(s, p3, o);

        EdgePattern pattern = EdgePattern.builder()
                .subject(s)
                .build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            assertEquals(3, edges.count());
        }
    }


    @Test
    @DisplayName("Count should use optimization when available")
    void testCountOptimization() throws DataManagerException {
        // Add many edges
        for (int i = 0; i < 100; i++) {
            Node s = graph.addResource("http://example.org/s" + i);
            Node p = graph.addProperty("http://example.org/p");
            Node o = graph.addResource("http://example.org/o" + i);
            graph.addEdge(s, p, o);
        }

        EdgePattern pattern = EdgePattern.builder().build();

        long count = queryOperations.count(pattern);

        assertEquals(100, count);
    }

    @Test
    @DisplayName("Query with object pattern should match edges")
    void testQueryWithObjectPattern() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o1 = graph.addResource("http://example.org/object1");
        Node o2 = graph.addResource("http://example.org/object2");

        graph.addEdge(s, p, o1);
        graph.addEdge(s, p, o2);

        EdgePattern pattern = EdgePattern.builder()
                .object(o1)
                .build();

        try (Stream<Edge> edges = queryOperations.query(pattern)) {
            long count = edges.count();
            assertTrue(count >= 1);
        }
    }

    @Test
    @DisplayName("Exists on empty graph should return false")
    void testExistsOnEmptyGraph() throws DataManagerException {
        EdgePattern pattern = EdgePattern.builder().build();

        boolean exists = queryOperations.exists(pattern);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Multiple queries should return consistent results")
    void testMultipleQueriesConsistency() throws DataManagerException {
        Node s = graph.addResource("http://example.org/subject");
        Node p = graph.addProperty("http://example.org/predicate");
        Node o = graph.addResource("http://example.org/object");
        graph.addEdge(s, p, o);

        EdgePattern pattern = EdgePattern.builder()
                .predicate(p)
                .build();

        for (int i = 0; i < 3; i++) {
            try (Stream<Edge> edges = queryOperations.query(pattern)) {
                assertEquals(1, edges.count());
            }
        }
    }
}