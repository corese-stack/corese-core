package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrEndsAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRENDS")
class SparqlParserStrEndsTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(STRENDS(?label, \"core\"))")
    void shouldParseStrEndsInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRENDS(?label, "core"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        StrEndsAst strEnds = assertInstanceOf(StrEndsAst.class, filter.operator());
        assertEquals("label", assertInstanceOf(VarAst.class, strEnds.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strEnds.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("BIND(STRENDS(?label, \"core\") AS ?endsWithCore)")
    void shouldParseStrEndsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRENDS(?label, "core") AS ?endsWithCore)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrEndsAst strEnds = assertInstanceOf(StrEndsAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strEnds.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strEnds.getRightArgument()).lexical());
        assertEquals("endsWithCore", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRENDS(\"foobar\", \"bar\") AS ?endsWithBar)")
    void shouldParseStrEndsWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRENDS("foobar", "bar") AS ?endsWithBar)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrEndsAst strEnds = assertInstanceOf(StrEndsAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, strEnds.getLeftArgument()).lexical());
        assertEquals("\"bar\"", assertInstanceOf(LiteralAst.class, strEnds.getRightArgument()).lexical());
        assertEquals("endsWithBar", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY STRENDS(?label, ?suffix)")
    void shouldParseOrderByStrEndsAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/suffix> ?suffix .
                } ORDER BY STRENDS(?label, ?suffix)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        StrEndsAst strEnds = assertInstanceOf(StrEndsAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strEnds.getLeftArgument()).name());
        assertEquals("suffix", assertInstanceOf(VarAst.class, strEnds.getRightArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY STRENDS(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrEndsWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRENDS(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
