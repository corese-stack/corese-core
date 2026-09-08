package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

class SimpleDoubleTest {

    @ParameterizedTest
    @ValueSource(strings = {"+01.0", "-0", "0", "42", "-42"})
    void preservesLexicalForm(String label) {
        SimpleDouble literal = new SimpleDouble(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(XSDDatatype.DOUBLE.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DOUBLE, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void defaultsAndResolvesDatatypes() {
        assertEquals(new SimpleDouble("1"), new SimpleDouble("1", null));
        assertEquals(XSDDatatype.DOUBLE, new SimpleDouble("1", null, null).getCoreDatatype());
        IRI custom = new SimpleIRI("urn:custom:number");
        SimpleDouble literal = new SimpleDouble("1", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.DOUBLE, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE,
                new SimpleDouble("1", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE,
                new SimpleDouble("1", XSDDatatype.STRING.getIRI()).getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE,
                new SimpleDouble("1", custom, XSDDatatype.STRING).getCoreDatatype());
    }

    @Test
    void equalityUsesLexicalFormDatatypeAndLanguage() {
        SimpleDouble first = new SimpleDouble("1");
        SimpleDouble second = new SimpleDouble("1", XSDDatatype.DOUBLE.getIRI());
        SimpleLiteral generic = new SimpleLiteral("1", XSDDatatype.DOUBLE.getIRI());
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, generic);
        assertEquals(generic, first);
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleDouble("+01"));
        assertNotEquals(first, new SimpleDouble("2"));
        assertNotEquals(first, new SimpleDouble("1", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("1", "en"));
        assertNotEquals(first, null);
        assertNotEquals(first, "1");
    }

    @Test
    void rejectsMutationAndUnsupportedConversions() {
        SimpleDouble literal = new SimpleDouble("1");
        IncorrectOperationException error = assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING));
        assertEquals("SimpleDouble is immutable", error.getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.DOUBLE, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE.getIRI(), literal.getDatatype());
        assertEquals("1", literal.getLabel());
        assertThrows(IncorrectOperationException.class, literal::booleanValue);
        assertThrows(IncorrectOperationException.class, literal::calendarValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAccessorValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAmountValue);
        assertThrows(NullPointerException.class, () -> literal.compareTo(null));
    }

    @Test
    void rejectsNullLexicalValues() {
        assertEquals("lexicalValue", assertThrows(NullPointerException.class,
                () -> new SimpleDouble((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleDouble(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleDouble(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "+", "1.2.3"})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(NumberFormatException.class, () -> new SimpleDouble(label));
    }

    @Test
    void constructsDoubleAndFloatWithTheirOwnLabelsAndDatatypes() {
        SimpleDouble literal = new SimpleDouble(0.1);
        assertEquals("0.1", literal.getLabel());
        assertEquals(0.1, literal.doubleValue());
        assertEquals(XSDDatatype.DOUBLE.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DOUBLE, literal.getCoreDatatype());
        SimpleDouble single = new SimpleDouble(0.1f);
        assertEquals(Float.toString(0.1f), single.getLabel());
        assertEquals(0.1f, single.floatValue());
        assertEquals(XSDDatatype.FLOAT.getIRI(), single.getDatatype());
        assertEquals(XSDDatatype.FLOAT, single.getCoreDatatype());
        assertEquals(XSDDatatype.FLOAT,
                new SimpleDouble("1", XSDDatatype.FLOAT.getIRI()).getCoreDatatype());
        assertEquals(XSDDatatype.FLOAT, new SimpleDouble("1", null, XSDDatatype.FLOAT).getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE, new SimpleDouble("1", null, XSDDatatype.DOUBLE).getCoreDatatype());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-123.75, 0, 32768.5, 1e30, -1e30})
    void convertsFiniteValues(double value) {
        SimpleDouble literal = new SimpleDouble(value);
        assertEquals((byte) value, literal.byteValue());
        assertEquals((short) value, literal.shortValue());
        assertEquals((int) value, literal.intValue());
        assertEquals((long) value, literal.longValue());
        assertEquals(BigInteger.valueOf((long) value), literal.integerValue());
        assertEquals(BigDecimal.valueOf(value), literal.decimalValue());
        assertEquals((float) value, literal.floatValue());
        assertEquals(value, literal.doubleValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "INF", "+INF", "-INF", "Infinity", "-Infinity"})
    void handlesSpecialValues(String label) {
        SimpleDouble literal = new SimpleDouble(label);
        double expected = switch (label) {
            case "NaN" -> Double.NaN;
            case "-INF", "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> Double.POSITIVE_INFINITY;
        };
        assertEquals(label, literal.getLabel());
        assertEquals(expected, literal.doubleValue());
        assertEquals((float) expected, literal.floatValue());
        assertEquals((byte) expected, literal.byteValue());
        assertEquals((short) expected, literal.shortValue());
        assertEquals((int) expected, literal.intValue());
        assertEquals((long) expected, literal.longValue());
        assertEquals(BigInteger.valueOf((long) expected), literal.integerValue());
        assertEquals("Cannot convert NaN/INF to BigDecimal",
                assertThrows(IncorrectOperationException.class, literal::decimalValue).getMessage());
        assertEquals(expected, new SimpleDouble(expected).doubleValue());
        assertEquals(expected, new SimpleDouble((float) expected).doubleValue());
    }

    @Test
    void comparesSpecialValuesSignedZeroAndOtherNumericTypes() {
        SimpleDouble negativeZero = new SimpleDouble("-0");
        assertEquals(-0.0, negativeZero.doubleValue());
        assertTrue(negativeZero.compareTo(new SimpleDouble("0")) < 0);
        assertTrue(new SimpleDouble("-INF").compareTo(new SimpleDouble(-1.0)) < 0);
        assertTrue(new SimpleDouble("INF").compareTo(new SimpleDouble(Double.MAX_VALUE)) > 0);
        assertTrue(new SimpleDouble("NaN").compareTo(new SimpleDouble("INF")) > 0);
        assertEquals(0, new SimpleDouble("NaN").compareTo(new SimpleDouble("NaN")));
        assertEquals(0, new SimpleDouble("1e0").compareTo(new SimpleDouble("1.0")));
        assertEquals(0, new SimpleDouble(1.0).compareTo(new SimpleInteger(1)));
        assertTrue(new SimpleDouble(1.0).compareTo(new SimpleDecimal("2")) < 0);
        assertNotEquals(new SimpleDouble("INF"), new SimpleDouble("+INF"));
        assertNotEquals(new SimpleDouble(1.0), new SimpleDouble(1.0f));
    }
}
