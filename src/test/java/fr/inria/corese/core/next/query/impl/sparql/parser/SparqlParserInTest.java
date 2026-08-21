package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.InAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NotInAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SPARQL 1.1 - Parser and AST : IN / NOT IN")
class SparqlParserInTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(?x IN (1, 2, 3))")
    void shouldParseInWithLiterals() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?x .
                  FILTER(?x IN (1, 2, 3))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        InAst in = assertInstanceOf(InAst.class, filter.operator());
        assertEquals("x", assertInstanceOf(VarAst.class, in.left()).name());
        assertEquals(3, in.candidates().size());
        assertEquals("1", assertInstanceOf(LiteralAst.class, in.candidates().get(0)).lexical());
        assertEquals("2", assertInstanceOf(LiteralAst.class, in.candidates().get(1)).lexical());
        assertEquals("3", assertInstanceOf(LiteralAst.class, in.candidates().get(2)).lexical());
    }

    @Test
    @DisplayName("FILTER(2 IN ())")
    void shouldParseInEmptyList() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(2 IN ())
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        InAst in = assertInstanceOf(InAst.class, filter.operator());
        assertEquals("2", assertInstanceOf(LiteralAst.class, in.left()).lexical());
        assertTrue(in.candidates().isEmpty());
    }

    @Test
    @DisplayName("FILTER(?x NOT IN (1, 2))")
    void shouldParseNotIn() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?x .
                  FILTER(?x NOT IN (1, 2))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        NotInAst notIn = assertInstanceOf(NotInAst.class, filter.operator());
        assertEquals("x", assertInstanceOf(VarAst.class, notIn.left()).name());
        assertEquals(2, notIn.candidates().size());
    }

    @Test
    @DisplayName("ORDER BY (?label IN (?altLabel, \"core\"))")
    void shouldParseOrderByInAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/altLabel> ?altLabel .
                } ORDER BY (?label IN (?altLabel, "core"))
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        InAst in = assertInstanceOf(InAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, in.left()).name());
        assertEquals("altLabel", assertInstanceOf(VarAst.class, in.candidates().get(0)).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, in.candidates().get(1)).lexical());
    }

    @Test
    @DisplayName("ORDER BY (?label NOT IN (?altLabel, ?missing)) — should reject variable not visible in WHERE")
    void shouldRejectOrderByNotInWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/altLabel> ?altLabel .
                } ORDER BY (?label NOT IN (?altLabel, ?missing))
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
