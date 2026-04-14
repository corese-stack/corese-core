package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EncodeForUriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : ENCODE FOR URI")
class SparqlParserEncodeForUriTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(ENCODE_FOR_URI(?label) AS ?encoded)")
    void shouldParseEncodeForUriInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(ENCODE_FOR_URI(?label) AS ?encoded)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        EncodeForUriAst encode = assertInstanceOf(EncodeForUriAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, encode.getArgument()).name());
        assertEquals("encoded", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(ENCODE_FOR_URI(\"Los Angeles\") AS ?encoded)")
    void shouldParseEncodeForUriWithLiteralArgument() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(ENCODE_FOR_URI("Los Angeles") AS ?encoded)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        EncodeForUriAst encode = assertInstanceOf(EncodeForUriAst.class, bind.expression());
        LiteralAst literal = assertInstanceOf(LiteralAst.class, encode.getArgument());
        assertEquals("\"Los Angeles\"", literal.lexical());
        assertEquals("encoded", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(ENCODE_FOR_URI(?label) = \"abc\")")
    void shouldParseEncodeForUriInFilterComparison() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(ENCODE_FOR_URI(?label) = "abc")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        EncodeForUriAst encode = assertInstanceOf(EncodeForUriAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, encode.getArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY ENCODE_FOR_URI(?label)")
    void shouldParseOrderByEncodeForUri() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY ENCODE_FOR_URI(?label)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(EncodeForUriAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY ENCODE_FOR_URI(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByEncodeForUriWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY ENCODE_FOR_URI(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
