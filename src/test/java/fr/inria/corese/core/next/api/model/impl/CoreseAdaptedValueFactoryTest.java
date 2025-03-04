package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.model.impl.corese.CoreseAdaptedValueFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreseAdaptedValueFactoryTest {

    @Test
    public void testCreateIRI() {
        String correctIRI = "http://example.org";
        String incorrectIRI = "http:/example.org#";

        assertNotNull(CoreseAdaptedValueFactory.getInstance().createIRI(correctIRI));
        assertThrows(IncorrectFormatException.class, () -> CoreseAdaptedValueFactory.getInstance().createIRI(incorrectIRI));
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
