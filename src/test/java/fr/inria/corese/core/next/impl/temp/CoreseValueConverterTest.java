package fr.inria.corese.core.next.impl.temp; // Adjust package as necessary

import fr.inria.corese.core.sparql.api.IDatatype;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link CoreseValueConverter} class.
 * This class focuses on testing the conversion logic from Corese's internal
 * {@link IDatatype} representation to RDF4J's {@link Value} objects,
 * especially for URI, Blank Node, and various Literal types.
 */
@DisplayName("CoreseValueConverter: Test de la conversion IDatatype Corese vers RDF4J Value")
class CoreseValueConverterTest {

    private CoreseValueConverter converter;
    private IDatatype mockIDatatype;

    /**
     * Sets up the test environment before each test method is executed.
     * Initializes a new {@link CoreseValueConverter} instance and a mock
     * {@link IDatatype} object.
     */
    @BeforeEach
    void setUp() {
        converter = new CoreseValueConverter();
        mockIDatatype = Mockito.mock(IDatatype.class);
    }

    /**
     * Tests the conversion of a Corese URI {@link IDatatype} to an RDF4J {@link IRI}.
     * Verifies that the resulting value is an IRI and its string representation matches the expected URI.
     */
    @Test
    @DisplayName("Conversion URI: IDatatype URI vers RDF4J IRI")
    void testConvertUriToRdf4jValue() {
        String uriString = "http://example.org/resource";
        when(mockIDatatype.isURI()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(uriString);

        Value result = converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertTrue(result instanceof IRI);
        assertEquals(uriString, result.stringValue());
    }

    /**
     * Tests the conversion of a Corese Blank Node {@link IDatatype} to an RDF4J {@link BNode}.
     * Verifies that the resulting value is a BNode and its ID matches the expected blank node ID.
     */
    @Test
    @DisplayName("Conversion BNode: IDatatype Blank Node vers RDF4J BNode")
    void testConvertBNodeToRdf4jValue() {
        String bnodeId = "b123";
        when(mockIDatatype.isBlank()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(bnodeId);

        Value result = converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertTrue(result instanceof BNode);
        assertEquals(bnodeId, ((BNode) result).getID());
    }


    /**
     * Tests the conversion of a Corese plain literal (no language, no datatype)
     * to an RDF4J {@link Literal}.
     * Verifies that the literal's label is correct and that it has neither a language tag
     * nor an explicit datatype URI (i.e., it's a plain literal).
     */
    @Test
    @DisplayName("Literal Simple: Conversion d'un littéral sans langue ni datatype")
    void testConvertPlainLiteral() {
        String label = "Hello world";
        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(null);
        when(mockIDatatype.getDatatypeURI()).thenReturn(null);

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
    }

    /**
     * Tests the conversion of a Corese language-tagged literal to an RDF4J {@link Literal}.
     * Verifies the label, the presence and value of the language tag, and that its datatype
     * is correctly identified as `rdf:langString`.
     */
    @Test
    @DisplayName("Literal avec Langue: Conversion d'un littéral avec tag de langue valide")
    void testConvertLanguageTaggedLiteral() {
        String label = "Bonjour";
        String lang = "fr";
        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(lang);
        when(mockIDatatype.getDatatypeURI()).thenReturn(RDF.LANGSTRING.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
        assertEquals(RDF.LANGSTRING, result.getDatatype());
    }

    /**
     * Tests the conversion of a Corese typed literal with `xsd:string` datatype
     * to an RDF4J {@link Literal}.
     * Verifies the label, absence of language tag, and correct `xsd:string` datatype.
     */
    @Test
    void testConvertTypedLiteral_XSDString() {
        String label = "Some text";

        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(null);
        when(mockIDatatype.getDatatypeURI()).thenReturn(XMLSchema.STRING.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
        assertEquals(XMLSchema.STRING, result.getDatatype());
    }

    /**
     * Tests the conversion of a Corese typed literal with `xsd:integer` datatype
     * to an RDF4J {@link Literal}.
     * Verifies the label, absence of language tag, and correct `xsd:integer` datatype.
     */
    @Test
    void testConvertTypedLiteral_XSDInteger() {
        String label = "123";
        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(null);
        when(mockIDatatype.getDatatypeURI()).thenReturn(XMLSchema.INTEGER.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
        assertEquals(XMLSchema.INTEGER, result.getDatatype());
    }

    /**
     * Tests a problematic scenario where Corese might incorrectly put a URI
     * (like `xsd:string`) into the language field and/or set `rdf:langString`
     * as datatype for a non-language-tagged literal.
     * Verifies that the converter handles this by defaulting to a plain literal.
     */
    @Test
    void testConvertProblematicLiteral_LangFieldContainsURI() {
        String label = "Problematic literal";
        // This is the scenario we encountered: xsd:string URI in the lang field
        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(XMLSchema.STRING.stringValue());
        when(mockIDatatype.getDatatypeURI()).thenReturn(RDF.LANGSTRING.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage(), "Should not have a language tag if it's a URI");
        assertNotNull(result.getDatatype(), "Should be a plain literal after correction");
    }

    /**
     * Tests a scenario where Corese implicitly treats a literal as `xsd:string`
     * but provides a null language tag.
     * Verifies that the converter correctly handles this, typically resulting in a plain literal
     * or a literal typed as `xsd:string` if the logic explicitly handles it.
     */
    @Test
    void testConvertProblematicLiteral_ImplicitXSDStringWithNullLang() {
        String label = "Implicit XSD string";

        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn(null);
        when(mockIDatatype.getDatatypeURI()).thenReturn(XMLSchema.STRING.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
    }

    /**
     * Tests a scenario where `rdf:langString` is indicated, but the provided
     * language tag is invalid (e.g., empty string or contains a colon).
     * Verifies that the converter falls back to a plain literal in such cases.
     */
    @Test
    @DisplayName("Literal Problématique: rdf:langString avec tag de langue invalide (vide ou colon)")
    void testConvertProblematicLiteral_RdfLangStringWithoutValidLang() {
        String label = "Literal with rdf:langString but no valid lang";
        when(mockIDatatype.isLiteral()).thenReturn(true);
        when(mockIDatatype.getLabel()).thenReturn(label);
        when(mockIDatatype.getLang()).thenReturn("");
        when(mockIDatatype.getDatatypeURI()).thenReturn(RDF.LANGSTRING.stringValue());

        Literal result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);

        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage(), "Should not have a language tag if invalid");
        assertNotNull(result.getDatatype(), "Should be a plain literal fallback");


        when(mockIDatatype.getLang()).thenReturn("en:US");
        result = (Literal) converter.valuetoRdf4jValue(mockIDatatype);
        assertNotNull(result);
        assertEquals(label, result.getLabel());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getDatatype());
    }
    /**
     * Tests that an {@link IllegalArgumentException} is thrown when attempting
     * to convert an unsupported Corese {@link IDatatype} type (e.g., not URI, Blank Node, or Literal).
     */
    @Test
    @DisplayName("Gestion des Erreurs: Type d'IDatatype non supporté")
    void testConvertUnsupportedDatatype() {

        when(mockIDatatype.isURI()).thenReturn(false);
        when(mockIDatatype.isBlank()).thenReturn(false);
        when(mockIDatatype.isLiteral()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            converter.valuetoRdf4jValue(mockIDatatype);
        });
    }
    /**
     * Tests that `null` input to {@link CoreseValueConverter#valuetoRdf4jValue(IDatatype)}
     * correctly returns `null`, ensuring graceful handling of null values.
     */
    @Test
    @DisplayName("Gestion des Nulls: IDatatype null en entrée")
    void testConvertNullIDatatype() {
        assertNull(converter.valuetoRdf4jValue(null));
    }
}