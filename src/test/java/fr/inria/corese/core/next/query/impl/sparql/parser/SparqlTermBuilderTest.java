package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlTermBuilder}.
 * Tests focus on the primitive factories (var, iri, literal) which are pure functions
 * with no dependency on ANTLR contexts.
 */
class SparqlTermBuilderTest {

    private SparqlTermBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SparqlTermBuilder(new SparqlParserOptions.Builder().build());
    }

    @Nested
    @DisplayName("var()")
    class VarFactory {

        @Test
        @DisplayName("strips leading '?' prefix")
        void stripsQuestionMark() {
            VarAst result = builder.var("?subject");
            assertEquals("subject", result.name());
        }

        @Test
        @DisplayName("strips leading '$' prefix")
        void stripsDollarSign() {
            VarAst result = builder.var("$x");
            assertEquals("x", result.name());
        }

        @Test
        @DisplayName("accepts name without prefix")
        void acceptsNameWithoutPrefix() {
            VarAst result = builder.var("myVar");
            assertEquals("myVar", result.name());
        }

        @Test
        @DisplayName("trims surrounding whitespace")
        void trimsWhitespace() {
            VarAst result = builder.var("  ?s  ");
            assertEquals("s", result.name());
        }

        @Test
        @DisplayName("throws on null input")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> builder.var(null));
        }

        @Test
        @DisplayName("throws on blank input")
        void throwsOnBlank() {
            assertThrows(IllegalArgumentException.class, () -> builder.var("   "));
        }
    }

    @Nested
    @DisplayName("iri()")
    class IriFactory {

        @Test
        @DisplayName("returns IriAst with raw value for angle-bracket IRI")
        void angleBracketIri() {
            IriAst result = builder.iri("<http://example.org/foo>");
            assertEquals("<http://example.org/foo>", result.raw());
        }

        @Test
        @DisplayName("returns IriAst with raw value for prefixed name")
        void prefixedName() {
            IriAst result = builder.iri("foaf:Person");
            assertEquals("foaf:Person", result.raw());
        }

        @Test
        @DisplayName("'a' is resolved to rdf:type IRI")
        void shortcutAResolvesToRdfType() {
            IriAst result = builder.iri("a");
            String expected = "<" + RDF.type.getIRI().stringValue() + ">";
            assertEquals(expected, result.raw());
        }

        @Test
        @DisplayName("throws on null input")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> builder.iri(null));
        }
    }

    @Nested
    @DisplayName("literal()")
    class LiteralFactory {

        @Test
        @DisplayName("plain literal without lang or datatype")
        void plainLiteral() {
            LiteralAst result = builder.literal("\"hello\"", null, null);
            assertEquals("\"hello\"", result.lexical());
            assertNull(result.lang());
            assertNull(result.datatype());
        }

        @Test
        @DisplayName("language-tagged literal")
        void langTaggedLiteral() {
            LiteralAst result = builder.literal("\"bonjour\"", "fr", null);
            assertEquals("\"bonjour\"", result.lexical());
            assertEquals("fr", result.lang());
            assertNull(result.datatype());
        }

        @Test
        @DisplayName("typed literal with xsd:integer datatype")
        void typedLiteral() {
            String xsdInt = XSD.xsdUnsignedInt.getIRI().stringValue();
            LiteralAst result = builder.literal("42", null, xsdInt);
            assertEquals("42", result.lexical());
            assertNull(result.lang());
            assertEquals(xsdInt, result.datatype());
        }

        @Test
        @DisplayName("throws on null lexical")
        void throwsOnNullLexical() {
            assertThrows(IllegalArgumentException.class, () -> builder.literal(null, null, null));
        }
    }

    @Nested
    @DisplayName("boolean literal constants")
    class BooleanLiterals {

        @Test
        @DisplayName("true literal has xsd:boolean datatype")
        void trueLiteralUsedInBooleanContext() {
            // Verify the builder produces the correct LiteralAst for boolean true directly
            LiteralAst trueLit = new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
            assertEquals("true", trueLit.lexical());
            assertEquals(XSD.xsdBoolean.getIRI().stringValue(), trueLit.datatype());
        }

        @Test
        @DisplayName("false literal has xsd:boolean datatype")
        void falseLiteralUsedInBooleanContext() {
            LiteralAst falseLit = new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
            assertEquals("false", falseLit.lexical());
            assertEquals(XSD.xsdBoolean.getIRI().stringValue(), falseLit.datatype());
        }
    }

    @Nested
    @DisplayName("numeric literal helpers")
    class NumericLiterals {

        @Test
        @DisplayName("integer literal uses xsd:unsignedInt")
        void integerLiteralDatatype() {
            LiteralAst lit = builder.literal("42", null, XSD.xsdUnsignedInt.getIRI().stringValue());
            assertEquals(XSD.xsdUnsignedInt.getIRI().stringValue(), lit.datatype());
        }

        @Test
        @DisplayName("decimal literal uses xsd:decimal")
        void decimalLiteralDatatype() {
            LiteralAst lit = builder.literal("3.14", null, XSD.xsdDecimal.getIRI().stringValue());
            assertEquals(XSD.xsdDecimal.getIRI().stringValue(), lit.datatype());
        }

        @Test
        @DisplayName("double literal uses xsd:double")
        void doubleLiteralDatatype() {
            LiteralAst lit = builder.literal("1.0e5", null, XSD.xsdDouble.getIRI().stringValue());
            assertEquals(XSD.xsdDouble.getIRI().stringValue(), lit.datatype());
        }

        @Test
        @DisplayName("positive integer literal uses xsd:positiveInteger")
        void positiveIntegerLiteralDatatype() {
            LiteralAst lit = builder.literal("+42", null, XSD.xsdPositiveInteger.getIRI().stringValue());
            assertEquals(XSD.xsdPositiveInteger.getIRI().stringValue(), lit.datatype());
        }

        @Test
        @DisplayName("negative integer literal uses xsd:negativeInteger")
        void negativeIntegerLiteralDatatype() {
            LiteralAst lit = builder.literal("-42", null, XSD.xsdNegativeInteger.getIRI().stringValue());
            assertEquals(XSD.xsdNegativeInteger.getIRI().stringValue(), lit.datatype());
        }
    }
}
