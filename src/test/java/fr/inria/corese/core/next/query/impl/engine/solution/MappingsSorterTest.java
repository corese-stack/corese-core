package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MappingsSorter Tests")
class MappingsSorterTest {

    @Test
    @DisplayName("sort by variable names sorts rows correctly")
    void testSortByVariableNames() {
        Mappings mappings = new Mappings();
        MappingsSorter sorter = new MappingsSorter(mappings);

        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("alpha", null, null);
        Node v2 = NodeImpl.forLiteral("beta", null, null);

        Mapping m1 = Mapping.create();
        m1.addNode(x, v2);

        Mapping m2 = Mapping.create();
        m2.addNode(x, v1);

        mappings.add(m1);
        mappings.add(m2);

        sorter.sort(List.of("?x"));

        assertEquals(v1, mappings.get(0).getNodeValue(x));
        assertEquals(v2, mappings.get(1).getNodeValue(x));
    }

    @Test
    @DisplayName("limitOffset applies offset and limit correctly")
    void testLimitOffset() {
        QueryPlan query = mock(QueryPlan.class);
        when(query.getOffset()).thenReturn(1);
        when(query.getLimit()).thenReturn(2);
        when(query.getOrderBy()).thenReturn(Collections.emptyList());

        Mappings mappings = Mappings.create(query);
        MappingsSorter sorter = new MappingsSorter(mappings);

        mappings.add(Mapping.create());
        mappings.add(Mapping.create());
        mappings.add(Mapping.create());
        mappings.add(Mapping.create());

        sorter.limitOffset();

        assertEquals(2, mappings.size());
    }

    @Test
    @DisplayName("minusCompatible checks compatibility")
    void testMinusCompatible() {
        Mappings mappings = new Mappings();
        MappingsSorter sorter = new MappingsSorter(mappings);

        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("val", null, null);

        Mapping m1 = Mapping.create();
        m1.addNode(x, v1);
        mappings.add(m1);

        Mapping target = Mapping.create();
        target.addNode(x, v1);

        assertTrue(sorter.minusCompatible(target, List.of("?x")));

        Mapping nonCompat = Mapping.create();
        nonCompat.addNode(x, NodeImpl.forLiteral("other", null, null));
        assertFalse(sorter.minusCompatible(nonCompat, List.of("?x")));
    }
}
