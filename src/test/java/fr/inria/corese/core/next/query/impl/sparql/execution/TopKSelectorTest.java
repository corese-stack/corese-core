package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TopKSelectorTest {
    @Test
    void theHeapNeverRetainsMoreThanLimitPlusOffsetAndReleasesDiscardedSlots() throws ReflectiveOperationException {
        QueryPlan query = QueryPlan.create();
        query.setLimit(5);
        query.setOffset(2);
        query.addOrderBy(NodeImpl.forVariable("key"));
        var selector = new TopKSelector(query, null);
        PriorityQueue<?> heap = heap(selector);
        var input = new ArrayList<Mapping>();
        for (int i = 999; i >= 0; i--) {
            Mapping row = row(i);
            input.add(row);
            selector.offer(row);
            assertTrue(heap.size() <= 7);
        }
        input.sort(Comparator.comparingInt(row -> row.getOrderBy()[0].getDatatypeValue().intValue()));
        var selected = selector.finish();
        assertEquals(input.subList(2, 7), selected.getList());
        assertTrue(heap.isEmpty());
        assertNull(selected.getEval());
    }

    @Test
    void officialComparatorVisitorOverridesAreUsedForSelectionAndFinalOrdering() {
        QueryPlan query = QueryPlan.create();
        query.setLimit(2);
        query.addOrderBy(NodeImpl.forVariable("key"));
        Eval eval = mock(Eval.class);
        ProcessVisitor visitor = mock(ProcessVisitor.class);
        when(eval.getVisitor()).thenReturn(visitor);
        when(visitor.compare(eq(eval), anyInt(), any(), any())).thenAnswer(call -> -(int) call.getArgument(1));
        var selector = new TopKSelector(query, eval);
        Mapping low = row(1);
        Mapping middle = row(2);
        Mapping high = row(3);
        selector.offer(low);
        selector.offer(middle);
        selector.offer(high);
        assertEquals(java.util.List.of(high, middle), selector.finish().getList());
        verify(visitor, atLeastOnce()).compare(eq(eval), anyInt(), any(), any());
    }

    private static Mapping row(int key) {
        Mapping mapping = Mapping.create();
        mapping.setOrderBy(new Node[]{NodeImpl.forValue(Values.factory().createLiteral(key))});
        return mapping;
    }

    @SuppressWarnings("java:S3011") // Inspect the private heap only to assert the algorithm's retained-memory bound.
    private static PriorityQueue<?> heap(TopKSelector selector) throws ReflectiveOperationException {
        var field = TopKSelector.class.getDeclaredField("heap");
        field.setAccessible(true);
        return (PriorityQueue<?>) field.get(selector);
    }
}
