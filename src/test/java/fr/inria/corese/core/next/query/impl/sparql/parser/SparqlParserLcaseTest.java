package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LcaseAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : LCASE")
class SparqlParserLcaseTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(LCASE(?label) AS ?lower)")
    void shouldParseLcaseInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(LCASE(?label) AS ?lower)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        LcaseAst lcase = assertInstanceOf(LcaseAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, lcase.argument()).name());
        assertEquals("lower", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(LCASE(\"CORESE\") AS ?lower)")
    void shouldParseLcaseWithLiteralArgument() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(LCASE("CORESE") AS ?lower)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        LcaseAst lcase = assertInstanceOf(LcaseAst.class, bind.expression());
        LiteralAst literal = assertInstanceOf(LiteralAst.class, lcase.argument());
        assertEquals("\"CORESE\"", literal.lexical());
        assertEquals("lower", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(LCASE(?label) = \"core\")")
    void shouldParseLcaseInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(LCASE(?label) = "core")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        LcaseAst lcase = assertInstanceOf(LcaseAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, lcase.argument()).name());
    }

    @Test
    @DisplayName("ORDER BY LCASE(?label)")
    void shouldParseOrderByLcase() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY LCASE(?label)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(LcaseAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY LCASE(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByLcaseWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY LCASE(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
