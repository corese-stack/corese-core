package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrUuidAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : STRUUID")
public class SparqlParserStrUuidTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRUUID() AS ?id)")
    void shouldParseStrUuid() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(STRUUID() AS ?id)
                }
                """);

        assertNotNull(ast);
        BindAst bind = (BindAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(StrUuidAst.class, bind.expression());
    }

    @Test
    @DisplayName("FILTER(STRUUID())")
    void shouldParseStrUuidInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(STRUUID())
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(StrUuidAst.class, filter.operator());
    }
}