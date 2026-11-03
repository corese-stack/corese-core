package fr.inria.corese.core.next.data.impl.io.parser.trig;

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
 * Unit tests for {@link TriGScanner}.
 * Verifies correct parsing of TriG syntax and interaction with Model and
 * ValueFactory, including named graphs, default graph, blank nodes, and
 * error handling.
 */
@ExtendWith(MockitoExtension.class)
class TriGScannerTest {

    @Mock private Model mockModel;
    @Mock private ValueFactory mockValueFactory;

    @Mock private IRI mockSubjectIRI;
    @Mock private IRI mockPredicateIRI;
    @Mock private IRI mockObjectIRI;
    @Mock private IRI mockGraphIRI;
    @Mock private IRI mockGraph2IRI;
    @Mock private IRI mockRdfTypeIRI;
    @Mock private BNode mockBNode1;
    @Mock private BNode mockBNode2;
    @Mock private Literal mockSimpleLiteral;
    @Mock private Literal mockLangLiteral;
    @Mock private Literal mockTypedLiteral;

    private TriGScanner parser;

    private static final String NS  = "http://example.org/";
    private static final String S   = NS + "subject";
    private static final String P   = NS + "predicate";
    private static final String O   = NS + "object";
    private static final String G   = NS + "graph";
    private static final String G2  = NS + "graph2";
    private static final String RDF_TYPE    = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    @BeforeEach
    void setUp() {
        parser = new TriGScanner(mockModel, mockValueFactory);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(inv -> {
            String uri = inv.getArgument(0);
            return switch (uri) {
                case S        -> mockSubjectIRI;
                case P        -> mockPredicateIRI;
                case O        -> mockObjectIRI;
                case G        -> mockGraphIRI;
                case G2       -> mockGraph2IRI;
                case RDF_TYPE -> mockRdfTypeIRI;
                default       -> mock(IRI.class);
            };
        });

        lenient().when(mockValueFactory.createBNode())
                 .thenReturn(mockBNode1, mockBNode2, mock(BNode.class));

        lenient().when(mockValueFactory.createLiteral(eq("simple string"))).thenReturn(mockSimpleLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("hello"), eq("en"))).thenReturn(mockLangLiteral);
        lenient().when(mockValueFactory.createLiteral(eq("123"), any(IRI.class))).thenReturn(mockTypedLiteral);
    }

    // ── Format ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRDFFormat returns TRIG")
    void testGetRDFFormat() {
        assertEquals(RDFFormat.TRIG, parser.getRDFFormat());
    }

    // ── Named graph – GRAPH keyword ───────────────────────────────────────────

    @Test
    @DisplayName("Parse named graph block using GRAPH keyword with prefixed IRI label")
    void testParseNamedGraphWithGRAPHKeyword() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  ex:subject ex:predicate ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse named graph block using GRAPH keyword with absolute IRI label")
    void testParseNamedGraphWithAbsoluteIRI() {
        parser.parse(new StringReader("""
                GRAPH <http://example.org/graph> {
                  <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                }
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse named graph block using GRAPH keyword with blank node label")
    void testParseNamedGraphWithBNodeLabel() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH _:g {
                  ex:subject ex:predicate ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockBNode1);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("GRAPH keyword is case-insensitive (lowercase accepted)")
    void testParseNamedGraphLowercaseKeyword() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                graph ex:graph {
                  ex:subject ex:predicate ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Named graph – bare label ──────────────────────────────────────────────

    @Test
    @DisplayName("Parse named graph block using bare prefixed IRI label (no GRAPH keyword)")
    void testParseNamedGraphBarePrefixedLabel() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                ex:graph {
                  ex:subject ex:predicate ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse named graph block using bare absolute IRI label (no GRAPH keyword)")
    void testParseNamedGraphBareAbsoluteLabel() {
        parser.parse(new StringReader("""
                <http://example.org/graph> {
                  <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                }
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Default graph ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse top-level triple (no graph block) goes to default graph (null context)")
    void testParseDefaultGraphTriple() {
        parser.parse(new StringReader("""
                <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, (IRI) null);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse anonymous graph block {} goes to default graph (null context)")
    void testParseAnonymousGraphBlock() {
        parser.parse(new StringReader("""
                {
                  <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                }
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, (IRI) null);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Multiple graphs ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse two triples in two different named graphs")
    void testParseTwoNamedGraphs() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph  { ex:subject ex:predicate ex:object . }
                GRAPH ex:graph2 { ex:subject ex:predicate ex:object . }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraph2IRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse mixed default graph and named graph in same document")
    void testParseMixedDefaultAndNamed() {
        parser.parse(new StringReader("""
                <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                GRAPH <http://example.org/graph> {
                  <http://example.org/subject> <http://example.org/predicate> <http://example.org/object> .
                }
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, (IRI) null);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Features inside graph blocks ──────────────────────────────────────────

    @Test
    @DisplayName("Parse 'a' shorthand inside a named graph block")
    void testParseAShorthandInsideGraph() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  ex:subject a ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockRdfTypeIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse simple literal inside named graph block")
    void testParseLiteralInsideGraph() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  ex:subject ex:predicate "simple string" .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockSimpleLiteral, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse language-tagged literal inside named graph block")
    void testParseLangLiteralInsideGraph() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  ex:subject ex:predicate "hello"@en .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockLangLiteral, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse typed literal inside named graph block")
    void testParseTypedLiteralInsideGraph() {
        parser.parse(new StringReader("""
                @prefix ex:  <http://example.org/> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                GRAPH ex:graph {
                  ex:subject ex:predicate "123"^^xsd:integer .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockTypedLiteral, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── SPARQL-style directives ───────────────────────────────────────────────

    @Test
    @DisplayName("Parse named graph using SPARQL PREFIX directive")
    void testParseSparqlPrefix() {
        parser.parse(new StringReader("""
                PREFIX ex: <http://example.org/>
                GRAPH ex:graph { ex:subject ex:predicate ex:object . }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse named graph using SPARQL BASE directive")
    void testParseSparqlBase() {
        parser.parse(new StringReader("""
                BASE <http://example.org/>
                <graph> { <subject> <predicate> <object> . }
                """));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Blank nodes in named graphs ───────────────────────────────────────────

    @Test
    @DisplayName("Parse labeled blank node as subject inside named graph")
    void testParseBlankNodeSubjectInGraph() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  _:b1 ex:predicate ex:object .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockBNode1, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse labeled blank node as object inside named graph")
    void testParseBlankNodeObjectInGraph() {
        parser.parse(new StringReader("""
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                  ex:subject ex:predicate _:b1 .
                }
                """));
        verify(mockModel).setNamespace("ex", NS);
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, mockBNode1, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Unicode escapes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse triple with Unicode escape in literal inside named graph (4-hex form)")
    void testParseUnicodeEscapeInLiteralInsideGraph() {
        char bs = '\\';
        String input = "GRAPH <http://example.org/graph> {\n"
                + "  <http://example.org/subject> <http://example.org/predicate> \"Hello"
                + bs + "u0020World\" .\n}";
        Literal expected = mock(Literal.class);
        lenient().when(mockValueFactory.createLiteral(eq("Hello World"))).thenReturn(expected);

        parser.parse(new StringReader(input));
        verify(mockModel).add(mockSubjectIRI, mockPredicateIRI, expected, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("Parse triple with Unicode escape in IRI inside named graph (4-hex form)")
    void testParseUnicodeEscapeInIRIInsideGraph() {
        char bs = '\\';
        String input = "GRAPH <http://example.org/graph> {\n"
                + "  <http://example.org/sub" + bs + "u006Aect>"
                + " <http://example.org/predicate>"
                + " <http://example.org/object> .\n}";
        IRI expected = mock(IRI.class);
        lenient().when(mockValueFactory.createIRI(eq(S))).thenReturn(expected);

        parser.parse(new StringReader(input));
        verify(mockModel).add(expected, mockPredicateIRI, mockObjectIRI, mockGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Unclosed graph block throws ParsingException")
    void testUnclosedGraphBlockThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader("""
                        @prefix ex: <http://example.org/> .
                        GRAPH ex:graph {
                          ex:subject ex:predicate ex:object .
                        """)));
    }

    @Test
    @DisplayName("Extra closing brace throws ParsingException")
    void testExtraClosingBraceThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader("""
                        @prefix ex: <http://example.org/> .
                        GRAPH ex:graph { ex:subject ex:predicate ex:object . } }
                        """)));
    }

    @Test
    @DisplayName("Nested graph blocks throw ParsingException")
    void testNestedGraphBlocksThrow() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader("""
                        @prefix ex: <http://example.org/> .
                        GRAPH ex:graph {
                          GRAPH ex:inner { ex:subject ex:predicate ex:object . }
                        }
                        """)));
    }

    @Test
    @DisplayName("Undeclared prefix in graph block throws ParsingException")
    void testUndeclaredPrefixInGraphThrows() {
        assertThrows(ParsingException.class,
                () -> parser.parse(new StringReader("""
                        GRAPH <http://example.org/graph> {
                          undeclared:s undeclared:p undeclared:o .
                        }
                        """)));
    }
}
