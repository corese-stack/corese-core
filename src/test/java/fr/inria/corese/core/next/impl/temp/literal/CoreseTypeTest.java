package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseLiteral;
import fr.inria.corese.core.sparql.datatype.CoreseString;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreseTypeTest {
    private String testValue;
    private IRI testDatatypeIRI;
    private CoreDatatype testCoreDatatype;

    @Before
    public void setUp() {
        testValue = "Test String";
        testDatatypeIRI = XSD.STRING.getIRI();
        testCoreDatatype = XSD.STRING;
    }

    @Test
    public void testConstructorWithIDatatype() {
        // Create a mock CoreseLiteral with value
        fr.inria.corese.core.sparql.datatype.CoreseString coreseLiteral = new CoreseString(testValue);

        CoreseTyped coreseTyped = new CoreseTyped(coreseLiteral);
        assertEquals(testValue, coreseTyped.getLabel());
        assertEquals(testDatatypeIRI.stringValue(), coreseLiteral.getDatatype().stringValue());
    }

    @Test
    public void testCoreseTypedConstructorWithString() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);

        // Validate the core datatype and value
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testCoreseTypedConstructorWithIRI() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue, testDatatypeIRI);

        // Validate the core datatype and value
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testCoreseTypedConstructorWithCoreDatatype() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue, testCoreDatatype);

        // Validate the core datatype and value
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testCoreseTypedConstructorWithIRIAndCoreDatatype() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue, testDatatypeIRI, testCoreDatatype);

        // Validate the core datatype and value
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testIncorrectDatatypeInCoreseTypedConstructor() {
        // Test for creating CoreseTyped with an invalid datatype (e.g., mismatched CoreDatatype and IRI)
        IRI invalidIRI = new CoreseIRI("http://example.com/invalidDatatype");

        Exception exception = assertThrows(IncorrectOperationException.class, () -> {
            new CoreseTyped(testValue, invalidIRI, testCoreDatatype);
        });

        assertEquals("Datatype IRI does not match CoreDatatype's IRI", exception.getMessage());
    }

    @Test
    public void testGetLabel() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);

        // Validate that getLabel() returns the correct value
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testGetCoreDatatype() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);

        // Validate that getCoreDatatype() returns the correct CoreDatatype (XSD.STRING)
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
    }

    @Test
    public void testGetIDatatype() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);
        IDatatype coreseObject = coreseTyped.getIDatatype();

        // Validate that getIDatatype() returns the correct CoreseString object
        assertTrue(coreseObject instanceof CoreseString);
    }

    @Test
    public void testGetCoreseNode() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);

        // Validate that getCoreseNode() returns the correct CoreseString node
        assertTrue(coreseTyped.getCoreseNode() instanceof CoreseString);
    }

    @Test
    public void testSetCoreDatatype() {
        CoreseTyped coreseTyped = new CoreseTyped(testValue);

        // Test that setCoreDatatype throws IncorrectOperationException, as it's not allowed
        assertThrows(IncorrectOperationException.class, () -> {
            coreseTyped.setCoreDatatype(XSD.STRING);
        });
    }

    @Test
    public void testConstructorWithNullDatatype() {
        // Expect the default XSD.STRING
        Literal coreseTyped = new CoreseTyped(testValue, (IRI) null);
        assertEquals(XSD.STRING.getIRI(), coreseTyped.getDatatype());
        assertEquals(XSD.STRING, coreseTyped.getCoreDatatype());
        assertEquals(testValue, coreseTyped.getLabel());
    }

    @Test
    public void testConstructorWithNonNullDatatypeUnknownCoreDatatype() {
        CoreseIRI unknown = new CoreseIRI("http://example.org");
        // Expect the default XSD.STRING
        Literal coreseTyped = new CoreseTyped(testValue, unknown);
        assertEquals(coreseTyped.getCoreDatatype(), CoreDatatype.NONE);
    }
}
