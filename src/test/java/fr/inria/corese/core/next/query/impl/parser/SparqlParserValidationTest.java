package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SparqlParserValidationTest extends AbstractSparqlParserFeatureTest {

    private static final String ORDER_BY_SCOPE_MESSAGE =
            "Variable ?z used in ORDER BY is not visible in WHERE clause";
    private static final String SELECT_PROJECTION_SCOPE_MESSAGE =
            "Variable ?x used in SELECT projection is not visible in WHERE clause";
    private static final String CITY_LABEL_SCOPE_MESSAGE =
            "Variable ?cityLabel used in SELECT projection is not visible in WHERE clause";
    private static final String LABEL_SCOPE_MESSAGE =
            "Variable ?label used in SELECT projection is not visible in WHERE clause";
    private static final String GROUP_BY_SCOPE_MESSAGE =
            "Variable ?z used in GROUP BY is not visible in WHERE clause";
    private static final String HAVING_SCOPE_MESSAGE =
            "Variable ?z used in HAVING is not visible in WHERE clause";
    private static final String UNGROUPED_PROJECTION_MESSAGE =
            "Variable ?o used in SELECT projection must be grouped or aggregated";
    private static final String SELECT_ALL_GROUP_BY_MESSAGE =
            "SELECT * is not permitted with GROUP BY";
    private static final String BIND_SCOPE_MESSAGE =
            "Variable ?x used in BIND is already declared in the same group graph pattern";

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSelectQueries")
    void shouldRejectInvalidSelectQueries(String testName, String query, String expectedMessage) {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse(query));

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Nested
    class AskValidationTest {

        @Test
        @DisplayName("Should reject ASK ORDER BY variable not visible in WHERE")
        void shouldRejectAskOrderByVariableNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    ASK
                    WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?z
                """));

            assertEquals(ORDER_BY_SCOPE_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    class ConstructValidationTest {

        @Test
        @DisplayName("Should reject CONSTRUCT ORDER BY variable not visible in WHERE")
        void shouldRejectConstructOrderByVariableNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    CONSTRUCT {
                        ?s ?p ?o
                    }
                    WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?z
                """));

            assertEquals(ORDER_BY_SCOPE_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    class DescribeValidationTest {

        @Test
        @DisplayName("Should reject DESCRIBE ORDER BY variable not visible in WHERE")
        void shouldRejectDescribeOrderByVariableNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    DESCRIBE ?s
                    WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?z
                """));

            assertEquals(ORDER_BY_SCOPE_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    class BindValidationTest {

        @Test
        @DisplayName("Should reject BIND when variable is already introduced by a triple pattern")
        void shouldRejectBindVariableAlreadyVisibleFromTriple() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      BIND(?o AS ?x)
                    }
                """));

            assertEquals(BIND_SCOPE_MESSAGE, exception.getMessage());
        }

        @Test
        @DisplayName("Should reject BIND when variable is already introduced by a previous BIND")
        void shouldRejectBindVariableAlreadyVisibleFromPreviousBind() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?s ?p ?o .
                      BIND(?s AS ?x)
                      BIND(?p AS ?x)
                    }
                """));

            assertEquals(BIND_SCOPE_MESSAGE, exception.getMessage());
        }
    }

    private static Stream<Arguments> invalidSelectQueries() {
        return Stream.of(
                Arguments.of(
                        "Should reject SELECT * with ORDER BY variable not visible in WHERE",
                        """
                        SELECT * WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY ?z
                        """,
                        ORDER_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject projection variable only referenced in FILTER",
                        """
                        SELECT ?x WHERE {
                          ?s ?p ?o .
                          FILTER(BOUND(?x))
                        }
                        """,
                        SELECT_PROJECTION_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject projection variable not visible through UNION",
                        """
                        SELECT ?cityLabel
                        WHERE {
                          { ?country wdt:P36 ?city. }
                          UNION
                          { ?city wdt:P17 ?country. }
                        }
                        """,
                        CITY_LABEL_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject ORDER BY variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY ?z
                        """,
                        ORDER_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject multiple ORDER BY clauses when one variable is not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY ?s ?z
                        """,
                        ORDER_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject ORDER BY expression using a variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY STR(?z)
                        """,
                        ORDER_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject ORDER BY IF expression using a variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY IF(BOUND(?o), ?o, ?z)
                        """,
                        ORDER_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject SELECT expression projection using a variable not visible in WHERE",
                        """
                        SELECT (STR(?x) AS ?label) WHERE {
                            ?s ?p ?o
                        }
                        """,
                        SELECT_PROJECTION_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject SELECT expression projection using a later alias not yet visible in SELECT",
                        """
                        SELECT (STR(?label) AS ?copy) (STR(?p) AS ?label) WHERE {
                            ?s ?p ?o
                        }
                        """,
                        LABEL_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject GROUP BY variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?z
                        """,
                        GROUP_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject GROUP BY expression using a variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY CONCAT(?s, ?z)
                        """,
                        GROUP_BY_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject grouped SELECT projection variable that is neither grouped nor aggregated",
                        """
                        SELECT ?s ?o WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        """,
                        UNGROUPED_PROJECTION_MESSAGE),
                Arguments.of(
                        "Should reject grouped SELECT expression using a variable that is neither grouped nor aggregated",
                        """
                        SELECT ?s (STR(?o) AS ?label) WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        """,
                        UNGROUPED_PROJECTION_MESSAGE),
                Arguments.of(
                        "Should reject grouped SELECT expression that only reuses GROUP BY expression variables without aggregation",
                        """
                        SELECT (CONCAT(?s, ?o) AS ?key) WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY CONCAT(?s, ?o)
                        """,
                        "Variable ?s used in SELECT projection must be grouped or aggregated"),
                Arguments.of(
                        "Should reject implicit aggregate query projecting a non-aggregated variable",
                        """
                        SELECT ?s (COUNT(?o) AS ?count) WHERE {
                            ?s ?p ?o
                        }
                        """,
                        "Variable ?s used in SELECT projection must be grouped or aggregated"),
                Arguments.of(
                        "Should reject implicit aggregate query projection expression using a non-aggregated variable",
                        """
                        SELECT (STR(?s) AS ?label) (COUNT(?o) AS ?count) WHERE {
                            ?s ?p ?o
                        }
                        """,
                        "Variable ?s used in SELECT projection must be grouped or aggregated"),
                Arguments.of(
                        "Should reject SELECT * when GROUP BY is present",
                        """
                        SELECT * WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        """,
                        SELECT_ALL_GROUP_BY_MESSAGE),
                Arguments.of(
                        "Should reject HAVING expression using a variable not visible in WHERE",
                        """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        HAVING (BOUND(?z))
                        """,
                        HAVING_SCOPE_MESSAGE),
                Arguments.of(
                        "Should reject SELECT projection variables not visible in WHERE",
                        """
                        SELECT ?x WHERE {
                            ?s ?p ?o
                        }
                        """,
                        SELECT_PROJECTION_SCOPE_MESSAGE));
    }
}
