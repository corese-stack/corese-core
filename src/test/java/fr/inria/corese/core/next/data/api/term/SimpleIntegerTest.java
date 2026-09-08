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

class SimpleIntegerTest {

    @ParameterizedTest
    @ValueSource(strings = {"+01", "-0", "0", "42", "-42"})
    void preservesLexicalForm(String label) {
        SimpleInteger literal = new SimpleInteger(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(XSDDatatype.INTEGER.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.INTEGER, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void defaultsAndResolvesDatatypes() {
        assertEquals(new SimpleInteger("1"), new SimpleInteger("1", null));
        assertEquals(XSDDatatype.INTEGER, new SimpleInteger("1", null, null).getCoreDatatype());
        IRI custom = new SimpleIRI("urn:custom:number");
        SimpleInteger literal = new SimpleInteger("1", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.INTEGER, literal.getCoreDatatype());
        assertEquals(XSDDatatype.INTEGER,
                new SimpleInteger("1", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(XSDDatatype.INTEGER,
                new SimpleInteger("1", XSDDatatype.STRING.getIRI()).getCoreDatatype());
        assertEquals(XSDDatatype.INTEGER,
                new SimpleInteger("1", custom, XSDDatatype.STRING).getCoreDatatype());
    }

    @Test
    void equalityUsesLexicalFormDatatypeAndLanguage() {
        SimpleInteger first = new SimpleInteger("1");
        SimpleInteger second = new SimpleInteger("1", XSDDatatype.INTEGER.getIRI());
        SimpleLiteral generic = new SimpleLiteral("1", XSDDatatype.INTEGER.getIRI());
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, generic);
        assertEquals(generic, first);
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleInteger("+01"));
        assertNotEquals(first, new SimpleInteger("2"));
        assertNotEquals(first, new SimpleInteger("1", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("1", "en"));
        assertNotEquals(first, null);
        assertNotEquals(first, "1");
    }

    @Test
    void rejectsMutationAndUnsupportedConversions() {
        SimpleInteger literal = new SimpleInteger("1");
        IncorrectOperationException error = assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING));
        assertEquals("SimpleInteger is immutable", error.getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.INTEGER, literal.getCoreDatatype());
        assertEquals(XSDDatatype.INTEGER.getIRI(), literal.getDatatype());
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
                () -> new SimpleInteger((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleInteger(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleInteger(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "+", "1.2.3", "1.5", "1e2", "NaN", "INF", " 1 "})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(NumberFormatException.class, () -> new SimpleInteger(label));
    }

    @Test
    void constructsFromLongAndBigInteger() {
        for (long value : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            SimpleInteger literal = new SimpleInteger(value);
            assertEquals(Long.toString(value), literal.getLabel());
            assertEquals(BigInteger.valueOf(value), literal.integerValue());
            assertEquals(XSDDatatype.INTEGER, literal.getCoreDatatype());
        }
        BigInteger value = new BigInteger("123456789012345678901234567890");
        SimpleInteger literal = new SimpleInteger(value);
        assertEquals(value.toString(), literal.getLabel());
        assertEquals(value, literal.integerValue());
        assertEquals(new BigDecimal(value), literal.decimalValue());
        assertEquals(value.byteValue(), literal.byteValue());
        assertEquals(value.shortValue(), literal.shortValue());
        assertEquals(value.intValue(), literal.intValue());
        assertEquals(value.longValue(), literal.longValue());
        assertEquals(value.floatValue(), literal.floatValue());
        assertEquals(value.doubleValue(), literal.doubleValue());
        assertEquals("value", assertThrows(NullPointerException.class,
                () -> new SimpleInteger((BigInteger) null)).getMessage());
    }

    @Test
    void resolvesIntegerSubtypesAndExplicitOverride() {
        for (XSDDatatype type : new XSDDatatype[] {XSDDatatype.INTEGER, XSDDatatype.BYTE,
                XSDDatatype.SHORT, XSDDatatype.INT, XSDDatatype.LONG, XSDDatatype.UNSIGNED_BYTE,
                XSDDatatype.UNSIGNED_SHORT, XSDDatatype.UNSIGNED_INT, XSDDatatype.UNSIGNED_LONG,
                XSDDatatype.POSITIVE_INTEGER, XSDDatatype.NEGATIVE_INTEGER,
                XSDDatatype.NON_NEGATIVE_INTEGER, XSDDatatype.NON_POSITIVE_INTEGER}) {
            assertEquals(type, new SimpleInteger("1", type.getIRI()).getCoreDatatype());
            assertEquals(type, new SimpleInteger("1", null, type).getCoreDatatype());
        }
    }

    @Test
    void comparesExactlyWithinIntegerTypeAndAcrossNumericTypes() {
        SimpleInteger smaller = new SimpleInteger("9007199254740992");
        SimpleInteger larger = new SimpleInteger("9007199254740993");
        assertTrue(smaller.compareTo(larger) < 0);
        assertTrue(larger.compareTo(smaller) > 0);
        assertEquals(0, new SimpleInteger("+01").compareTo(new SimpleInteger(1)));
        assertEquals(0, new SimpleInteger(1).compareTo(new SimpleDecimal("1.0")));
        assertTrue(new SimpleInteger(1).compareTo(new SimpleDouble(2.0)) < 0);
    }
}
