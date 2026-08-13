package fr.inria.corese.core.next.query.kgram.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NodeImpl factory methods: forIRI, forBlank, forLiteral, forVariable")
class NodeImplFactoryTest {

    @Test
    @DisplayName("forIRI creates a constant non-variable non-blank node with the given label")
    void forIRICreatesConstantIRINode() {
        NodeImpl node = NodeImpl.forIRI("http://example.org/alice");

        assertTrue(node.isConstant());
        assertFalse(node.isVariable());
        assertFalse(node.isBlank());
        assertEquals("http://example.org/alice", node.getLabel());
    }

    @Test
    @DisplayName("forIRI: two nodes with the same URI compare as the same RDF term")
    void twoIRINodesWithSameURIAreSameTerm() {
        NodeImpl a = NodeImpl.forIRI("http://example.org/alice");
        NodeImpl b = NodeImpl.forIRI("http://example.org/alice");

        assertNotSame(a, b);
        assertTrue(a.same(b));
        assertEquals(0, a.compare(b));
    }

    @Test
    @DisplayName("forIRI: two nodes with different URIs are not the same RDF term")
    void twoIRINodesWithDifferentURIAreNotSame() {
        NodeImpl a = NodeImpl.forIRI("http://example.org/alice");
        NodeImpl b = NodeImpl.forIRI("http://example.org/bob");

        assertFalse(a.same(b));
    }

    @Test
    @DisplayName("forBlank creates a constant blank node with the given ID as label")
    void forBlankCreatesBlankNode() {
        NodeImpl node = NodeImpl.forBlank("b42");

        assertTrue(node.isConstant());
        assertTrue(node.isBlank());
        assertFalse(node.isVariable());
        assertEquals("b42", node.getLabel());
    }

    @Test
    @DisplayName("forLiteral creates a constant non-blank non-variable node with the given label")
    void forLiteralCreatesLiteralNode() {
        NodeImpl node = NodeImpl.forLiteral("hello", "http://www.w3.org/2001/XMLSchema#string", null);

        assertTrue(node.isConstant());
        assertFalse(node.isVariable());
        assertFalse(node.isBlank());
        assertEquals("hello", node.getLabel());
    }

    @Test
    @DisplayName("forLiteral with lang tag stores the label correctly")
    void forLiteralWithLangStoresLabel() {
        NodeImpl node = NodeImpl.forLiteral("bonjour", null, "fr");

        assertEquals("bonjour", node.getLabel());
        assertTrue(node.isConstant());
        assertFalse(node.isBlank());
    }

    @Test
    @DisplayName("forVariable creates a variable non-constant node with the given name")
    void forVariableCreatesVariableNode() {
        NodeImpl node = NodeImpl.forVariable("x");

        assertTrue(node.isVariable());
        assertFalse(node.isConstant());
        assertFalse(node.isBlank());
        assertEquals("x", node.getLabel());
    }

    @Test
    @DisplayName("forVariable: two variables with the same name compare as same")
    void twoVariablesWithSameNameAreSame() {
        NodeImpl a = NodeImpl.forVariable("x");
        NodeImpl b = NodeImpl.forVariable("x");

        assertNotSame(a, b);
        assertTrue(a.same(b));
    }

    @Test
    @DisplayName("forVariable: two variables with different names are not same")
    void twoVariablesWithDifferentNamesAreNotSame() {
        NodeImpl a = NodeImpl.forVariable("x");
        NodeImpl b = NodeImpl.forVariable("y");

        assertFalse(a.same(b));
    }
}
