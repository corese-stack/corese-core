package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrLenAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : STRLEN")
class SparqlParserStrLenTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRLEN(?s) AS ?len)")
    void shouldParseStrLen() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(STRLEN(?s) AS ?len)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrLenAst strLen = assertInstanceOf(StrLenAst.class, bind.expression());
        assertEquals("s", assertInstanceOf(VarAst.class, strLen.argument()).name());
        assertEquals("len", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRLEN(?s) = 2)")
    void shouldParseStrLenInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(STRLEN(?s) = 2)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        StrLenAst strLen = assertInstanceOf(StrLenAst.class, equals.getLeftArgument());
        assertEquals("s", assertInstanceOf(VarAst.class, strLen.argument()).name());
        assertEquals("2", assertInstanceOf(LiteralAst.class, equals.getRightArgument()).lexical());
    }
}