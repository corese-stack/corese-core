package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.CoalesceAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SPARQL 1.1 - Parser and AST : COALESCE")
public class SparqlParserCoalesceTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("COALESCE(?a, ?b, ?c)")
    void shouldParseCoalesceWithThreeArgs() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(COALESCE(?s, ?p, ?o))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(CoalesceAst.class, filter.operator());

        CoalesceAst coalesce = (CoalesceAst) filter.operator();
        assertEquals(3, coalesce.arguments().size());
        assertInstanceOf(VarAst.class, coalesce.arguments().get(0));
        assertInstanceOf(VarAst.class, coalesce.arguments().get(1));
        assertInstanceOf(VarAst.class, coalesce.arguments().get(2));
        assertEquals("s", ((VarAst) coalesce.arguments().get(0)).name());
        assertEquals("p", ((VarAst) coalesce.arguments().get(1)).name());
        assertEquals("o", ((VarAst) coalesce.arguments().get(2)).name());
    }

    @Test
    @DisplayName("BIND(COALESCE(?a, \"default\") AS ?result)")
    void shouldParseCoalesceInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(COALESCE(?s, "default") AS ?result)
                }
                """);

        assertNotNull(ast);
        BindAst bind = (BindAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(CoalesceAst.class, bind.expression());

        CoalesceAst coalesce = (CoalesceAst) bind.expression();
        assertEquals(2, coalesce.arguments().size());
        assertInstanceOf(VarAst.class, coalesce.arguments().getFirst());
        assertInstanceOf(LiteralAst.class, coalesce.arguments().getLast());
        assertEquals("s", ((VarAst) coalesce.arguments().getFirst()).name());
    }

    @Test
    @DisplayName("COALESCE(?a) — single argument")
    void shouldParseCoalesceWithOneArg() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(COALESCE(?s))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(CoalesceAst.class, filter.operator());

        CoalesceAst coalesce = (CoalesceAst) filter.operator();
        assertEquals(1, coalesce.arguments().size());
    }
}