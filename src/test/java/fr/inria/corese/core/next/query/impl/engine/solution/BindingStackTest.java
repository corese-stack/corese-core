package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BindingStackTest {

    private BindingStack stack;
    private Matcher matcher;

    @BeforeEach
    void setUp() {
        stack = new BindingStack();
        matcher = mock(Matcher.class);
    }

    private static Node varNode(String name, int index) {
        NodeImpl n = NodeImpl.forVariable(name);
        n.setIndex(index);
        return n;
    }

    private static Node litNode(String value) {
        return NodeImpl.forLiteral(value, null, null);
    }

    @Test
    void testNodePushPopAndQuery() {
        Query q = mock(Query.class);
        when(q.nbNodes()).thenReturn(4);
        when(q.nbEdges()).thenReturn(2);

        stack.init(q, false);
        assertEquals(0, stack.getBoundNodeCount());
        assertFalse(stack.isRecordEdges());

        Node qNode0 = varNode("?x", 0);
        Node target0 = litNode("foo");

        assertTrue(stack.push(qNode0, target0, 1, matcher, null));
        assertEquals(1, stack.getBoundNodeCount());
        assertTrue(stack.isBound(qNode0));
        assertSame(target0, stack.getNode(qNode0));
        assertSame(target0, stack.getNode(0));
        assertSame(target0, stack.getNode("?x"));
        assertSame(qNode0, stack.getQueryNode(0));
        assertSame(qNode0, stack.getQueryNode("?x"));
        assertEquals(1, stack.getIndex(qNode0));

        // Pushing same binding increases counter
        when(matcher.same(any(), any(), any(), any())).thenReturn(true);
        assertTrue(stack.push(qNode0, target0, 2, matcher, null));
        assertEquals(1, stack.getBoundNodeCount());

        stack.pop(qNode0);
        assertTrue(stack.isBound(qNode0));
        stack.pop(qNode0);
        assertFalse(stack.isBound(qNode0));
        assertNull(stack.getNode(qNode0));
        assertEquals(0, stack.getBoundNodeCount());
    }

    @Test
    void testConstantNodeAndConflict() {
        Query q = mock(Query.class);
        when(q.nbNodes()).thenReturn(3);
        stack.init(q, false);

        Node constant = litNode("const");
        assertTrue(stack.push(constant, constant, 0, matcher, null));

        Node qNode = varNode("?y", 1);
        Node val1 = litNode("v1");
        Node val2 = litNode("v2");

        assertTrue(stack.push(qNode, val1, 0, matcher, null));
        when(matcher.same(eq(qNode), eq(val1), eq(val2), any())).thenReturn(false);
        assertFalse(stack.push(qNode, val2, 1, matcher, null));
    }

    @Test
    void testEdgeRecordAndPop() {
        Query q = mock(Query.class);
        when(q.nbNodes()).thenReturn(2);
        when(q.nbEdges()).thenReturn(2);
        stack.init(q, true);
        assertTrue(stack.isRecordEdges());

        Edge qEdge = mock(Edge.class);
        when(qEdge.getEdgeIndex()).thenReturn(0);
        Edge ent = mock(Edge.class);
        when(ent.getEdgeIndex()).thenReturn(0);

        stack.recordEdge(qEdge, ent);
        assertEquals(1, stack.getBoundEdgeCount());
        assertSame(ent, stack.getEdge(qEdge));
        assertNotNull(stack.getEdges());
        assertNotNull(stack.getQueryEdges());

        stack.popEdge(qEdge);
        assertEquals(0, stack.getBoundEdgeCount());
        assertNull(stack.getEdge(qEdge));
    }

    @Test
    void testGetIndexAcrossCollections() {
        Query q = mock(Query.class);
        when(q.nbNodes()).thenReturn(3);
        stack.init(q, false);

        Node n0 = varNode("?a", 0);
        Node n1 = varNode("?b", 1);
        stack.push(n0, litNode("A"), 5, matcher, null);
        stack.push(n1, litNode("B"), 10, matcher, null);

        assertEquals(10, stack.getIndex(List.of(n0, n1)));

        Edge edge = mock(Edge.class);
        when(edge.nbNode()).thenReturn(2);
        when(edge.getNode(0)).thenReturn(n0);
        when(edge.getNode(1)).thenReturn(n1);
        when(edge.getEdgeVariable()).thenReturn(null);

        assertEquals(10, stack.getIndex(null, edge));
    }
}
