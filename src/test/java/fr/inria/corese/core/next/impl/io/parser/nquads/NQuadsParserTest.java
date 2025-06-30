package fr.inria.corese.core.next.impl.io.parser.nquads;


import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Reader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NQuadsParserTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    private NQuadsParser parser;


    @Mock private IRI mockSubjectIRI;
    @Mock private IRI mockPredicateIRI;
    @Mock private IRI mockObjectIRI;
    @Mock private IRI mockGraphIRI;
    @Mock private BNode mockSubjectBNode;
    @Mock private BNode mockObjectBNode;
    @Mock private BNode mockGraphBNode;
    @Mock private Literal mockSimpleLiteral;
    @Mock private Literal mockLangLiteral;
    @Mock private Literal mockTypedLiteral;
    @Mock private IRI mockDatatypeIRI;


    @BeforeEach
    void setUp() {
        parser = new NQuadsParser(mockModel, mockValueFactory);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.equals("http://example.org/subject")) return mockSubjectIRI;
            if (uri.equals("http://example.org/predicate")) return mockPredicateIRI;
            if (uri.equals("http://example.org/object")) return mockObjectIRI;
            if (uri.equals("http://example.org/graph")) return mockGraphIRI;
            if (uri.equals("http://www.w3.org/2001/XMLSchema#integer")) return mockDatatypeIRI;
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
    }

    @Test
    @DisplayName("Test get RDF format returns NQUADS")
    void testGetRDFFormat() {
        assertEquals(RdfFormat.NQUADS, parser.getRDFFormat());
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
    @DisplayName("Test that invalid N-Quads (less than 3 parts) throws ParsingErrorException")
    void testInvalidNQuadsThrowsParsingErrorException_TooFewParts() {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> .";
        StringReader reader = new StringReader(nquad);
        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that invalid N-Quads (missing period) is warned and skipped")
    void testInvalidNQuadsMissingPeriodIsSkipped() throws ParsingErrorException {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object>";
        parser.parse(new StringReader(nquad));
        verifyNoInteractions(mockModel);
    }


    @Test
    @DisplayName("Test that invalid IRI format throws ParsingErrorException")
    void testParseInvalidResourceThrowsException() {
        String nquad = "invalid_subject <http://example.org/predicate> <http://example.org/object> <http://example.org/graph> .";
        Reader reader = new StringReader(nquad);
        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that invalid literal format throws ParsingErrorException")
    void testParseInvalidLiteralThrowsException() {
        String nquad = "<http://example.org/subject> <http://example.org/predicate> \"unclosed literal <http://example.org/graph> .";
        Reader reader = new StringReader(nquad);
        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

}