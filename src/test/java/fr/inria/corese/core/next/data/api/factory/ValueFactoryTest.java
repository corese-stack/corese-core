package fr.inria.corese.core.next.data.api.factory;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.*;

public abstract class ValueFactoryTest {

    protected ValueFactory valueFactory;

    @BeforeEach
    public abstract void setUp();

    @Test
    void testCreateIRI() {
        String correctIRI = "http://example.org";
        String incorrectIRI = "test";

        assertNotNull(this.valueFactory.createIRI(correctIRI));
        assertThrows(IncorrectFormatException.class, () -> this.valueFactory.createIRI(incorrectIRI));
    }

    @Test
    void testCreateBNode() {
        assertNotNull(this.valueFactory.createBNode(), "Created BNode should not be null");
    }

    @Test
    void testCreateLiteralString() {
        Literal lit = this.valueFactory.createLiteral("test");
        assertNotNull(lit, "Created literal should not be null");
        assertEquals("test", lit.getLabel(), "Literal label should match input");
    }

    @Test
    void testCreateLiteralStringString() {
        Literal lit = this.valueFactory.createLiteral("hello", "en");
        assertNotNull(lit, "Created literal should not be null");
        assertEquals("hello", lit.getLabel(), "Literal label should match input");
        assertTrue(lit.getLanguage().isPresent(), "Language tag should be present");
        assertEquals("en", lit.getLanguage().get(), "Language tag should match input");
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testCreateLiteralStringIRI() {

        // Temporal point
        // // Datetime
        String fullXSDDateTimeString = "2021-01-01T23:59:59";
        Literal fullXSDDateTime = this.valueFactory.createLiteral(fullXSDDateTimeString, XSDDatatype.DATETIME.getIRI());
        assertEquals(fullXSDDateTimeString, fullXSDDateTime.stringValue());
        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDate = this.valueFactory.createLiteral(fullXSDDateString, XSDDatatype.DATE.getIRI());
        assertEquals(fullXSDDateString, fullXSDDate.stringValue());
        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTime = this.valueFactory.createLiteral(fullXSDTimeString, XSDDatatype.TIME.getIRI());
        assertEquals(fullXSDTimeString, fullXSDTime.stringValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSDDatatype.DURATION.getIRI());
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());

        // Number

        // // Integer
        String integerString = "-42";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, XSDDatatype.INTEGER.getIRI());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // NonNegativeInteger
        String nonNegativeIntegerString = "42";
        Literal nonnegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, XSDDatatype.NON_NEGATIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(nonNegativeIntegerString), nonnegativeIntegerLiteral.intValue());

        // // NonPositiveInteger
        String nonPositiveIntegerString = "-42";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, XSDDatatype.NON_POSITIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(nonPositiveIntegerString), nonPositiveIntegerLiteral.intValue());

        // // PositiveInteger
        String positiveIntegerString = "42";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, XSDDatatype.POSITIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(positiveIntegerString), positiveIntegerLiteral.intValue());

        // // NegativeInteger
        String negativeIntegerString = "-42";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, XSDDatatype.NEGATIVE_INTEGER.getIRI());
        assertEquals(Integer.parseInt(negativeIntegerString), negativeIntegerLiteral.intValue());

        // // Int
        String intString = "-42";
        Literal intLiteral = this.valueFactory.createLiteral(intString, XSDDatatype.INT.getIRI());
        assertEquals(Integer.parseInt(intString), intLiteral.intValue());

        // // UnsignedInt
        String unsignedIntString = "42";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, XSDDatatype.UNSIGNED_INT.getIRI());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());

        // // Long
        String longString = "-1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, XSDDatatype.LONG.getIRI());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // UnsignedLong
        String unsignedLongString = "1234567890123456789";
        Literal unsignedLongLiteral = this.valueFactory.createLiteral(unsignedLongString, XSDDatatype.UNSIGNED_LONG.getIRI());
        assertEquals(Long.parseLong(unsignedLongString), unsignedLongLiteral.longValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, XSDDatatype.DECIMAL.getIRI());
        assertEquals(Double.parseDouble(decimalString), decimalLiteral.doubleValue(), 0);

        // // short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, XSDDatatype.SHORT.getIRI());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, XSDDatatype.UNSIGNED_SHORT.getIRI());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "-64";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, XSDDatatype.BYTE.getIRI());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, XSDDatatype.UNSIGNED_BYTE.getIRI());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());

        // // float
        String floatString = "345.2345";
        Literal floatLiteral = this.valueFactory.createLiteral(floatString, XSDDatatype.FLOAT.getIRI());
        assertEquals(Float.parseFloat(floatString), floatLiteral.floatValue(), 0);

        // // double
        String doubleString = "345678.3456789";
        Literal doubleLiteral = this.valueFactory.createLiteral(doubleString, XSDDatatype.DOUBLE.getIRI());
        assertEquals(Double.parseDouble(doubleString), doubleLiteral.doubleValue(), 0);
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testCreateLiteralStringCoreDatatype() {
        // Temporal point
        // // Datetime
        String fullXSDDateTimeString = "2021-01-01T23:59:59";
        Literal fullXSDDateTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString, XSDDatatype.DATETIME.getIRI(), XSDDatatype.DATETIME);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDDateTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateTimeBadDatatype = this.valueFactory.createLiteral(fullXSDDateTimeString,new SimpleIRI("http://example.com/test"), XSDDatatype.DATETIME);
        assertEquals(fullXSDDateTimeString, fullXSDDateTimeBadDatatype.stringValue());
        assertNotNull(fullXSDDateTimeBadDatatype.temporalAccessorValue());

        // // Date
        String fullXSDDateString = "2021-01-01";
        Literal fullXSDDateGoodDatatype = this.valueFactory.createLiteral(fullXSDDateString, XSDDatatype.DATE.getIRI(), XSDDatatype.DATE);
        assertEquals(fullXSDDateString, fullXSDDateGoodDatatype.stringValue());
        assertNotNull(fullXSDDateGoodDatatype.temporalAccessorValue());
        Literal fullXSDDateBadDatatype = this.valueFactory.createLiteral(fullXSDDateString,new SimpleIRI("http://example.com/test"), XSDDatatype.DATE);
        assertEquals(fullXSDDateString, fullXSDDateBadDatatype.stringValue());
        assertNotNull(fullXSDDateBadDatatype.temporalAccessorValue());

        // // Time
        String fullXSDTimeString = "23:59:59";
        Literal fullXSDTimeGoodDatatype = this.valueFactory.createLiteral(fullXSDTimeString, XSDDatatype.TIME.getIRI(), XSDDatatype.TIME);
        assertEquals(fullXSDTimeString, fullXSDTimeGoodDatatype.stringValue());
        assertNotNull(fullXSDTimeGoodDatatype.temporalAccessorValue());
        Literal fullXSDTimeBadDatatype = this.valueFactory.createLiteral(fullXSDTimeString,new SimpleIRI("http://example.com/test"), XSDDatatype.TIME);
        assertEquals(fullXSDTimeString, fullXSDTimeBadDatatype.stringValue());
        assertNotNull(fullXSDTimeBadDatatype.temporalAccessorValue());

        // Duration
        String fullXSDDurationString = "P100DT23H";
        Literal fullXSDDuration = this.valueFactory.createLiteral(fullXSDDurationString, XSDDatatype.DURATION.getIRI(), XSDDatatype.DURATION);
        assertEquals(fullXSDDurationString, fullXSDDuration.stringValue());

        // Numbers

        // // Integer
        String integerString = "1234567890";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, XSDDatatype.INTEGER);
        assertEquals(integerString, integerLiteral.stringValue());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, XSDDatatype.DECIMAL.getIRI(), XSDDatatype.DECIMAL);
        assertEquals(decimalString, decimalLiteral.stringValue());
        assertEquals(new BigDecimal(decimalString), decimalLiteral.decimalValue());

        // // Long
        String longString = "1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, XSDDatatype.LONG);
        assertEquals(longString, longLiteral.stringValue());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // Short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, XSDDatatype.SHORT);
        assertEquals(shortString, shortLiteral.stringValue());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, XSDDatatype.UNSIGNED_SHORT);
        assertEquals(unsignedShortString, unsignedShortLiteral.stringValue());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "127";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, XSDDatatype.BYTE);
        assertEquals(byteString, byteLiteral.stringValue());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, XSDDatatype.UNSIGNED_BYTE);
        assertEquals(unsignedByteString, unsignedByteLiteral.stringValue());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());

        // // Float
        String floatString = "345678.3456789";
        Literal floatLiteral = this.valueFactory.createLiteral(floatString, XSDDatatype.FLOAT);
        assertEquals(floatString, floatLiteral.stringValue());
        assertEquals(Float.parseFloat(floatString), floatLiteral.floatValue(), 0);

        // // Double
        String doubleString = "345678.3456789";
        Literal doubleLiteral = this.valueFactory.createLiteral(doubleString, XSDDatatype.DOUBLE);
        assertEquals(doubleString, doubleLiteral.stringValue());
        assertEquals(Double.parseDouble(doubleString), doubleLiteral.doubleValue(), 0);

        // // Int
        String intString = "1234567890";
        Literal intLiteral = this.valueFactory.createLiteral(intString, XSDDatatype.INT);
        assertEquals(intString, intLiteral.stringValue());

        // // Non negative integer
        String nonNegativeIntegerString = "1234567890";
        Literal nonNegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, XSDDatatype.NON_NEGATIVE_INTEGER);
        assertEquals(nonNegativeIntegerString, nonNegativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonNegativeIntegerString), nonNegativeIntegerLiteral.longValue());

        // // Non positive integer
        String nonPositiveIntegerString = "-1234567890";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, XSDDatatype.NON_POSITIVE_INTEGER);
        assertEquals(nonPositiveIntegerString, nonPositiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonPositiveIntegerString), nonPositiveIntegerLiteral.longValue());

        // // Positive integer
        String positiveIntegerString = "1234567890";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, XSDDatatype.POSITIVE_INTEGER);
        assertEquals(positiveIntegerString, positiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(positiveIntegerString), positiveIntegerLiteral.longValue());

        // // Negative integer
        String negativeIntegerString = "-1234567890";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, XSDDatatype.NEGATIVE_INTEGER);
        assertEquals(negativeIntegerString, negativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(negativeIntegerString), negativeIntegerLiteral.longValue());

        // // Unsigned long
        String unsignedLongString = "1234567890123456789";
        Literal unsignedLongLiteral = this.valueFactory.createLiteral(unsignedLongString, XSDDatatype.UNSIGNED_LONG);
        assertEquals(unsignedLongString, unsignedLongLiteral.stringValue());
        assertEquals(Long.parseLong(unsignedLongString), unsignedLongLiteral.longValue());

        // // Unsigned int
        String unsignedIntString = "1234567890";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, XSDDatatype.UNSIGNED_INT);
        assertEquals(unsignedIntString, unsignedIntLiteral.stringValue());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testCreateLiteralStringIRICoreDatatype() {
        // Numeric Datatypes

        // // Integer
        String integerString = "1234567890";
        Literal integerLiteral = this.valueFactory.createLiteral(integerString, new SimpleIRI("http://example.com/test"), XSDDatatype.INTEGER);
        assertEquals(integerString, integerLiteral.stringValue());
        assertEquals(Integer.parseInt(integerString), integerLiteral.intValue());

        // // Non Negative Integer
        String nonNegativeIntegerString = "1234567890";
        Literal nonNegativeIntegerLiteral = this.valueFactory.createLiteral(nonNegativeIntegerString, new SimpleIRI("http://example.com/test"), XSDDatatype.NON_NEGATIVE_INTEGER);
        assertEquals(nonNegativeIntegerString, nonNegativeIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonNegativeIntegerString), nonNegativeIntegerLiteral.longValue());

        // // Non Positive Integer
        String nonPositiveIntegerString = "-1234567890";
        Literal nonPositiveIntegerLiteral = this.valueFactory.createLiteral(nonPositiveIntegerString, new SimpleIRI("http://example.com/test"), XSDDatatype.NON_POSITIVE_INTEGER);
        assertEquals(nonPositiveIntegerString, nonPositiveIntegerLiteral.stringValue());
        assertEquals(Long.parseLong(nonPositiveIntegerString), nonPositiveIntegerLiteral.longValue());

        // // Positive Integer
        String positiveIntegerString = "1234567890";
        Literal positiveIntegerLiteral = this.valueFactory.createLiteral(positiveIntegerString, new SimpleIRI("http://example.com/test"), XSDDatatype.POSITIVE_INTEGER);
        assertEquals(positiveIntegerString, positiveIntegerLiteral.stringValue());
        assertEquals(Integer.parseInt(positiveIntegerString), positiveIntegerLiteral.intValue());

        // // Negative Integer
        String negativeIntegerString = "-1234567890";
        Literal negativeIntegerLiteral = this.valueFactory.createLiteral(negativeIntegerString, new SimpleIRI("http://example.com/test"), XSDDatatype.NEGATIVE_INTEGER);
        assertEquals(negativeIntegerString, negativeIntegerLiteral.stringValue());
        assertEquals(Integer.parseInt(negativeIntegerString), negativeIntegerLiteral.intValue());

        // // Long
        String longString = "1234567890123456789";
        Literal longLiteral = this.valueFactory.createLiteral(longString, new SimpleIRI("http://example.com/test"), XSDDatatype.LONG);
        assertEquals(longString, longLiteral.stringValue());
        assertEquals(Long.parseLong(longString), longLiteral.longValue());

        // // Decimal
        String decimalString = "1234567890123456789.1234567890123456789";
        Literal decimalLiteral = this.valueFactory.createLiteral(decimalString, new SimpleIRI("http://example.com/test"), XSDDatatype.DECIMAL);
        assertEquals(decimalString, decimalLiteral.stringValue());
        assertEquals(new BigDecimal(decimalString), decimalLiteral.decimalValue());

        // // Int
        String intString = "1234567890";
        Literal intLiteral = this.valueFactory.createLiteral(intString, new SimpleIRI("http://example.com/test"), XSDDatatype.INT);
        assertEquals(intString, intLiteral.stringValue());
        assertEquals(Integer.parseInt(intString), intLiteral.intValue());

        // // Unsigned Int
        String unsignedIntString = "1234567890";
        Literal unsignedIntLiteral = this.valueFactory.createLiteral(unsignedIntString, new SimpleIRI("http://example.com/test"), XSDDatatype.UNSIGNED_INT);
        assertEquals(unsignedIntString, unsignedIntLiteral.stringValue());
        assertEquals(Integer.parseInt(unsignedIntString), unsignedIntLiteral.intValue());

        // // Short
        String shortString = "7851";
        Literal shortLiteral = this.valueFactory.createLiteral(shortString, new SimpleIRI("http://example.com/test"), XSDDatatype.SHORT);
        assertEquals(shortString, shortLiteral.stringValue());
        assertEquals(Short.parseShort(shortString), shortLiteral.shortValue());

        // // UnsignedShort
        String unsignedShortString = "7851";
        Literal unsignedShortLiteral = this.valueFactory.createLiteral(unsignedShortString, new SimpleIRI("http://example.com/test"), XSDDatatype.UNSIGNED_SHORT);
        assertEquals(unsignedShortString, unsignedShortLiteral.stringValue());
        assertEquals(Short.parseShort(unsignedShortString), unsignedShortLiteral.shortValue());

        // // Byte
        String byteString = "127";
        Literal byteLiteral = this.valueFactory.createLiteral(byteString, new SimpleIRI("http://example.com/test"), XSDDatatype.BYTE);
        assertEquals(byteString, byteLiteral.stringValue());
        assertEquals(Byte.parseByte(byteString), byteLiteral.byteValue());

        // // UnsignedByte
        String unsignedByteString = "64";
        Literal unsignedByteLiteral = this.valueFactory.createLiteral(unsignedByteString, new SimpleIRI("http://example.com/test"), XSDDatatype.UNSIGNED_BYTE);
        assertEquals(unsignedByteString, unsignedByteLiteral.stringValue());
        assertEquals(Byte.parseByte(unsignedByteString), unsignedByteLiteral.byteValue());
    }

    @Test
    void testCreateLiteralBoolean() {
        Literal trueLit = this.valueFactory.createLiteral(true);
        Literal falseLit = this.valueFactory.createLiteral(false);
        assertTrue(trueLit.booleanValue(), "Literal boolean should be true");
        assertFalse(falseLit.booleanValue(), "Literal boolean should be false");
    }

    @Test
    void testCreateStatement() {
        IRI subject = this.valueFactory.createIRI("http://example.org/s");
        IRI predicate = this.valueFactory.createIRI("http://example.org/p");
        Literal object = this.valueFactory.createLiteral("o");
        Statement stmt = this.valueFactory.createStatement(subject, predicate, object);
        assertNotNull(stmt, "Created statement should not be null");
        assertEquals(subject, stmt.getSubject());
        assertEquals(predicate, stmt.getPredicate());
        assertEquals(object, stmt.getObject());
    }

    @Test
    void testCreateLiteralByte() {
        byte b = 64;
        Literal literal = this.valueFactory.createLiteral(b);
        assertEquals(b, literal.byteValue());
    }

    @Test
    void testCreateLiteralShort() {
        short s = 7851;
        Literal literal = this.valueFactory.createLiteral(s);
        assertEquals(s, literal.shortValue());
    }

    @Test
    void testCreateLiteralInt() {
        int i = 1234567890;
        Literal literal = this.valueFactory.createLiteral(i);
        assertEquals(i, literal.intValue());
    }

    @Test
    void testCreateLiteralLong() {
        long l = 1234567890123456789L;
        Literal literal = this.valueFactory.createLiteral(l);
        assertEquals(l, literal.longValue());
    }

    @Test
    void testCreateLiteralFloat() {
        float f = 1234567890.1234567890123456789f;
        Literal literal = this.valueFactory.createLiteral(f);
        assertEquals(f, literal.floatValue(), 0);
    }

    @Test
    void testCreateLiteralDouble() {
        double d = 1234567890.1234567890123456789;
        Literal literal = this.valueFactory.createLiteral(d);
        assertEquals(d, literal.doubleValue(), 0);
    }

    @Test
    void testCreateLiteralBigDecimal() {
        BigDecimal bd = new BigDecimal("1234567890.1234567890123456789");
        Literal literal = this.valueFactory.createLiteral(bd);
        assertEquals(bd, literal.decimalValue());
    }

    @Test
    void testCreateLiteralBigInteger() {
        BigInteger bi = new BigInteger("1234567890123456789");
        Literal literal = this.valueFactory.createLiteral(bi);
        assertEquals(bi, literal.integerValue());
    }

    @Test
    void testCreateLiteralTemporalAccessor() {
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
    void testCreateLiteralXMLGregorianCalendar() throws DatatypeConfigurationException {
        XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("2021-01-01T23:59:59");
        Literal literal = this.valueFactory.createLiteral(calendar);

        assertNotNull(literal);
        assertEquals(calendar, literal.calendarValue());
    }

    @Test
    void testCreateLiteralDate() {
        java.time.LocalDate date = java.time.LocalDate.parse("2021-01-01");
        Literal literal = this.valueFactory.createLiteral(date);

        assertNotNull(literal);
        assertEquals("2021-01-01", literal.stringValue());
    }
}
