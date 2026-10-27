package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@DisplayName("EvalService Tests")
class EvalServiceTest {

    private EvalService evaluator;

    @BeforeEach
    void setUp() {
        Producer mockProducer = mock(Producer.class);
        Evaluator mockEvaluator = mock(Evaluator.class);
        Matcher mockMatcher = mock(Matcher.class);
        Eval engine = new Eval(mockProducer, mockEvaluator, mockMatcher);
        QueryPlan plan = QueryPlan.create();
        engine.init(plan);
        evaluator = new EvalService(engine);
    }

    @Test
    @DisplayName("Should throw QueryEvaluationException when SERVICE is executed without Provider")
    void shouldThrowWhenNoProvider() {
        Node servNode = NodeImpl.forIRI("http://example.org/sparql");
        Exp servExp = Exp.create(Exp.Type.SERVICE, Exp.create(Exp.Type.NODE, servNode), Exp.create(Exp.Type.EMPTY));
        Stack stack = new Stack();
        Producer producer = mock(Producer.class);

        assertThrows(QueryEvaluationException.class, () ->
                evaluator.eval(producer, null, servExp, null, stack, 1));
    }

    @Test
    @DisplayName("Should silently bypass when SERVICE SILENT is executed without Provider")
    void shouldBypassWhenSilentWithoutProvider() {
        Node servNode = NodeImpl.forIRI("http://example.org/sparql");
        Exp servExp = Exp.create(Exp.Type.SERVICE, Exp.create(Exp.Type.NODE, servNode), Exp.create(Exp.Type.EMPTY));
        servExp.setSilent(true);
        Stack stack = new Stack();
        Producer producer = mock(Producer.class);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                evaluator.eval(producer, null, servExp, null, stack, 1));
    }

    @Test
    @DisplayName("Should map outer query nodes during complete")
    void shouldCompleteOuterQueryNodes() {
        QueryPlan plan = QueryPlan.create();
        Node qNode = NodeImpl.forVariable("?x");
        plan.getSelect().add(qNode);

        Mapping mapping = Mapping.create(new Node[]{NodeImpl.forVariable("?x")}, new Node[]{NodeImpl.forIRI("http://example.org/val")});
        evaluator.complete(plan, mapping, false);

        assertEquals("?x", mapping.getQueryNodes()[0].getLabel());
    }
}
