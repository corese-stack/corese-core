package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.junit.Assert.assertEquals;

public class CoreseDatetimeTest {

    /**
     * test of the constructor using Corese object
     */
    @Test
    public void constructorCoreseDatetimeTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        fr.inria.corese.core.sparql.datatype.CoreseDateTime coreseDateTime = new fr.inria.corese.core.sparql.datatype.CoreseDateTime(calendar);
        CoreseDatetime newAPICoreseDatetime = new CoreseDatetime(coreseDateTime);

        assertEquals(calendar, newAPICoreseDatetime.calendarValue());
        assertEquals(XSD.xsdDateTime.getIRI().stringValue(), newAPICoreseDatetime.getDatatype().stringValue());
        assertEquals(XSD.xsdDateTime, newAPICoreseDatetime.getCoreDatatype());
    }

    /**
     * Test of the constructor using a string
     */
    @Test
    public void constructorStringTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        CoreseDatetime newAPICoreseDate = new CoreseDatetime("2000-01-15T00:00:00");

        assertEquals(calendar.toXMLFormat(), newAPICoreseDate.calendarValue().toXMLFormat());
        assertEquals(XSD.xsdDateTime.getIRI().stringValue(), newAPICoreseDate.getDatatype().stringValue());
        assertEquals(XSD.xsdDateTime, newAPICoreseDate.getCoreDatatype());
    }

    /**
     * Test of the constructor using a XMLGregorianCalendar
     */
    @Test
    public void constructorXMLGregorianCalendarTest() {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar("2000-01-15T00:00:00");
        CoreseDatetime newAPICoreseDate = new CoreseDatetime(calendar);

        assertEquals(calendar.toXMLFormat(), newAPICoreseDate.calendarValue().toXMLFormat()); // Converted to string format because of freak Xerces cast exception
        assertEquals(XSD.xsdDateTime.getIRI().stringValue(), newAPICoreseDate.getDatatype().stringValue());
        assertEquals(XSD.xsdDateTime, newAPICoreseDate.getCoreDatatype());
    }

    /**
     * Test of the comparison between two dates
     */
    @Test
    public void comparisonTest() {
        CoreseDatetime date1 = new CoreseDatetime("2000-01-15T00:00:00");
        CoreseDatetime date2 = new CoreseDatetime("2000-01-15T00:00:00");
        CoreseDatetime date3 = new CoreseDatetime("2000-01-16T00:00:00");

        assertEquals(0, date1.compareTo(date2));
        assertEquals(0, date2.compareTo(date1));
        assertEquals(-1, date1.compareTo(date3));
        assertEquals(1, date3.compareTo(date1));
    }
}
