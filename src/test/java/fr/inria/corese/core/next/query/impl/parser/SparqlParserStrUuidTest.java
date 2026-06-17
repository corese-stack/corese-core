package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrUuidAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : STRUUID")
class SparqlParserStrUuidTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRUUID() AS ?id)")
    void shouldParseStrUuid() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(STRUUID() AS ?id)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        assertInstanceOf(StrUuidAst.class, bind.expression());
        assertEquals("id", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRUUID() = STR(?s))")
    void shouldParseStrUuidInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(STRUUID() = STR(?s))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        assertInstanceOf(StrUuidAst.class, equals.getLeftArgument());
        StrAst str = assertInstanceOf(StrAst.class, equals.getRightArgument());
        assertEquals("s", assertInstanceOf(VarAst.class, str.argument()).name());
    }

    @Test
    @DisplayName("SELECT (STRUUID() AS ?id) ?s WHERE { ?s ?p ?o }")
    void shouldParseStrUuidInProjectionBinding() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT (STRUUID() AS ?id) ?s WHERE {
                  ?s ?p ?o .
                }
                """);

        assertNotNull(ast);
        SelectQueryAst selectAst = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(selectAst.projection().selectAll());
        assertEquals(2, selectAst.projection().variables().size());
        assertEquals("id", selectAst.projection().variables().getFirst().name());
        assertEquals("s", selectAst.projection().variables().getLast().name());
    }
}