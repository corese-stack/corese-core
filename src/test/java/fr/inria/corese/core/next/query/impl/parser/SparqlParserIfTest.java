package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SPARQL 1.1 - Parser and AST : IF")
public class SparqlParserIfTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(IF(?s > 0, true, false)) — IF inside FILTER")
    void shouldParseIfInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(IF(?s > 0, true, false))
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        FilterAst filter = assertInstanceOf(FilterAst.class, where.patterns().getLast());

        IfAst ifAst = assertInstanceOf(IfAst.class, filter.operator());
        assertNotNull(ifAst.condition());
        assertInstanceOf(LiteralAst.class, ifAst.thenExpr());
        assertInstanceOf(LiteralAst.class, ifAst.elseExpr());
    }

    @Test
    @DisplayName("BIND(IF(?s > 0, ?s, ?o) AS ?result) — IF inside BIND")
    void shouldParseIfInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(IF(?s > 0, ?s, ?o) AS ?result)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        BindAst bind = assertInstanceOf(BindAst.class, where.patterns().getLast());

        IfAst ifAst = assertInstanceOf(IfAst.class, bind.expression());
        assertInstanceOf(GreaterThanAst.class, ifAst.condition());
        assertEquals("s", assertInstanceOf(VarAst.class, ifAst.thenExpr()).name());
        assertEquals("o", assertInstanceOf(VarAst.class, ifAst.elseExpr()).name());
    }

    @Test
    @DisplayName("BIND(IF(BOUND(?o), ?o, \"default\") AS ?val) — BOUND condition")
    void shouldParseIfWithBoundCondition() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(IF(BOUND(?o), ?o, "default") AS ?val)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        BindAst bind = assertInstanceOf(BindAst.class, where.patterns().getLast());

        IfAst ifAst = assertInstanceOf(IfAst.class, bind.expression());
        assertInstanceOf(BoundAst.class, ifAst.condition());
        assertEquals("o", assertInstanceOf(VarAst.class, ifAst.thenExpr()).name());
        assertInstanceOf(LiteralAst.class, ifAst.elseExpr());
    }

    @Test
    @DisplayName("BIND(IF(?s, IF(?p, ?p, ?o), ?o) AS ?r) — nested IF")
    void shouldParseNestedIf() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(IF(?s, IF(?p, ?p, ?o), ?o) AS ?r)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        BindAst bind = assertInstanceOf(BindAst.class, where.patterns().getLast());

        IfAst outer = assertInstanceOf(IfAst.class, bind.expression());
        assertEquals("s", assertInstanceOf(VarAst.class, outer.condition()).name());
        assertInstanceOf(IfAst.class, outer.thenExpr());
        assertEquals("o", assertInstanceOf(VarAst.class, outer.elseExpr()).name());
    }
}
