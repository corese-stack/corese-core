package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName; // Import DisplayName
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Reader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NTriplesParserTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    private NTriplesParser parser;

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


    @BeforeEach
    void setUp() {
        parser = new NTriplesParser(mockModel, mockValueFactory);


        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);

            if (uri.equals("http://example.org/subject")) return mockSubjectIRI;
            if (uri.equals("http://example.org/predicate")) return mockPredicateIRI;
            if (uri.equals("http://example.org/object")) return mockObjectIRI;
            if (uri.equals("http://www.w3.org/2001/XMLSchema#integer")) return mockDatatypeIRI;
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
    }

    @Test
    @DisplayName("Test get RDF format returns NTRIPLES")
    void testGetRDFFormat() {
        assertEquals(RdfFormat.NTRIPLES, parser.getRDFFormat());
    }

    @Test
    @DisplayName("Test parsing a basic triple")
    void testParseBasicTriple() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .";
        parser.parse(new StringReader(ntriple));

        verify(mockValueFactory).createIRI("http://example.org/subject");
        verify(mockValueFactory).createIRI("http://example.org/predicate");
        verify(mockValueFactory).createIRI("http://example.org/object");

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Test parsing a triple with a blank node subject")
    void testParseBlankNodeSubject() throws ParsingErrorException {
        String ntriple = "_:sub1 <http://example.org/predicate> <http://example.org/object> .";
        parser.parse(new StringReader(ntriple));

        verify(mockValueFactory).createBNode("sub1");
        verify(mockValueFactory).createIRI("http://example.org/predicate");
        verify(mockValueFactory).createIRI("http://example.org/object");

        verify(mockModel).add(mockSubjectBNode, mockPredicateIRI, mockObjectIRI);
    }

    @Test
    @DisplayName("Test parsing a triple with a blank node object")
    void testParseBlankNodeObject() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> _:obj1 .";
        parser.parse(new StringReader(ntriple));

        verify(mockValueFactory).createIRI("http://example.org/subject");
        verify(mockValueFactory).createIRI("http://example.org/predicate");
        verify(mockValueFactory).createBNode("obj1");

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectBNode);
    }

    @Test
    @DisplayName("Test parsing a simple literal")
    void testParseSimpleLiteral() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"simple string\" .";
        parser.parse(new StringReader(ntriple));

        verify(mockValueFactory).createLiteral("simple string");
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockSimpleLiteral);
    }

    @Test
    @DisplayName("Test parsing a language-tagged literal")
    void testParseLanguageTaggedLiteral() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"hello\"@en .";
        parser.parse(new StringReader(ntriple));

        verify(mockValueFactory).createLiteral("hello", "en");
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockLangLiteral);
    }

    @Test
    @DisplayName("Test parsing a typed literal")
    void testParseTypedLiteral() throws ParsingErrorException {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .";
        parser.parse(new StringReader(ntriple));

        ArgumentCaptor<IRI> datatypeCaptor = ArgumentCaptor.forClass(IRI.class);
        verify(mockValueFactory).createLiteral(eq("123"), datatypeCaptor.capture());
        verify(mockValueFactory).createIRI("http://www.w3.org/2001/XMLSchema#integer");

        assertEquals(mockDatatypeIRI, datatypeCaptor.getValue());
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockTypedLiteral);
    }

    @Test
    @DisplayName("Test parsing multiple triples in one go")
    void testParseMultipleTriples() throws ParsingErrorException {
        String ntriples =
                "<http://example.org/s1> <http://example.org/p1> <http://example.org/o1> .\n" +
                        "_:b2 <http://example.org/p2> \"literal\"@fr .";
        parser.parse(new StringReader(ntriples));


        verify(mockValueFactory).createIRI("http://example.org/s1");
        verify(mockValueFactory).createIRI("http://example.org/p1");
        verify(mockValueFactory).createIRI("http://example.org/o1");
        verify(mockModel).add(any(IRI.class), any(IRI.class), any(IRI.class));


        verify(mockValueFactory).createBNode("b2");
        verify(mockValueFactory).createIRI("http://example.org/p2");
        verify(mockValueFactory).createLiteral("literal", "fr");

        ArgumentCaptor<Resource> subjCaptor = ArgumentCaptor.forClass(Resource.class);
        ArgumentCaptor<IRI> predCaptor = ArgumentCaptor.forClass(IRI.class);
        ArgumentCaptor<Value> objCaptor = ArgumentCaptor.forClass(Value.class);
        verify(mockModel, times(2)).add(subjCaptor.capture(), predCaptor.capture(), objCaptor.capture());
    }

    @Test
    @DisplayName("Test parsing with comments and empty lines ignored")
    void testParseWithCommentsAndEmptyLines() throws ParsingErrorException {
        String ntriples = """
                # This is a comment
                
                <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                 # Another comment with leading spaces
                
                """;


        parser.parse(new StringReader(ntriples));

        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "<http://example.org/subject> <http://example.org/predicate> .",
            "<http://example.org/subject> <http://example.org/predicate> <http://example.org/object>"
    })
    @DisplayName("Invalid N-Triples should throw ParsingErrorException")
    void testInvalidNTriplesThrowsParsingErrorException(String invalidNTriple) {
        StringReader reader = new StringReader(invalidNTriple);
        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that invalid line (too few parts) throws ParsingErrorException")
    void testParseInvalidLineTooFewPartsThrowsException() {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> .";
        StringReader reader = new StringReader(ntriple);

        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that invalid IRI format throws ParsingErrorException")
    void testParseInvalidResourceThrowsException() {
        String ntriple = "invalid_subject <http://example.org/predicate> <http://example.org/object> .";
        Reader reader = new StringReader(ntriple);

        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that invalid predicate IRI format throws ParsingErrorException")
    void testParseInvalidIRITThrowsException() {
        String ntriple = "<http://example.org/subject> invalid_predicate <http://example.org/object> .";
        StringReader reader = new StringReader(ntriple);

        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }
    @Test
    @DisplayName("Test that invalid literal format throws ParsingErrorException")
    void testParseInvalidLiteralThrowsException() {
        String ntriple = "<http://example.org/subject> <http://example.org/predicate> \"unclosed literal .";
        Reader reader = new StringReader(ntriple);

        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }

    @Test
    @DisplayName("Test that I/O errors during parsing throw ParsingErrorException")
    void testParseIOErrorThrowsException() {
        StringReader reader = new StringReader("test line .");
        reader.close();

        assertThrows(ParsingErrorException.class, () -> parser.parse(reader));
    }
}
