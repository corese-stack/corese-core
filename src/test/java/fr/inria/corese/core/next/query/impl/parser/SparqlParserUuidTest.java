package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UuidAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("SPARQL 1.1 - Parser and AST : UUID")
class SparqlParserUuidTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(UUID() AS ?id)")
    void shouldParseUuid() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(UUID() AS ?id)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        assertInstanceOf(UuidAst.class, bind.expression());
        assertEquals("id", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(UUID() = ?s)")
    void shouldParseUuidInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(UUID() = ?s)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        assertInstanceOf(UuidAst.class, equals.getLeftArgument());
        assertEquals("s", assertInstanceOf(VarAst.class, equals.getRightArgument()).name());
    }

    @Test
    @DisplayName("SELECT (UUID() AS ?id) ?s WHERE { ?s ?p ?o }")
    void shouldParseUuidInProjectionBinding() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT (UUID() AS ?id) ?s WHERE {
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