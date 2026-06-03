package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TrinaryRegexAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : REGEX")
class SparqlParserRegexTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(REGEX(?label, \"test\"))")
    void shouldParseBinaryRegexInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(REGEX(?label, "test"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        BinaryRegexAst regex = assertInstanceOf(BinaryRegexAst.class, filter.operator());
        assertEquals("label", assertInstanceOf(VarAst.class, regex.getString()).name());
        assertEquals("\"test\"", assertInstanceOf(LiteralAst.class, regex.getPattern()).lexical());
    }

    @Test
    @DisplayName("BIND(REGEX(?label, \"test\") AS ?matches)")
    void shouldParseBinaryRegexInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(REGEX(?label, "test") AS ?matches)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        BinaryRegexAst regex = assertInstanceOf(BinaryRegexAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, regex.getString()).name());
        assertEquals("\"test\"", assertInstanceOf(LiteralAst.class, regex.getPattern()).lexical());
        assertEquals("matches", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(REGEX(?label, \"test\", \"i\"))")
    void shouldParseTrinaryRegexInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(REGEX(?label, "test", "i"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        TrinaryRegexAst regex = assertInstanceOf(TrinaryRegexAst.class, filter.operator());
        assertEquals("label", assertInstanceOf(VarAst.class, regex.getString()).name());
        assertEquals("\"test\"", assertInstanceOf(LiteralAst.class, regex.getPattern()).lexical());
        assertEquals("\"i\"", assertInstanceOf(LiteralAst.class, regex.getFlags()).lexical());
    }

    @Test
    @DisplayName("BIND(REGEX(?label, \"^hello\", \"i\") AS ?matches)")
    void shouldParseTrinaryRegexInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(REGEX(?label, "^hello", "i") AS ?matches)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        TrinaryRegexAst regex = assertInstanceOf(TrinaryRegexAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, regex.getString()).name());
        assertEquals("\"^hello\"", assertInstanceOf(LiteralAst.class, regex.getPattern()).lexical());
        assertEquals("\"i\"", assertInstanceOf(LiteralAst.class, regex.getFlags()).lexical());
        assertEquals("matches", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY REGEX(?label, ?pattern)")
    void shouldParseOrderByRegexAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/pattern> ?pattern .
                } ORDER BY REGEX(?label, ?pattern)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        BinaryRegexAst regex = assertInstanceOf(BinaryRegexAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, regex.getString()).name());
        assertEquals("pattern", assertInstanceOf(VarAst.class, regex.getPattern()).name());
    }

    @Test
    @DisplayName("ORDER BY REGEX(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByRegexWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY REGEX(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
