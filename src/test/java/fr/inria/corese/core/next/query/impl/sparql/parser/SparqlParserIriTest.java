package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IriFunctionAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : IRI")
class SparqlParserIriTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(IRI(?lex) AS ?iri)")
    void shouldParseIriInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?lex .
                  BIND(IRI(?lex) AS ?iri)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        IriFunctionAst iri = assertInstanceOf(IriFunctionAst.class, bind.expression());
        assertEquals("lex", assertInstanceOf(VarAst.class, iri.argument()).name());
        assertEquals("iri", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(URI(?lex) AS ?iri) — alias form")
    void shouldParseUriAliasInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?lex .
                  BIND(URI(?lex) AS ?iri)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        IriFunctionAst iri = assertInstanceOf(IriFunctionAst.class, bind.expression());
        assertEquals("lex", assertInstanceOf(VarAst.class, iri.argument()).name());
        assertEquals("iri", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(IRI(?lex) = <http://example.org/resource>)")
    void shouldParseIriInFilterComparison() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?lex .
                  FILTER(IRI(?lex) = <http://example.org/resource>)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        IriFunctionAst iri = assertInstanceOf(IriFunctionAst.class, equals.getLeftArgument());
        assertEquals("lex", assertInstanceOf(VarAst.class, iri.argument()).name());
        assertEquals("<http://example.org/resource>", assertInstanceOf(IriAst.class, equals.getRightArgument()).raw());
    }

    @Test
    @DisplayName("ORDER BY IRI(?lex)")
    void shouldParseOrderByIri() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT ?lex WHERE {
                  ?s ?p ?lex .
                } ORDER BY IRI(?lex)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(IriFunctionAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY IRI(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByIriWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?lex WHERE {
                  ?s ?p ?lex .
                } ORDER BY IRI(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
