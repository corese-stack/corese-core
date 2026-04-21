package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ReplaceAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : REPLACE")
class SparqlParserReplaceTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(REPLACE(?label, \"core\", \"CORE\") AS ?normalized)")
    void shouldParseReplaceWithoutFlagsInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(REPLACE(?label, "core", "CORE") AS ?normalized)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ReplaceAst replace = assertInstanceOf(ReplaceAst.class, bind.expression());

        assertEquals("label", assertInstanceOf(VarAst.class, replace.getString()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, replace.getPattern()).lexical());
        assertEquals("\"CORE\"", assertInstanceOf(LiteralAst.class, replace.getReplacement()).lexical());
        assertNull(replace.getFlags());
        assertEquals("normalized", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(REPLACE(?label, \"core\", \"CORE\", \"i\") AS ?normalized)")
    void shouldParseReplaceWithFlagsInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(REPLACE(?label, "core", "CORE", "i") AS ?normalized)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        ReplaceAst replace = assertInstanceOf(ReplaceAst.class, bind.expression());

        assertEquals("label", assertInstanceOf(VarAst.class, replace.getString()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, replace.getPattern()).lexical());
        assertEquals("\"CORE\"", assertInstanceOf(LiteralAst.class, replace.getReplacement()).lexical());
        assertEquals("\"i\"", assertInstanceOf(LiteralAst.class, replace.getFlags()).lexical());
        assertEquals("normalized", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY REPLACE(?label, ?pattern, ?replacement, ?flags)")
    void shouldParseOrderByReplaceAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/pattern> ?pattern .
                  ?s <http://example.com/replacement> ?replacement .
                  ?s <http://example.com/flags> ?flags .
                } ORDER BY REPLACE(?label, ?pattern, ?replacement, ?flags)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());

        ReplaceAst replace = assertInstanceOf(
                ReplaceAst.class,
                select.solutionModifier().orderBy().getFirst().expression());

        assertEquals("label", assertInstanceOf(VarAst.class, replace.getString()).name());
        assertEquals("pattern", assertInstanceOf(VarAst.class, replace.getPattern()).name());
        assertEquals("replacement", assertInstanceOf(VarAst.class, replace.getReplacement()).name());
        assertEquals("flags", assertInstanceOf(VarAst.class, replace.getFlags()).name());
    }

    @Test
    @DisplayName("ORDER BY REPLACE(?label, ?pattern, ?replacement, ?missingFlags) — should reject variable not visible in WHERE")
    void shouldRejectOrderByReplaceWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/pattern> ?pattern .
                  ?s <http://example.com/replacement> ?replacement .
                } ORDER BY REPLACE(?label, ?pattern, ?replacement, ?missingFlags)
                """));

        assertEquals("Variable ?missingFlags used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
