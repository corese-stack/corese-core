package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GroupAggregatorTest {

    private GroupAggregator original;

    @BeforeEach
    void setUp() {
        original = Mappings.getGroupAggregator();
    }

    @AfterEach
    void tearDown() {
        Mappings.setGroupAggregator(original);
    }

    @Test
    void canConfigureAndRetrieveCustomGroupAggregator() {
        GroupAggregator mockAggregator = (query, mappings) -> true;
        Mappings.setGroupAggregator(mockAggregator);

        assertSame(mockAggregator, Mappings.getGroupAggregator());
        assertTrue(Mappings.getGroupAggregator().tryAggregate(new QueryPlan(), new Mappings()));
    }

    @Test
    void nullAggregatorFallsBackToOrdinaryAggregation() {
        Mappings.setGroupAggregator(null);
        assertNull(Mappings.getGroupAggregator());

        QueryPlan q = new QueryPlan();
        Mappings mappings = new Mappings();
        mappings.add(new Mapping());
        Evaluator eval = mock(Evaluator.class);
        Memory mem = mock(Memory.class);
        Producer p = mock(Producer.class);

        // Does not throw and finishes cleanly
        mappings.aggregate(q, eval, mem, p);
        assertFalse(mappings.isFake());
    }

    @Test
    void falseReturningAggregatorFallsBackToOrdinaryAggregation() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        Mappings.setGroupAggregator((query, mappings) -> {
            invoked.set(true);
            return false;
        });

        QueryPlan q = new QueryPlan();
        Mappings mappings = new Mappings();
        mappings.add(new Mapping());
        Evaluator eval = mock(Evaluator.class);
        Memory mem = mock(Memory.class);
        Producer p = mock(Producer.class);

        mappings.aggregate(q, eval, mem, p);
        assertTrue(invoked.get());
    }
}
