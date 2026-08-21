package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UcaseAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : UCASE")
class SparqlParserUcaseTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(UCASE(?label) AS ?upper)")
    void shouldParseUcaseInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(UCASE(?label) AS ?upper)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        UcaseAst ucase = assertInstanceOf(UcaseAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, ucase.argument()).name());
        assertEquals("upper", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(UCASE(\"corese\") AS ?upper)")
    void shouldParseUcaseWithLiteralArgument() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(UCASE("corese") AS ?upper)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        UcaseAst ucase = assertInstanceOf(UcaseAst.class, bind.expression());
        LiteralAst literal = assertInstanceOf(LiteralAst.class, ucase.argument());
        assertEquals("\"corese\"", literal.lexical());
        assertEquals("upper", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(UCASE(?label) = \"CORE\")")
    void shouldParseUcaseInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(UCASE(?label) = "CORE")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        UcaseAst ucase = assertInstanceOf(UcaseAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, ucase.argument()).name());
    }

    @Test
    @DisplayName("ORDER BY UCASE(?label)")
    void shouldParseOrderByUcase() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY UCASE(?label)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(UcaseAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY UCASE(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByUcaseWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY UCASE(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
