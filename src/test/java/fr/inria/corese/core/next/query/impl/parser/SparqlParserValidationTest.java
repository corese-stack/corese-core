package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SparqlParserValidationTest extends AbstractSparqlParserFeatureTest {

    @Nested
    class SelectValidationTest {

        @Test
        @DisplayName("Should reject SELECT * with ORDER BY variable not visible in WHERE")
        void shouldRejectSelectAllWithOrderByVariableNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT * WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?z
                """));

            assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
        }

        @Test
        void shouldRejectProjectionVariableOnlyReferencedInFilter() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?x WHERE {
                  ?s ?p ?o .
                  FILTER(BOUND(?x))
                }
                """));

            assertEquals("Variable ?x used in SELECT projection is not visible in WHERE clause", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject projection variable not visible through UNION")
        void shouldRejectInvalidProjectionWithUnion() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT ?cityLabel
                    WHERE {
                      { ?country wdt:P36 ?city. }
                      UNION
                      { ?city wdt:P17 ?country. }
                    }
                """));

            assertEquals("Variable ?cityLabel used in SELECT projection is not visible in WHERE clause",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Should reject ORDER BY variable not visible in WHERE")
        void shouldRejectOrderByVariableNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?z
                """));

            assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject multiple ORDER BY clauses when one variable is not visible in WHERE")
        void shouldRejectMultipleOrderByWhenOneVariableIsNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?s ?z
                """));

            assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject ORDER BY expression using a variable not visible in WHERE")
        void shouldRejectOrderByExpressionNotVisibleInWhere() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY STR(?z)
                """));

            assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject SELECT projection variables not visible in WHERE")
        void shouldRejectInvalidProjection() {
            SparqlParser parser = newParserDefault();

            QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                    SELECT ?x WHERE {
                        ?s ?p ?o
                    }
                """));

            assertEquals("Variable ?x used in SELECT projection is not visible in WHERE clause", exception.getMessage());
        }

    }

}
