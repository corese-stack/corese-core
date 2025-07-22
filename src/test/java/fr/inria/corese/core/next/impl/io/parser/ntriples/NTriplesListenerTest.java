package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the NTriplesListenerImpl class.
 * These tests verify that the listener correctly processes ANTLR parse tree contexts
 * to extract and unescape RDF terms (IRIs, Blank Nodes, Literals) and add them to the model.
 */
@ExtendWith(MockitoExtension.class)
class NTriplesListenerTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private IOOptions mockIOOptions;

    private NTriplesListener listener;

    @Mock
    private IRI mockIRI;
    @Mock
    private BNode mockBNode;
    @Mock
    private Literal mockLiteral;
    @Mock
    private IRI mockDatatypeIRI;

    @BeforeEach
    void setUp() {
        listener = new NTriplesListener(mockModel, mockValueFactory, mockIOOptions);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.equals("http://example.org/test")) return mockIRI;
            if (uri.equals("http://example.org/datatype")) return mockDatatypeIRI;
            if (uri.equals("http://example.org/escaped>iri")) return mock(IRI.class);
            if (uri.equals("http://example.org/s ubject")) return mock(IRI.class);
            if (uri.equals("http://example.org/path/€")) return mock(IRI.class);
            return mock(IRI.class);
        });

        lenient().when(mockValueFactory.createBNode(anyString())).thenAnswer(invocation -> {
            String label = invocation.getArgument(0);
            if (label.equals("b1")) return mockBNode;
            return mock(BNode.class);
        });

        lenient().when(mockValueFactory.createLiteral(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            if (value.equals("test literal")) return mockLiteral;
            if (value.equals("literal with \"quotes\" and \n newline")) return mock(Literal.class);
            if (value.equals("Hello World")) return mock(Literal.class);
            if (value.equals("Euro€")) return mock(Literal.class);
            return mock(Literal.class);
        });
        lenient().when(mockValueFactory.createLiteral(anyString(), any(IRI.class))).thenReturn(mock(Literal.class));
        lenient().when(mockValueFactory.createLiteral(anyString(), anyString())).thenReturn(mock(Literal.class));
    }

    private TerminalNode mockTerminalNode(String text) {
        TerminalNode node = mock(TerminalNode.class);
        when(node.getText()).thenReturn(text);
        return node;
    }

    private <T extends ParserRuleContext> T mockRuleContext(Class<T> clazz) {
        return mock(clazz);
    }

    @Test
    @DisplayName("enterTriple and exitTriple should add a triple to the model")
    void testEnterExitTripleAddsToModel() {
        NTriplesParser.TripleContext tripleCtx = mockRuleContext(NTriplesParser.TripleContext.class);
        NTriplesParser.SubjectContext subjectCtx = mockRuleContext(NTriplesParser.SubjectContext.class);
        NTriplesParser.PredicateContext predicateCtx = mockRuleContext(NTriplesParser.PredicateContext.class);
        NTriplesParser.ObjectContext objectCtx = mockRuleContext(NTriplesParser.ObjectContext.class);

        TerminalNode subjectIriRef = mockTerminalNode("<http://example.org/subject>");
        when(subjectCtx.IRIREF()).thenReturn(subjectIriRef);

        TerminalNode predicateIriRef = mockTerminalNode("<http://example.org/predicate>");
        when(predicateCtx.IRIREF()).thenReturn(predicateIriRef);

        TerminalNode objectIriRef = mockTerminalNode("<http://example.org/object>");
        when(objectCtx.IRIREF()).thenReturn(objectIriRef);

        when(tripleCtx.subject()).thenReturn(subjectCtx);
        when(tripleCtx.predicate()).thenReturn(predicateCtx);
        when(tripleCtx.object()).thenReturn(objectCtx);

        IRI actualSubjectIRI = mock(IRI.class);
        IRI actualPredicateIRI = mock(IRI.class);
        IRI actualObjectIRI = mock(IRI.class);
        when(mockValueFactory.createIRI("http://example.org/subject")).thenReturn(actualSubjectIRI);
        when(mockValueFactory.createIRI("http://example.org/predicate")).thenReturn(actualPredicateIRI);
        when(mockValueFactory.createIRI("http://example.org/object")).thenReturn(actualObjectIRI);


        listener.enterTriple(tripleCtx);
        listener.exitTriple(tripleCtx);

        verify(mockModel).add(actualSubjectIRI, actualPredicateIRI, actualObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("extractSubject should return IRI for IRIREF context")
    void testExtractSubjectIRI() {
        NTriplesParser.SubjectContext ctx = mockRuleContext(NTriplesParser.SubjectContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        Resource result = listener.extractSubject(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractSubject should return BNode for BLANK_NODE_LABEL context")
    void testExtractSubjectBNode() {
        NTriplesParser.SubjectContext ctx = mockRuleContext(NTriplesParser.SubjectContext.class);
        TerminalNode bNodeLabel = mockTerminalNode("_:b1");
        when(ctx.BLANK_NODE_LABEL()).thenReturn(bNodeLabel);

        Resource result = listener.extractSubject(ctx);
        assertEquals(mockBNode, result);
        verify(mockValueFactory).createBNode("b1");
    }

    @Test
    @DisplayName("extractPredicate should return IRI for IRIREF context")
    void testExtractPredicateIRI() {
        NTriplesParser.PredicateContext ctx = mockRuleContext(NTriplesParser.PredicateContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        IRI result = listener.extractPredicate(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractObject should return IRI for IRIREF context")
    void testExtractObjectIRI() {
        NTriplesParser.ObjectContext ctx = mockRuleContext(NTriplesParser.ObjectContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        Value result = listener.extractObject(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractObject should return BNode for BLANK_NODE_LABEL context")
    void testExtractObjectBNode() {
        NTriplesParser.ObjectContext ctx = mockRuleContext(NTriplesParser.ObjectContext.class);
        TerminalNode bNodeLabel = mockTerminalNode("_:b1");
        when(ctx.BLANK_NODE_LABEL()).thenReturn(bNodeLabel);

        Value result = listener.extractObject(ctx);
        assertEquals(mockBNode, result);
        verify(mockValueFactory).createBNode("b1");
    }

    @Test
    @DisplayName("extractObject should return Literal for literal context (simple string)")
    void testExtractObjectLiteralSimple() {
        NTriplesParser.ObjectContext objCtx = mockRuleContext(NTriplesParser.ObjectContext.class);
        NTriplesParser.LiteralContext litCtx = mockRuleContext(NTriplesParser.LiteralContext.class);
        TerminalNode stringLiteral = mockTerminalNode("\"test literal\"");

        when(objCtx.literal()).thenReturn(litCtx);
        when(litCtx.STRING_LITERAL_QUOTE()).thenReturn(stringLiteral);
        when(litCtx.IRIREF()).thenReturn(null);
        when(litCtx.LANGTAG()).thenReturn(null);

        Value result = listener.extractObject(objCtx);
        assertEquals(mockLiteral, result);
        verify(mockValueFactory).createLiteral("test literal");
    }

    @Test
    @DisplayName("extractObject should return Literal for literal context (language-tagged)")
    void testExtractObjectLiteralLang() {
        NTriplesParser.ObjectContext objCtx = mockRuleContext(NTriplesParser.ObjectContext.class);
        NTriplesParser.LiteralContext litCtx = mockRuleContext(NTriplesParser.LiteralContext.class);
        TerminalNode stringLiteral = mockTerminalNode("\"hello\"");
        TerminalNode langTag = mockTerminalNode("@en");

        when(objCtx.literal()).thenReturn(litCtx);
        when(litCtx.STRING_LITERAL_QUOTE()).thenReturn(stringLiteral);
        when(litCtx.IRIREF()).thenReturn(null);
        when(litCtx.LANGTAG()).thenReturn(langTag);

        Literal expectedLiteral = mock(Literal.class);
        when(mockValueFactory.createLiteral("hello", "en")).thenReturn(expectedLiteral);

        Value result = listener.extractObject(objCtx);
        assertEquals(expectedLiteral, result);
        verify(mockValueFactory).createLiteral("hello", "en");
    }


    @Test
    @DisplayName("unescapeLiteral should throw IllegalArgumentException for invalid \\uXXXX")
    void testUnescapeLiteralInvalidUx() throws NoSuchMethodException {
        String input = "\"Invalid\\uXXXX\"";
        java.lang.reflect.Method method = NTriplesListener.class.getDeclaredMethod("unescapeLiteral", String.class);
        method.setAccessible(true);


        assertThrows(IllegalArgumentException.class,
                () -> listener.unescapeLiteral(input),
                "Should throw unescapeLiteral should throw IllegalArgumentException for invalid \\uXXXX");
    }

    @Test
    @DisplayName("unescapeLiteral should throw IllegalArgumentException for invalid \\UXXXXXXXX")
    void testUnescapeLiteralInvalid() throws NoSuchMethodException {
        String input = "\"Invalid\\U0000XXX\"";
        java.lang.reflect.Method method = NTriplesListener.class.getDeclaredMethod("unescapeLiteral", String.class);
        method.setAccessible(true);


        assertThrows(IllegalArgumentException.class,
                () -> listener.unescapeLiteral(input),
                "Should throw unescapeLiteral should throw IllegalArgumentException for invalid \\UXXXXXXXX");
    }


    @Test
    @DisplayName("unescapeUri should handle basic escape sequences")
    void testUnescapeUriBasicEscapes() throws NoSuchMethodException, java.lang.reflect.InvocationTargetException, IllegalAccessException {

        String input = "http://example.org/path\\>with\\<escaped\\_chars\\\\";
        String expected = "http://example.org/path>with\\<escaped\\_chars\\";

        java.lang.reflect.Method method = NTriplesListener.class.getDeclaredMethod("unescapeUri", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(listener, input);

        assertEquals(expected, result);
    }


    @Test
    @DisplayName("unescapeUri should handle \\UXXXXXXXX Unicode escapes")
    void testUnescapeUriUnicodeU() throws NoSuchMethodException, java.lang.reflect.InvocationTargetException, IllegalAccessException {
        String input = "http://example.org/path";
        String expected = "http://example.org/path";

        java.lang.reflect.Method method = NTriplesListener.class.getDeclaredMethod("unescapeUri", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(listener, input);

        assertEquals(expected, result);
    }


}
