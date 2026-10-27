package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that blank-node labels are validated against the SPARQL 1.1 rule:
 * a blank-node label may not appear in more than one Basic Graph Pattern (BGP).
 *
 * <p>A new BGP starts after each BGP-breaking construct: OPTIONAL, UNION branch,
 * GRAPH, SERVICE, MINUS, EXISTS, or any nested plain group.</p>
 */
@DisplayName("SPARQL parser - blank-node BGP scope validation")
class SparqlParserBnodeBgpScopeTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("same blank-node label used twice within one BGP is valid")
    void sameLabelInOneBgpIsValid() {
        String query = """
                SELECT * WHERE {
                  _:b <http://example.org/p> ?o .
                  _:b <http://example.org/q> ?x
                }
                """;
        assertDoesNotThrow(() -> newParserDefault().parse(query));
    }

    @Test
    @DisplayName("two different blank-node labels in one BGP are valid")
    void differentLabelsInOneBgpAreValid() {
        String query = """
                SELECT * WHERE {
                  _:b1 <http://example.org/p> _:b2 .
                  _:b1 <http://example.org/q> _:b2
                }
                """;
        assertDoesNotThrow(() -> newParserDefault().parse(query));
    }

    @Test
    @DisplayName("same blank-node label in main BGP and OPTIONAL body is rejected")
    void sameLabelInOptionalIsRejected() {
        String query = """
                SELECT * WHERE {
                  _:b <http://example.org/p> ?o .
                  OPTIONAL { _:b <http://example.org/q> ?x }
                }
                """;
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse(query), "A blank-node label must not cross an OPTIONAL boundary");
    }

    @Test
    @DisplayName("same blank-node label in two UNION branches is rejected")
    void sameLabelInUnionBranchesIsRejected() {
        String query = """
                SELECT * WHERE {
                  { _:b <http://example.org/p> ?o }
                  UNION
                  { _:b <http://example.org/q> ?x }
                }
                """;
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse(query), "A blank-node label must not appear in both UNION branches");
    }

    @Test
    @DisplayName("same blank-node label across a GRAPH boundary is rejected")
    void sameLabelAcrossGraphBoundaryIsRejected() {
        String query = """
                SELECT * WHERE {
                  GRAPH <http://example.org/g> { _:b <http://example.org/p> ?o }
                  _:b <http://example.org/q> ?x
                }
                """;
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse(query), "A blank-node label must not cross a GRAPH clause boundary");
    }

    @Test
    @DisplayName("same blank-node label in WHERE BGP and MINUS body is rejected")
    void sameLabelInMinusBodyIsRejected() {
        String query = """
                SELECT * WHERE {
                  _:b <http://example.org/p> ?o .
                  MINUS { _:b <http://example.org/q> ?x }
                }
                """;
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse(query), "A blank-node label must not cross a MINUS boundary");
    }
}
