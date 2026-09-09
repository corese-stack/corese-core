package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

class SimpleTimeTest {

    @ParameterizedTest
    @ValueSource(strings = {"12:30:45", "12:30:45Z", "12:30:45.123456789+05:30", "00:00:00Z"})
    void preservesLexicalFormAndExposesCalendar(String label) {
        SimpleTime literal = new SimpleTime(label);
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(calendar, literal.calendarValue());
        assertEquals(calendar.toGregorianCalendar().toZonedDateTime(), literal.temporalAccessorValue());
        assertEquals(XSDDatatype.TIME.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.TIME, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void constructsFromCalendar() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("12:30:45Z");
        SimpleTime literal = new SimpleTime(calendar);
        assertSame(calendar, literal.calendarValue());
        assertEquals(calendar.toXMLFormat(), literal.getLabel());
        assertEquals(new SimpleTime("12:30:45Z"), literal);
        assertEquals("calendar", assertThrows(NullPointerException.class,
                () -> new SimpleTime((XMLGregorianCalendar) null)).getMessage());
    }

    @Test
    void handlesDatatypeConstructors() {
        assertEquals(new SimpleTime("12:30:45Z"), new SimpleTime("12:30:45Z", null));
        assertEquals(new SimpleTime("12:30:45Z"), new SimpleTime("12:30:45Z", null, null));
        IRI custom = new SimpleIRI("urn:custom:temporal");
        SimpleTime literal = new SimpleTime("12:30:45Z", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.TIME, literal.getCoreDatatype());
        assertEquals(XSDDatatype.TIME, new SimpleTime("12:30:45Z", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(custom, new SimpleTime("12:30:45Z", custom, XSDDatatype.STRING).getDatatype());
    }

    @Test
    void comparesValuesSeparatelyFromRdfTerms() {
        SimpleTime first = new SimpleTime("12:30:45Z");
        SimpleTime later = new SimpleTime("12:30:46Z");
        SimpleTime equivalent = new SimpleTime("13:30:45+01:00");
        assertEquals(DatatypeConstants.LESSER, first.compareTo(later));
        assertEquals(DatatypeConstants.GREATER, later.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(equivalent));
        assertNotEquals(first, equivalent);
        SimpleTime withoutTimezone = new SimpleTime("12:30:45");
        // Preserve XMLGregorianCalendar's ordering for timezone-less time values.
        assertEquals(first.calendarValue().compare(withoutTimezone.calendarValue()),
                first.compareTo(withoutTimezone));
        assertThrows(NullPointerException.class, () -> first.compareTo(null));
    }

    @Test
    void equalityAndHashCodeUseRdfTerms() {
        SimpleTime first = new SimpleTime("12:30:45Z");
        SimpleTime second = new SimpleTime("12:30:45Z");
        SimpleLiteral generic = new SimpleLiteral("12:30:45Z", XSDDatatype.TIME.getIRI());
        Literal firstTerm = first;
        Literal genericTerm = generic;
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(firstTerm, genericTerm);
        assertEquals(genericTerm, firstTerm);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleTime("12:30:46Z"));
        assertNotEquals(first, new SimpleTime("12:30:45Z", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("12:30:45Z", "en"));
        assertNotEquals((Object) null, first);
        assertNotEquals((Object) "12:30:45Z", first);
    }

    @Test
    void rejectsDatatypeMutationAndUnsupportedConversions() {
        SimpleTime literal = new SimpleTime("12:30:45Z");
        assertEquals("SimpleTime is immutable", assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING)).getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.TIME, literal.getCoreDatatype());
        assertEquals(XSDDatatype.TIME.getIRI(), literal.getDatatype());
        assertEquals("12:30:45Z", literal.getLabel());
        assertThrows(IncorrectOperationException.class, literal::booleanValue);
        assertThrows(IncorrectOperationException.class, literal::intValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAmountValue);
    }

    @Test
    void rejectsNullLexicalValues() {
        assertEquals("lexicalValue", assertThrows(NullPointerException.class,
                () -> new SimpleTime((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleTime(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleTime(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "25:00:00", "12:60:00"})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleTime(label));
    }
}
