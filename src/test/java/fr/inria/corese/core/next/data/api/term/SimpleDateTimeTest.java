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

class SimpleDateTimeTest {

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29T12:30:45", "2024-02-29T12:30:45Z", "2024-02-29T12:30:45.123456789+05:30"})
    void preservesLexicalFormAndExposesCalendar(String label) {
        SimpleDateTime literal = new SimpleDateTime(label);
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(calendar, literal.calendarValue());
        assertEquals(calendar.toGregorianCalendar().toZonedDateTime(), literal.temporalAccessorValue());
        assertEquals(XSDDatatype.DATETIME.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DATETIME, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void constructsFromCalendar() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2024-02-29T12:30:45Z");
        SimpleDateTime literal = new SimpleDateTime(calendar);
        assertSame(calendar, literal.calendarValue());
        assertEquals(calendar.toXMLFormat(), literal.getLabel());
        assertEquals(new SimpleDateTime("2024-02-29T12:30:45Z"), literal);
        assertEquals("calendar", assertThrows(NullPointerException.class,
                () -> new SimpleDateTime((XMLGregorianCalendar) null)).getMessage());
    }

    @Test
    void handlesDatatypeConstructors() {
        assertEquals(new SimpleDateTime("2024-02-29T12:30:45Z"), new SimpleDateTime("2024-02-29T12:30:45Z", null));
        assertEquals(new SimpleDateTime("2024-02-29T12:30:45Z"), new SimpleDateTime("2024-02-29T12:30:45Z", null, null));
        IRI custom = new SimpleIRI("urn:custom:temporal");
        SimpleDateTime literal = new SimpleDateTime("2024-02-29T12:30:45Z", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.DATETIME, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DATETIME, new SimpleDateTime("2024-02-29T12:30:45Z", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(custom, new SimpleDateTime("2024-02-29T12:30:45Z", custom, XSDDatatype.STRING).getDatatype());
    }

    @Test
    void comparesValuesSeparatelyFromRdfTerms() {
        SimpleDateTime first = new SimpleDateTime("2024-02-29T12:30:45Z");
        SimpleDateTime later = new SimpleDateTime("2024-02-29T12:30:46Z");
        SimpleDateTime equivalent = new SimpleDateTime("2024-02-29T13:30:45+01:00");
        assertEquals(DatatypeConstants.LESSER, first.compareTo(later));
        assertEquals(DatatypeConstants.GREATER, later.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(equivalent));
        assertNotEquals(first, equivalent);
        assertEquals(DatatypeConstants.INDETERMINATE,
                first.compareTo(new SimpleDateTime("2024-02-29T12:30:45Z".replace("Z", ""))));
        assertThrows(NullPointerException.class, () -> first.compareTo(null));
    }

    @Test
    void equalityAndHashCodeUseRdfTerms() {
        SimpleDateTime first = new SimpleDateTime("2024-02-29T12:30:45Z");
        SimpleDateTime second = new SimpleDateTime("2024-02-29T12:30:45Z");
        SimpleLiteral generic = new SimpleLiteral("2024-02-29T12:30:45Z", XSDDatatype.DATETIME.getIRI());
        Literal firstTerm = first;
        Literal genericTerm = generic;
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(firstTerm, genericTerm);
        assertEquals(genericTerm, firstTerm);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleDateTime("2024-02-29T12:30:46Z"));
        assertNotEquals(first, new SimpleDateTime("2024-02-29T12:30:45Z", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("2024-02-29T12:30:45Z", "en"));
        assertNotEquals((Object) null, first);
        assertNotEquals((Object) "2024-02-29T12:30:45Z", first);
    }

    @Test
    void rejectsDatatypeMutationAndUnsupportedConversions() {
        SimpleDateTime literal = new SimpleDateTime("2024-02-29T12:30:45Z");
        assertEquals("SimpleDateTime is immutable", assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING)).getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.DATETIME, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DATETIME.getIRI(), literal.getDatatype());
        assertEquals("2024-02-29T12:30:45Z", literal.getLabel());
        assertThrows(IncorrectOperationException.class, literal::booleanValue);
        assertThrows(IncorrectOperationException.class, literal::intValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAmountValue);
    }

    @Test
    void rejectsNullLexicalValues() {
        assertEquals("lexicalValue", assertThrows(NullPointerException.class,
                () -> new SimpleDateTime((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleDateTime(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleDateTime(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "2023-02-29T12:00:00", "2024-01-01T25:00:00"})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleDateTime(label));
    }
}
