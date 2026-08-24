package fr.inria.corese.core.next.data.impl.adapter.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

class CoreseAdapterSerializationTest {

    @Test
    void booleanRetainsItsValueAndLegacyAdapter() throws IOException, ClassNotFoundException {
        CoreseBoolean trueValue = roundTrip(new CoreseBoolean(true), CoreseBoolean.class);
        CoreseBoolean falseValue = roundTrip(new CoreseBoolean(false), CoreseBoolean.class);

        assertTrue(trueValue.booleanValue());
        assertFalse(falseValue.booleanValue());
        assertNotNull(trueValue.getIDatatype());
    }

    @Test
    void booleanRetainsItsNumericLexicalForm() throws IOException, ClassNotFoundException {
        CoreseBoolean value = roundTrip(new CoreseBoolean("1"), CoreseBoolean.class);

        assertTrue(value.booleanValue());
        assertEquals("1", value.getLabel());
        assertNotNull(value.getIDatatype());
    }

    @Test
    void numbersRetainTheirLexicalValueAndLegacyAdapter() throws IOException, ClassNotFoundException {
        CoreseInteger integer = roundTrip(new CoreseInteger("42"), CoreseInteger.class);
        CoreseDecimal decimal = roundTrip(new CoreseDecimal("12.50"), CoreseDecimal.class);

        assertEquals("42", integer.getLabel());
        assertEquals(42, integer.intValue());
        assertEquals(new BigDecimal("12.50"), decimal.decimalValue());
        assertNotNull(integer.getIDatatype());
        assertNotNull(decimal.getIDatatype());
    }

    @Test
    void arbitrarySizeIntegerRetainsItsExactValue() throws IOException, ClassNotFoundException {
        BigInteger value = new BigInteger("1234567890123456789012345678901234567890");

        CoreseInteger integer = roundTrip(new CoreseInteger(value), CoreseInteger.class);

        assertEquals(value.toString(), integer.getLabel());
        assertEquals(value, integer.integerValue());
        assertEquals(value.toString(), integer.getIDatatype().getLabel());
    }

    @Test
    void numericSubtypeRetainsItsCoreDatatype() throws IOException, ClassNotFoundException {
        CoreseInteger integer = roundTrip(
                new CoreseInteger("42", XSDDatatype.INT.getIRI(), XSDDatatype.INT),
                CoreseInteger.class);
        CoreseDecimal decimal = roundTrip(
                new CoreseDecimal("12.5", XSDDatatype.DOUBLE.getIRI(), XSDDatatype.DOUBLE),
                CoreseDecimal.class);

        assertEquals(XSDDatatype.INT, integer.getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE, decimal.getCoreDatatype());
    }

    @Test
    void illTypedLiteralRetainsItsTermAndCoreDatatype() throws IOException, ClassNotFoundException {
        CoreseIllTypedLiteral literal = roundTrip(
                new CoreseIllTypedLiteral("truth", XSDDatatype.BOOLEAN.getIRI(), XSDDatatype.BOOLEAN),
                CoreseIllTypedLiteral.class);

        assertEquals("truth", literal.getLabel());
        assertEquals(XSDDatatype.BOOLEAN.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.BOOLEAN, literal.getCoreDatatype());
        assertEquals("truth", literal.getIDatatype().getLabel());
    }

    @Test
    void durationRetainsItsLexicalValueAndLegacyAdapter() throws IOException, ClassNotFoundException {
        CoreseDuration duration = roundTrip(new CoreseDuration("P2DT3H"), CoreseDuration.class);

        assertEquals("P2DT3H", duration.getLabel());
        assertNotNull(duration.getIDatatype());
    }

    private static <T> T roundTrip(T value, Class<T> type) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return type.cast(input.readObject());
        }
    }
}
