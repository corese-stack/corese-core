package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SparqlParserMinusTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Should parse a SELECT with a MINUS graph pattern")
    void shouldParseSelectWithMinusPattern() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                PREFIX : <http://example/>
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT DISTINCT ?s WHERE {
                  ?s ?p ?o .
                  MINUS { ?s foaf:givenName "Bob" . }
                }
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        GroupGraphPatternAst where = select.whereClause();
        assertEquals(2, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().get(0));

        MinusAst minus = assertInstanceOf(MinusAst.class, where.patterns().get(1));
        assertEquals(1, minus.pattern().patterns().size());
        assertInstanceOf(BgpAst.class, minus.pattern().patterns().getFirst());
    }

    @Test
    @DisplayName("Variables declared inside MINUS should not leak into the following scope")
    void shouldNotExposeMinusVariablesToFollowingBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  MINUS { ?s ?q ?hidden . }
                  BIND(?s AS ?hidden)
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(3, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().get(0));
        assertInstanceOf(MinusAst.class, where.patterns().get(1));
        assertInstanceOf(BindAst.class, where.patterns().get(2));
    }

    @Test
    @DisplayName("SELECT * should ignore variables declared only inside MINUS")
    void shouldExcludeMinusVariablesFromSelectAllScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  MINUS { ?s ?q ?hidden . }
                }
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Set<String> visibleVariables = new VariableScopeAnalyzer()
                .collectVisibleVariables(select.whereClause());

        assertEquals(Set.of("s", "p", "o"), visibleVariables);
    }

    @Test
    @DisplayName("SELECT projection should reject a variable declared only inside MINUS")
    void shouldRejectProjectionOfMinusOnlyVariable() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?hidden WHERE {
                  ?s ?p ?o .
                  MINUS { ?s ?q ?hidden . }
                }
                """));

        assertEquals(
                "Variable ?hidden used in SELECT projection is not visible in WHERE clause",
                exception.getMessage());
    }
}
