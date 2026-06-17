package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BnodeAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsBlankAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : BNODE")
class SparqlParserBnodeTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(BNODE() AS ?b)")
    void shouldParseBnodeWithoutLabelInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(BNODE() AS ?b)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        BnodeAst bnode = assertInstanceOf(BnodeAst.class, bind.expression());
        assertNull(bnode.getLabel());
        assertEquals("b", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(BNODE(?label) AS ?b)")
    void shouldParseBnodeWithLabelInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(BNODE(?label) AS ?b)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        BnodeAst bnode = assertInstanceOf(BnodeAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, bnode.getLabel()).name());
        assertEquals("b", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(isBlank(BNODE(?label)))")
    void shouldParseNestedBnodeInIsBlankFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(isBlank(BNODE(?label)))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        IsBlankAst isBlank = assertInstanceOf(IsBlankAst.class, filter.operator());
        BnodeAst bnode = assertInstanceOf(BnodeAst.class, isBlank.argument());
        assertEquals("label", assertInstanceOf(VarAst.class, bnode.getLabel()).name());
    }

    @Test
    @DisplayName("ORDER BY BNODE(?label)")
    void shouldParseOrderByBnode() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY BNODE(?label)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(BnodeAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY BNODE(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByBnodeWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY BNODE(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
