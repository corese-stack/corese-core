package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AstBackedEdge: contains() and getEdgeVariable()")
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
    @DisplayName("getEdgeVariable() exposes the predicate when it is a variable, null otherwise")
    void edgeVariableExposedOnlyForVariablePredicate() {
        Node predicateVar = new NodeImpl(Variable.create("p"));
        AstBackedEdge withVarPredicate = new AstBackedEdge(
                new NodeImpl(Variable.create("s")), predicateVar, new NodeImpl(Variable.create("o")));
        assertSame(predicateVar, withVarPredicate.getEdgeVariable());

        AstBackedEdge withIriPredicate = new AstBackedEdge(
                new NodeImpl(Variable.create("s")),
                new NodeImpl(Constant.createResource("http://example.org/p")),
                new NodeImpl(Variable.create("o")));
        assertNull(withIriPredicate.getEdgeVariable());
    }
}