package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class NativeBooleanExpressionEvaluatorTest {
    /**
     * Checks that FILTER removes errors only after logical evaluation.
     *
     * @param expression filter condition
     * @param expected whether the solution survives
     */
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "?v != 1;false", "?v = 1 || ?v != 1;false",
            "true || ?v != 1;true", "?v != 1 || true;true",
            "false || ?v != 1;false", "?v != 1 || false;false",
            "!(false && ?v != 1);true", "!(?v != 1 && false);true",
            "true && ?v != 1;false", "?v != 1 && true;false",
            "!(?v != 1);false"
    })
    void filtersRespectThreeValuedLogic(String expression, boolean expected) {
        try (var repository = Repositories.create(MemoryStorageManager.builder().build());
             var connection = repository.getConnection()) {
            assertEquals(expected, connection.prepareBooleanQuery(
                    "ASK { VALUES ?v { \"xyz\"^^<urn:unknown> } FILTER(" + expression + ") }")
                    .evaluate());
        }
    }

    /**
     * Checks comparisons and logical expressions that must preserve errors.
     *
     * @param expression erroneous expression
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "\"xyz\"^^<urn:unknown> = 1", "\"xyz\"^^<urn:unknown> != 1",
            "\"a\"^^<urn:unknown> = \"b\"^^<urn:unknown>",
            "\"a\"^^<urn:unknown> != \"b\"^^<urn:unknown>",
            "\"2000-01-01\"^^xsd:date != 1", "1 = \"2000-01-01\"^^xsd:date",
            "\"a\" = 1", "true != 1", "\"a\"^^<urn:unknown> < 1",
            "\"a\"^^<urn:unknown> <= 1", "1 > \"a\"^^<urn:unknown>",
            "1 >= \"a\"^^<urn:unknown>",
            "(\"xyz\"^^<urn:unknown> = 1) || (\"xyz\"^^<urn:unknown> != 1)",
            "false || ?missing", "?missing || false", "?missing || ?missing",
            "true && ?missing", "?missing && true", "?missing && ?missing", "!?missing",
            "1 IN (\"x\"^^<urn:unknown>, 2)", "1 NOT IN (\"x\"^^<urn:unknown>, 2)"
    })
    void preservesTypeErrors(String expression) {
        assertThrows(QueryTypeErrorException.class, () -> evaluate(expression));
    }

    /**
     * Checks value comparisons and the successful logical truth-table entries.
     *
     * @param expression expression to evaluate
     * @param expected expected boolean value
     */
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "true || ?missing;true", "?missing || true;true",
            "\"true\"^^xsd:boolean || ?missing;true",
            "\"false\"^^xsd:boolean && ?missing;false",
            "true || \"x\"^^<urn:unknown>;true",
            "\"x\"^^<urn:unknown> && false;false",
            "false && ?missing;false", "?missing && false;false",
            "true && true;true", "true && false;false", "false && true;false", "false && false;false",
            "true || true;true", "true || false;true", "false || true;true", "false || false;false",
            "!true;false", "!false;true",
            "\"x\"^^<urn:unknown> = \"x\"^^<urn:unknown>;true",
            "\"x\"^^<urn:unknown> != \"x\"^^<urn:unknown>;false",
            "1 = 1.0;true", "1 != 2;true", "\"a\" = \"b\";false",
            "true = \"1\"^^xsd:boolean;true", "true != false;true",
            "<urn:a> = <urn:a>;true", "<urn:a> = <urn:b>;false",
            "\"hello\"@en = \"hello\"@fr;false",
            "\"hello\"@en = \"hello\"^^<urn:unknown>;false",
            "\"2000-01-01\"^^xsd:date != \"2000-01-02\"^^xsd:date;true",
            "1 IN (\"x\"^^<urn:unknown>, 1);true", "1 NOT IN (\"x\"^^<urn:unknown>, 1);false"
    })
    void evaluatesBooleanResults(String expression, boolean expected) {
        assertEquals(expected, evaluate(expression).isTrue());
    }

    /**
     * Evaluates an expression through the native parser and evaluator.
     *
     * @param expression SPARQL expression
     * @return expression value
     * @throws QueryTypeErrorException if evaluation raises a type error
     */
    private static DatatypeValue evaluate(String expression) {
        var query = (SelectQueryAst) new SparqlParser().parse(
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ("
                        + expression + " AS ?result) WHERE {}");
        var term = query.projection().expressionTerms().values().iterator().next();
        return NativeExpressionEvaluator.evaluate(term, mock(Evaluator.class), null, null, null);
    }
}
