package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TurtleScanner}.
 * Verifies correct parsing of Turtle 1.1 syntax and interaction with
 * Model and ValueFactory, including prefixes, literals, blank nodes,
 * shorthands, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class TurtleScannerTest {

    @Mock private Model mockModel;
    @Mock private ValueFactory mockValueFactory;

    @Mock private IRI mockSubjectIRI;
    @Mock private IRI mockPredicateIRI;
    @Mock private IRI mockObjectIRI;
    @Mock private IRI mockPredicate2IRI;
    @Mock private IRI mockObject2IRI;
    @Mock private IRI mockRdfTypeIRI;
    @Mock private IRI mockDatatypeIRI;
    @Mock private BNode mockBNode1;
    @Mock private BNode mockBNode2;
    @Mock private Literal mockSimpleLiteral;
    @Mock private Literal mockLangLiteral;
    @Mock private Literal mockTypedLiteral;
    @Mock private Literal mockIntLiteral;
    @Mock private Literal mockDecimalLiteral;
    @Mock private Literal mockDoubleLiteral;
    @Mock private Literal mockBoolLiteral;
    @Mock private Literal mockEscapedLiteral;

    private TurtleScanner parser;

    private static final String NS  = "http://example.org/";
    private static final String S   = NS + "subject";
    private static final String P   = NS + "predicate";
    private static final String P2  = NS + "predicate2";
    private static final String O   = NS + "object";
    private static final String O2  = NS + "object2";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    private static final String XSD_DECIMAL = "http://www.w3.org/2001/XMLSchema#decimal";
    private static final String XSD_DOUBLE  = "http://www.w3.org/2001/XMLSchema#double";
    private static final String XSD_BOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";

    @BeforeEach
    void setUp() {
        parser = new TurtleScanner(mockModel, mockValueFactory);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(inv -> {
            String uri = inv.getArgument(0);
            return switch (uri) {
                case S          -> mockSubjectIRI;
                case P          -> mockPredicateIRI;
                case P2         -> mockPredicate2IRI;
                case O          -> mockObjectIRI;
                case O2         -> mockObject2IRI;
                case RDF_TYPE   -> mockRdfTypeIRI;
                case XSD_INTEGER, XSD_DECIMAL, XSD_DOUBLE, XSD_BOOLEAN -> mockDatatypeIRI;
                default         -> mock(IRI.class);
            };
        });

        lenient().when(mockValueFactory.createBNode())
                 .thenReturn(mockBNode1, mockBNode2, mock(BNode.class));

        lenient().when(mockValueFactory.createLiteral(eq("simple string"))).thenReturn(mockSimpleLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("hello"), eq("en"))).thenReturn(mockLangLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("123"), any(IRI.class))).thenReturn(mockTypedLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("42"),   any(IRI.class))).thenReturn(mockIntLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("3.14"), any(IRI.class))).thenReturn(mockDecimalLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("1.5e3"), any(IRI.class))).thenReturn(mockDoubleLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("true"),  any(IRI.class))).thenReturn(mockBoolLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("false"), any(IRI.class))).thenReturn(mockBoolLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("literal with \"quotes\" and \n newline")))
                 .thenReturn(mockEscapedLiteral);
    }

    // ── Format ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRDFFormat returns TURTLE")
    void testGetRDFFormat() {
        assertEquals(RDFFormat.TURTLE, parser.getRDFFormat());
    }

    // ── @prefix / PREFIX ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse basic triple with @prefix")
    void testParseBasicTripleWithAtPrefix() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate ex:object .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse basic triple with SPARQL PREFIX")
    void testParseBasicTripleWithSparqlPrefix() {
        parser.parse(new StringReader("""
                PREFIX ex: <http://example.org/>
                ex:subject ex:predicate ex:object .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── @base / BASE ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with @base for relative IRI resolution")
    void testParseBasicTripleWithAtBase() {
        parser.parse(new StringReader("""
                @base <http://example.org/> .
                <subject> <predicate> <object> .
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with SPARQL BASE")
    void testParseBasicTripleWithSparqlBase() {
        parser.parse(new StringReader("""
                BASE <http://example.org/>
                <subject> <predicate> <object> .
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── "a" shorthand ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple using 'a' shorthand for rdf:type")
    void testParseAShorthand() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject a ex:object .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockRdfTypeIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Predicate-object list (;) and object list (,) ─────────────────────────

    @Test
    @DisplayName("Parse predicate-object list separated by semicolons")
    void testParsePredicateObjectList() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate  ex:object ;
                           ex:predicate2 ex:object2 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI,  mockObjectIRI);
        verify(mockModel).add(mockSubjectIRI, mockPredicate2IRI, mockObject2IRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse object list separated by commas")
    void testParseObjectList() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate ex:object , ex:object2 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObject2IRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Literals ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with a simple string literal")
    void testParseSimpleLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate "simple string" .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockSimpleLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with a language-tagged literal")
    void testParseLangTaggedLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate "hello"@en .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockLangLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with a typed literal using prefixed datatype")
    void testParseTypedLiteral() {
        parser.parse(new StringReader("""
                @prefix ex:  <http://example.org/> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                ex:subject ex:predicate "123"^^xsd:integer .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockTypedLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with escaped characters in literal")
    void testParseEscapedLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate "literal with \\"quotes\\" and \\n newline" .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockEscapedLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Numeric and boolean literals ──────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with an integer literal")
    void testParseIntegerLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate 42 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockIntLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with a decimal literal")
    void testParseDecimalLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate 3.14 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockDecimalLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with a double literal")
    void testParseDoubleLiteral() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate 1.5e3 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockDoubleLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with boolean true literal")
    void testParseBooleanTrue() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate true .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockBoolLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with boolean false literal")
    void testParseBooleanFalse() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate false .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockBoolLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Unicode escapes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with Unicode escape in literal (4-hex form)")
    void testParseUnicodeEscapeInLiteral() {
        char bs = '\\';
        String input = "@prefix ex: <http://example.org/> .\n"
                + "ex:subject ex:predicate \"Hello" + bs + "u0020World\" .";
        Literal expected = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Hello World"))).thenReturn(expected);

        parser.parse(new StringReader(input));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expected);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with Unicode escape in literal (8-hex form)")
    void testParseUnicodeEscapeInLiteralLong() {
        char bs = '\\';
        String input = "@prefix ex: <http://example.org/> .\n"
                + "ex:subject ex:predicate \"Euro" + bs + "U000020AC\" .";
        Literal expected = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Euro€"))).thenReturn(expected);

        parser.parse(new StringReader(input));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expected);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with Unicode escape in IRI (4-hex form)")
    void testParseUnicodeEscapeInIRI() {
        char bs = '\\';
        String input = "@prefix ex: <http://example.org/> .\n"
                + "<http://example.org/sub" + bs + "u006Aect> ex:predicate ex:object .";
        IRI expected = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq(S))).thenReturn(expected);

        parser.parse(new StringReader(input));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(expected, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Blank nodes ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with a labeled blank node as subject")
    void testParseBlankNodeSubject() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                _:b1 ex:predicate ex:object .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockBNode1, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with a labeled blank node as object")
    void testParseBlankNodeObject() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate _:b1 .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockBNode1);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Blank node property list edge cases ──────────────────────────────────

    @Test
    @DisplayName("Parse blank node property list with trailing semicolon before ']'")
    void testParseBlankNodePropertyListTrailingSemicolon() {
        // Trailing ';' before ']' is legal Turtle: [ :p :o ; ]
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:subject ex:predicate [ ex:predicate ex:object ; ] .
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockBNode1);
        verify(mockModel).add(mockBNode1, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Undeclared prefix throws ParsingException")
    void testUndeclaredPrefixThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader("undeclared:s undeclared:p undeclared:o .")));
    }

    @Test
    @DisplayName("Undeclared empty prefix throws ParsingException")
    void testUndeclaredEmptyPrefixThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader(":s :p :o .")));
    }

    @Test
    @DisplayName("Unclosed IRI throws ParsingException")
    void testUnclosedIRIThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader(
                        "<http://example.org/s <http://example.org/p> <http://example.org/o> .")));
    }
}
