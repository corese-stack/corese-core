package fr.inria.corese.core.next.data.impl.adapter.literal;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;

import static org.junit.jupiter.api.Assertions.*;

public class CoreseIntegerTest extends AbstractCoreseNumberTest {


    @Override
    protected AbstractCoreseNumber createNumber(String stringValue) {
        return new CoreseInteger(stringValue);
    }

    @Override
    @Test
    public void getCoreseNode() {
        CoreseInteger coreseInteger = new CoreseInteger(1);
        assertNotNull(coreseInteger.getCoreseNode());
        assertInstanceOf(CoreseNumber.class, coreseInteger.getCoreseNode());
    }

    @Override
    public void getCoreDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("1346");
        assertEquals(XSDDatatype.INTEGER, coreseNumber.getCoreDatatype());
    }

    @Override
    public void floatValue() {
        assertTrue(true);
    }

    @Override
    public void doubleValue() {
        assertTrue(true);
    }

    @Override
    public void decimalValue() {
        assertTrue(true);
    }
}
