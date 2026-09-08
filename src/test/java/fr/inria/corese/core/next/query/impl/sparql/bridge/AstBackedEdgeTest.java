package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.KgramNodes;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AstBackedEdge: contains(), getEdgeVariable() and getGraph()")
class AstBackedEdgeTest {

    @Test
    @DisplayName("contains() recognises the subject and the object")
    void edgeContainsSubjectAndObject() {
        Node subject = NodeImpl.forVariable("s");
        Node predicate = NodeImpl.forVariable("p");
        Node object = NodeImpl.forVariable("o");

        AstBackedEdge edge = new AstBackedEdge(subject, predicate, object);

        assertTrue(edge.contains(subject));
        assertTrue(edge.contains(object));
    }

    @Test
    @DisplayName("contains() recognises a variable by name (not only the same instance)")
    void edgeContainsByVariableName() {
        AstBackedEdge edge = new AstBackedEdge(
                NodeImpl.forVariable("s"),
                NodeImpl.forVariable("p"),
                NodeImpl.forVariable("o"));

        assertTrue(edge.contains(NodeImpl.forVariable("s")));
        assertTrue(edge.contains(NodeImpl.forVariable("o")));
    }

    @Test
    @DisplayName("contains() returns false for an absent variable and for null")
    void edgeDoesNotContainOtherNode() {
        AstBackedEdge edge = new AstBackedEdge(
                NodeImpl.forVariable("s"),
                NodeImpl.forVariable("p"),
                NodeImpl.forVariable("o"));

        assertFalse(edge.contains(NodeImpl.forVariable("x")));
        assertFalse(edge.contains(null));
    }

    @Test
    @DisplayName("Variable predicate is exposed as edge variable with root property as edge node")
    void edgeVariableExposedOnlyForVariablePredicate() {
        Node predicateVar = NodeImpl.forVariable("p");
        AstBackedEdge withVarPredicate = new AstBackedEdge(
                NodeImpl.forVariable("s"), predicateVar, NodeImpl.forVariable("o"));

        assertEquals(KgramNodes.ROOT_PROPERTY_URI, withVarPredicate.getEdgeNode().getLabel());
        assertEquals(KgramNodes.ROOT_PROPERTY_URI, withVarPredicate.getEdgeLabel());
        assertSame(predicateVar, withVarPredicate.getProperty());
        assertSame(predicateVar, withVarPredicate.getEdgeVariable());

        Node iriPredicate = NodeImpl.forIRI("http://example.org/p");
        AstBackedEdge withIriPredicate = new AstBackedEdge(
                NodeImpl.forVariable("s"),
                iriPredicate,
                NodeImpl.forVariable("o"));
        assertSame(iriPredicate, withIriPredicate.getEdgeNode());
        assertSame(iriPredicate, withIriPredicate.getProperty());
        assertEquals("http://example.org/p", withIriPredicate.getEdgeLabel());
        assertNull(withIriPredicate.getEdgeVariable());
    }

    @Test
    @DisplayName("getGraph() is null for the default graph (engine represents it as null)")
    void defaultGraphIsNull() {
        AstBackedEdge edge = new AstBackedEdge(
                NodeImpl.forVariable("s"),
                NodeImpl.forVariable("p"),
                NodeImpl.forVariable("o"));

        assertNull(edge.getGraph());
    }

    @Test
    @DisplayName("getGraph() returns the graph node passed to the constructor")
    void explicitGraphIsReturned() {
        Node graph = NodeImpl.forIRI("http://example.org/g");
        AstBackedEdge edge = new AstBackedEdge(
                NodeImpl.forVariable("s"),
                NodeImpl.forVariable("p"),
                NodeImpl.forVariable("o"),
                graph);

        assertSame(graph, edge.getGraph());
    }
}
