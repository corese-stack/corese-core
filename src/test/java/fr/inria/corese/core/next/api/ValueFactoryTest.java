package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.impl.common.BasicIRI;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
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
        Literal fullXSDDateTime = this.valueFactory.createLiteral(fullXSDDateTimeString, XSD.DATETIME.getIRI());
        assertEquals(fullXSDDateTimeString, fullXSDDateTime.stringValue());
        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDate = this.valueFactory.createLiteral(fullXSDDateString, XSD.DATE.getIRI());
        assertEquals(fullXSDDateString, fullXSDDate.stringValue());
        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTime = this.valueFactory.createLiteral(fullXSDTimeString, XSD.TIME.getIRI());
        assertEquals(fullXSDTimeString, fullXSDTime.stringValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSD.DURATION.getIRI());
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());

        // Number

        // // Integer
        String integerString = "-42";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, XSD.INTEGER.getIRI());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // NonNegativeInteger
        String nonNegativeIntegerString = "42";
        Literal nonnegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, XSD.NON_NEGATIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(nonNegativeIntegerString), nonnegativeIntegerLiteral.intValue());

        // // NonPositiveInteger
        String nonPositiveIntegerString = "-42";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, XSD.NON_POSITIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(nonPositiveIntegerString), nonPositiveIntegerLiteral.intValue());

        // // PositiveInteger
        String positiveIntegerString = "42";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, XSD.POSITIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(positiveIntegerString), positiveIntegerLiteral.intValue());

        // // NegativeInteger
        String negativeIntegerString = "-42";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, XSD.NEGATIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(negativeIntegerString), negativeIntegerLiteral.intValue());

        // // Int
        String intString = "-42";
        Literal intLiteral = this.valueFactory.createLiteral(intString, XSD.INT.getIRI());
        assertEquals(Integer.parseInt(intString), intLiteral.intValue());

        // // UnsignedInt
        String unsignedIntString = "42";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, XSD.UNSIGNED_INT.getIRI());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());

        // // Long
        String longString = "-1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, XSD.LONG.getIRI());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // UnsignedLong
        String unsignedLongString = "1234567890123456789";
        Literal unsignedLongLiteral = this.valueFactory.createLiteral(unsignedLongString, XSD.UNSIGNED_LONG.getIRI());
        assertEquals(Long.parseLong(unsignedLongString), unsignedLongLiteral.longValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, XSD.DECIMAL.getIRI());
        assertEquals(Double.parseDouble(decimalString), decimalLiteral.doubleValue(), 0);

        // // short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, XSD.SHORT.getIRI());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, XSD.UNSIGNED_SHORT.getIRI());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "-64";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, XSD.BYTE.getIRI());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, XSD.UNSIGNED_BYTE.getIRI());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());

        // // float
        String floatString = "345.2345";
        Literal floatLiteral = this.valueFactory.createLiteral(floatString, XSD.FLOAT.getIRI());
        assertEquals(Float.parseFloat(floatString), floatLiteral.floatValue(), 0);

        // // double
        String doubleString = "345678.3456789";
        Literal doubleLiteral = this.valueFactory.createLiteral(doubleString, XSD.DOUBLE.getIRI());
        assertEquals(Double.parseDouble(doubleString), doubleLiteral.doubleValue(), 0);
    }

    @Test
    public void testCreateLiteralStringCoreDatatype() {
        // Temporal point
        // // Datetime
        String fullXSDDateTimeString = "2021-01-01T23:59:59";
        Literal fullXSDDateTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString, XSD.DATETIME.getIRI(), XSD.DATETIME);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDDateTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateTimeBadDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString,new BasicIRI("http://example.com/test"), XSD.DATETIME);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeBadDatatype.stringValue());
        assertNotNull(fullXSDDateTimeBadDatatype.temporalAccessorValue());

        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDateGoodDatatype = this.valueFactory.createLiteral(fullXSDDateString, XSD.DATE.getIRI(), XSD.DATE);
        assertEquals(fullXSDDateString, fullXSDDateGoodDatatype.stringValue());
        assertNotNull(fullXSDDateGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateBadDatatype = this.valueFactory.createLiteral(fullXSDDateString,new BasicIRI("http://example.com/test"), XSD.DATE);
        assertEquals(fullXSDDateString, fullXSDDateBadDatatype.stringValue());
        assertNotNull(fullXSDDateBadDatatype.temporalAccessorValue());

        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDTimeString, XSD.TIME.getIRI(), XSD.TIME);
        assertEquals(fullXSDTimeString, fullXSDTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDTimeBadDatatype = this.valueFactory.createLiteral(fullXSDTimeString,new BasicIRI("http://example.com/test"), XSD.TIME);
        assertEquals(fullXSDTimeString, fullXSDTimeBadDatatype.stringValue());
        assertNotNull(fullXSDTimeBadDatatype.temporalAccessorValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSD.DURATION.getIRI(), XSD.DURATION);
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());

        // Numbers

        // // Integer
        String integerString = "1234567890";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, XSD.INTEGER);
        assertEquals(integerString, integerLiteral.stringValue());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, XSD.DECIMAL.getIRI(), XSD.DECIMAL);
        assertEquals(decimalString, decimalLiteral.stringValue());
        assertEquals(new BigDecimal(decimalString), decimalLiteral.decimalValue());

        // // Long
        String longString = "1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, XSD.LONG);
        assertEquals(longString, longLiteral.stringValue());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // Short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, XSD.SHORT);
        assertEquals(shortString, shortLiteral.stringValue());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, XSD.UNSIGNED_SHORT);
        assertEquals(unsignedShortString, unsignedShortLiteral.stringValue());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "127";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, XSD.BYTE);
        assertEquals(byteString, byteLiteral.stringValue());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, XSD.UNSIGNED_BYTE);
        assertEquals(unsignedByteString, unsignedByteLiteral.stringValue());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());

        // // Float
        String floatString = "345678.3456789";
        Literal floatLiteral = this.valueFactory.createLiteral(floatString, XSD.FLOAT);
        assertEquals(floatString, floatLiteral.stringValue());
        assertEquals(Float.parseFloat(floatString), floatLiteral.floatValue(), 0);

        // // Double
        String doubleString = "345678.3456789";
        Literal doubleLiteral = this.valueFactory.createLiteral(doubleString, XSD.DOUBLE);
        assertEquals(doubleString, doubleLiteral.stringValue());
        assertEquals(Double.parseDouble(doubleString), doubleLiteral.doubleValue(), 0);

        // // Int
        String intString = "1234567890";
        Literal intLiteral = this.valueFactory.createLiteral(intString, XSD.INT);
        assertEquals(intString, intLiteral.stringValue());

        // // Non negative integer
        String nonNegativeIntegerString = "1234567890";
        Literal nonNegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, XSD.NON_NEGATIVE_INTEGER);
        assertEquals(nonNegativeIntegerString, nonNegativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonNegativeIntegerString), nonNegativeIntegerLiteral.longValue());

        // // Non positive integer
        String nonPositiveIntegerString = "-1234567890";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, XSD.NON_POSITIVE_INTEGER);
        assertEquals(nonPositiveIntegerString, nonPositiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonPositiveIntegerString), nonPositiveIntegerLiteral.longValue());

        // // Positive integer
        String positiveIntegerString = "1234567890";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, XSD.POSITIVE_INTEGER);
        assertEquals(positiveIntegerString, positiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(positiveIntegerString), positiveIntegerLiteral.longValue());

        // // Negative integer
        String negativeIntegerString = "-1234567890";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, XSD.NEGATIVE_INTEGER);
        assertEquals(negativeIntegerString, negativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(negativeIntegerString), negativeIntegerLiteral.longValue());

        // // Unsigned long
        String unsignedLongString = "1234567890123456789";
        Literal unsignedLongLiteral = this.valueFactory.createLiteral(unsignedLongString, XSD.UNSIGNED_LONG);
        assertEquals(unsignedLongString, unsignedLongLiteral.stringValue());
        assertEquals(Long.parseLong(unsignedLongString), unsignedLongLiteral.longValue());

        // // Unsigned int
        String unsignedIntString = "1234567890";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, XSD.UNSIGNED_INT);
        assertEquals(unsignedIntString, unsignedIntLiteral.stringValue());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());
    }

    @Test
    public void testCreateLiteralStringIRICoreDatatype() {
        // Numeric Datatypes

        // // Integer
        String integerString = "1234567890";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, new BasicIRI("http://example.com/test"), XSD.INTEGER);
        assertEquals(integerString, integerLiteral.stringValue());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // Non Negative Integer
        String nonNegativeIntegerString = "1234567890";
        Literal nonNegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, new BasicIRI("http://example.com/test"), XSD.NON_NEGATIVE_INTEGER);
        assertEquals(nonNegativeIntegerString, nonNegativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonNegativeIntegerString), nonNegativeIntegerLiteral.longValue());

        // // Non Positive Integer
        String nonPositiveIntegerString = "-1234567890";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, new BasicIRI("http://example.com/test"), XSD.NON_POSITIVE_INTEGER);
        assertEquals(nonPositiveIntegerString, nonPositiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonPositiveIntegerString), nonPositiveIntegerLiteral.longValue());

        // // Positive Integer
        String positiveIntegerString = "1234567890";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, new BasicIRI("http://example.com/test"), XSD.POSITIVE_INTEGER);
        assertEquals(positiveIntegerString, positiveIntegerLiteral.stringValue());
        assertEquals(Integer.parseInt(positiveIntegerString), positiveIntegerLiteral.intValue());

        // // Negative Integer
        String negativeIntegerString = "-1234567890";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, new BasicIRI("http://example.com/test"), XSD.NEGATIVE_INTEGER);
        assertEquals(negativeIntegerString, negativeIntegerLiteral.stringValue());
        assertEquals(Integer.parseInt(negativeIntegerString), negativeIntegerLiteral.intValue());

        // // Long
        String longString = "1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, new BasicIRI("http://example.com/test"), XSD.LONG);
        assertEquals(longString, longLiteral.stringValue());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, new BasicIRI("http://example.com/test"), XSD.DECIMAL);
        assertEquals(decimalString, decimalLiteral.stringValue());
        assertEquals(new BigDecimal(decimalString), decimalLiteral.decimalValue());

        // // Int
        String intString = "1234567890";
        Literal intLiteral = this.valueFactory.createLiteral(intString, new BasicIRI("http://example.com/test"), XSD.INT);
        assertEquals(intString, intLiteral.stringValue());
        assertEquals(Integer.parseInt(intString), intLiteral.intValue());

        // // Unsigned Int
        String unsignedIntString = "1234567890";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, new BasicIRI("http://example.com/test"), XSD.UNSIGNED_INT);
        assertEquals(unsignedIntString, unsignedIntLiteral.stringValue());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());

        // // Short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, new BasicIRI("http://example.com/test"), XSD.SHORT);
        assertEquals(shortString, shortLiteral.stringValue());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, new BasicIRI("http://example.com/test"), XSD.UNSIGNED_SHORT);
        assertEquals(unsignedShortString, unsignedShortLiteral.stringValue());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "127";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, new BasicIRI("http://example.com/test"), XSD.BYTE);
        assertEquals(byteString, byteLiteral.stringValue());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, new BasicIRI("http://example.com/test"), XSD.UNSIGNED_BYTE);
        assertEquals(unsignedByteString, unsignedByteLiteral.stringValue());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());
    }

    @Test
    public void testCreateLiteralBoolean() {
    }

    @Test
    public void testCreateLiteralByte() {
        byte b = 64;
        Literal literal = this.valueFactory.createLiteral(b);
        assertEquals(b, literal.byteValue());
    }

    @Test
    public void testCreateLiteralShort() {
        short s = 7851;
        Literal literal = this.valueFactory.createLiteral(s);
        assertEquals(s, literal.shortValue());
    }

    @Test
    public void testCreateLiteralInt() {
        int i = 1234567890;
        Literal literal = this.valueFactory.createLiteral(i);
        assertEquals(i, literal.intValue());
    }

    @Test
    public void testCreateLiteralLong() {
        long l = 1234567890123456789L;
        Literal literal = this.valueFactory.createLiteral(l);
        assertEquals(l, literal.longValue());
    }

    @Test
    public void testCreateLiteralFloat() {
        float f = 1234567890.1234567890123456789f;
        Literal literal = this.valueFactory.createLiteral(f);
        assertEquals(f, literal.floatValue(), 0);
    }

    @Test
    public void testCreateLiteralDouble() {
        double d = 1234567890.1234567890123456789;
        Literal literal = this.valueFactory.createLiteral(d);
        assertEquals(d, literal.doubleValue(), 0);
    }

    @Test
    public void testCreateLiteralBigDecimal() {
        BigDecimal bd = new BigDecimal("1234567890.1234567890123456789");
        Literal literal = this.valueFactory.createLiteral(bd);
        assertEquals(bd, literal.decimalValue());
    }

    @Test
    public void testCreateLiteralBigInteger() {
        BigInteger bi = new BigInteger("1234567890123456789");
        Literal literal = this.valueFactory.createLiteral(bi);
        assertEquals(bi, literal.integerValue());
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
