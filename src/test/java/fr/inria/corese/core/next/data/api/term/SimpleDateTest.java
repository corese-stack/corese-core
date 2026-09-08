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

class SimpleDateTest {

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29", "2024-02-29Z", "2024-02-29+05:30", "-0001-01-01Z"})
    void preservesLexicalFormAndExposesCalendar(String label) {
        SimpleDate literal = new SimpleDate(label);
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(calendar, literal.calendarValue());
        assertEquals(calendar.toGregorianCalendar().toZonedDateTime(), literal.temporalAccessorValue());
        assertEquals(XSDDatatype.DATE.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DATE, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void constructsFromCalendar() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2024-02-29Z");
        SimpleDate literal = new SimpleDate(calendar);
        assertSame(calendar, literal.calendarValue());
        assertEquals(calendar.toXMLFormat(), literal.getLabel());
        assertEquals(new SimpleDate("2024-02-29Z"), literal);
        assertEquals("calendar", assertThrows(NullPointerException.class,
                () -> new SimpleDate((XMLGregorianCalendar) null)).getMessage());
    }

    @Test
    void handlesDatatypeConstructors() {
        assertEquals(new SimpleDate("2024-02-29Z"), new SimpleDate("2024-02-29Z", null));
        assertEquals(new SimpleDate("2024-02-29Z"), new SimpleDate("2024-02-29Z", null, null));
        IRI custom = new SimpleIRI("urn:custom:temporal");
        SimpleDate literal = new SimpleDate("2024-02-29Z", custom);
        assertEquals(custom, literal.getDatatype());
        assertEquals(XSDDatatype.DATE, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DATE, new SimpleDate("2024-02-29Z", custom, CoreDatatype.NONE).getCoreDatatype());
        assertEquals(custom, new SimpleDate("2024-02-29Z", custom, XSDDatatype.STRING).getDatatype());
    }

    @Test
    void comparesValuesSeparatelyFromRdfTerms() {
        SimpleDate first = new SimpleDate("2024-02-29Z");
        SimpleDate later = new SimpleDate("2024-03-01Z");
        SimpleDate equivalent = new SimpleDate("2024-02-29+00:00");
        assertEquals(DatatypeConstants.LESSER, first.compareTo(later));
        assertEquals(DatatypeConstants.GREATER, later.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(first));
        assertEquals(DatatypeConstants.EQUAL, first.compareTo(equivalent));
        assertNotEquals(first, equivalent);
        assertEquals(DatatypeConstants.INDETERMINATE,
                first.compareTo(new SimpleDate("2024-02-29Z".replace("Z", ""))));
        assertThrows(NullPointerException.class, () -> first.compareTo(null));
    }

    @Test
    void equalityAndHashCodeUseRdfTerms() {
        SimpleDate first = new SimpleDate("2024-02-29Z");
        SimpleDate second = new SimpleDate("2024-02-29Z");
        SimpleLiteral generic = new SimpleLiteral("2024-02-29Z", XSDDatatype.DATE.getIRI());
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first, generic);
        assertEquals(generic, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleDate("2024-03-01Z"));
        assertNotEquals(first, new SimpleDate("2024-02-29Z", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("2024-02-29Z", "en"));
        assertNotEquals(first, null);
        assertNotEquals(first, "2024-02-29Z");
    }

    @Test
    void rejectsDatatypeMutationAndUnsupportedConversions() {
        SimpleDate literal = new SimpleDate("2024-02-29Z");
        assertEquals("SimpleDate is immutable", assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING)).getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.DATE, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DATE.getIRI(), literal.getDatatype());
        assertEquals("2024-02-29Z", literal.getLabel());
        assertThrows(IncorrectOperationException.class, literal::booleanValue);
        assertThrows(IncorrectOperationException.class, literal::intValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAmountValue);
    }

    @Test
    void rejectsNullLexicalValues() {
        assertEquals("lexicalValue", assertThrows(NullPointerException.class,
                () -> new SimpleDate((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleDate(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleDate(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "2023-02-29", "2024-13-01", "2024-01-32"})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleDate(label));
    }
}
