package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
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
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(coreseLiteral);
        // Test that the coreseObject is correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(CoreDatatype.RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(CoreDatatype.RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    public void testConstructorWithValueAndLanguage() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);

        // Test that the value and language are correctly assigned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().orElse(null));
        assertEquals(CoreDatatype.RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
        assertEquals(CoreDatatype.RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }

    @Test
    public void testGetLabel() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);
        // Test that the label (value) is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getLabel());
    }

    @Test
    public void testGetLanguage() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);
        // Test that the language is correctly returned as an Optional
        assertTrue(coreseLanguageTaggedString.getLanguage().isPresent());
        assertEquals(testLanguage, coreseLanguageTaggedString.getLanguage().get());
    }

    @Test
    public void testGetValue() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);
        // Test that the value is correctly returned
        assertEquals(testValue, coreseLanguageTaggedString.getValue());
    }

    @Test
    public void testGetCoreDatatype() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);
        // Test that the CoreDatatype is correctly returned (should be RDF.LANGSTRING)
        assertEquals(CoreDatatype.RDF.LANGSTRING, coreseLanguageTaggedString.getCoreDatatype());
    }

    @Test
    public void testGetDatatype() {
        CoreseLanguageTaggedString coreseLanguageTaggedString = new CoreseLanguageTaggedString(testValue, testLanguage);
        // Test that the datatype IRI is correctly returned
        assertEquals(CoreDatatype.RDF.LANGSTRING.getIRI(), coreseLanguageTaggedString.getDatatype());
    }
}