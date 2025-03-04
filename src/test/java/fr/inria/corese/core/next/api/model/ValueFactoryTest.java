package fr.inria.corese.core.next.api.model;

import fr.inria.corese.core.next.api.exception.IncorrectFormatException;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Period;

import static org.junit.Assert.*;

public abstract class ValueFactoryTest {

    protected ValueFactory valueFactory;

    @Before
    public abstract void setUp();

    @Test
    public void testCreateIRI() {
        String correctIRI = "http://example.org";
        String incorrectIRI = "http:/example.org#";

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
    }

    @Test
    public void testCreateLiteralStringCoreDatatype() {
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
    }

    @Test
    public void testCreateLiteralTemporalAmount() {
        Duration duration = Duration.ofDays(1);
        Period period = Period.ofDays(100);

        Literal durationDuration = this.valueFactory.createLiteral(duration);
        Literal periodDuration = this.valueFactory.createLiteral(duration);

        assertNotNull(this.valueFactory.createLiteral(duration));
        assertNotNull(this.valueFactory.createLiteral(period));
        assertEquals(duration, durationDuration.temporalAmountValue());
        assertEquals(period, periodDuration.temporalAmountValue());
    }

    @Test
    public void testCreateLiteralXMLGregorianCalendar() {
    }

    @Test
    public void testCreateLiteralDate() {
    }

    @Test
    public void testCreateStatement() {
    }
}
