package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SPARQL 1.1 - Parser and AST : BIND")
class SparqlParserBindTest extends AbstractSparqlParserFeatureTest {

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

    @Test
    @DisplayName("BIND(\"hello\" AS ?label) — string literal expression")
    void shouldParseBindWithStringLiteral() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND("hello" AS ?label)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(BindAst.class, last);

        BindAst bindAst = (BindAst) last;
        assertInstanceOf(LiteralAst.class, bindAst.expression());
        assertEquals("label", bindAst.variable().name());
        assertEquals("\"hello\"", ((LiteralAst) bindAst.expression()).lexical());
    }

    @Test
    @DisplayName("BIND(<http://example.org> AS ?type) — IRI expression")
    void shouldParseBindWithIri() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(<http://example.org> AS ?type)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(BindAst.class, last);

        BindAst bindAst = (BindAst) last;
        assertInstanceOf(IriAst.class, bindAst.expression());
        assertEquals("type", bindAst.variable().name());
        assertEquals("<http://example.org>", ((IriAst) bindAst.expression()).raw());
    }

    @Test
    @DisplayName("Multiple BIND clauses in the same group")
    void shouldParseMultipleBinds() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(?s AS ?x)
                  BIND(?p AS ?y)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(3, where.patterns().size());

        assertInstanceOf(BgpAst.class, where.patterns().get(0));

        BindAst first = assertInstanceOf(BindAst.class, where.patterns().get(1));
        assertEquals("x", first.variable().name());

        BindAst second = assertInstanceOf(BindAst.class, where.patterns().get(2));
        assertEquals("y", second.variable().name());
    }

    @Test
    @DisplayName("BIND inside an OPTIONAL block")
    void shouldParseBindInsideOptional() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  OPTIONAL { BIND(?s AS ?x) }
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        assertInstanceOf(BgpAst.class, where.patterns().get(0));

        OptionalAst optional = assertInstanceOf(OptionalAst.class, where.patterns().get(1));
        GroupGraphPatternAst optionalGroup = assertInstanceOf(GroupGraphPatternAst.class, optional.ast());

        assertEquals(1, optionalGroup.patterns().size());
        BindAst bind = assertInstanceOf(BindAst.class, optionalGroup.patterns().getFirst());
        assertEquals("x", bind.variable().name());
    }

    @Test
    @DisplayName("SELECT ?x with BIND(... AS ?x) — BIND variable is visible in projection")
    void shouldAcceptBindVariableInSelectProjection() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?x WHERE {
                  ?s ?p ?o .
                  BIND(?s AS ?x)
                }
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertEquals(1, select.projection().variables().size());
        assertEquals("x", select.projection().variables().getFirst().name());
    }
}