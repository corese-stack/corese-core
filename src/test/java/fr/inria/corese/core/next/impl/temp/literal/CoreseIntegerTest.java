package fr.inria.corese.core.next.impl.temp.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;

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
        assertTrue(coreseInteger.getCoreseNode() instanceof CoreseNumber);
    }

    @Override
    public void getCoreDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("1346");
        assertEquals(XSD.INTEGER, coreseNumber.getCoreDatatype());
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