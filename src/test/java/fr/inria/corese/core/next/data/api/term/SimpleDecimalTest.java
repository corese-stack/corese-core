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

class SimpleDecimalTest {

    @ParameterizedTest
    @ValueSource(strings = {"+01.00", "-0", "0", "42", "-42"})
    void preservesLexicalForm(String label) {
        SimpleDecimal literal = new SimpleDecimal(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(XSDDatatype.DECIMAL.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DECIMAL, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void defaultsAndResolvesDatatypes() {
        assertEquals(new SimpleDecimal("1"), new SimpleDecimal("1", null));
        assertEquals(XSDDatatype.DECIMAL, new SimpleDecimal("1", null, null).getCoreDatatype());
        IRI custom = new SimpleIRI("urn:custom:number");
        SimpleDecimal literal = new SimpleDecimal("1", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.DECIMAL, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DECIMAL,
                new SimpleDecimal("1", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(XSDDatatype.DECIMAL,
                new SimpleDecimal("1", XSDDatatype.STRING.getIRI()).getCoreDatatype());
        assertEquals(XSDDatatype.DECIMAL,
                new SimpleDecimal("1", custom, XSDDatatype.STRING).getCoreDatatype());
    }

    @Test
    void equalityUsesLexicalFormDatatypeAndLanguage() {
        SimpleDecimal first = new SimpleDecimal("1");
        SimpleDecimal second = new SimpleDecimal("1", XSDDatatype.DECIMAL.getIRI());
        SimpleLiteral generic = new SimpleLiteral("1", XSDDatatype.DECIMAL.getIRI());
        Literal firstTerm = first;
        Literal genericTerm = generic;
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(firstTerm, genericTerm);
        assertEquals(genericTerm, firstTerm);
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleDecimal("+01"));
        assertNotEquals(first, new SimpleDecimal("2"));
        assertNotEquals(first, new SimpleDecimal("1", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("1", "en"));
        assertNotEquals((Object) null, first);
        assertNotEquals((Object) "1", first);
    }

    @Test
    void rejectsMutationAndUnsupportedConversions() {
        SimpleDecimal literal = new SimpleDecimal("1");
        IncorrectOperationException error = assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING));
        assertEquals("SimpleDecimal is immutable", error.getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.DECIMAL, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DECIMAL.getIRI(), literal.getDatatype());
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
                () -> new SimpleDecimal((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleDecimal(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleDecimal(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "+", "1.2.3", "NaN", "INF", " 1 "})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(NumberFormatException.class, () -> new SimpleDecimal(label));
    }

    @Test
    void constructsFromBigDecimalAndDouble() {
        BigDecimal value = new BigDecimal("-12345678901234567890.1250");
        SimpleDecimal literal = new SimpleDecimal(value);
        assertEquals(value.toPlainString(), literal.getLabel());
        assertEquals(value, literal.decimalValue());
        assertEquals(value.toBigInteger(), literal.integerValue());
        assertEquals(value.byteValue(), literal.byteValue());
        assertEquals(value.shortValue(), literal.shortValue());
        assertEquals(value.intValue(), literal.intValue());
        assertEquals(value.longValue(), literal.longValue());
        assertEquals(value.floatValue(), literal.floatValue());
        assertEquals(value.doubleValue(), literal.doubleValue());
        assertEquals("1000", new SimpleDecimal(new BigDecimal("1E+3")).getLabel());
        assertEquals(new BigDecimal("0.1"), new SimpleDecimal(0.1).decimalValue());
        assertEquals(XSDDatatype.DECIMAL, literal.getCoreDatatype());
        assertEquals("value", assertThrows(NullPointerException.class,
                () -> new SimpleDecimal((BigDecimal) null)).getMessage());
        assertThrows(NumberFormatException.class, () -> new SimpleDecimal(Double.NaN));
        assertThrows(NumberFormatException.class, () -> new SimpleDecimal(Double.POSITIVE_INFINITY));
        assertThrows(NumberFormatException.class, () -> new SimpleDecimal(Double.NEGATIVE_INFINITY));
        assertEquals(BigInteger.valueOf(-1), new SimpleDecimal("-1.9").integerValue());
    }

    @Test
    void resolvesDecimalFamilyAndExplicitOverride() {
        for (XSDDatatype type : new XSDDatatype[] {XSDDatatype.DECIMAL, XSDDatatype.FLOAT, XSDDatatype.DOUBLE}) {
            assertEquals(type, new SimpleDecimal("1", type.getIRI()).getCoreDatatype());
            assertEquals(type, new SimpleDecimal("1", null, type).getCoreDatatype());
        }
    }

    @Test
    void comparesExactlyWithinDecimalTypeAndAcrossNumericTypes() {
        SimpleDecimal smaller = new SimpleDecimal("1.00000000000000000001");
        SimpleDecimal larger = new SimpleDecimal("1.00000000000000000002");
        assertTrue(smaller.compareTo(larger) < 0);
        assertTrue(larger.compareTo(smaller) > 0);
        assertEquals(0, new SimpleDecimal("1.00").compareTo(new SimpleDecimal("1.0")));
        assertEquals(0, new SimpleDecimal("1.0").compareTo(new SimpleInteger(1)));
        assertTrue(new SimpleDecimal("1.0").compareTo(new SimpleDouble(2.0)) < 0);
    }
}
