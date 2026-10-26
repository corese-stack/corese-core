package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("QueryExpIndexer Tests")
class QueryExpIndexerTest {

    @Test
    @DisplayName("collect variables from patterns into layout")
    void testCollect() {
        QueryPlan query = QueryPlan.create();
        VariableLayout layout = query.getLayout();
        QueryExpIndexer indexer = new QueryExpIndexer(query, layout);

        Node s = NodeImpl.forVariable("?s");
        Node p = NodeImpl.forVariable("?p");
        Node o = NodeImpl.forVariable("?o");

        Edge edge = mock(Edge.class);
        when(edge.nbNode()).thenReturn(2);
        when(edge.getNode(0)).thenReturn(s);
        when(edge.getEdgeVariable()).thenReturn(p);
        when(edge.getNode(1)).thenReturn(o);

        query.add(Exp.create(Exp.Type.EDGE, edge));

        indexer.collect();

        assertNotNull(layout.getPatternNode("?s"));
        assertNotNull(layout.getPatternNode("?p"));
        assertNotNull(layout.getPatternNode("?o"));
    }

    @Test
    @DisplayName("index allocates sequential indexes to edges and nodes")
    void testIndex() {
        QueryPlan query = QueryPlan.create();
        VariableLayout layout = query.getLayout();
        QueryExpIndexer indexer = new QueryExpIndexer(query, layout);

        Node s = NodeImpl.forVariable("?s");
        Node o = NodeImpl.forVariable("?o");

        Edge edge = mock(Edge.class);
        when(edge.nbNode()).thenReturn(2);
        when(edge.getNode(0)).thenReturn(s);
        when(edge.getNode(1)).thenReturn(o);

        Exp edgeExp = Exp.create(Exp.Type.EDGE, edge);

        int min = indexer.index(query, edgeExp, false, -1);

        assertTrue(min >= 0);
        assertEquals(min, s.getIndex());
    }
}
