package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrLenAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : STRLEN")
public class SparqlParserStrLenTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRLEN(?s) AS ?len)")
    void shouldParseStrLen() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(STRLEN(?s) AS ?len)
                }
                """);

        assertNotNull(ast);
        BindAst bind = (BindAst) ast.whereClause().patterns().getLast();
        StrLenAst strLen = assertInstanceOf(StrLenAst.class, bind.expression());
        assertEquals("s", assertInstanceOf(VarAst.class, strLen.argument()).name());
    }

    @Test
    @DisplayName("FILTER(STRLEN(?s))")
    void shouldParseStrLenInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(STRLEN(?s))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(StrLenAst.class, filter.operator());
    }
}