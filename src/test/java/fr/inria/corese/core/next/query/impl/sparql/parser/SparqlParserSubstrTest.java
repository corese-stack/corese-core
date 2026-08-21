package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SubstrAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : SUBSTR")
class SparqlParserSubstrTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(SUBSTR(?label, 2) AS ?slice)")
    void shouldParseSubstrWithTwoArgumentsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SUBSTR(?label, 2) AS ?slice)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        SubstrAst substr = assertInstanceOf(SubstrAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, substr.getString()).name());
        assertEquals("2", assertInstanceOf(LiteralAst.class, substr.getStart()).lexical());
        assertNull(substr.getLength());
        assertEquals("slice", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(SUBSTR(\"foobar\", 4, 3) AS ?slice)")
    void shouldParseSubstrWithThreeArgumentsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SUBSTR("foobar", 4, 3) AS ?slice)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        SubstrAst substr = assertInstanceOf(SubstrAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, substr.getString()).lexical());
        assertEquals("4", assertInstanceOf(LiteralAst.class, substr.getStart()).lexical());
        assertEquals("3", assertInstanceOf(LiteralAst.class, substr.getLength()).lexical());
        assertEquals("slice", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(SUBSTR(?label, 2, 3) = \"ore\")")
    void shouldParseSubstrInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(SUBSTR(?label, 2, 3) = "ore")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        SubstrAst substr = assertInstanceOf(SubstrAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, substr.getString()).name());
        assertEquals("2", assertInstanceOf(LiteralAst.class, substr.getStart()).lexical());
        assertEquals("3", assertInstanceOf(LiteralAst.class, substr.getLength()).lexical());
    }

    @Test
    @DisplayName("ORDER BY SUBSTR(?label, ?start, ?length)")
    void shouldParseOrderBySubstrAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/start> ?start .
                  ?s <http://example.com/length> ?length .
                } ORDER BY SUBSTR(?label, ?start, ?length)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        SubstrAst substr = assertInstanceOf(SubstrAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, substr.getString()).name());
        assertEquals("start", assertInstanceOf(VarAst.class, substr.getStart()).name());
        assertEquals("length", assertInstanceOf(VarAst.class, substr.getLength()).name());
    }

    @Test
    @DisplayName("ORDER BY SUBSTR(?label, 1, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderBySubstrWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY SUBSTR(?label, 1, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
