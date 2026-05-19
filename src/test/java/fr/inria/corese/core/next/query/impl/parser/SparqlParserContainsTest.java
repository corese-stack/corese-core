package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ContainsAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : CONTAINS")
class SparqlParserContainsTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(CONTAINS(?label, \"core\"))")
    void shouldParseContainsInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(CONTAINS(?label, "core"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        ContainsAst contains = assertInstanceOf(ContainsAst.class, filter.operator());
        assertEquals("label", assertInstanceOf(VarAst.class, contains.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, contains.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("BIND(CONTAINS(?label, \"core\") AS ?hasCore)")
    void shouldParseContainsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(CONTAINS(?label, "core") AS ?hasCore)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ContainsAst contains = assertInstanceOf(ContainsAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, contains.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, contains.getRightArgument()).lexical());
        assertEquals("hasCore", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(CONTAINS(\"foobar\", \"bar\") AS ?hasBar)")
    void shouldParseContainsWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(CONTAINS("foobar", "bar") AS ?hasBar)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ContainsAst contains = assertInstanceOf(ContainsAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, contains.getLeftArgument()).lexical());
        assertEquals("\"bar\"", assertInstanceOf(LiteralAst.class, contains.getRightArgument()).lexical());
        assertEquals("hasBar", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY CONTAINS(?label, ?needle)")
    void shouldParseOrderByContainsAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/needle> ?needle .
                } ORDER BY CONTAINS(?label, ?needle)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        ContainsAst contains = assertInstanceOf(ContainsAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, contains.getLeftArgument()).name());
        assertEquals("needle", assertInstanceOf(VarAst.class, contains.getRightArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY CONTAINS(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByContainsWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY CONTAINS(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
