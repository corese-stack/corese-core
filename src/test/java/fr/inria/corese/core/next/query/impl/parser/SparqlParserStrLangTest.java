package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrLangAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRLANG")
class SparqlParserStrLangTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRLANG(?label, \"en\") AS ?tagged)")
    void shouldParseStrLangInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRLANG(?label, "en") AS ?tagged)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrLangAst strLang = assertInstanceOf(StrLangAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strLang.getLeftArgument()).name());
        assertEquals("\"en\"", assertInstanceOf(LiteralAst.class, strLang.getRightArgument()).lexical());
        assertEquals("tagged", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRLANG(\"chat\", \"fr\") AS ?tagged)")
    void shouldParseStrLangWithLiteralArgument() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRLANG("chat", "fr") AS ?tagged)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrLangAst strLang = assertInstanceOf(StrLangAst.class, bind.expression());
        assertEquals("\"chat\"", assertInstanceOf(LiteralAst.class, strLang.getLeftArgument()).lexical());
        assertEquals("\"fr\"", assertInstanceOf(LiteralAst.class, strLang.getRightArgument()).lexical());
        assertEquals("tagged", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRLANG(\"chat\", \"fr\") = \"chat\"@fr)")
    void shouldParseStrLangInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRLANG("chat", "fr") = "chat"@fr)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        StrLangAst strLang = assertInstanceOf(StrLangAst.class, equals.getLeftArgument());
        assertEquals("\"chat\"", assertInstanceOf(LiteralAst.class, strLang.getLeftArgument()).lexical());
        assertEquals("\"fr\"", assertInstanceOf(LiteralAst.class, strLang.getRightArgument()).lexical());
        LiteralAst literal = assertInstanceOf(LiteralAst.class, equals.getRightArgument());
        assertEquals("\"chat\"", literal.lexical());
        assertEquals("fr", literal.lang());
    }

    @Test
    @DisplayName("ORDER BY STRLANG(?label, \"en\")")
    void shouldParseOrderByStrLang() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRLANG(?label, "en")
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(StrLangAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY STRLANG(?missing, \"en\") — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrLangWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRLANG(?missing, "en")
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
