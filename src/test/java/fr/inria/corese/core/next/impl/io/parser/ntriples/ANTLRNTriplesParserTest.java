package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ANTLRNTriplesParser class.
 * These tests verify the parser's ability to correctly parse N-Triples
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals.
 */
@ExtendWith(MockitoExtension.class)
class ANTLRNTriplesParserTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    private ANTLRNTriplesParser parser;

    @Mock
    private IRI mockSubjectIRI;
    @Mock
    private IRI mockPredicateIRI;
    @Mock
    private IRI mockObjectIRI;
    @Mock
    private BNode mockSubjectBNode;
    @Mock
    private BNode mockObjectBNode;
    @Mock
    private Literal mockSimpleLiteral;
    @Mock
    private Literal mockLangLiteral;
    @Mock
    private Literal mockTypedLiteral;
    @Mock
    private IRI mockDatatypeIRI;
    @Mock
    private IRI mockEscapedIRI;
    @Mock
    private Literal mockEscapedLiteral;


    @BeforeEach
    void setUp() {
        parser = new ANTLRNTriplesParser(mockModel, mockValueFactory);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.equals("http://example.org/subject")) return mockSubjectIRI;
            if (uri.equals("http://example.org/predicate")) return mockPredicateIRI;
            if (uri.equals("http://example.org/object")) return mockObjectIRI;
            if (uri.equals("http://www.w3.org/2001/XMLSchema#integer")) return mockDatatypeIRI;
            if (uri.equals("http://example.org/escaped>uri")) return mockEscapedIRI;
            return mock(IRI.class);
        });

        lenient().when(mockValueFactory.createBNode(anyString())).thenAnswer(invocation -> {
            String label = invocation.getArgument(0);
            if (label.equals("sub1")) return mockSubjectBNode;
            if (label.equals("obj1")) return mockObjectBNode;
            return mock(BNode.class);
        });

        lenient().when(mockValueFactory.createLiteral(eq("simple string"))).thenReturn(mockSimpleLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("hello"), eq("en"))).thenReturn(mockLangLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("123"), any(IRI.class))).thenReturn(mockTypedLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("literal with \"quotes\" and \n newline"))).thenReturn(mockEscapedLiteral);
    }

    @Test
    @DisplayName("Test get RDF format returns NTRIPLES")
    void testGetRDFFormat() {
        assertEquals(RdfFormat.NTRIPLES, parser.getRDFFormat());
    }

    @Test
    @DisplayName("Test parsing a basic triple with IRIs")
    void testParseBasicTriple() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a blank node subject")
    void testParseBlankNodeSubject() throws ParsingErrorException {
        String ntriple = "_:sub1 <http://example.org/predicate> <http://example.org/object> .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectBNode, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a blank node object")
    void testParseBlankNodeObject() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> _:obj1 .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectBNode);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a simple literal object")
    void testParseSimpleLiteralObject() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"simple string\" .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockSimpleLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a language-tagged literal object")
    void testParseLangLiteralObject() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"hello\"@en .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockLangLiteral);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a typed literal object")
    void testParseTypedLiteralObject() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockTypedLiteral);
        verifyNoMoreInteractions(mockModel);
    }


    @Test
    @DisplayName("Test parsing a triple with escaped characters in literal")
    void testParseEscapedLiteral() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"literal with \\\"quotes\\\" and \\n newline\" .";
        parser.parse(new StringReader(ntriple));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockEscapedLiteral);
        verifyNoMoreInteractions(mockModel);
    }


    @Test
    @DisplayName("Test parsing a triple with Unicode escape in literal (\\uXXXX)")
    void testParseUnicodeEscapeLiteralUxxxx() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"Hello\\u0020World\" .";

        Literal expectedLiteral = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Hello World"))).thenReturn(expectedLiteral);

        parser.parse(new StringReader(ntriple));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expectedLiteral);
    }

    @Test
    @DisplayName("Test parsing a triple with Unicode escape in literal (\\UXXXXXXXX)")
    void testParseUnicodeEscapeLiteral() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"Euro\\U000020AC\" .";

        Literal expectedLiteral = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Euro€"))).thenReturn(expectedLiteral);

        parser.parse(new StringReader(ntriple));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expectedLiteral);
    }

    @Test
    @DisplayName("Test parsing a triple with Unicode escape in IRI (\\uXXXX)")
    void testParseUnicodeEscapeIRIUxxxx() throws ParsingErrorException {
        String ntriple = "<http://example.org/s\\u0020ubject> <http://example.org/predicate> <http://example.org/object> .";
        IRI expectedSubjectIRI = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq("http://example.org/s ubject"))).thenReturn(expectedSubjectIRI);

        parser.parse(new StringReader(ntriple));
        verify(mockModel).add(expectedSubjectIRI, mockPredicateIRI, mockObjectIRI);
    }

    @Test
    @DisplayName("Test parsing a triple with Unicode escape in IRI (\\UXXXXXXXX)")
    void testParseUnicodeEscapeIRIU() throws ParsingErrorException {
        String ntriple = "<http://example.org/path/\\U000020AC> <http://example.org/predicate> <http://example.org/object> .";
        IRI expectedSubjectIRI = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq("http://example.org/path/€"))).thenReturn(expectedSubjectIRI);

        parser.parse(new StringReader(ntriple));
        verify(mockModel).add(expectedSubjectIRI, mockPredicateIRI, mockObjectIRI);
    }
}
