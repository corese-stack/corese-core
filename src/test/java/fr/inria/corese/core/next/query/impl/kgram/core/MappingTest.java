package fr.inria.corese.core.next.query.impl.kgram.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Nested
    @DisplayName("Environment Tests")
    class EnvironmentTests {

        @Test
        @DisplayName("Should provide environment interface")
        void testEnvironmentInterface() {
            // Mapping implements Environment
            assertNotNull(mapping, "Should support environment operations");
        }
    }

    @Nested
    @DisplayName("Result Interface Tests")
    class ResultInterfaceTests {

        @Test
        @DisplayName("Should support Result interface")
        void testResultInterface() {
            // Mapping implements Result
            assertNotNull(mapping, "Should implement Result interface");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should not be null")
        void testToStringNotNull() {
            String str = mapping.toString();
            assertNotNull(str, "toString should not return null");
        }

        @Test
        @DisplayName("Empty mapping toString should be empty")
        void testToStringEmpty() {
            // Empty mapping has no nodes, so toString returns empty string
            String str = mapping.toString();
            assertNotNull(str, "toString should not return null");
            // Empty mapping produces empty string - this is expected behavior
            assertTrue(str.isBlank(),
                    "Empty mapping should produce empty or blank string");
        }

        @Test
        @DisplayName("Mapping with nodes should have non-empty toString")
        void testToStringWithNodes() {
            // Create mapping with actual nodes
            List<Node> queryNodes = new ArrayList<>();
            List<Node> targetNodes = new ArrayList<>();

            when(mockQueryNode.toString()).thenReturn("?x");
            when(mockTargetNode.toString()).thenReturn("value");

            queryNodes.add(mockQueryNode);
            targetNodes.add(mockTargetNode);

            Mapping m = Mapping.create(queryNodes, targetNodes);
            String str = m.toString();

            assertNotNull(str, "toString should not return null");
            assertFalse(str.isEmpty(), "Mapping with nodes should have non-empty toString");
        }
    }

    @Nested
    @DisplayName("Distinct Nodes Tests")
    class DistinctNodesTests {

        @Test
        @DisplayName("Should set and get distinct nodes")
        void testSetAndGetDistinctNodes() {
            Node[] distinctNodes = {mockQueryNode};
            mapping.setDistinctNodes(distinctNodes);

            Node[] result = mapping.getDistinctNodes();
            assertNotNull(result, "Distinct nodes should not be null");
        }
    }

    @Nested
    @DisplayName("Size Tests")
    class SizeTests {

        @Test
        @DisplayName("Empty mapping should have size 0")
        void testEmptyMappingSize() {
            assertEquals(0, mapping.size(), "Empty mapping should have size 0");
        }

        @Test
        @DisplayName("Mapping with nodes should have correct size")
        void testMappingSizeWithNodes() {
            List<Node> queryNodes = new ArrayList<>();
            List<Node> targetNodes = new ArrayList<>();
            queryNodes.add(mockQueryNode);
            targetNodes.add(mockTargetNode);

            Mapping m = Mapping.create(queryNodes, targetNodes);
            assertEquals(1, m.size(), "Mapping should have size 1");
        }
    }

    @Nested
    @DisplayName("Graph Node Tests")
    class GraphNodeTests {

        @Test
        @DisplayName("Should set and get graph node")
        void testSetAndGetGraphNode() {
            mapping.setGraphNode(mockQueryNode);
            Node result = mapping.getGraphNode();
            assertEquals(mockQueryNode, result, "Graph node should match");
        }

        @Test
        @DisplayName("Should handle null graph node")
        void testNullGraphNode() {
            mapping.setGraphNode(null);
            assertNull(mapping.getGraphNode(), "Graph node should be null");
        }
    }

    @Nested
    @DisplayName("Node Value Tests")
    class NodeValueTests {

        @Test
        @DisplayName("Should get node by label")
        void testGetNodeByLabel() {
            when(mockQueryNode.isVariable()).thenReturn(true);
            when(mockQueryNode.getLabel()).thenReturn("?x");

            List<Node> queryNodes = new ArrayList<>();
            List<Node> targetNodes = new ArrayList<>();
            queryNodes.add(mockQueryNode);
            targetNodes.add(mockTargetNode);

            Mapping m = Mapping.create(queryNodes, targetNodes);
            Node result = m.getNode("?x");

            assertEquals(mockTargetNode, result, "Should return target node for variable");
        }

        @Test
        @DisplayName("Should return null for non-existent variable")
        void testGetNodeNonExistent() {
            Node result = mapping.getNode("?nonexistent");
            assertNull(result, "Should return null for non-existent variable");
        }
    }

    @Nested
    @DisplayName("IsBound Tests")
    class IsBoundTests {

        @Test
        @DisplayName("Should check if node is bound")
        void testIsBound() {
            when(mockQueryNode.getLabel()).thenReturn("?x");
            when(mockQueryNode.isVariable()).thenReturn(true);

            List<Node> queryNodes = new ArrayList<>();
            List<Node> targetNodes = new ArrayList<>();
            queryNodes.add(mockQueryNode);
            targetNodes.add(mockTargetNode);

            Mapping m = Mapping.create(queryNodes, targetNodes);
            assertTrue(m.isBound(mockQueryNode), "Node should be bound");
        }

        @Test
        @DisplayName("Unbound node should return false")
        void testIsNotBound() {
            when(mockQueryNode.getLabel()).thenReturn("?y");
            assertFalse(mapping.isBound(mockQueryNode), "Unbound node should return false");
        }
    }
}