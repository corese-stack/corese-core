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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DisplayName("EvalSubQuery Tests")
class EvalSubQueryTest {

    private Producer mockProducer;
    private EvalSubQuery evaluator;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        Eval engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalSubQuery(engine);
    }

    @Test
    @DisplayName("Should backtrack when subquery produces empty result")
    void shouldBacktrackOnEmptySubQuery() {
        QueryPlan subQuery = QueryPlan.create();
        subQuery.setBody(Exp.create(Exp.Type.EMPTY));

        Stack stack = new Stack();
        int result = evaluator.eval(mockProducer, null, subQuery, null, stack, 1);
        assertEquals(1, result);
    }
}
