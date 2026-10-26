package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MappingsDistinctFilter Tests")
class MappingsDistinctFilterTest {

    @Test
    @DisplayName("distinct removes duplicate solutions")
    void testDistinctRemovesDuplicates() {
        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("a", null, null);

        QueryPlan query = mock(QueryPlan.class);
        when(query.getSelect()).thenReturn(List.of(x));
        when(query.selectNodesFromPattern()).thenReturn(List.of(x));

        Mappings mappings = Mappings.create(query);
        MappingsDistinctFilter filter = new MappingsDistinctFilter(mappings);

        Mapping m1 = Mapping.create();
        m1.addNode(x, v1);
        Mapping m2 = Mapping.create();
        m2.addNode(x, v1);

        mappings.add(m1);
        mappings.add(m2);

        Mappings distinct = filter.distinct(List.of(x));
        assertEquals(1, distinct.size());
    }

    @Test
    @DisplayName("submit filters non-acceptable mappings")
    void testSubmitFiltersNonAcceptable() {
        QueryPlan query = mock(QueryPlan.class);
        when(query.isAggregate()).thenReturn(false);

        Mappings mappings = Mappings.create(query);
        MappingsDistinctFilter filter = new MappingsDistinctFilter(mappings);

        filter.submit(null);
        assertEquals(0, mappings.size());

        Mapping m = Mapping.create();
        filter.submit(m);
        assertEquals(1, mappings.size());
    }

    @Test
    @DisplayName("accept returns true when no select projection")
    void testAcceptWithoutSelect() {
        Mappings mappings = new Mappings();
        MappingsDistinctFilter filter = new MappingsDistinctFilter(mappings);

        assertTrue(filter.accept(Mapping.create()));
    }
}
