package fr.inria.corese.core.next.api.model.impl.basic.literal;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseBoolean;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreseBooleanTest {


    @Test
    public void testConstructorWithBoolean() {
        // Test creating CoreseBoolean with a boolean value
        CoreseBoolean trueBoolean = new CoreseBoolean(true);
        CoreseBoolean falseBoolean = new CoreseBoolean(false);

        // Test that the boolean value is correctly set
        assertTrue(trueBoolean.booleanValue());
        assertFalse(falseBoolean.booleanValue());

        // Test that the label is correctly returned
        assertEquals("true", trueBoolean.getLabel());
        assertEquals("false", falseBoolean.getLabel());

        // Test the stringValue method
        assertEquals("true", trueBoolean.stringValue());
        assertEquals("false", falseBoolean.stringValue());

        // Test that coreDatatype is set to XSD.BOOLEAN
        assertEquals(CoreDatatype.XSD.BOOLEAN.getIRI(), trueBoolean.getDatatype());
        assertEquals(CoreDatatype.XSD.BOOLEAN.getIRI(), falseBoolean.getDatatype());
    }

    @Test
    public void testConstructorWithIDatatype() {
        IDatatype coreseBooleanDatatype = new fr.inria.corese.core.sparql.datatype.CoreseBoolean(true);

        CoreseBoolean coreseBoolean = new CoreseBoolean(coreseBooleanDatatype);

        // Test that the CoreseBoolean is created correctly
        assertTrue(coreseBoolean.booleanValue());
        assertEquals("true", coreseBoolean.getLabel());
        assertEquals(CoreDatatype.XSD.BOOLEAN.getIRI(), coreseBoolean.getDatatype());
    }

    @Test
    public void testValueOf() {
        CoreseBoolean trueBoolean = CoreseBoolean.valueOf(true);
        CoreseBoolean falseBoolean = CoreseBoolean.valueOf(false);

        assertTrue(trueBoolean.booleanValue());
        assertFalse(falseBoolean.booleanValue());
    }

}
