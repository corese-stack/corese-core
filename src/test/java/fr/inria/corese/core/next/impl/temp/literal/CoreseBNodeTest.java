package fr.inria.corese.core.next.impl.temp.literal;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import fr.inria.corese.core.sparql.datatype.CoreseBlankNode;

public class CoreseBNodeTest {

    private static final String BNODE_ID = "bnodeCorese123";
    private static final CoreseBlankNode coreseBlankNode = new CoreseBlankNode(BNODE_ID);

    private CoreseBNode coreseBNodeFromCoreseObject;
    private CoreseBNode coreseBNodeFromStringId;

    @Before
    public void setUp() {
        coreseBNodeFromCoreseObject = new CoreseBNode(coreseBlankNode);
        coreseBNodeFromStringId = new CoreseBNode(BNODE_ID);
    }

    @Test
    public void testConstructorFromString() {
        // Test creating CoreseBnode for a string ID
        assertNotNull(coreseBNodeFromStringId);
        assertEquals(BNODE_ID, coreseBNodeFromStringId.getID());
    }

    @Test
    public void testConstructorFromCoreseObject() {
        // Test creating CoreseBnode for a CoreseBlankNode (old API)
        assertNotNull(coreseBNodeFromCoreseObject);
        assertEquals(BNODE_ID, coreseBNodeFromCoreseObject.getID());
    }

    @Test
    public void testToString() {
        // Test the toString method to ensure it outputs the correct representation
        String expectedString = "_:" + BNODE_ID;
        assertEquals(expectedString, coreseBNodeFromStringId.toString());
    }
}
