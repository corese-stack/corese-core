package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseLiteral;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoreseLanguageTaggedStringTest {

    private String testValue;
    private String testLanguage;

    @Before
    public void setUp() {
        testValue = "Hello";
        testLanguage = "en";
    }

    @Test
    public void testConstructorWithIDatatype() {
        // Create a mock CoreseLiteral with value and language
        IDatatype coreseLiteral = new CoreseLiteral(testValue, testLanguage);
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(coreseLiteral);
        // Test that the coreseObject is correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    public void testConstructorWithValueAndLanguage() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);

        // Test that the value and language are correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    public void testGetLabel() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the label (value) is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
    }

    @Test
    public void testGetLanguage() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the language is correctly returned as an Optional
        assertTrue(coreseLanguageTaggedString.getLanguage().isPresent());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().get());
    }

    @Test
    public void testGetValue() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the value is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getValue());
    }

    @Test
    public void testGetCoreDatatype() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the CoreDatatype is correctly returned (should be RDF.LANGSTRING)
        assertEquals(RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
    }

    @Test
    public void testGetDatatype() {
        CoreseLanguageTaggedStringLiteral coreseLanguageTaggedString = new CoreseLanguageTaggedStringLiteral(testValue, testLanguage);
        // Test that the datatype IRI is correctly returned
        assertEquals(RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }
}