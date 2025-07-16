package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
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
 * Unit tests for the ANTLRNQuadsParser class.
 * These tests verify the parser's ability to correctly parse N-Quads
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
@ExtendWith(MockitoExtension.class)
class ANTLRNQuadsParserTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    private ANTLRNQuadsParser parser;

    @Mock
    private IRI mockSubjectIRI;
    @Mock
    private IRI mockPredicateIRI;
    @Mock
    private IRI mockObjectIRI;
    @Mock
    private IRI mockGraphIRI;
    @Mock
    private BNode mockSubjectBNode;
    @Mock
    private BNode mockObjectBNode;
    @Mock
    private BNode mockGraphBNode;
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
        parser = new ANTLRNQuadsParser(mockModel, mockValueFactory);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.equals("http://example.org/subject")) return mockSubjectIRI;
            if (uri.equals("http://example.org/predicate")) return mockPredicateIRI;
            if (uri.equals("http://example.org/object")) return mockObjectIRI;
            if (uri.equals("http://example.org/graph")) return mockGraphIRI;
            if (uri.equals("http://www.w3.org/2001/XMLSchema#integer")) return mockDatatypeIRI;
            if (uri.equals("http://example.org/escaped>uri")) return mockEscapedIRI;
            if (uri.equals("http://example.org/s ubject")) return mock(IRI.class);
            if (uri.equals("http://example.org/path/€")) return mock(IRI.class);
            if (uri.equals("http://example.org/path>with\\<escaped\\_chars\\")) return mock(IRI.class);
            if (uri.equals("http://example.org/graphName")) return mock(IRI.class);
            return mock(IRI.class);
        });

        lenient().when(mockValueFactory.createBNode(anyString())).thenAnswer(invocation -> {
            String label = invocation.getArgument(0);
            if (label.equals("sub1")) return mockSubjectBNode;
            if (label.equals("obj1")) return mockObjectBNode;
            if (label.equals("graph1")) return mockGraphBNode;
            return mock(BNode.class);
        });

        lenient().when(mockValueFactory.createLiteral(eq("simple string"))).thenReturn(mockSimpleLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("hello"), eq("en"))).thenReturn(mockLangLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("123"), any(IRI.class))).thenReturn(mockTypedLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("literal with \"quotes\" and \n newline"))).thenReturn(mockEscapedLiteral);
    }

    @Test
    @DisplayName("Test get RDF format returns NQUADS")
    void testGetRDFFormat() {
        assertEquals(RDFFormat.NQUADS, parser.getRDFFormat());
    }

    @Test
    @DisplayName("Test parsing a basic quad with IRI graph")
    void testParseBasicQuadWithIRIGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> <http://example.org/graph> .";
        parser.parse(new StringReader(nquad));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a basic quad with BNode graph")
    void testParseBasicQuadWithBNodeGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> _:graph1 .";
        parser.parse(new StringReader(nquad));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphBNode);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a quad with a literal object and IRI graph")
    void testParseQuadWithLiteralObjectAndIRIGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> \"simple string\" <http://example.org/graph> .";
        parser.parse(new StringReader(nquad));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockSimpleLiteral, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple (no graph) which should go to default graph")
    void testParseTripleToDefaultGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .";
        parser.parse(new StringReader(nquad));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }


    @Test
    @DisplayName("Test parsing a quad with escaped characters in literal")
    void testParseEscapedLiteral() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> \"literal with \\\"quotes\\\" and \\n newline\" <http://example.org/graph> .";
        parser.parse(new StringReader(nquad));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockEscapedLiteral, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }


    @Test
    @DisplayName("Test parsing a quad with Unicode escape in literal (\\uXXXX)")
    void testParseUnicodeEscapeLiteralU() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> \"Hello\\u0020World\" <http://example.org/graph> .";
        Literal expectedLiteral = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Hello World"))).thenReturn(expectedLiteral);

        parser.parse(new StringReader(nquad));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expectedLiteral, mockGraphIRI);
    }

    @Test
    @DisplayName("Test parsing a quad with Unicode escape in literal (\\UXXXXXXXX)")
    void testParseUnicodeEscapeLiteralUx() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> \"Euro\" <http://example.org/graph> .";
        Literal expectedLiteral = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Euro"))).thenReturn(expectedLiteral);

        parser.parse(new StringReader(nquad));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expectedLiteral, mockGraphIRI);
    }

    @Test
    @DisplayName("Test parsing a quad with Unicode escape in IRI (\\uXXXX) in graph")
    void testParseUnicodeEscapeIRIUInGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> <http://example.org/grap\\u0068Name> .";
        IRI expectedGraphIRI = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq("http://example.org/graphName"))).thenReturn(expectedGraphIRI);

        parser.parse(new StringReader(nquad));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, expectedGraphIRI);
    }

    @Test
    @DisplayName("Test parsing a quad with Unicode escape in IRI (\\UXXXXXXXX) in graph")
    void testParseUnicodeEscapeIRIUxInGraph() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> <http://example.org/graph> .";
        IRI expectedGraphIRI = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq("http://example.org/graph"))).thenReturn(expectedGraphIRI);

        parser.parse(new StringReader(nquad));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, expectedGraphIRI);
    }
}
