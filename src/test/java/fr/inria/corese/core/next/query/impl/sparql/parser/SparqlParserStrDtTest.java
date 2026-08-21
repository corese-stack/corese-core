package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrDtAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRDT")
class SparqlParserStrDtTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRDT(?label, xsd:string) AS ?typed)")
    void shouldParseStrDtInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRDT(?label, xsd:string) AS ?typed)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrDtAst strDt = assertInstanceOf(StrDtAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strDt.getLeftArgument()).name());
        assertEquals("xsd:string", assertInstanceOf(IriAst.class, strDt.getRightArgument()).raw());
        assertEquals("typed", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRDT(\"chat\", xsd:string) AS ?typed)")
    void shouldParseStrDtWithLiteralArgument() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRDT("chat", xsd:string) AS ?typed)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrDtAst strDt = assertInstanceOf(StrDtAst.class, bind.expression());
        assertEquals("\"chat\"", assertInstanceOf(LiteralAst.class, strDt.getLeftArgument()).lexical());
        assertEquals("xsd:string", assertInstanceOf(IriAst.class, strDt.getRightArgument()).raw());
        assertEquals("typed", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRDT(\"chat\", xsd:string) = \"chat\"^^xsd:string)")
    void shouldParseStrDtInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRDT("chat", xsd:string) = "chat"^^xsd:string)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        StrDtAst strDt = assertInstanceOf(StrDtAst.class, equals.getLeftArgument());
        assertEquals("\"chat\"", assertInstanceOf(LiteralAst.class, strDt.getLeftArgument()).lexical());
        assertEquals("xsd:string", assertInstanceOf(IriAst.class, strDt.getRightArgument()).raw());
        LiteralAst literal = assertInstanceOf(LiteralAst.class, equals.getRightArgument());
        assertEquals("\"chat\"", literal.lexical());
        assertEquals("xsd:string", literal.datatype());
    }

    @Test
    @DisplayName("ORDER BY STRDT(?label, xsd:string)")
    void shouldParseOrderByStrDt() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRDT(?label, xsd:string)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(StrDtAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY STRDT(?missing, xsd:string) — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrDtWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRDT(?missing, xsd:string)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
