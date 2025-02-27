package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.junit.Assert.assertEquals;

public class CoreseDateTest {

    /**
     * test of the constructor using Corese object
     */
    @Test
    public void constructorCoreseDateTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        fr.inria.corese.core.sparql.datatype.CoreseDate coreseDate = new fr.inria.corese.core.sparql.datatype.CoreseDate(calendar);
        CoreseDate newAPICoreseDate = new CoreseDate(coreseDate);

        assertEquals(calendar, newAPICoreseDate.calendarValue());
        assertEquals(XSD.xsdDate.getIRI().stringValue(), newAPICoreseDate.getDatatype().stringValue());
        assertEquals(XSD.xsdDate, newAPICoreseDate.getCoreDatatype());
    }

    /**
     * Test of the constructor using a string
     */
    @Test
    public void constructorStringTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        CoreseDate newAPICoreseDate = new CoreseDate("2000-01-15T00:00:00");

        assertEquals(calendar.toXMLFormat(), newAPICoreseDate.calendarValue().toXMLFormat());
        assertEquals(XSD.xsdDate.getIRI().stringValue(), newAPICoreseDate.getDatatype().stringValue());
        assertEquals(XSD.xsdDate, newAPICoreseDate.getCoreDatatype());
    }

    /**
     * Test of the constructor using a XMLGregorianCalendar
     */
    @Test
    public void constructorXMLGregorianCalendarTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        CoreseDate newAPICoreseDate = new CoreseDate(calendar);

        assertEquals(calendar.toXMLFormat(), newAPICoreseDate.calendarValue().toXMLFormat()); // Converted to string format because of freak Xerces cast exception
        assertEquals(XSD.xsdDate.getIRI().stringValue(), newAPICoreseDate.getDatatype().stringValue());
        assertEquals(XSD.xsdDate, newAPICoreseDate.getCoreDatatype());
    }

    /**
     * Test of the comparison between two dates
     */
    @Test
    public void comparisonTest() {
        CoreseDate date1 = new CoreseDate("2000-01-15T00:00:00");
        CoreseDate date2 = new CoreseDate("2000-01-15T00:00:00");
        CoreseDate date3 = new CoreseDate("2000-01-16T00:00:00");

        assertEquals(0, date1.compareTo(date2));
        assertEquals(0, date2.compareTo(date1));
        assertEquals(-1, date1.compareTo(date3));
        assertEquals(1, date3.compareTo(date1));
    }
}
