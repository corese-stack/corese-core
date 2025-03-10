package fr.inria.corese.core.next.api.model;

import fr.inria.corese.core.next.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import static org.junit.Assert.*;

public abstract class ValueFactoryTest {

    protected ValueFactory valueFactory;

    @Before
    public abstract void setUp();

    @Test
    public void testCreateIRI() {
        String correctIRI = "http://example.org";
        String incorrectIRI = "test";

        assertNotNull(this.valueFactory.createIRI(correctIRI));
        assertThrows(IncorrectFormatException.class, () -> this.valueFactory.createIRI(incorrectIRI));
    }

    @Test
    public void testCreateBNode() {
    }

    @Test
    public void testCreateLiteralString() {
    }

    @Test
    public void testCreateLiteralStringString() {
    }

    @Test
    public void testCreateLiteralStringIRI() {

        // Temporal point
        // // Datetime
        String fullXSDDateTimeString = "2021-01-01T23:59:59";
        Literal fullXSDDateTime = this.valueFactory.createLiteral(fullXSDDateTimeString, XSD.xsdDateTime.getIRI());
        assertEquals(fullXSDDateTimeString, fullXSDDateTime.stringValue());
        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDate = this.valueFactory.createLiteral(fullXSDDateString, XSD.xsdDate.getIRI());
        assertEquals(fullXSDDateString, fullXSDDate.stringValue());
        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTime = this.valueFactory.createLiteral(fullXSDTimeString, XSD.xsdTime.getIRI());
        assertEquals(fullXSDTimeString, fullXSDTime.stringValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSD.xsdDuration.getIRI());
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());
    }

    @Test
    public void testCreateLiteralStringCoreDatatype() {
        // Temporal point
        // // Datetime
        String fullXSDDateTimeString = "2021-01-01T23:59:59";
        Literal fullXSDDateTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString, XSD.xsdDateTime.getIRI(), XSD.xsdDateTime);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDDateTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateTimeBadDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString,new BasicIRI("http://example.com/test"), XSD.xsdDateTime);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeBadDatatype.stringValue());
        assertNotNull(fullXSDDateTimeBadDatatype.temporalAccessorValue());
        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDateGoodDatatype = this.valueFactory.createLiteral(fullXSDDateString, XSD.xsdDate.getIRI(), XSD.xsdDate);
        assertEquals(fullXSDDateString, fullXSDDateGoodDatatype.stringValue());
        assertNotNull(fullXSDDateGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateBadDatatype = this.valueFactory.createLiteral(fullXSDDateString,new BasicIRI("http://example.com/test"), XSD.xsdDate);
        assertEquals(fullXSDDateString, fullXSDDateBadDatatype.stringValue());
        assertNotNull(fullXSDDateBadDatatype.temporalAccessorValue());
        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDTimeString, XSD.xsdTime.getIRI(), XSD.xsdTime);
        assertEquals(fullXSDTimeString, fullXSDTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDTimeBadDatatype = this.valueFactory.createLiteral(fullXSDTimeString,new BasicIRI("http://example.com/test"), XSD.xsdTime);
        assertEquals(fullXSDTimeString, fullXSDTimeBadDatatype.stringValue());
        assertNotNull(fullXSDTimeBadDatatype.temporalAccessorValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSD.xsdDuration.getIRI());
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());

    }

    @Test
    public void testCreateLiteralStringIRICoreDatatype() {
    }

    @Test
    public void testCreateLiteralBoolean() {
    }

    @Test
    public void testCreateLiteralByte() {
    }

    @Test
    public void testCreateLiteralShort() {
    }

    @Test
    public void testCreateLiteralInt() {
    }

    @Test
    public void testCreateLiteralLong() {
    }

    @Test
    public void testCreateLiteralFloat() {
    }

    @Test
    public void testCreateLiteralDouble() {
    }

    @Test
    public void testCreateLiteralBigDecimal() {
    }

    @Test
    public void testCreateLiteralBigInteger() {
    }

    @Test
    public void testCreateLiteralTemporalAccessor() {
        TemporalAccessor datetime = this.valueFactory.createLiteral(LocalTime.parse("01:01:01")).temporalAccessorValue();
        TemporalAccessor date = this.valueFactory.createLiteral(LocalDate.parse("2021-01-01")).temporalAccessorValue();

        assertNotNull(date);
        assertNotNull(datetime);
    }

    @Test
    public void testCreateLiteralTemporalAmount() {
        Duration duration = Duration.ofHours(23);
        Period period = Period.ofDays(100);

        Literal durationDuration = this.valueFactory.createLiteral(duration);
        Literal periodDuration = this.valueFactory.createLiteral(period);

        assertNotNull(this.valueFactory.createLiteral(duration));
        assertNotNull(this.valueFactory.createLiteral(period));
        assertEquals(duration, durationDuration.temporalAmountValue());
        assertEquals(period, periodDuration.temporalAmountValue());
    }

    @Test
    public void testCreateLiteralXMLGregorianCalendar() throws DatatypeConfigurationException {
        XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("2021-01-01T23:59:59");
        Literal literal = this.valueFactory.createLiteral(calendar);

        assertNotNull(literal);
        assertEquals(calendar, literal.calendarValue());
    }

    @Test
    public void testCreateLiteralDate() throws ParseException {
        String dateString = "2021-01-01";
        Date date = (new SimpleDateFormat("yyyy-MM-dd" )).parse(dateString);
        Literal literal = this.valueFactory.createLiteral(date);

        assertNotNull(literal);
        assertEquals((new SimpleDateFormat("yyyy-MM-dd" )).format(date), literal.stringValue());
    }

    @Test
    public void testCreateStatement() {
    }
}
