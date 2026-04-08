package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UuidAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : UUID")
public class SparqlParserUuidTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(UUID() AS ?id)")
    void shouldParseUuid() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(UUID() AS ?id)
                }
                """);

        assertNotNull(ast);
        BindAst bind = (BindAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(UuidAst.class, bind.expression());
    }

    @Test
    @DisplayName("FILTER(UUID())")
    void shouldParseUuidInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(UUID())
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(UuidAst.class, filter.operator());
    }
}