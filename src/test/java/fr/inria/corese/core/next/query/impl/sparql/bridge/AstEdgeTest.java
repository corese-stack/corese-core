package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.core.PointerType;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AstEdge}.
 */
class AstEdgeTest {

    private static Node variable(String name) {
        return NodeImpl.createVariable(name);
    }

    private static Node ressource(String iri) {
        return NodeImpl.createResource(iri);
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("creates edge with subject, predicate, object")
        void createEdge() {
            Node subject = variable("s");
            Node predicate = ressource("http://example.org/hasObject");
            Node object = variable("o");
            Edge edge = AstEdge.create(subject, predicate, object);
            assertNotNull(edge);
            assertSame(subject, edge.getNode(0));
            assertSame(object, edge.getNode(1));
            assertSame(predicate, edge.getProperty());
        }

        @Test
        @DisplayName("getEdgeLabel returns predicate label when constant")
        void edgeLabelFromConstantPredicate() {
            Node predicate = ressource("http://example.org/predicate");
            Edge edge = AstEdge.create(variable("s"), predicate, variable("o"));
            assertEquals("http://example.org/predicate", edge.getEdgeLabel());
        }

        @Test
        @DisplayName("getEdgeVariable returns predicate when variable")
        void edgeVariable() {
            Node predicate = variable("p");
            Edge edge = AstEdge.create(variable("s"), predicate, variable("o"));
            assertSame(predicate, edge.getEdgeVariable());
        }

        @Test
        @DisplayName("getEdgeVariable returns null when predicate is constant")
        void edgeVariableNullWhenConstant() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/predicate"), variable("o"));
            assertNull(edge.getEdgeVariable());
        }
    }

    @Nested
    @DisplayName("getNode / getProperty")
    class AccessorsTest {

        @Test
        @DisplayName("getNode(0) is subject, getNode(1) is object")
        void getNodeIndices() {
            Node subject = variable("s");
            Node object = variable("o");
            Edge edge = AstEdge.create(subject, ressource("http://example.org/p"), object);
            assertEquals(subject, edge.getNode(0));
            assertEquals(object, edge.getNode(1));
            assertThrows(IndexOutOfBoundsException.class, () -> edge.getNode(2));
        }

        @Test
        @DisplayName("getNode() returns subject")
        void getNode() {
            Node subject = variable("s");
            Edge edge = AstEdge.create(subject, ressource("http://example.org/p"), variable("o"));
            assertSame(subject, edge.getNode());
        }

        @Test
        @DisplayName("getGraph returns null")
        void getGraph() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/p"), variable("o"));
            assertNull(edge.getGraph());
        }
    }

    @Nested
    @DisplayName("contains")
    class ContainsTest {

        @Test
        @DisplayName("contains returns true for subject, predicate, object")
        void containsNodes() {
            Node subject = variable("s");
            Node predicate = variable("p");
            Node object = variable("o");
            Edge edge = AstEdge.create(subject, predicate, object);
            assertTrue(edge.contains(subject));
            assertTrue(edge.contains(predicate));
            assertTrue(edge.contains(object));
        }

        @Test
        @DisplayName("contains returns false for other node")
        void containsFalseForOther() {
            Edge edge = AstEdge.create(variable("s"), variable("p"), variable("o"));
            assertFalse(edge.contains(variable("x")));
        }

        @Test
        @DisplayName("contains returns false for null")
        void containsFalseForNull() {
            Edge edge = AstEdge.create(variable("s"), variable("p"), variable("o"));
            assertFalse(edge.contains(null));
        }
    }

    @Nested
    @DisplayName("edge index")
    class EdgeIndexTest {

        @Test
        @DisplayName("getEdgeIndex default is -1, setEdgeIndex updates it")
        void edgeIndex() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/p"), variable("o"));
            assertEquals(-1, edge.getEdgeIndex());
            edge.setEdgeIndex(3);
            assertEquals(3, edge.getEdgeIndex());
        }
    }

    @Nested
    @DisplayName("pointerType / getEdge")
    class PointerTest {

        @Test
        @DisplayName("pointerType is STATEMENT")
        void pointerType() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/p"), variable("o"));
            assertEquals(PointerType.STATEMENT, edge.pointerType());
        }

        @Test
        @DisplayName("getEdge returns this")
        void getEdge() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/p"), variable("o"));
            assertSame(edge, edge.getEdge());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString includes subject predicate object")
        void toStringNonEmpty() {
            Edge edge = AstEdge.create(variable("s"), ressource("http://example.org/predicate"), variable("o"));
            String stringRepresentation = edge.toString();
            assertNotNull(stringRepresentation);
            assertFalse(stringRepresentation.isEmpty());
        }
    }
}
