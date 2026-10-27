package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitorDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
