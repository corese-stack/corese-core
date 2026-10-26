package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitorDefault;
import fr.inria.corese.core.next.query.impl.engine.event.ResultListener;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EvalVisitorDispatcher Tests")
class EvalVisitorDispatcherTest {

    private EvalVisitorDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new EvalVisitorDispatcher();
    }

    @Test
    @DisplayName("Should initialize with default visitor and false flags")
    void testDefaults() {
        assertNotNull(dispatcher.getVisitor());
        assertTrue(dispatcher.getVisitor() instanceof ProcessVisitorDefault);
        assertFalse(dispatcher.hasStatement());
        assertFalse(dispatcher.hasCandidate());
        assertFalse(dispatcher.hasFilter());
        assertFalse(dispatcher.hasListener());
        assertNull(dispatcher.getListener());
    }

    @Test
    @DisplayName("Should update flags when custom visitor is configured")
    void testCustomVisitorFlags() {
        ProcessVisitor mockVisitor = mock(ProcessVisitor.class);
        when(mockVisitor.statement()).thenReturn(true);
        when(mockVisitor.candidate()).thenReturn(false);
        when(mockVisitor.filter()).thenReturn(true);

        dispatcher.setVisitor(mockVisitor);

        assertSame(mockVisitor, dispatcher.getVisitor());
        assertTrue(dispatcher.hasStatement());
        assertFalse(dispatcher.hasCandidate());
        assertTrue(dispatcher.hasFilter());
    }

    @Test
    @DisplayName("Should fall back to default visitor when null is passed")
    void testNullVisitorFallback() {
        dispatcher.setVisitor(null);
        assertNotNull(dispatcher.getVisitor());
        assertTrue(dispatcher.getVisitor() instanceof ProcessVisitorDefault);
        assertFalse(dispatcher.hasStatement());
        assertFalse(dispatcher.hasCandidate());
        assertFalse(dispatcher.hasFilter());
    }

    @Test
    @DisplayName("Should dispatch listener calls when listener is present")
    void testListenerDispatching() {
        ResultListener mockListener = mock(ResultListener.class);
        dispatcher.setListener(mockListener);

        assertTrue(dispatcher.hasListener());
        assertSame(mockListener, dispatcher.getListener());

        Exp exp = mock(Exp.class);
        Exp transformedExp = mock(Exp.class);
        when(mockListener.listen(exp, 1)).thenReturn(transformedExp);
        assertSame(transformedExp, dispatcher.listen(exp, 1));

        Expr expr = mock(Expr.class);
        dispatcher.listen(expr);
        verify(mockListener).listen(expr);

        Edge qEdge = mock(Edge.class);
        Edge entity = mock(Edge.class);
        when(mockListener.listen(exp, qEdge, entity)).thenReturn(false);
        assertFalse(dispatcher.listen(exp, qEdge, entity));
    }

    @Test
    @DisplayName("Should safely bypass listener calls when listener is null")
    void testListenerBypassWhenNull() {
        Exp exp = mock(Exp.class);
        assertSame(exp, dispatcher.listen(exp, 1));

        Expr expr = mock(Expr.class);
        assertDoesNotThrow(() -> dispatcher.listen(expr));

        Edge qEdge = mock(Edge.class);
        Edge entity = mock(Edge.class);
        assertTrue(dispatcher.listen(exp, qEdge, entity));
    }
}
