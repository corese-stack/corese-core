package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SPARQL 1.1 - Parser and AST : BIND")
public class SparqlParserBindTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(?s AS ?x) — variable expression")
    void shouldParseBindWithVariable() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(?s AS ?x)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(BindAst.class, last);

        BindAst bindAst = (BindAst) last;
        assertInstanceOf(VarAst.class, bindAst.expression());
        assertEquals("s", ((VarAst) bindAst.expression()).name());
        assertEquals("x", bindAst.variable().name());
    }

    @Test
    @DisplayName("BIND(CONCAT(?a, ?b) AS ?c) — built-in function expression")
    void shouldParseBindWithConcat() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(CONCAT(?a, ?b) AS ?c)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(BindAst.class, last);

        BindAst bindAst = (BindAst) last;
        assertInstanceOf(FunctionCallAst.class, bindAst.expression());
        assertEquals("c", bindAst.variable().name());

        FunctionCallAst concatAst = (FunctionCallAst) bindAst.expression();
        assertEquals("CONCAT", ((IriAst) concatAst.functionName()).raw());
        assertEquals(2, concatAst.arguments().size());
        assertEquals("a", ((VarAst) concatAst.arguments().getFirst()).name());
        assertEquals("b", ((VarAst) concatAst.arguments().getLast()).name());
    }

    @Test
    @DisplayName("BIND(?x + 1 AS ?y) — arithmetic expression")
    void shouldParseBindWithArithmetic() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(?x + 1 AS ?y)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(BindAst.class, last);

        BindAst bindAst = (BindAst) last;
        assertEquals("y", bindAst.variable().name());
        assertNotNull(bindAst.expression());
    }
}