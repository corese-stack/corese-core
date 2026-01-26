package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.kgram.api.core.Edge;
import fr.inria.corese.core.next.kgram.api.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for Mapping class.
 * Tests mapping creation, node/edge management, and result operations.
 */
@DisplayName("Mapping Tests")
class MappingTest {

    private Mapping mapping;
    private Node mockQueryNode;
    private Node mockTargetNode;
    private Edge mockQueryEdge;
    private Edge mockTargetEdge;

    @BeforeEach
    void setUp() {
        mapping = new Mapping();
        mockQueryNode = mock(Node.class);
        mockTargetNode = mock(Node.class);
        mockQueryEdge = mock(Edge.class);
        mockTargetEdge = mock(Edge.class);
    }

    @Nested
    @DisplayName("Mapping Creation Tests")
    class MappingCreationTests {

        @Test
        @DisplayName("Should create empty mapping")
        void testCreateEmptyMapping() {
            Mapping m = new Mapping();
            assertNotNull(m, "Mapping should not be null");
        }

        @Test
        @DisplayName("Should create mapping using factory method")
        void testCreateMappingWithFactory() {
            Mapping m = Mapping.create();
            assertNotNull(m, "Factory-created mapping should not be null");
        }

        @Test
        @DisplayName("Should create mapping with node lists")
        void testCreateMappingWithNodeLists() {
            List<Node> queryNodes = new ArrayList<>();
            List<Node> targetNodes = new ArrayList<>();
            queryNodes.add(mockQueryNode);
            targetNodes.add(mockTargetNode);

            Mapping m = Mapping.create(queryNodes, targetNodes);
            assertNotNull(m, "Mapping with node lists should not be null");
        }

    }

    @Nested
    @DisplayName("Node Management Tests")
    class NodeManagementTests {

        @Test
        @DisplayName("Should handle query nodes array")
        void testQueryNodesArray() {
            Node[] nodes = {mockQueryNode};
            Node[] targets = {mockTargetNode};

            Mapping m = Mapping.create(nodes, targets);
            assertNotNull(m, "Mapping should handle node arrays");
        }

        @Test
        @DisplayName("Should set and get select nodes")
        void testSetAndGetSelectNodes() {
            Node[] selectNodes = {mockQueryNode};
            mapping.setSelectNodes(selectNodes);

            Node[] result = mapping.getSelectNodes();
            assertNotNull(result, "Select nodes should not be null");
            assertEquals(1, result.length, "Should have one select node");
        }


    }

    @Nested
    @DisplayName("Edge Management Tests")
    class EdgeManagementTests {

        @Test
        @DisplayName("Should set and get query edges")
        void testSetAndGetQueryEdges() {
            Edge[] edges = {mockQueryEdge};
            mapping.setQueryEdges(edges);

            Edge[] result = mapping.getQueryEdges();
            assertNotNull(result, "Query edges should not be null");
        }

        @Test
        @DisplayName("Should set and get target edges")
        void testSetAndGetTargetEdges() {
            Edge[] edges = {mockTargetEdge};
            mapping.setTargetEdges(edges);

            Edge[] result = mapping.getTargetEdges();
            assertNotNull(result, "Target edges should not be null");
        }

        @Test
        @DisplayName("Should handle empty edge arrays")
        void testEmptyEdgeArrays() {
            Edge[] emptyEdges = {};
            mapping.setQueryEdges(emptyEdges);

            Edge[] result = mapping.getQueryEdges();
            assertNotNull(result, "Should return non-null array");
            assertEquals(0, result.length, "Should be empty array");
        }
    }

    @Nested
    @DisplayName("Query Node Tests")
    class QueryNodeTests {

        @Test
        @DisplayName("Should get query nodes array")
        void testGetQueryNodes() {
            Node[] nodes = mapping.getQueryNodes();
            assertNotNull(nodes, "Query nodes should not be null");
        }
    }

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Should compare mappings")
        void testCompareMappings() {
            Mapping other = new Mapping();

            assertDoesNotThrow(() -> mapping.compare(other),
                    "compare should not throw exception");
        }

        @Test
        @DisplayName("Mapping should equal itself")
        void testMappingEqualsItself() {
            assertEquals(mapping, mapping, "Mapping should equal itself");
        }

        @Test
        @DisplayName("Should handle null in equals")
        void testEqualsWithNull() {
            assertNotEquals(null, mapping, "Mapping should not equal null");
        }
    }


}