package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class CoreseDecimalTest  extends AbstractCoreseNumberTest {
    @Override
    protected AbstractCoreseNumber createNumber(String stringValue) {
        return new CoreseDecimal(stringValue);
    }

    @Override
    public void getCoreseNode() {
        CoreseDecimal coreseDecimal = new CoreseDecimal(1.0);
        assertNotNull(coreseDecimal.getCoreseNode());
        assertTrue(coreseDecimal.getCoreseNode() instanceof CoreseNumber);
    }

    @Override
    @Test
    public void getCoreDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("13.46");
        assertEquals(XSD.DECIMAL, coreseNumber.getCoreDatatype());
    }

    @Override
    @Test
    public void floatValue() {
        AbstractCoreseNumber coreseNumber = createNumber("110.220");
        assertEquals((float) 110.220, coreseNumber.floatValue(), 0.0);
    }

    @Override
    @Test
    public void doubleValue() {
        AbstractCoreseNumber coreseNumber = createNumber("111.222");
        assertEquals(111.222, coreseNumber.doubleValue(), 0.0);
    }

    @Override
    @Test
    public void decimalValue() {
        AbstractCoreseNumber coreseNumber = createNumber("113.224");
        assertEquals(BigDecimal.valueOf(113.224), coreseNumber.decimalValue());
    }
}
