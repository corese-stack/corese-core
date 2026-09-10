package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NativeExpressionErrorTest {
    @ParameterizedTest
    @ValueSource(strings = {"COALESCE(?x, 1)", "IF(true, ?x, 0)", "?x || true", "?x && false"})
    void functionalFormsDoNotHideInfrastructureFailures(String expression) {
        assertFailurePropagates(expression, new QueryEvaluationException("Storage unavailable"));
        assertFailurePropagates(expression, new IllegalStateException("Invalid runtime state"));
    }

    private void assertFailurePropagates(String expression, RuntimeException failure) {
        var environment = mock(Environment.class);
        when(environment.getNode(anyString())).thenThrow(failure);
        var query = (SelectQueryAst) new SparqlParser().parse("SELECT (" + expression + " AS ?result) WHERE {}");
        var term = query.projection().expressionTerms().values().iterator().next();
        RuntimeException actual = assertThrows(failure.getClass(), () -> NativeExpressionEvaluator.evaluate(
                term, mock(Evaluator.class), environment, null, null));
        assertSame(failure, actual);
    }
}
