package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MappingsAggregator Tests")
class MappingsAggregatorTest {

    @Test
    @DisplayName("min and max compute correct boundary values")
    void testMinMax() {
        Mappings mappings = new Mappings();
        MappingsAggregator aggregator = new MappingsAggregator(mappings);

        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("10", null, null);
        Node v2 = NodeImpl.forLiteral("20", null, null);

        Mapping m1 = Mapping.create();
        m1.addNode(x, v1);
        Mapping m2 = Mapping.create();
        m2.addNode(x, v2);

        mappings.add(m1);
        mappings.add(m2);

        assertEquals(v1, aggregator.min(x));
        assertEquals(v2, aggregator.max(x));
    }

    @Test
    @DisplayName("aggregate collects distinct node values for a variable")
    void testAggregateVarNode() {
        Mappings mappings = new Mappings();
        MappingsAggregator aggregator = new MappingsAggregator(mappings);

        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("a", null, null);

        Mapping m1 = Mapping.create();
        m1.addNode(x, v1);
        Mapping m2 = Mapping.create();
        m2.addNode(x, v1);

        mappings.add(m1);
        mappings.add(m2);

        List<Node> collected = aggregator.aggregate(x);
        assertEquals(1, collected.size());
        assertEquals(v1, collected.get(0));
    }

    @Test
    @DisplayName("clean reduces mappings with size > 1 to first row")
    void testClean() {
        Mappings mappings = new Mappings();
        mappings.add(Mapping.create());
        mappings.add(Mapping.create());

        MappingsAggregator aggregator = new MappingsAggregator(mappings);
        aggregator.clean();

        assertEquals(1, mappings.size());
    }

    @Test
    @DisplayName("finish without group by or having cleans aggregate")
    void testFinish() {
        QueryPlan query = mock(QueryPlan.class);
        when(query.hasGroupBy()).thenReturn(false);
        when(query.getHaving()).thenReturn(null);
        when(query.isAggregate()).thenReturn(true);
        when(query.isConstruct()).thenReturn(false);

        Mappings mappings = Mappings.create(query);
        mappings.add(Mapping.create());
        mappings.add(Mapping.create());

        MappingsAggregator aggregator = new MappingsAggregator(mappings);
        aggregator.finish(query);

        assertEquals(1, mappings.size());
    }
}
