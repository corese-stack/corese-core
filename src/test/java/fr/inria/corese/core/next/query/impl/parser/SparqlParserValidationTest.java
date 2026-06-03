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
        @DisplayName("Should accept FILTER with numeric operator")
        void shouldAcceptFilterWithNumericOperator() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER(RAND())
                        }
                    """));
        }

        @Test
        @DisplayName("Should reject FILTER with IRI-returning operator")
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
        @DisplayName("Should accept FILTER with numeric expression derived from datetime")
        void shouldAcceptFilterWithDuration() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER(DAY(NOW()))
                        }
                    """));
        }

        @Test
        @DisplayName("Should accept FILTER with plain literal")
        void shouldAcceptFilterWithPlainLiteral() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER("test")
                        }
                    """));
        }

        @Test
        @DisplayName("Should accept FILTER with CONCAT result")
        void shouldAcceptFilterWithConcat() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER(CONCAT("te", "st"))
                        }
                    """));
        }

        @Test
        @DisplayName("Should reject FILTER when IF condition is not EBV-compatible")
        void shouldRejectFilterContainingIfWithIncorrectConditionType() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER(IF(NOW(), true, false))
                        }
                    """));

            assertEquals("IF used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should let pass FILTER with simple literal operator")
        void shouldAcceptFilterWithStringOperator() {
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
        void shouldAcceptFilterWithVariable() {
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
        void shouldAcceptFilterWithBoolean() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          ?x ?p ?o .
                          FILTER(true)
                        }
                    """));
        }

        @Test
        @DisplayName("Should let pass FILTER containing a IF that returns boolean or any acceptable AST type.")
        void shouldAcceptFilterContainingIfWithCorrectType() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(IF(?s, true, ?o))
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should reject FILTER containing a IF that does not returns boolean or any acceptable AST type.")
        void shouldRejectFilterContainingIfWithIncorrectType() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                       SELECT * WHERE {
                         ?x ?p ?o .
                         FILTER(IF(?s, 4, <http://test.inria.fr>))
                       }
                    """));

            assertEquals("IF used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject FILTER containing a IF that does not returns only boolean or any acceptable AST type.")
        void shouldRejectFilterContainingIfWithIncorrectTypeMix() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                       SELECT * WHERE {
                         ?x ?p ?o .
                         FILTER(IF(?s, true, <http://test.inria.fr>))
                       }
                    """));

            assertEquals("IF used in FILTER should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should accept FILTER with numeric operator in a nested BGP")
        void shouldAcceptFilterWithNumericOperatorInNestedBGP() {
            SparqlParser parser = newParserDefault();

            assertDoesNotThrow(() -> parser.parse("""
                        SELECT * WHERE {
                          {
                              ?x ?p ?o .
                              FILTER(RAND())
                          } UNION {
                              ?s ?p ?o .
                          }
                        }
                    """));
        }
    }

    @Nested
    public class OperandTypeTest {

        @Test
        @DisplayName("Should accept + operator with numerics")
        void shouldAcceptPlusWithNumerics() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(RAND() + 1 = ?o)
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should accept - operator with numerics")
        void shouldAcceptMinusWithNumerics() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(STRLEN("test") - 1 = ?o)
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should accept * operator with numerics")
        void shouldAcceptMultiplyWithNumerics() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(STRLEN("test") * RAND() = ?o)
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should accept / operator with numerics")
        void shouldAcceptDivideWithNumerics() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(DAY(NOW()) / 2 = ?o)
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should reject + operator with non numerics")
        void shouldRefusePlusWithNonNumerics() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(RAND() + "one" = ?o)
                           }
                        """);
            });
            assertEquals("\"one\" used in + should be resolvable to a numeric", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject - operator with non numerics")
        void shouldAcceptMinusWithNonNumerics() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER((3 - STRENDS("test", "")) = ?o)
                           }
                        """);
            });
            assertEquals("STRENDS used in - should be resolvable to a numeric", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject * operator with non numerics")
        void shouldAcceptDivideWithNonNumerics() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(<http://ns.inria.fr/test> / 2 = ?o)
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in / should be resolvable to a numeric", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject / operator with non numerics")
        void shouldAcceptMultiplyWithNonNumerics() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(STRLEN("test") * NOW() = ?o)
                           }
                        """);
            });
            assertEquals("NOW used in * should be resolvable to a numeric", exception.getMessage());
        }

        @Test
        @DisplayName("Should accept || operator with booleans")
        void shouldAcceptOrWithBoolean() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(IsIri(?s) || false)
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should accept && operator with booleans")
        void shouldAcceptAndWithBoolean() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(NOT EXISTS { ?s ?p false } && STRSTARTS("test", "t"))
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should reject || operator with non booleans")
        void shouldRejectOrWithNonBoolean() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(IsIri(?s) || "potato")
                           }
                        """);
            });
            assertEquals("\"potato\" used in || should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject && operator with non booleans")
        void shouldRejectAndWithNonBoolean() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(<http://ns.inria.fr/test> && langMatches("potato", "fr"))
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in && should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should accept ! operator with booleans")
        void shouldAcceptNotWithBoolean() {
            SparqlParser parser = newParserDefault();
            assertDoesNotThrow(() -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(! NOT EXISTS { ?s ?p false } && STRSTARTS("test", "t"))
                           }
                        """);
            });
        }

        @Test
        @DisplayName("Should reject ! operator with non booleans")
        void shouldRejectNotWithNonBoolean() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(! <http://ns.inria.fr/test>)
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in ! should be resolvable to a boolean", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject < operator with IRIs")
        void shouldRejectLTWithIRIs() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(2 < <http://ns.inria.fr/test>)
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in < should be resolvable to a not an IRI", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject <= operator with IRIs")
        void shouldRejectLTEWithIRIs() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(STRLEN("test") <= <http://ns.inria.fr/test>)
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in <= should be resolvable to a not an IRI", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject > operator with IRIs")
        void shouldRejectGTWithIRIs() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER("2"^^<http://www.w3.org/2001/XMLSchema#integer> > <http://ns.inria.fr/test>)
                           }
                        """);
            });
            assertEquals("<http://ns.inria.fr/test> used in > should be resolvable to a not an IRI", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject >= operator with IRIs")
        void shouldRejectGTEWithIRIs() {
            SparqlParser parser = newParserDefault();
            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> {
                parser.parse("""
                           SELECT * WHERE {
                             ?x ?p ?o .
                             FILTER(datatype("2"^^<http://www.w3.org/2001/XMLSchema#integer>) >= 4)
                           }
                        """);
            });
            assertEquals("DATATYPE used in >= should be resolvable to a not an IRI", exception.getMessage());
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
