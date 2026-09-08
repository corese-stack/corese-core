package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SimpleBNodeTest {

    @Test
    void getIDReturnsSuppliedIdentifier() {
        SimpleBNode node = new SimpleBNode("node1");

        assertEquals("node1", node.getID());
    }

    @Test
    void stringValueReturnsIdentifier() {
        SimpleBNode node = new SimpleBNode("node1");

        assertEquals("node1", node.stringValue());
    }

    @Test
    void equalityDependsOnIdentifier() {
        SimpleBNode node = new SimpleBNode("node1");
        SimpleBNode equalNode = new SimpleBNode("node1");

        assertEquals(node, equalNode);
        assertEquals(equalNode, node);
        assertNotEquals(node, new SimpleBNode("node2"));
        assertNotEquals(null, node);
        assertNotEquals("node1", node);
    }

    @Test
    void hashCodeUsesIdentifier() {
        SimpleBNode node = new SimpleBNode("node1");

        assertEquals("node1".hashCode(), node.hashCode());
        assertEquals(node.hashCode(), new SimpleBNode("node1").hashCode());
    }

    @Test
    void toStringPrefixesIdentifier() {
        SimpleBNode node = new SimpleBNode("node1");

        assertEquals("_:node1", node.toString());
    }

    @Test
    void rejectsNullIdentifier() {
        assertThrows(NullPointerException.class, () -> new SimpleBNode(null));
    }

    @Test
    void supportsEmptyIdentifier() {
        SimpleBNode node = new SimpleBNode("");
        assertEquals("", node.getID());
        assertEquals("", node.stringValue());
        assertEquals("_:", node.toString());
    }
}
