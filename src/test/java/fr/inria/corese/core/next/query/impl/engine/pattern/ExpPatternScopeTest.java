package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ExpPatternScope Unit Tests")
class ExpPatternScopeTest {

    @Test
    @DisplayName("Should add variable node only when valid and not blank or duplicate")
    void testAddVariable() {
        List<Node> list = new ArrayList<>();
        Node var1 = NodeImpl.forVariable("?x");
        Node varDuplicate = NodeImpl.forVariable("?x");
        Node blank = NodeImpl.forBlank("_:b1");

        ExpPatternScope.add(list, var1);
        ExpPatternScope.add(list, varDuplicate);
        ExpPatternScope.add(list, blank);
        ExpPatternScope.add(list, null);

        assertEquals(1, list.size());
        assertEquals("?x", list.get(0).getLabel());
    }

    @Test
    @DisplayName("Should add variable label string to list")
    void testAddVariableString() {
        List<String> list = new ArrayList<>();
        Node var1 = NodeImpl.forVariable("?y");
        Node literal = NodeImpl.forLiteral("val", null, null);

        ExpPatternScope.addVariable(list, var1);
        ExpPatternScope.addVariable(list, var1); // duplicate
        ExpPatternScope.addVariable(list, literal); // not variable
        ExpPatternScope.addVariable(list, null);

        assertEquals(1, list.size());
        assertEquals("?y", list.get(0));
    }

    @Test
    @DisplayName("Should correctly evaluate bound variables subset check")
    void testBound() {
        List<String> fvec = List.of("?x", "?y");
        List<String> evecMatch = List.of("?x", "?y", "?z");
        List<String> evecMismatch = List.of("?x", "?z");

        assertTrue(ExpPatternScope.bound(fvec, evecMatch));
        assertFalse(ExpPatternScope.bound(fvec, evecMismatch));
    }

    @Test
    @DisplayName("Should share variables between filter and node")
    void testShareNode() {
        List<String> filterVar = List.of("?x", "?y");
        List<String> expVar = new ArrayList<>();
        Node varX = NodeImpl.forVariable("?x");

        ExpPatternScope.share(varX, filterVar, expVar);
        assertEquals(1, expVar.size());
        assertEquals("?x", expVar.get(0));

        // Re-sharing shouldn't duplicate
        ExpPatternScope.share(varX, filterVar, expVar);
        assertEquals(1, expVar.size());
    }

    @Test
    @DisplayName("Should share variables across BGP with NODE and EDGE")
    void testShareBgp() {
        Exp bgp = Exp.create(ExpType.Type.BGP);
        Node varX = NodeImpl.forVariable("?x");
        Node varY = NodeImpl.forVariable("?y");

        Edge edge = mock(Edge.class);
        when(edge.nbNode()).thenReturn(2);
        when(edge.getNode(0)).thenReturn(varX);
        when(edge.getNode(1)).thenReturn(varY);

        Exp edgeExp = Exp.create(ExpType.Type.EDGE, edge);
        bgp.add(edgeExp);

        List<String> filterVar = List.of("?x", "?z");
        List<String> expVar = new ArrayList<>();

        ExpPatternScope.share(bgp, filterVar, expVar);

        assertEquals(1, expVar.size());
        assertTrue(expVar.contains("?x"));
    }

    @Test
    @DisplayName("Should collect variables from EDGE, BIND and VALUES")
    void testGetNodeVariables() {
        Exp bgp = Exp.create(ExpType.Type.BGP);

        Node varX = NodeImpl.forVariable("?x");
        Node varY = NodeImpl.forVariable("?y");
        Node varZ = NodeImpl.forVariable("?z");

        Edge edge = mock(Edge.class);
        when(edge.getNode(0)).thenReturn(varX);
        when(edge.getNode(1)).thenReturn(varY);
        when(edge.getProperty()).thenReturn(NodeImpl.forIRI("http://example.org/p"));
        Exp edgeExp = Exp.create(ExpType.Type.EDGE, edge);

        Exp bindExp = Exp.create(ExpType.Type.BIND, varZ);

        Exp valuesExp = Exp.create(ExpType.Type.VALUES);
        valuesExp.setNodeList(List.of(NodeImpl.forVariable("?v")));

        bgp.add(edgeExp);
        bgp.add(bindExp);
        bgp.add(valuesExp);

        List<String> vars = ExpPatternScope.getNodeVariables(bgp);

        assertTrue(vars.contains("?x"));
        assertTrue(vars.contains("?y"));
        assertTrue(vars.contains("?z"));
        assertTrue(vars.contains("?v"));
    }

    @Test
    @DisplayName("Should detect unbound variables for a Filter")
    void testHasUnboundVariables() {
        Exp bgp = Exp.create(ExpType.Type.BGP);
        Node varX = NodeImpl.forVariable("?x");
        bgp.add(Exp.create(ExpType.Type.BIND, varX));

        Filter filter1 = mock(Filter.class);
        when(filter1.getVariables()).thenReturn(List.of("?x"));

        Filter filter2 = mock(Filter.class);
        when(filter2.getVariables()).thenReturn(List.of("?x", "?unbound"));

        assertFalse(ExpPatternScope.hasUnboundVariables(bgp, filter1));
        assertTrue(ExpPatternScope.hasUnboundVariables(bgp, filter2));
    }

    @Test
    @DisplayName("Should compute in-scope nodes for simple BGP")
    void testInScopeNodes() {
        Exp bgp = Exp.create(ExpType.Type.BGP);
        Node varA = NodeImpl.forVariable("?a");
        bgp.add(Exp.create(ExpType.Type.NODE, varA));

        List<Node> inScope = ExpPatternScope.getInScopeNodes(bgp);
        assertEquals(1, inScope.size());
        assertEquals("?a", inScope.get(0).getLabel());

        List<Node> recordInScope = ExpPatternScope.getRecordInScopeNodes(bgp, true);
        assertEquals(1, recordInScope.size());
        assertEquals("?a", recordInScope.get(0).getLabel());
    }
}
