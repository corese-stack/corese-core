package fr.inria.corese.core.next.query.impl.sparql.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPARQL 1.1 sections 5.1.1, 16.2.1, 18.2.2, and 19.6. */
class SparqlParserBlankNodeScopeTest {

    private final SparqlParser parser = new SparqlParser();

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "FILTER (?x != ?y)",
            "FILTER EXISTS { ?s ?p ?o }",
            "FILTER NOT EXISTS { ?s ?p ?o }",
            "FILTER (EXISTS { ?s ?p ?o } && NOT EXISTS { ?t ?q ?r })",
            "FILTER EXISTS { _:inner <urn:p> ?v . OPTIONAL { ?s ?p ?o } }",
            "FILTER EXISTS { _:inner <urn:p> ?v . FILTER EXISTS { ?s ?p ?o } _:inner <urn:q> ?w }"
    })
    void filtersPreserveTheEnclosingBasicGraphPattern(String filter) {
        String query = "SELECT * WHERE { _:a <urn:p> ?x . " + filter + " _:a <urn:q> ?y }";
        assertDoesNotThrow(() -> parser.parse(query));
        assertTrue(parser.validate(query).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{ ?s ?p ?o }",
            "OPTIONAL { ?s ?p ?o }",
            "MINUS { ?s ?p ?o }",
            "GRAPH <urn:g> { ?s ?p ?o }",
            "SERVICE <urn:service> { ?s ?p ?o }",
            "{ ?s ?p ?o } UNION { ?t ?q ?r }",
            "{ SELECT ?s WHERE { ?s ?p ?o } }",
            "BIND (1 AS ?z)",
            "BIND (EXISTS { ?s ?p ?o } AS ?z)",
            "VALUES ?z { 1 }"
    })
    void nonFilterPatternsEndThePrecedingBasicGraphPattern(String pattern) {
        assertInvalidLabelReuse("SELECT * WHERE { _:a <urn:p> ?x . " + pattern + " _:a <urn:q> ?y }");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * WHERE { _:a <urn:p> ?x . OPTIONAL { _:a <urn:q> ?y } }",
            "SELECT * WHERE { _:a <urn:p> ?x . FILTER EXISTS { _:a <urn:q> ?y } }",
            "SELECT * WHERE { _:a <urn:p> ?x . FILTER NOT EXISTS { _:a <urn:q> ?y } }",
            "SELECT * WHERE { FILTER EXISTS { _:a <urn:p> ?x } _:a <urn:q> ?y }",
            "SELECT * WHERE { { _:a <urn:p> ?x } UNION { _:a <urn:q> ?y } }",
            "SELECT * WHERE { { SELECT ?x WHERE { _:a <urn:p> ?x } } _:a <urn:q> ?y }",
            "SELECT * WHERE { GRAPH <urn:g> { _:a <urn:p> ?x } GRAPH <urn:h> { _:a <urn:q> ?y } }",
            "CONSTRUCT { _:a <urn:p> ?x } WHERE { _:a <urn:p> ?x . OPTIONAL { _:a <urn:q> ?y } }",
            "INSERT {} WHERE { _:a <urn:p> ?x }; INSERT {} WHERE { _:a <urn:q> ?y }"
    })
    void labelsCannotBeSharedByDistinctBasicGraphPatterns(String query) {
        assertInvalidLabelReuse(query);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONSTRUCT { _:a <urn:p> ?x . _:a <urn:q> ?x } WHERE { _:a <urn:r> ?x }",
            "CONSTRUCT WHERE { _:a <urn:p> ?x . _:a <urn:q> ?y }",
            "INSERT { _:a <urn:p> ?x } WHERE { _:a <urn:q> ?x }",
            "INSERT { _:a <urn:p> ?x } WHERE { ?s <urn:q> ?x }; INSERT { _:a <urn:q> ?x } WHERE { ?s <urn:p> ?x }",
            "INSERT DATA { _:a <urn:p> 1 . GRAPH <urn:g> { _:a <urn:q> 2 } }",
            "SELECT * WHERE { [] <urn:p> ?x . OPTIONAL { [] <urn:q> ?y } }",
            "SELECT * WHERE { _:a <urn:p> ?x . OPTIONAL { _:b <urn:q> ?y } _:c <urn:r> ?z }"
    })
    void templatesAndAnonymousNodesHaveIndependentScopes(String query) {
        assertDoesNotThrow(() -> parser.parse(query));
    }

    @Test
    void insertDataStillRejectsLabelsReusedAcrossOperations() {
        assertThrows(QuerySyntaxException.class, () -> parser.parse(
                "INSERT DATA { _:a <urn:p> 1 }; INSERT DATA { _:a <urn:q> 2 }"));
    }

    @Test
    void scopeStateDoesNotLeakBetweenParserInvocations() {
        String query = "SELECT * WHERE { _:a <urn:p> ?x }";
        assertDoesNotThrow(() -> parser.parse(query));
        assertInvalidLabelReuse("SELECT * WHERE { _:a <urn:p> ?x . OPTIONAL { _:a <urn:q> ?y } }");
        assertDoesNotThrow(() -> parser.parse(query));
    }

    private void assertInvalidLabelReuse(String query) {
        QuerySyntaxException error = assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
        assertTrue(error.getMessage().contains("Blank node label '_:a'"), error.getMessage());
        assertFalse(parser.validate(query).isValid());
    }
}
