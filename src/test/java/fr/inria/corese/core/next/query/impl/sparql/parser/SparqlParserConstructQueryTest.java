package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserConstructQueryTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Should parse a basic CONSTRUCT query")
    void shouldParseBasicConstructQuery() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            CONSTRUCT {
                ?s ?p ?o
            }
            WHERE {
                ?s ?p ?o
            }
            """);

        assertInstanceOf(ConstructQueryAst.class, ast);

        ConstructQueryAst construct = (ConstructQueryAst) ast;

        assertNotNull(construct.constructTemplate());
        assertNotNull(construct.whereClause());

        assertEquals(1, construct.constructTemplate().triplePatternAsts().size());

        TriplePatternAst templateTriple = construct.constructTemplate().triplePatternAsts().getFirst();
        assertInstanceOf(VarAst.class, templateTriple.subject());
        assertInstanceOf(VarAst.class, simplePredicateTerm(templateTriple));
        assertInstanceOf(VarAst.class, templateTriple.object());

        GroupGraphPatternAst where = construct.whereClause();
        assertEquals(1, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().getFirst());
    }

    @Test
    @DisplayName("Should parse a template with Blank Nodes")
    void shouldParseATemplateWithBlankNodes() {
        SparqlParser sparqlParser = newParserDefault();

        QueryAst ast = sparqlParser.parse("""
                PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
                PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>

                CONSTRUCT { ?x  vcard:N _:v .
                            _:v vcard:givenName ?gname .
                            _:v vcard:familyName ?fname }
                WHERE
                 {
                    { ?x foaf:firstname ?gname } UNION  { ?x foaf:givenname   ?gname } .
                    { ?x foaf:surname   ?fname } UNION  { ?x foaf:family_name ?fname } .
                 }
                """);

        assertInstanceOf(ConstructQueryAst.class, ast);
        ConstructQueryAst construct = (ConstructQueryAst) ast;

        assertNotNull(construct.constructTemplate());
        assertNotNull(construct.whereClause());
        assertTemplateWithBlankNodes(construct.constructTemplate());
        assertWhereContainsTwoUnions(construct.whereClause());
    }

    @Test
    @DisplayName("Should parse CONSTRUCT solution modifiers")
    void shouldParseConstructSolutionModifiers() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            CONSTRUCT {
                ?s ?p ?o
            }
            WHERE {
                ?s ?p ?o
            }
            ORDER BY DESC(?o)
            LIMIT 10
            OFFSET 5
            """);

        assertInstanceOf(ConstructQueryAst.class, ast);

        ConstructQueryAst construct = (ConstructQueryAst) ast;
        SolutionModifierAst solutionModifier = construct.solutionModifier();

        assertNotNull(solutionModifier);
        assertTrue(solutionModifier.groupBy().isEmpty());
        assertEquals(1, solutionModifier.orderBy().size());
        assertEquals(ASTConstants.OrderDirection.DESC, solutionModifier.orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class, solutionModifier.orderBy().getFirst().expression());
        assertEquals("o", ((VarAst) solutionModifier.orderBy().getFirst().expression()).name());
        assertEquals(10L, solutionModifier.limit());
        assertEquals(5L, solutionModifier.offset());
    }

    private void assertTemplateWithBlankNodes(ConstructTemplateAst template) {
        assertEquals(3, template.triplePatternAsts().size());

        TriplePatternAst firstTriple = template.triplePatternAsts().get(0);
        assertInstanceOf(VarAst.class, firstTriple.subject());
        assertInstanceOf(IriAst.class, simplePredicateTerm(firstTriple));
        assertInstanceOf(IriAst.class, firstTriple.object());
        assertEquals("x", ((VarAst) firstTriple.subject()).name());
        assertEquals("vcard:N", ((IriAst) simplePredicateTerm(firstTriple)).raw());
        assertTrue(((IriAst) firstTriple.object()).raw().startsWith("_:"), "object should be a blank node");

        TriplePatternAst secondTriple = template.triplePatternAsts().get(1);
        assertInstanceOf(IriAst.class, secondTriple.subject());
        assertInstanceOf(IriAst.class, simplePredicateTerm(secondTriple));
        assertInstanceOf(VarAst.class, secondTriple.object());
        assertTrue(((IriAst) secondTriple.subject()).raw().startsWith("_:"), "subject should be a blank node");
        assertEquals("vcard:givenName", ((IriAst) simplePredicateTerm(secondTriple)).raw());
        assertEquals("gname", ((VarAst) secondTriple.object()).name());

        TriplePatternAst thirdTriple = template.triplePatternAsts().get(2);
        assertInstanceOf(IriAst.class, thirdTriple.subject());
        assertInstanceOf(IriAst.class, simplePredicateTerm(thirdTriple));
        assertInstanceOf(VarAst.class, thirdTriple.object());
        assertTrue(((IriAst) thirdTriple.subject()).raw().startsWith("_:"), "subject should be a blank node");
        assertEquals("vcard:familyName", ((IriAst) simplePredicateTerm(thirdTriple)).raw());
        assertEquals("fname", ((VarAst) thirdTriple.object()).name());
    }

    private void assertWhereContainsTwoUnions(GroupGraphPatternAst whereClause) {
        assertEquals(2, whereClause.patterns().size());
        assertInstanceOf(UnionAst.class, whereClause.patterns().get(0));
        assertInstanceOf(UnionAst.class, whereClause.patterns().get(1));

        UnionAst firstUnion = (UnionAst) whereClause.patterns().get(0);
        UnionAst secondUnion = (UnionAst) whereClause.patterns().get(1);
        assertNotNull(firstUnion.left());
        assertNotNull(firstUnion.right());
        assertNotNull(secondUnion.left());
        assertNotNull(secondUnion.right());
    }
}
