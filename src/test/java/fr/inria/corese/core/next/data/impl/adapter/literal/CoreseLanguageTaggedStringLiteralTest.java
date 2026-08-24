package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreseLanguageTaggedStringLiteralTest {

    private String testValue;
    private String testLanguage;

    @BeforeEach
    void setUp() {
        testValue = "Hello";
        testLanguage = "en";
    }

    @Test
    void testConstructorWithIDatatype() {
        // Create a mock CoreseLiteral with value and language
        IDatatype coreseLiteral = new CoreseLiteral(testValue, testLanguage);
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(coreseLiteral);
        // Test that the coreseObject is correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(RDFDatatype.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(RDFDatatype.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    void testConstructorWithValueAndLanguage() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);

        // Test that the value and language are correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(RDFDatatype.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(RDFDatatype.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    void testGetLabel() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the label (value) is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
    }

    @Test
    void testGetLanguage() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the language is correctly returned as an Optional
        assertTrue(coreseLanguageTaggedString.getLanguage().isPresent());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().get());
    }

    @Test
    void testGetValue() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the value is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getValue());
    }

    @Test
    void testGetCoreDatatype() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the CoreDatatype is correctly returned (should be RDFDatatype.LANGSTRING)
        assertEquals(RDFDatatype.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
    }

    @Test
    void testGetDatatype() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the datatype IRI is correctly returned
        assertEquals(RDFDatatype.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    void equalityIncludesLanguageTagAndIgnoresItsCase() {
        var english = new CoreseLanguageTaggedStringLiteral("Hello", "en");
        var uppercaseEnglish = new CoreseLanguageTaggedStringLiteral("Hello", "EN");
        var french = new CoreseLanguageTaggedStringLiteral("Hello", "fr");

        assertEquals(english, uppercaseEnglish);
        assertEquals(english.hashCode(), uppercaseEnglish.hashCode());
        assertEquals("en", uppercaseEnglish.getLanguage().orElseThrow());
        assertNotEquals(english, french);
    }
}
