package fr.inria.corese.core.next.query.impl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OptionalAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;

class SparqlParserOptionalTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Should parse a basic SELECT With Optional triples")
    void shouldParseBasicSelectWithOptionalTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?child ?birth_date ?date_of_death
                 WHERE
                 {
                   ?child wdt:P22 wd:Q76.
                   OPTIONAL{ ?child wdt:P21 ?gender. }
                   OPTIONAL{ ?child wdt:P569 ?birth_date. }
                   OPTIONAL{ ?child wdt:P570 ?date_of_death. }
                 }
                """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst select = (SelectQueryAst) ast;

        GroupGraphPatternAst where = select.whereClause();

        assertEquals(4, where.patterns().size());

        assertInstanceOf(BgpAst.class, where.patterns().get(0));
        assertInstanceOf(OptionalAst.class, where.patterns().get(1));
        assertInstanceOf(OptionalAst.class, where.patterns().get(2));
        assertInstanceOf(OptionalAst.class, where.patterns().get(3));

    }

    @Test
    @DisplayName("Should parse a basic SELECT With Optional Block")
    void shouldParseBasicSelectWithOptionalBlockTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?child ?birth_date ?date_of_death
                WHERE
                {
                  ?child wdt:P22 wd:Q76.
                  OPTIONAL{ ?child wdt:P21 ?gender.
                            ?child wdt:P569 ?birth_date.
                            ?child wdt:P570 ?date_of_death.
                          }
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst select = (SelectQueryAst) ast;

        GroupGraphPatternAst where = select.whereClause();

        assertEquals(2, where.patterns().size());

        assertInstanceOf(BgpAst.class, where.patterns().get(0));
        assertInstanceOf(OptionalAst.class, where.patterns().get(1));
    }

    @Test
    @DisplayName("Should parse a basic SELECT With nested Optional triples")
    void shouldParseOptionalPattern() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s ?name WHERE {
                        ?s a foaf:Person .
                        OPTIONAL { OPTIONAL { ?s foaf:name ?name } }
                    }
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        GroupGraphPatternAst where = select.whereClause();

        assertEquals(2, where.patterns().size());

        assertInstanceOf(BgpAst.class, where.patterns().get(0));
        assertInstanceOf(OptionalAst.class, where.patterns().get(1));
    }

}
