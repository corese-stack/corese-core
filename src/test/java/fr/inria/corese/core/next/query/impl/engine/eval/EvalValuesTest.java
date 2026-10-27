package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EvalValues Tests")
class EvalValuesTest {

    private Eval engine;
    private Producer mockProducer;
    private EvalValues evaluator;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalValues(engine);
    }

    @Test
    @DisplayName("Should return STOP if engine is stopped")
    void shouldReturnStopWhenStopped() {
        engine.getState().stop();
        Exp valuesExp = Exp.create(Exp.Type.VALUES);
        Mappings map = new Mappings();
        map.add(Mapping.create());
        valuesExp.setMappings(map);
        valuesExp.setNodeList(List.of(NodeImpl.forVariable("?x")));

        Stack stack = new Stack();
        int result = evaluator.eval(mockProducer, null, valuesExp, stack, 1);
        assertEquals(STOP, result);
    }

    @Test
    @DisplayName("Should bind and unbind values row")
    void shouldBindAndUnbindValuesRow() {
        QueryPlan plan = QueryPlan.create();
        Node variable = NodeImpl.forVariable("?x");
        variable.setIndex(plan.allocateNodeIndex());
        Eval eval = new Eval(mockProducer, mock(Evaluator.class), mock(Matcher.class));
        eval.init(plan);
        EvalValues customEvaluator = new EvalValues(eval);

        Node val = NodeImpl.forLiteral("test", null, null);
        when(mockProducer.getNode(any())).thenReturn(val);

        Mapping row = Mapping.create(new Node[]{variable}, new Node[]{val});
        boolean pushed = customEvaluator.valuesBinding(List.of(variable), row, 1);
        assertTrue(pushed);

        customEvaluator.free(List.of(variable), row);
    }
}
