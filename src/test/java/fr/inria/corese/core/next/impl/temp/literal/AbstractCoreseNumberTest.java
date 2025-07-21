package fr.inria.corese.core.next.impl.temp.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

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
    public abstract void getCoreDatatype();

    @Test
    public void stringValue() {
        AbstractCoreseNumber coreseNumber = createNumber("14");
        assertEquals("14", coreseNumber.stringValue());
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
    public void integerValue() {
        AbstractCoreseNumber coreseNumber = createNumber("112");
        assertEquals(new BigInteger("112"), coreseNumber.integerValue());
    }

    @Test
    public abstract void floatValue();

    @Test
    public abstract void doubleValue();

    @Test
    public abstract void decimalValue();
}