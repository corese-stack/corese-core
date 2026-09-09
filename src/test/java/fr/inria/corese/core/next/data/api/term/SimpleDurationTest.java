package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Period;
import java.time.temporal.TemporalAmount;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

class SimpleDurationTest {

    @Test
    void serializationPreservesTemporalValues() throws Exception {
        for (SimpleDuration original : new SimpleDuration[]{new SimpleDuration("PT1.25S"),
                new SimpleDuration(Period.ofDays(2)), new SimpleDuration("P1Y2M")}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(original);
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                SimpleDuration restored = (SimpleDuration) input.readObject();
                assertEquals(original, restored);
                assertEquals(original.temporalAmountValue(), restored.temporalAmountValue());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "PT1H", "P2D", "-PT1.25S", "PT0.123456789S", "P1Y2M", "-P2M", "P1Y2M3DT4H5M6.7S"})
    void preservesLexicalForm(String label) {
        SimpleDuration literal = new SimpleDuration(label);
        assertEquals(label, literal.getLabel());
        assertEquals(label, literal.stringValue());
        assertEquals(label, literal.toString());
        assertEquals(XSDDatatype.DURATION.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.DURATION, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void constructsFromTemporalAmounts() {
        for (TemporalAmount amount : new TemporalAmount[] {Duration.ofSeconds(42, 123456789),
                Duration.ZERO, Duration.ofSeconds(-5), Period.of(1, 2, 3), Period.ZERO}) {
            SimpleDuration literal = new SimpleDuration(amount);
            assertSame(amount, literal.temporalAmountValue());
            assertEquals(amount.toString(), literal.getLabel());
            assertEquals(XSDDatatype.DURATION.getIRI(), literal.getDatatype());
            assertEquals(XSDDatatype.DURATION, literal.getCoreDatatype());
            assertEquals(new SimpleDuration(amount.toString()), literal);
        }
        assertEquals("temporalAmount", assertThrows(NullPointerException.class,
                () -> new SimpleDuration((TemporalAmount) null)).getMessage());
    }

    @Test
    void retainsDurationDatatypeForAllConstructors() {
        SimpleDuration expected = new SimpleDuration("PT1S");
        assertEquals(expected, new SimpleDuration("PT1S", null));
        assertEquals(expected, new SimpleDuration("PT1S", null, null));
        assertEquals(expected, new SimpleDuration("PT1S", XSDDatatype.DURATION.getIRI()));
        assertEquals(expected, new SimpleDuration("PT1S", new SimpleIRI("urn:custom:duration")));
        assertEquals(expected, new SimpleDuration("PT1S", XSDDatatype.STRING.getIRI(), CoreDatatype.NONE));
        assertEquals(XSDDatatype.DURATION,
                new SimpleDuration("PT1S", null, XSDDatatype.STRING).getCoreDatatype());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "PT1H", "P2D", "-PT1.25S", "PT0.123456789S"})
    void parsesDayTimeDurations(String label) {
        assertEquals(Duration.parse(label), new SimpleDuration(label).temporalAmountValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"P1Y", "P1Y2M3D", "-P2M"})
    void parsesCalendarPeriods(String label) {
        assertEquals(Period.parse(label), new SimpleDuration(label).temporalAmountValue());
    }

    @Test
    void preservesMixedXmlDurationWithUnsupportedJavaConversion() {
        SimpleDuration literal = new SimpleDuration("P1Y2M3DT4H5M6.7S");
        assertEquals("P1Y2M3DT4H5M6.7S", literal.getLabel());
        assertThrows(IncorrectOperationException.class, literal::temporalAmountValue);
        SimpleDuration oneSecond = new SimpleDuration("PT1S");
        assertThrows(IncorrectOperationException.class, () -> literal.compareTo(oneSecond));
    }

    @Test
    void comparesValuesSeparatelyFromRdfTerms() {
        SimpleDuration first = new SimpleDuration("PT1H");
        SimpleDuration second = new SimpleDuration("PT2H");
        SimpleDuration equivalent = new SimpleDuration("PT60M");
        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
        assertEquals(0, first.compareTo(first));
        assertEquals(0, first.compareTo(equivalent));
        assertNotEquals(first, equivalent);
        assertTrue(new SimpleDuration("-PT2S").compareTo(new SimpleDuration("PT0S")) < 0);
        assertTrue(new SimpleDuration("PT0.1S").compareTo(new SimpleDuration("PT0.2S")) < 0);
        assertTrue(new SimpleDuration("P1Y").compareTo(new SimpleDuration("P2Y")) < 0);
        assertThrows(NullPointerException.class, () -> first.compareTo(null));
    }

    @Test
    void equalityAndHashCodeUseRdfTerms() {
        SimpleDuration first = new SimpleDuration("PT1S");
        SimpleDuration second = new SimpleDuration("PT1S");
        SimpleLiteral generic = new SimpleLiteral("PT1S", XSDDatatype.DURATION.getIRI());
        Literal firstTerm = first;
        Literal genericTerm = generic;
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(firstTerm, genericTerm);
        assertEquals(genericTerm, firstTerm);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), generic.hashCode());
        assertNotEquals(first, new SimpleDuration("PT2S"));
        assertNotEquals(first, new SimpleLiteral("PT1S", XSDDatatype.STRING.getIRI()));
        assertNotEquals(first, new SimpleLiteral("PT1S", "en"));
        assertNotEquals(first, (Object) null);
        assertNotEquals(first, (Object) "PT1S");
    }

    @Test
    void rejectsMutationAndUnsupportedConversions() {
        SimpleDuration literal = new SimpleDuration("PT1S");
        assertEquals("SimpleDuration is immutable", assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.STRING)).getMessage());
        assertThrows(IncorrectOperationException.class, () -> literal.setCoreDatatype(null));
        assertEquals(XSDDatatype.DURATION, literal.getCoreDatatype());
        assertEquals(XSDDatatype.DURATION.getIRI(), literal.getDatatype());
        assertEquals("PT1S", literal.getLabel());
        assertEquals(Duration.ofSeconds(1), literal.temporalAmountValue());
        assertThrows(IncorrectOperationException.class, literal::booleanValue);
        assertThrows(IncorrectOperationException.class, literal::intValue);
        assertThrows(IncorrectOperationException.class, literal::calendarValue);
        assertThrows(IncorrectOperationException.class, literal::temporalAccessorValue);
    }

    @Test
    void rejectsNullLexicalValues() {
        assertEquals("lexicalValue", assertThrows(NullPointerException.class,
                () -> new SimpleDuration((String) null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleDuration(null, null));
        assertThrows(NullPointerException.class, () -> new SimpleDuration(null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "P", "PT", "P1YT", "2024-01-01", "PT1.2.3S"})
    void rejectsInvalidLexicalValues(String label) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleDuration(label));
    }
}
