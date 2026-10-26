package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VariableLayout Tests")
class VariableLayoutTest {

    @Test
    @DisplayName("Should allocate sequential node and edge indexes")
    void shouldAllocateIndexes() {
        VariableLayout layout = new VariableLayout();

        assertEquals(0, layout.getNodeIndex());
        assertEquals(0, layout.getEdgeIndex());

        assertEquals(0, layout.allocateNodeIndex());
        assertEquals(1, layout.allocateNodeIndex());
        assertEquals(2, layout.getNodeIndex());

        assertEquals(0, layout.allocateEdgeIndex());
        assertEquals(1, layout.allocateEdgeIndex());
        assertEquals(2, layout.getEdgeIndex());

        layout.setNodeIndex(10);
        assertEquals(10, layout.getNodeIndex());
        layout.setEdgeIndex(20);
        assertEquals(20, layout.getEdgeIndex());
    }

    @Test
    @DisplayName("Should store nodes in appropriate pattern and query collections")
    void shouldStoreNodesCorrectly() {
        VariableLayout layout = new VariableLayout();
        Node pNode = NodeImpl.forVariable("?p");
        Node psNode = NodeImpl.forVariable("?ps");
        Node qNode = NodeImpl.forVariable("?q");
        Node qsNode = NodeImpl.forVariable("?qs");

        // Pattern regular node (exist = false, select = false)
        layout.store(pNode, false, false);
        // Pattern select node (exist = false, select = true)
        layout.store(psNode, false, true);
        // Query regular node (exist = true, select = false)
        layout.store(qNode, true, false);
        // Query select node (exist = true, select = true)
        layout.store(qsNode, true, true);

        assertEquals(List.of(pNode), layout.getPatternNodes());
        assertEquals(List.of(psNode), layout.getPatternSelectNodes());
        assertEquals(List.of(qNode), layout.getQueryNodes());
        assertEquals(List.of(qsNode), layout.getQuerySelectNodes());

        assertSame(pNode, layout.getPatternNode("?p"));
        assertSame(psNode, layout.getPatternSelectNode("?ps"));
        assertSame(qNode, layout.getQueryNode("?q"));
        assertSame(qsNode, layout.getQuerySelectNode("?qs"));

        assertNull(layout.getPatternNode("?unknown"));
    }

    @Test
    @DisplayName("Should ignore non-variable and blank nodes when storing")
    void shouldIgnoreNonVariableAndBlankNodes() {
        VariableLayout layout = new VariableLayout();

        Node blankNode = NodeImpl.forBlankVariable("_:b0");
        Node constantNode = NodeImpl.forIRI("http://example.org/item");

        layout.store(blankNode, false, false);
        layout.store(constantNode, false, false);

        assertTrue(layout.getPatternNodes().isEmpty());
    }

    @Test
    @DisplayName("Should resolve external nodes in hierarchical precedence")
    void shouldResolveExtNodeWithPrecedence() {
        VariableLayout layout = new VariableLayout();
        Node pNode = NodeImpl.forVariable("?x");
        layout.store(pNode, false, false);

        Node selectNode = NodeImpl.forVariable("?selectX");

        assertSame(pNode, layout.getExtNode("?x", false, null));
        assertSame(pNode, layout.getNode("?x", null));

        assertSame(selectNode, layout.getExtNode("?y", true, selectNode));
        assertNull(layout.getExtNode("?y", false, selectNode));
    }

    @Test
    @DisplayName("Should copy layout cleanly using PlanCopy")
    void shouldCopyFromCleanly() {
        VariableLayout source = new VariableLayout();
        Node n1 = NodeImpl.forVariable("?n1");
        Node n2 = NodeImpl.forVariable("?n2");
        source.store(n1, false, false);
        source.store(n2, true, true);
        source.allocateNodeIndex();
        source.allocateEdgeIndex();

        PlanCopy copyContext = new PlanCopy();
        VariableLayout target = source.copyPlan(copyContext);

        assertEquals(source.getNodeIndex(), target.getNodeIndex());
        assertEquals(source.getEdgeIndex(), target.getEdgeIndex());
        assertEquals(1, target.getPatternNodes().size());
        assertEquals(1, target.getQuerySelectNodes().size());
        assertEquals("?n1", target.getPatternNodes().get(0).getLabel());
        assertEquals("?n2", target.getQuerySelectNodes().get(0).getLabel());
    }

    @Test
    @DisplayName("Should manage binding, construct and arg node collections")
    void shouldManageAuxiliaryNodeLists() {
        VariableLayout layout = new VariableLayout();
        Node bNode = NodeImpl.forVariable("?b");
        Node cNode = NodeImpl.forVariable("?c");

        layout.setBindingNodes(List.of(bNode));
        layout.setConstructNodes(List.of(cNode));

        assertEquals(List.of(bNode), layout.getBindingNodes());
        assertEquals(List.of(cNode), layout.getConstructNodes());
        assertNotNull(layout.getArgList());
    }
}
