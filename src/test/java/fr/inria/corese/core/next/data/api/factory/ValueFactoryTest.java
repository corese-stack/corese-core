package fr.inria.corese.core.next.data.api.factory;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
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
