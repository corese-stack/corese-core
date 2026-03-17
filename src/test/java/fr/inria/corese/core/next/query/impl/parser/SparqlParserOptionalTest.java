package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SparqlParserOptionalTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Should parse a basic SELECT With Optional triples")
    public void shouldParseBasicSelectWithOptionalTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?child ?childLabel ?genderLabel ?birth_date ?date_of_death
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
    public void shouldParseBasicSelectWithOptionalBlockTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?child ?childLabel ?genderLabel ?birth_date ?date_of_death
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
