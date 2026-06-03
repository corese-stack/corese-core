package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ConcatAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : CONCAT")
class SparqlParserConcatTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(CONCAT(?given, \" \", ?family) AS ?label)")
    void shouldParseConcatInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?given, ?family .
                  BIND(CONCAT(?given, " ", ?family) AS ?label)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ConcatAst concat = assertInstanceOf(ConcatAst.class, bind.expression());
        assertEquals(3, concat.arguments().size());
        assertEquals("given", assertInstanceOf(VarAst.class, concat.arguments().get(0)).name());
        assertEquals("\" \"", assertInstanceOf(LiteralAst.class, concat.arguments().get(1)).lexical());
        assertEquals("family", assertInstanceOf(VarAst.class, concat.arguments().get(2)).name());
        assertEquals("label", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(CONCAT(\"foo\", \"bar\") AS ?label)")
    void shouldParseConcatWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(CONCAT("foo", "bar") AS ?label)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ConcatAst concat = assertInstanceOf(ConcatAst.class, bind.expression());
        assertEquals(2, concat.arguments().size());
        assertEquals("\"foo\"", assertInstanceOf(LiteralAst.class, concat.arguments().getFirst()).lexical());
        assertEquals("\"bar\"", assertInstanceOf(LiteralAst.class, concat.arguments().getLast()).lexical());
    }

    @Test
    @DisplayName("FILTER(CONCAT(?a, ?b) = \"ab\")")
    void shouldParseConcatInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(CONCAT(?a, ?b) = "ab")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        ConcatAst concat = assertInstanceOf(ConcatAst.class, equals.getLeftArgument());
        assertEquals(2, concat.arguments().size());
        assertEquals("a", assertInstanceOf(VarAst.class, concat.arguments().getFirst()).name());
        assertEquals("b", assertInstanceOf(VarAst.class, concat.arguments().getLast()).name());
    }

    @Test
    @DisplayName("ORDER BY CONCAT(?given, ?family)")
    void shouldParseOrderByConcat() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?given WHERE {
                  ?s ?p ?given .
                  ?s ?q ?family .
                } ORDER BY CONCAT(?given, ?family)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(ConcatAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY CONCAT(?given, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByConcatWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?given WHERE {
                  ?s ?p ?given .
                } ORDER BY CONCAT(?given, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
