package fr.inria.corese.core.next.data.api.factory;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.SimpleStatement;
import fr.inria.corese.core.next.data.api.term.*;

class NativeValueFactoryTest extends ValueFactoryTest {

    @Test
    void formatsJavaTimesAsXmlLexicalForms() {
        assertEquals("12:30:00", valueFactory.createLiteral(LocalTime.of(12, 30)).getLabel());
        assertEquals("2024-02-29T12:30:00",
                valueFactory.createLiteral(LocalDateTime.of(2024, 2, 29, 12, 30)).getLabel());
        assertEquals("2024-02-29T12:30:00+01:00",
                valueFactory.createLiteral(ZonedDateTime.of(2024, 2, 29, 12, 30, 0, 0,
                        ZoneId.of("Europe/Paris"))).getLabel());
        assertEquals("2024-02-29T12:30:00Z",
                valueFactory.createLiteral(Instant.parse("2024-02-29T12:30:00Z")).getLabel());
    }

    @BeforeEach
    @Override
    public void setUp() {
        valueFactory = new NativeValueFactory();
    }

    @Test
    void defaultFactoryIsSharedAndNative() {
        assertInstanceOf(NativeValueFactory.class, Values.factory());
        assertSame(Values.factory(), Values.factory());
        assertInstanceOf(SimpleIRI.class, Values.factory().createIRI("urn:test"));
    }

    @Test
    void createsNativeResources() {
        assertEquals(new SimpleIRI("urn:test"), valueFactory.createIRI("urn:", "test"));
        assertInstanceOf(SimpleIRI.class, valueFactory.createIRI("urn:test"));
        assertInstanceOf(SimpleIRI.class, valueFactory.createIRI("urn:", "test"));
        assertEquals(new SimpleBNode("id"), valueFactory.createBNode("id"));
        assertInstanceOf(SimpleBNode.class, valueFactory.createBNode("id"));
        Set<String> ids = ConcurrentHashMap.newKeySet();
        IntStream.range(0, 10000).parallel().forEach(i -> {
            BNode node = valueFactory.createBNode();
            assertInstanceOf(SimpleBNode.class, node);
            assertTrue(node.getID().matches("[0-9a-f]+"));
            assertTrue(ids.add(node.getID()));
        });
        assertEquals(10000, ids.size());
    }

    @Test
    void createsStringAndLanguageLiterals() {
        assertEquals(new SimpleLiteral("hello"), valueFactory.createLiteral("hello"));
        assertEquals(new SimpleLiteral("hello", "en"), valueFactory.createLiteral("hello", "EN"));
        assertEquals(SimpleLiteral.class, valueFactory.createLiteral("hello").getClass());
        assertEquals(SimpleLiteral.class, valueFactory.createLiteral("hello", "en").getClass());
        IRI custom = new SimpleIRI("urn:custom");
        assertEquals(new SimpleLiteral("value", custom), valueFactory.createLiteral("value", custom));
        assertEquals(new SimpleLiteral("value"), valueFactory.createLiteral("value", XSDDatatype.STRING.getIRI()));
    }

    @ParameterizedTest
    @CsvSource({"DATE,2024-02-29,SimpleDate", "DATETIME,2024-02-29T12:30:45Z,SimpleDateTime",
            "TIME,12:30:45Z,SimpleTime", "DURATION,P1DT2H,SimpleDuration", "DECIMAL,01.20,SimpleDecimal",
            "DOUBLE,1.2E3,SimpleDouble", "FLOAT,1.2E3,SimpleDouble", "BOOLEAN,1,SimpleLiteral"})
    void dispatchesTypedLiterals(XSDDatatype datatype, String label, String type) {
        Literal literal = valueFactory.createLiteral(label, datatype.getIRI());
        assertEquals(type, literal.getClass().getSimpleName());
        assertEquals(label, literal.getLabel());
        assertEquals(datatype.getIRI(), literal.getDatatype());
        assertEquals(datatype, literal.getCoreDatatype());
    }

    @ParameterizedTest
    @EnumSource(value = XSDDatatype.class, names = {"INTEGER", "LONG", "INT", "SHORT", "BYTE",
            "POSITIVE_INTEGER", "NEGATIVE_INTEGER", "NON_NEGATIVE_INTEGER", "NON_POSITIVE_INTEGER",
            "UNSIGNED_LONG", "UNSIGNED_INT", "UNSIGNED_SHORT", "UNSIGNED_BYTE"})
    void dispatchesAllIntegerDatatypes(XSDDatatype datatype) {
        String label = datatype == XSDDatatype.NEGATIVE_INTEGER || datatype == XSDDatatype.NON_POSITIVE_INTEGER ? "-1" : "1";
        Literal literal = valueFactory.createLiteral(label, datatype.getIRI());
        assertInstanceOf(SimpleInteger.class, literal);
        assertEquals(label, literal.getLabel());
        assertEquals(datatype.getIRI(), literal.getDatatype());
        assertEquals(new BigInteger(label), literal.integerValue());
    }

    @ParameterizedTest
    @EnumSource(value = XSDDatatype.class, names = {"DATE", "DATETIME", "TIME", "DURATION",
            "INTEGER", "DECIMAL", "DOUBLE", "FLOAT", "BOOLEAN"})
    void preservesInvalidLexicalForms(XSDDatatype datatype) {
        Literal literal = valueFactory.createLiteral("invalid", datatype.getIRI());
        assertEquals(SimpleLiteral.class, literal.getClass());
        assertEquals("invalid", literal.getLabel());
        assertEquals(datatype.getIRI(), literal.getDatatype());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "1,true", "false,false", "0,false"})
    void parsesBooleanLexicalForms(String label, boolean expected) {
        assertEquals(expected, valueFactory.createLiteral(label, XSDDatatype.BOOLEAN.getIRI()).booleanValue());
    }

    @Test
    void preservesUnspecializedDatatypes() {
        Literal literal = valueFactory.createLiteral("2024", XSDDatatype.YEAR.getIRI());
        assertLiteral(literal, SimpleLiteral.class, XSDDatatype.YEAR, "2024");
    }

    @Test
    void primitiveOverloadsPreserveNativeTypesAndDatatypes() {
        assertLiteral(valueFactory.createLiteral(true), SimpleLiteral.class, XSDDatatype.BOOLEAN, "true");
        assertLiteral(valueFactory.createLiteral(false), SimpleLiteral.class, XSDDatatype.BOOLEAN, "false");
        assertLiteral(valueFactory.createLiteral((byte) -128), SimpleInteger.class, XSDDatatype.BYTE, "-128");
        assertLiteral(valueFactory.createLiteral((short) -32768), SimpleInteger.class, XSDDatatype.SHORT, "-32768");
        assertLiteral(valueFactory.createLiteral(Integer.MIN_VALUE), SimpleInteger.class, XSDDatatype.INT, "-2147483648");
        assertLiteral(valueFactory.createLiteral(Long.MAX_VALUE), SimpleInteger.class, XSDDatatype.LONG, "9223372036854775807");
        assertLiteral(valueFactory.createLiteral(1.5f), SimpleDouble.class, XSDDatatype.FLOAT, "1.5");
        assertLiteral(valueFactory.createLiteral(1.5d), SimpleDouble.class, XSDDatatype.DOUBLE, "1.5");
        assertInstanceOf(SimpleDecimal.class, valueFactory.createLiteral(new BigDecimal("1.2300")));
        assertInstanceOf(SimpleInteger.class, valueFactory.createLiteral(new BigInteger("123456789012345678901234567890")));
        assertTrue(Double.isNaN(valueFactory.createLiteral(Double.NaN).doubleValue()));
        assertEquals(Float.POSITIVE_INFINITY, valueFactory.createLiteral(Float.POSITIVE_INFINITY).floatValue());
    }

    @Test
    void dispatchesJavaTemporalValues() {
        assertInstanceOf(SimpleDate.class, valueFactory.createLiteral(LocalDate.of(2024, 2, 29)));
        assertInstanceOf(SimpleTime.class, valueFactory.createLiteral(LocalTime.of(12, 30, 45)));
        assertInstanceOf(SimpleTime.class, valueFactory.createLiteral(OffsetTime.parse("12:30:45+02:00")));
        assertInstanceOf(SimpleDateTime.class, valueFactory.createLiteral(LocalDateTime.parse("2024-02-29T12:30:45")));
        assertInstanceOf(SimpleDateTime.class, valueFactory.createLiteral(OffsetDateTime.parse("2024-02-29T12:30:45Z")));
        assertLiteral(valueFactory.createLiteral(Instant.parse("2024-02-29T12:30:45Z")),
                SimpleDateTime.class, XSDDatatype.DATETIME, "2024-02-29T12:30:45Z");
        assertInstanceOf(SimpleDuration.class, valueFactory.createLiteral(Duration.ofSeconds(45)));
        assertInstanceOf(SimpleDuration.class, valueFactory.createLiteral(Period.ofMonths(2)));
    }

    @ParameterizedTest
    @CsvSource({"2024-02-29,DATE,SimpleDate", "12:30:45Z,TIME,SimpleTime",
            "2024-02-29T12:30:45Z,DATETIME,SimpleDateTime"})
    void dispatchesXmlCalendars(String label, XSDDatatype datatype, String type) {
        XMLGregorianCalendar calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(label);
        Literal literal = valueFactory.createLiteral(calendar);
        assertEquals(type, literal.getClass().getSimpleName());
        assertEquals(datatype.getIRI(), literal.getDatatype());
        assertEquals(calendar, literal.calendarValue());
        assertEquals(label, literal.getLabel());
    }

    @Test
    void createsNativeStatementsAndTriples() {
        Resource subject = valueFactory.createBNode("s");
        IRI predicate = valueFactory.createIRI("urn:p");
        Literal object = valueFactory.createLiteral("o");
        Resource context = valueFactory.createIRI("urn:g");
        assertEquals(new SimpleStatement(subject, predicate, object), valueFactory.createStatement(subject, predicate, object));
        assertInstanceOf(SimpleStatement.class, valueFactory.createStatement(subject, predicate, object));
        assertEquals(new SimpleStatement(subject, predicate, object, context), valueFactory.createStatement(subject, predicate, object, context));
        assertInstanceOf(SimpleStatement.class, valueFactory.createStatement(subject, predicate, object, context));
        assertEquals(new SimpleTriple(subject, predicate, object), valueFactory.createTriple(subject, predicate, object));
        assertInstanceOf(SimpleTriple.class, valueFactory.createTriple(subject, predicate, object));
        assertThrows(NullPointerException.class, () -> valueFactory.createStatement(null, predicate, object));
        assertThrows(NullPointerException.class, () -> valueFactory.createStatement(subject, null, object, context));
        assertThrows(NullPointerException.class, () -> valueFactory.createTriple(subject, predicate, null));
    }

    @Test
    void rejectsNullLiteralInputs() {
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral((String) null));
        String nullLabel = null;
        IRI integerDatatype = XSDDatatype.INT.getIRI();
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral(nullLabel, integerDatatype));
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral("1", (IRI) null));
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral((BigInteger) null));
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral((BigDecimal) null));
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral((TemporalAccessor) null));
        assertThrows(NullPointerException.class, () -> valueFactory.createLiteral((TemporalAmount) null));
        assertEquals("calendar", assertThrows(NullPointerException.class,
                () -> valueFactory.createLiteral((XMLGregorianCalendar) null)).getMessage());
    }

    private static void assertLiteral(Literal literal, Class<? extends Literal> type, XSDDatatype datatype, String label) {
        assertEquals(type, literal.getClass());
        assertEquals(datatype.getIRI(), literal.getDatatype());
        assertEquals(datatype, literal.getCoreDatatype());
        assertEquals(label, literal.getLabel());
    }
}
