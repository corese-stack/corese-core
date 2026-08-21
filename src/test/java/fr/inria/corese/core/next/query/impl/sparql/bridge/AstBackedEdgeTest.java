package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.tool.KgramNodes;
import fr.inria.corese.core.next.query.impl.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AstBackedEdge: contains(), getEdgeVariable() and getGraph()")
class AstBackedEdgeTest {

    @Test
    @DisplayName("contains() recognises the subject and the object")
    void edgeContainsSubjectAndObject() {
        Node subject = new NodeImpl(Variable.create("s"));
        Node predicate = new NodeImpl(Variable.create("p"));
        Node object = new NodeImpl(Variable.create("o"));

        AstBackedEdge edge = new AstBackedEdge(subject, predicate, object);

        assertTrue(edge.contains(subject));
        assertTrue(edge.contains(object));
    }

    @Test
    @DisplayName("contains() recognises a variable by name (not only the same instance)")
    void edgeContainsByVariableName() {
        AstBackedEdge edge = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                new NodeImpl(Variable.create("p")),
                new NodeImpl(Variable.create("o")));

        assertTrue(edge.contains(new NodeImpl(Variable.create("s"))));
        assertTrue(edge.contains(new NodeImpl(Variable.create("o"))));
    }

    @Test
    @DisplayName("contains() returns false for an absent variable and for null")
    void edgeDoesNotContainOtherNode() {
        AstBackedEdge edge = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                new NodeImpl(Variable.create("p")),
                new NodeImpl(Variable.create("o")));

        assertFalse(edge.contains(new NodeImpl(Variable.create("x"))));
        assertFalse(edge.contains(null));
    }

    @Test
    @DisplayName("Variable predicate is exposed as edge variable with root property as edge node")
    void edgeVariableExposedOnlyForVariablePredicate() {
        Node predicateVar = new NodeImpl(Variable.create("p"));
        AstBackedEdge withVarPredicate = new AstBackedEdge(
                new NodeImpl(Variable.create("s")), predicateVar, new NodeImpl(Variable.create("o")));

        assertEquals(KgramNodes.ROOT_PROPERTY_URI, withVarPredicate.getEdgeNode().getLabel());
        assertEquals(KgramNodes.ROOT_PROPERTY_URI, withVarPredicate.getEdgeLabel());
        assertSame(predicateVar, withVarPredicate.getProperty());
        assertSame(predicateVar, withVarPredicate.getEdgeVariable());

        Node iriPredicate = new NodeImpl(Constant.createResource("http://example.org/p"));
        AstBackedEdge withIriPredicate = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                iriPredicate,
                new NodeImpl(Variable.create("o")));
        assertSame(iriPredicate, withIriPredicate.getEdgeNode());
        assertSame(iriPredicate, withIriPredicate.getProperty());
        assertEquals("http://example.org/p", withIriPredicate.getEdgeLabel());
        assertNull(withIriPredicate.getEdgeVariable());
    }

    @Test
    @DisplayName("getGraph() is null for the default graph (engine represents it as null)")
    void defaultGraphIsNull() {
        AstBackedEdge edge = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                new NodeImpl(Variable.create("p")),
                new NodeImpl(Variable.create("o")));

        assertNull(edge.getGraph());
    }

    @Test
    @DisplayName("getGraph() returns the graph node passed to the constructor")
    void explicitGraphIsReturned() {
        Node graph = new NodeImpl(Constant.createResource("http://example.org/g"));
        AstBackedEdge edge = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                new NodeImpl(Variable.create("p")),
                new NodeImpl(Variable.create("o")),
                graph);

        assertSame(graph, edge.getGraph());
    }
}
