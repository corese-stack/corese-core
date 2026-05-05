package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LangMatchesAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : langMatches")
class SparqlParserLangMatchesTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(LANGMATCHES(?lang, \"fr\") AS ?match)")
    void shouldParseLangMatchesInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(LANGMATCHES(LANG(?o), "fr") AS ?match)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        LangMatchesAst langMatches = assertInstanceOf(LangMatchesAst.class, bind.expression());
        assertInstanceOf(BooleanExpressionAst.class, langMatches);
        assertEquals("match", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(LANGMATCHES(LANG(?title), \"fr\"))")
    void shouldParseLangMatchesInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/title> ?title .
                  FILTER(LANGMATCHES(LANG(?title), "fr"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        LangMatchesAst langMatches = assertInstanceOf(LangMatchesAst.class, filter.operator());
        assertInstanceOf(BooleanExpressionAst.class, langMatches);
    }

    @Test
    @DisplayName("FILTER(LANGMATCHES(LANG(?title), \"*\")) — wildcard range")
    void shouldParseLangMatchesWithWildcardRange() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/title> ?title .
                  FILTER(LANGMATCHES(LANG(?title), "*"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        LangMatchesAst langMatches = assertInstanceOf(LangMatchesAst.class, filter.operator());
        LiteralAst range = assertInstanceOf(LiteralAst.class, langMatches.getRightArgument());
        assertEquals("\"*\"", range.lexical());
    }

    @Test
    @DisplayName("FILTER(LANGMATCHES(?lang, \"fr\")) — variable as first argument")
    void shouldParseLangMatchesWithVariableArguments() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(LANGMATCHES(?lang, "fr"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        LangMatchesAst langMatches = assertInstanceOf(LangMatchesAst.class, filter.operator());
        assertEquals("lang", assertInstanceOf(VarAst.class, langMatches.getLeftArgument()).name());
        assertEquals("\"fr\"", assertInstanceOf(LiteralAst.class, langMatches.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("ORDER BY LANGMATCHES(LANG(?title), \"fr\")")
    void shouldParseOrderByLangMatches() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?title WHERE {
                  ?s <http://example.org/title> ?title .
                } ORDER BY LANGMATCHES(LANG(?title), "fr")
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(LangMatchesAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY LANGMATCHES(?missing, \"fr\") — should reject variable not visible in WHERE")
    void shouldRejectOrderByLangMatchesWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?title WHERE {
                  ?s <http://example.org/title> ?title .
                } ORDER BY LANGMATCHES(?missing, "fr")
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
