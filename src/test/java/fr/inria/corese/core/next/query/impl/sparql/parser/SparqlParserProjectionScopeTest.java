package fr.inria.corese.core.next.query.impl.sparql.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPARQL 1.1 sections 4.1.3 and 18.2.4.4: projection is a set, aliases must be new. */
class SparqlParserProjectionScopeTest {

    private final SparqlParser parser = new SparqlParser();

    @ParameterizedTest
    @ValueSource(strings = { "?x ?y ?x ?y", "?x $x ?y $y", "$x ?y ?x $y" })
    void plainVariablesAreDeduplicatedInFirstOccurrenceOrder(String projection) {
        SelectQueryAst ast = assertInstanceOf(SelectQueryAst.class,
                parser.parse("SELECT " + projection + " WHERE { ?x <urn:p> ?y }"));
        assertEquals(List.of(new VarAst("x"), new VarAst("y")), ast.projection().variables());
    }

    @ParameterizedTest
    @ValueSource(strings = { "(1 AS ?x) ?x", "(1 AS $x) ?x $x", "(1 AS ?x) $x" })
    void plainReferencesMayFollowAnAlias(String projection) {
        SelectQueryAst ast = assertInstanceOf(SelectQueryAst.class,
                parser.parse("SELECT " + projection + " WHERE {}"));
        assertEquals(List.of(new VarAst("x")), ast.projection().variables());
        assertTrue(ast.projection().expressionBoundVariables().contains("x"));
        assertEquals(1, ast.projection().expressionTerms().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?x (1 AS ?x)", "?x (1 AS $x)", "$x (1 AS ?x)",
            "(1 AS ?x) (2 AS ?x)", "(1 AS ?x) (2 AS $x)", "(1 AS $x) (2 AS ?x)"
    })
    void aliasesCannotRedefineAnEarlierProjectedVariable(String projection) {
        String query = "SELECT " + projection + " WHERE {}";
        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
        assertFalse(parser.validate(query).isValid());
        assertThrows(QuerySyntaxException.class, () -> parser.parse("SELECT * WHERE { { " + query + " } }"));
    }

    @Test
    void projectionScopesAreIndependentAcrossSubqueries() {
        String query = "SELECT ?x $x WHERE { { SELECT ?x $x WHERE { ?x ?p ?o } } }";
        SelectQueryAst ast = assertInstanceOf(SelectQueryAst.class, parser.parse(query));
        assertEquals(List.of(new VarAst("x")), ast.projection().variables());
    }

    @Test
    void aliasesStillCannotRedefineVariablesFromWhere() {
        assertFalse(parser.validate("SELECT (1 AS $x) WHERE { ?x ?p ?o }").isValid());
    }
}
