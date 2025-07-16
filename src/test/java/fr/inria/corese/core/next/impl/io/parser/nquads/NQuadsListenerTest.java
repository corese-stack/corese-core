package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsParser;
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
 * Unit tests for the NQuadsListener class.
 * These tests verify that the listener correctly processes ANTLR parse tree contexts
 * to extract and unescape RDF terms (IRIs, Blank Nodes, Literals) and add them to the model,
 * including named graphs.
 */
@ExtendWith(MockitoExtension.class)
class NQuadsListenerTest {

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private IOOptions mockIOOptions;

    private NQuadsListener listener;


    @Mock
    private IRI mockIRI;
    @Mock
    private BNode mockBNode;
    @Mock
    private Literal mockLiteral;
    @Mock
    private IRI mockDatatypeIRI;
    @Mock
    private IRI mockGraphIRI;
    @Mock
    private BNode mockGraphBNode;

    @BeforeEach
    void setUp() {
        listener = new NQuadsListener(mockModel, mockValueFactory, mockIOOptions);

        lenient().when(mockValueFactory.createIRI(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.equals("http://example.org/test")) return mockIRI;
            if (uri.equals("http://example.org/datatype")) return mockDatatypeIRI;
            if (uri.equals("http://example.org/graph")) return mockGraphIRI;
            if (uri.equals("http://example.org/escaped>uri")) return mock(IRI.class);
            if (uri.equals("http://example.org/s ubject")) return mock(IRI.class);
            if (uri.equals("http://example.org/path/€")) return mock(IRI.class);
            if (uri.equals("http://example.org/path>with\\<escaped\\_chars\\")) return mock(IRI.class);
            if (uri.equals("http://example.org/graphName")) return mock(IRI.class);
            return mock(IRI.class);
        });

        lenient().when(mockValueFactory.createBNode(anyString())).thenAnswer(invocation -> {
            String label = invocation.getArgument(0);
            if (label.equals("b1")) return mockBNode;
            if (label.equals("graph1")) return mockGraphBNode;
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
    @DisplayName("enterStatement and exitStatement should add a quad to the model with IRI graph")
    void testEnterExitStatementAddsToModelWithIRIGraph() {
        NQuadsParser.StatementContext statementCtx = mockRuleContext(NQuadsParser.StatementContext.class);
        NQuadsParser.SubjectContext subjectCtx = mockRuleContext(NQuadsParser.SubjectContext.class);
        NQuadsParser.PredicateContext predicateCtx = mockRuleContext(NQuadsParser.PredicateContext.class);
        NQuadsParser.ObjectContext objectCtx = mockRuleContext(NQuadsParser.ObjectContext.class);
        NQuadsParser.GraphLabelContext graphLabelCtx = mockRuleContext(NQuadsParser.GraphLabelContext.class);


        TerminalNode subjectIriRef = mockTerminalNode("<http://example.org/subject>");
        when(subjectCtx.IRIREF()).thenReturn(subjectIriRef);

        TerminalNode predicateIriRef = mockTerminalNode("<http://example.org/predicate>");
        when(predicateCtx.IRIREF()).thenReturn(predicateIriRef);

        TerminalNode objectIriRef = mockTerminalNode("<http://example.org/object>");
        when(objectCtx.IRIREF()).thenReturn(objectIriRef);

        TerminalNode graphIriRef = mockTerminalNode("<http://example.org/graph>");
        when(graphLabelCtx.IRIREF()).thenReturn(graphIriRef);


        when(statementCtx.subject()).thenReturn(subjectCtx);
        when(statementCtx.predicate()).thenReturn(predicateCtx);
        when(statementCtx.object()).thenReturn(objectCtx);
        when(statementCtx.graphLabel()).thenReturn(graphLabelCtx);

        IRI actualSubjectIRI = mock(IRI.class);
        IRI actualPredicateIRI = mock(IRI.class);
        IRI actualObjectIRI = mock(IRI.class);
        IRI actualGraphIRI = mock(IRI.class);
        when(mockValueFactory.createIRI("http://example.org/subject")).thenReturn(actualSubjectIRI);
        when(mockValueFactory.createIRI("http://example.org/predicate")).thenReturn(actualPredicateIRI);
        when(mockValueFactory.createIRI("http://example.org/object")).thenReturn(actualObjectIRI);
        when(mockValueFactory.createIRI("http://example.org/graph")).thenReturn(actualGraphIRI);


        listener.enterStatement(statementCtx);
        listener.exitStatement(statementCtx);

        verify(mockModel).add(actualSubjectIRI, actualPredicateIRI, actualObjectIRI, actualGraphIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("enterStatement and exitStatement should add a triple to the model (default graph)")
    void testEnterExitStatementAddsToModelDefaultGraph() {
        NQuadsParser.StatementContext statementCtx = mockRuleContext(NQuadsParser.StatementContext.class);
        NQuadsParser.SubjectContext subjectCtx = mockRuleContext(NQuadsParser.SubjectContext.class);
        NQuadsParser.PredicateContext predicateCtx = mockRuleContext(NQuadsParser.PredicateContext.class);
        NQuadsParser.ObjectContext objectCtx = mockRuleContext(NQuadsParser.ObjectContext.class);

        TerminalNode subjectIriRef = mockTerminalNode("<http://example.org/subject>");
        when(subjectCtx.IRIREF()).thenReturn(subjectIriRef);

        TerminalNode predicateIriRef = mockTerminalNode("<http://example.org/predicate>");
        when(predicateCtx.IRIREF()).thenReturn(predicateIriRef);

        TerminalNode objectIriRef = mockTerminalNode("<http://example.org/object>");
        when(objectCtx.IRIREF()).thenReturn(objectIriRef);

        when(statementCtx.subject()).thenReturn(subjectCtx);
        when(statementCtx.predicate()).thenReturn(predicateCtx);
        when(statementCtx.object()).thenReturn(objectCtx);
        when(statementCtx.graphLabel()).thenReturn(null);

        IRI actualSubjectIRI = mock(IRI.class);
        IRI actualPredicateIRI = mock(IRI.class);
        IRI actualObjectIRI = mock(IRI.class);
        when(mockValueFactory.createIRI("http://example.org/subject")).thenReturn(actualSubjectIRI);
        when(mockValueFactory.createIRI("http://example.org/predicate")).thenReturn(actualPredicateIRI);
        when(mockValueFactory.createIRI("http://example.org/object")).thenReturn(actualObjectIRI);


        listener.enterStatement(statementCtx);
        listener.exitStatement(statementCtx);

        verify(mockModel).add(actualSubjectIRI, actualPredicateIRI, actualObjectIRI);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    @DisplayName("extractSubject should return IRI for IRIREF context")
    void testExtractSubjectIRI() {
        NQuadsParser.SubjectContext ctx = mockRuleContext(NQuadsParser.SubjectContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        Resource result = listener.extractSubject(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractSubject should return BNode for BLANK_NODE_LABEL context")
    void testExtractSubjectBNode() {
        NQuadsParser.SubjectContext ctx = mockRuleContext(NQuadsParser.SubjectContext.class);
        TerminalNode bNodeLabel = mockTerminalNode("_:b1");
        when(ctx.BLANK_NODE_LABEL()).thenReturn(bNodeLabel);

        Resource result = listener.extractSubject(ctx);
        assertEquals(mockBNode, result);
        verify(mockValueFactory).createBNode("b1");
    }

    @Test
    @DisplayName("extractPredicate should return IRI for IRIREF context")
    void testExtractPredicateIRI() {
        NQuadsParser.PredicateContext ctx = mockRuleContext(NQuadsParser.PredicateContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        IRI result = listener.extractPredicate(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractObject should return IRI for IRIREF context")
    void testExtractObjectIRI() {
        NQuadsParser.ObjectContext ctx = mockRuleContext(NQuadsParser.ObjectContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/test>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        Value result = listener.extractObject(ctx);
        assertEquals(mockIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/test");
    }

    @Test
    @DisplayName("extractObject should return BNode for BLANK_NODE_LABEL context")
    void testExtractObjectBNode() {
        NQuadsParser.ObjectContext ctx = mockRuleContext(NQuadsParser.ObjectContext.class);
        TerminalNode bNodeLabel = mockTerminalNode("_:b1");
        when(ctx.BLANK_NODE_LABEL()).thenReturn(bNodeLabel);

        Value result = listener.extractObject(ctx);
        assertEquals(mockBNode, result);
        verify(mockValueFactory).createBNode("b1");
    }

    @Test
    @DisplayName("extractObject should return Literal for literal context (simple string)")
    void testExtractObjectLiteralSimple() {
        NQuadsParser.ObjectContext objCtx = mockRuleContext(NQuadsParser.ObjectContext.class);
        NQuadsParser.LiteralContext litCtx = mockRuleContext(NQuadsParser.LiteralContext.class);
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
        NQuadsParser.ObjectContext objCtx = mockRuleContext(NQuadsParser.ObjectContext.class);
        NQuadsParser.LiteralContext litCtx = mockRuleContext(NQuadsParser.LiteralContext.class);
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
    @DisplayName("extractGraph should return IRI for IRIREF context")
    void testExtractGraphIRI() {
        NQuadsParser.GraphLabelContext ctx = mockRuleContext(NQuadsParser.GraphLabelContext.class);
        TerminalNode iriRef = mockTerminalNode("<http://example.org/graph>");
        when(ctx.IRIREF()).thenReturn(iriRef);

        Resource result = listener.extractGraph(ctx);
        assertEquals(mockGraphIRI, result);
        verify(mockValueFactory).createIRI("http://example.org/graph");
    }

    @Test
    @DisplayName("extractGraph should return BNode for BLANK_NODE_LABEL context")
    void testExtractGraphBNode() {
        NQuadsParser.GraphLabelContext ctx = mockRuleContext(NQuadsParser.GraphLabelContext.class);
        TerminalNode bNodeLabel = mockTerminalNode("_:graph1");
        when(ctx.BLANK_NODE_LABEL()).thenReturn(bNodeLabel);

        Resource result = listener.extractGraph(ctx);
        assertEquals(mockGraphBNode, result);
        verify(mockValueFactory).createBNode("graph1");
    }




    @Test
    @DisplayName("unescapeLiteral should throw IllegalArgumentException for invalid \\UXXXXXXXX")
    void testUnescapeLiteralInvalidUx() throws NoSuchMethodException {
        String input = "\"Invalid\\U0000XXX\"";
        java.lang.reflect.Method method = NQuadsListener.class.getDeclaredMethod("unescapeLiteral", String.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class,
                () -> listener.unescapeLiteral(input),
                "Should throw for malformed \\UXXXXXXXX escape sequence");
    }


    @Test
    @DisplayName("unescapeUri should handle basic escape sequences")
    void testUnescapeUriBasicEscapes() throws NoSuchMethodException, java.lang.reflect.InvocationTargetException, IllegalAccessException {

        String input = "http://example.org/path\\>with\\<escaped\\_chars\\\\";
        String expected = "http://example.org/path>with\\<escaped\\_chars\\";

        java.lang.reflect.Method method = NQuadsListener.class.getDeclaredMethod("unescapeUri", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(listener, input);

        assertEquals(expected, result);
    }


    @Test
    @DisplayName("unescapeUri should handle \\uXXXX Unicode escapes")
    void testUnescapeUriUnicodeU() throws NoSuchMethodException, java.lang.reflect.InvocationTargetException, IllegalAccessException {
        String input = "http://example.org/path\\u0020with";
        String expected = "http://example.org/path with";

        java.lang.reflect.Method method = NQuadsListener.class.getDeclaredMethod("unescapeUri", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(listener, input);

        assertEquals(expected, result);
    }


    @Test
    @DisplayName("unescapeUri should throw IllegalArgumentException for invalid \\uXXXX")
    void testUnescapeUriInvalidU() throws NoSuchMethodException {
        String input = "http://example.org/invalid\\uXXX";
        java.lang.reflect.Method method = NQuadsListener.class.getDeclaredMethod("unescapeUri", String.class);
        method.setAccessible(true);

        assertThrows(IllegalArgumentException.class,
                () -> listener.unescapeLiteral(input),
                "Should throw unescapeUri should throw IllegalArgumentException for invalid \\uXXXX");

    }


}
