package fr.inria.corese.core.next.data.impl.adapter.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.sparql.datatype.CoreseBlankNode;

class CoreseBNodeTest {

    private static final String BNODE_ID = "bnodeCorese123";
    private static final CoreseBlankNode coreseBlankNode = new CoreseBlankNode(BNODE_ID);

    private CoreseBNode coreseBNodeFromCoreseObject;
    private CoreseBNode coreseBNodeFromStringId;

    @BeforeEach
    void setUp() {
        coreseBNodeFromCoreseObject = new CoreseBNode(coreseBlankNode);
        coreseBNodeFromStringId = new CoreseBNode(BNODE_ID);
    }

    @Test
    void testConstructorFromString() {
        // Test creating CoreseBnode for a string ID
        assertNotNull(coreseBNodeFromStringId);
        assertEquals(BNODE_ID, coreseBNodeFromStringId.getID());
    }

    @Test
    void testConstructorFromCoreseObject() {
        // Test creating CoreseBnode for a CoreseBlankNode (old API)
        assertNotNull(coreseBNodeFromCoreseObject);
        assertEquals(BNODE_ID, coreseBNodeFromCoreseObject.getID());
    }

    @Test
    void testToString() {
        // Test the toString method to ensure it outputs the correct representation
        String expectedString = "_:" + BNODE_ID;
        assertEquals(expectedString, coreseBNodeFromStringId.toString());
    }
}
