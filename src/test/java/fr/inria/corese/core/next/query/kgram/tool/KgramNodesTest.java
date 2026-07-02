package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.query.kgram.api.core.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KgramNodesTest {

    @Test
    @DisplayName("rootProperty() creates the KGRAM root property node")
    void rootPropertyCreatesRootPropertyNode() {
        Node rootProperty = KgramNodes.rootProperty();

        assertEquals(KgramNodes.ROOT_PROPERTY_URI, rootProperty.getLabel());
        assertEquals(KgramNodes.ROOT_PROPERTY_URI, rootProperty.getDatatypeValue().getLabel());
        assertTrue(rootProperty.isConstant());
        assertFalse(rootProperty.isVariable());
        assertFalse(rootProperty.isBlank());
        assertNull(rootProperty.getGraph());
        assertNull(rootProperty.getPath());
    }

    @Test
    @DisplayName("rootProperty() nodes compare as the same RDF term")
    void rootPropertyNodesCompareAsSameTerm() {
        Node left = KgramNodes.rootProperty();
        Node right = KgramNodes.rootProperty();

        assertNotSame(left, right);
        assertTrue(left.same(right));
        assertTrue(left.match(right));
        assertEquals(0, left.compare(right));
    }
}
