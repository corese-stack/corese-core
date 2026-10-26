package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EvalFilter Tests")
class EvalFilterTest {

    private Eval engine;
    private Producer mockProducer;
    private EvalFilter evaluator;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalFilter(engine);
    }

    @Test
    @DisplayName("Should evaluate boolean truth cleanly")
    void shouldEvaluateBooleanTruth() {
        assertFalse(evaluator.isTrue(null));
        assertTrue(evaluator.isTrue(Values.literal(true)));
        assertFalse(evaluator.isTrue(Values.literal(false)));
    }

    @Test
    @DisplayName("Should isolate QueryTypeErrorException as false during filter testing")
    void shouldIsolateTypeErrorAsFalse() {
        Filter mockFilter = mock(Filter.class);
        Expr mockExpr = mock(Expr.class);
        when(mockFilter.getExp()).thenReturn(mockExpr);
        when(mockExpr.evalWE(any(), any(), any(), any())).thenThrow(new QueryTypeErrorException("type error"));

        Memory env = engine.getMemory();
        boolean result = evaluator.test(null, mockFilter, env, mockProducer);
        assertFalse(result);
    }
}
