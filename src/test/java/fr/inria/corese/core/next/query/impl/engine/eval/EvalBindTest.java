package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EvalBind Tests")
class EvalBindTest {

    private Producer mockProducer;
    private EvalBind evaluator;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        Eval engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalBind(engine);
    }

    @Test
    @DisplayName("Should convert list of nodes to map indexed by label")
    void shouldConvertNodesToMap() {
        Node n1 = NodeImpl.forVariable("?a");
        Node n2 = NodeImpl.forVariable("?b");
        Map<String, Node> map = evaluator.toMap(List.of(n1, n2));

        assertEquals(2, map.size());
        assertEquals(n1, map.get("?a"));
        assertEquals(n2, map.get("?b"));
    }

    @Test
    @DisplayName("Should evaluate standard bind when expression is non-null")
    void shouldEvaluateStandardBind() {
        QueryPlan plan = QueryPlan.create();
        Node varNode = NodeImpl.forVariable("?target");
        varNode.setIndex(plan.allocateNodeIndex());
        Eval eval = new Eval(mockProducer, mock(Evaluator.class), mock(Matcher.class));
        eval.init(plan);
        EvalBind customEvaluator = new EvalBind(eval);

        Node valNode = NodeImpl.forLiteral("boundValue", null, null);
        when(mockProducer.getNode(any())).thenReturn(valNode);

        Filter mockFilter = mock(Filter.class);
        fr.inria.corese.core.next.query.impl.engine.model.Expr mockExpr = mock(fr.inria.corese.core.next.query.impl.engine.model.Expr.class);
        when(mockFilter.getExp()).thenReturn(mockExpr);

        Exp bindExp = Exp.create(Exp.Type.BIND, varNode);
        bindExp.setFilter(mockFilter);

        Stack stack = new Stack();
        int result = customEvaluator.eval(mockProducer, null, bindExp, null, stack, 1);
        assertEquals(1, result);
    }
}
