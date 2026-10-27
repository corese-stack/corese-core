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

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("EvalUnion Tests")
class EvalUnionTest {

    private Eval engine;
    private Producer mockProducer;
    private EvalUnion evaluator;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalUnion(engine);
    }

    @Test
    @DisplayName("Should detect non-federated expression when global query is not federate")
    void shouldDetectNonFederated() {
        Exp exp = Exp.create(Exp.Type.BGP);
        assertFalse(evaluator.isFederate(exp));
    }

    @Test
    @DisplayName("Should return STOP if engine is stopped before evaluating right branch")
    void shouldReturnStopWhenStopped() {
        engine.getState().stop();
        Exp unionExp = Exp.create(Exp.Type.UNION, Exp.create(Exp.Type.EMPTY), Exp.create(Exp.Type.EMPTY));
        Stack stack = new Stack();
        int result = evaluator.eval(mockProducer, null, unionExp, null, stack, 2);
        assertEquals(STOP, result);
    }
}
