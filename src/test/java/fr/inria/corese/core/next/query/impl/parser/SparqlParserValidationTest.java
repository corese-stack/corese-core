package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserValidationTest extends AbstractSparqlParserFeatureTest {

    private static final String ORDER_BY_SCOPE_MESSAGE =
            "Variable ?z used in ORDER BY is not visible in WHERE clause";
    private static final String SELECT_PROJECTION_SCOPE_MESSAGE =
            "Variable ?x used in SELECT projection is not visible in WHERE clause";
    private static final String CITY_LABEL_SCOPE_MESSAGE =
            "Variable ?cityLabel used in SELECT projection is not visible in WHERE clause";
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

    @Nested
    class FilterValidationTest {

        @Test
        @DisplayName("Should reject FILTER with numeric operator")
        void shouldRejectFilterWithNumericOperator() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(RAND())
                    }
                """));

            assertEquals("RAND used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject FILTER with IRI operator")
        void shouldRejectFilterWithIRIOperator() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(DATATYPE("test"^^<http://ns.inria.fr/test>))
                    }
                """));

            assertEquals("DATATYPE used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject FILTER with datetime operator")
        void shouldRejectFilterWithDatetime() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(NOW())
                    }
                """));

            assertEquals("NOW used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject FILTER with datetime duration operator")
        void shouldRejectFilterWithDuration() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(DAY(NOW()))
                    }
                """));

            assertEquals("DAY used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should let pass FILTER with simple literal operator")
        void shouldRejectFilterWithStringOperator() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(STR("test"^^<http://ns.inria.fr/test>))
                    }
                """));
        }

        @Test
        @DisplayName("Should let pass FILTER with variable")
        void shouldRejectFilterWithVariable() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(?o)
                    }
                """));
        }

        @Test
        @DisplayName("Should let pass FILTER with boolean")
        void shouldRejectFilterWithBoolean() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                    SELECT * WHERE {
                      ?x ?p ?o .
                      FILTER(true)
                    }
                """));
        }

        @Test
        @DisplayName("Should reject FILTER with numeric operator in a nested BGP")
        void shouldRejectFilterWithNumericOperatorInNestedBGP() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                      {
                          ?x ?p ?o .
                          FILTER(RAND())
                      } UNION {
                          ?s ?p ?o .
                      }
                    }
                """));

            assertEquals("RAND used in FILTER should be resolvable to a boolean", exception.getMessage());
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
                        "Should reject SELECT projection variables not visible in WHERE",
                        """
                        SELECT ?x WHERE {
                            ?s ?p ?o
                        }
                        """,
                        SELECT_PROJECTION_SCOPE_MESSAGE));
    }
}
