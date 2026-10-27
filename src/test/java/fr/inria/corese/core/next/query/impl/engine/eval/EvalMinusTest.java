package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EvalMinus Tests")
class EvalMinusTest {

    private Producer mockProducer;
    private EvalMinus evaluator;
    private EvaluationState state;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        when(mockProducer.getMappings(any(), any(), any(), any())).thenReturn(new Mappings());
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        Eval mockEngine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        mockEngine.init(plan);
        evaluator = new EvalMinus(mockEngine);
        state = mockEngine.getState();
    }

    @Test
    @DisplayName("Should return backtrack when left branch produces empty solutions")
    void shouldBacktrackWhenLeftBranchIsEmpty() {
        Exp minusExp = Exp.create(Exp.Type.MINUS, Exp.create(Exp.Type.BGP), Exp.create(Exp.Type.BGP));
        Stack stack = new Stack();
        int result = evaluator.eval(mockProducer, null, minusExp, null, stack, 2);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("Should return STOP immediately when engine is stopped during evaluation")
    void shouldReturnStopWhenEngineIsStopped() {
        state.stop();
        Exp minusExp = Exp.create(Exp.Type.MINUS, Exp.create(Exp.Type.BGP), Exp.create(Exp.Type.BGP));
        Stack stack = new Stack();
        int result = evaluator.eval(mockProducer, null, minusExp, null, stack, 2);
        assertEquals(STOP, result);
    }
}
