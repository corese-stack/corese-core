package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalizedAbstractStringTest {
    public static class LocalizedLiteralString extends LocalizedAbstractString {

        public LocalizedLiteralString(IRI datatype, String value, String language) {
            super(datatype, value, language);
        }

    }

    private LocalizedAbstractString localizedLiteral;

    @Before
    public void setUp() {
        IRI iri = new BasicIRI("http://www.w3.org/2001/XMLSchema#string");
        localizedLiteral = new LocalizedLiteralString(iri, "Corese", "en");
    }

    @Test
    public void testGetLabel() {
        assertEquals("Corese", localizedLiteral.getLabel());
    }

    @Test
    public void testGetDatatype() {
        assertNotNull(localizedLiteral.getDatatype());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", localizedLiteral.getDatatype().stringValue());
    }

    @Test
    public void testGetCoreDatatype() {
        assertEquals(CoreDatatype.XSD.STRING, localizedLiteral.getCoreDatatype());
    }

    @Test
    public void testStringValue() {
        assertEquals("Corese", localizedLiteral.stringValue());
    }

    @Test
    public void testGetLanguage() {
        assertTrue(localizedLiteral.getLanguage().isPresent());
        assertEquals("en", localizedLiteral.getLanguage().get());
    }

    @Test
    public void testToStringWithLanguage() {
        assertEquals("\"Corese\"@en", localizedLiteral.toString());
    }

    @Test
    public void testToStringWithoutLanguage() {
        // Create another instance without a language
        LocalizedAbstractString literalWithoutLanguage = new LocalizedLiteralString(XSD.xsdString.getIRI(), "Corese", null);
        assertEquals("\"Corese\"^^<http://www.w3.org/2001/XMLSchema#string>", literalWithoutLanguage.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        LocalizedAbstractString anotherLocalizedLiteral = new LocalizedLiteralString(XSD.xsdString.getIRI(), "Corese", "en");
        assertEquals(localizedLiteral, anotherLocalizedLiteral);
        assertEquals(localizedLiteral.hashCode(), anotherLocalizedLiteral.hashCode());
    }

    @Test
    public void testEqualsWithDifferentLanguage() {
        LocalizedAbstractString differentLocalizedLiteral = new LocalizedLiteralString(XSD.xsdString.getIRI(), "Corese", "fr");
        assertNotEquals(localizedLiteral, differentLocalizedLiteral);
    }

    @Test
    public void testEqualsWithDifferentValue() {
        LocalizedAbstractString differentLocalizedLiteral = new LocalizedLiteralString(XSD.xsdString.getIRI(), "Different", "en");
        assertNotEquals(localizedLiteral, differentLocalizedLiteral);
    }
}
