package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;

public abstract class AbstractCoreseNumberTest {

    protected abstract AbstractCoreseNumber createNumber(String stringValue);

    @Test
    public abstract void getCoreseNode();

    @Test
    public void getIDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("11");
        assertNotNull(coreseNumber.getIDatatype());
    }

    @Test
    public void getLabel() {
        AbstractCoreseNumber coreseNumber = createNumber("12");
        assertEquals("12", coreseNumber.getLabel());
    }

    @Test
    public void getCoreDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("13");
        assertEquals(CoreDatatype.XSD.INTEGER, coreseNumber.getCoreDatatype());
    }

    @Test
    public void stringValue() {
        AbstractCoreseNumber coreseNumber = createNumber("14");
        assertEquals("14", coreseNumber.stringValue());
    }

    @Test
    public void setCoreDatatype() {
        AbstractCoreseNumber coreseNumber = createNumber("15");
        assertThrows(IncorrectOperationException.class, () -> {
            coreseNumber.setCoreDatatype(CoreDatatype.XSD.INTEGER);
        });
    }

    @Test
    public void byteValue() {
        AbstractCoreseNumber coreseNumber = createNumber("16");
        assertEquals(16, coreseNumber.byteValue());
    }

    @Test
    public void intValue() {
        AbstractCoreseNumber coreseNumber = createNumber("17");
        assertEquals(17, coreseNumber.intValue());
    }

    @Test
    public void longValue() {
        AbstractCoreseNumber coreseNumber = createNumber("18");
        assertEquals(18, coreseNumber.longValue());
    }

    @Test
    public void shortValue() {
        AbstractCoreseNumber coreseNumber = createNumber("19");
        assertEquals(19, coreseNumber.shortValue());
    }

    @Test
    public void floatValue() {
        AbstractCoreseNumber coreseNumber = createNumber("110");
        assertEquals(110.0, coreseNumber.floatValue(), 0.0);
    }

    @Test
    public void doubleValue() {
        AbstractCoreseNumber coreseNumber = createNumber("111");
        assertEquals(111.0, coreseNumber.doubleValue(), 0.0);
    }

    @Test
    public void integerValue() {
        AbstractCoreseNumber coreseNumber = createNumber("112");
        assertEquals(new BigInteger("112"), coreseNumber.integerValue());
    }

    @Test
    public void decimalValue() {
        AbstractCoreseNumber coreseNumber = createNumber("113");
        assertEquals(new BigDecimal(113), coreseNumber.decimalValue());
    }
}