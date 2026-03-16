package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserConstructQueryTest extends AbstractSparqlParserFeatureTest {

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
        assertInstanceOf(VarAst.class, templateTriple.predicate());
        assertInstanceOf(VarAst.class, templateTriple.object());

        GroupGraphPatternAst where = construct.whereClause();
        assertEquals(1, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().getFirst());
    }

    @Test
    @DisplayName("Should parse a template with Blank Nodes")
    void shouldParseATemplateWithBlankNodes() {
        SparqlParser parse = newParserDefault();

        QueryAst ast = parse.parse("""
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

        // --- Template: 3 triples (?x vcard:N _:v . _:v vcard:givenName ?gname . _:v vcard:familyName ?fname)
        ConstructTemplateAst template = construct.constructTemplate();
        assertEquals(3, template.triplePatternAsts().size());

        TriplePatternAst t0 = template.triplePatternAsts().get(0);
        assertInstanceOf(VarAst.class, t0.subject());
        assertInstanceOf(IriAst.class, t0.predicate());
        assertInstanceOf(IriAst.class, t0.object());
        assertEquals("x", ((VarAst) t0.subject()).name());
        assertEquals("vcard:N", ((IriAst) t0.predicate()).raw());
        assertTrue(((IriAst) t0.object()).raw().startsWith("_:"), "object should be a blank node");

        TriplePatternAst t1 = template.triplePatternAsts().get(1);
        assertInstanceOf(IriAst.class, t1.subject());
        assertInstanceOf(IriAst.class, t1.predicate());
        assertInstanceOf(VarAst.class, t1.object());
        assertTrue(((IriAst) t1.subject()).raw().startsWith("_:"), "subject should be a blank node");
        assertEquals("vcard:givenName", ((IriAst) t1.predicate()).raw());
        assertEquals("gname", ((VarAst) t1.object()).name());

        TriplePatternAst t2 = template.triplePatternAsts().get(2);
        assertInstanceOf(IriAst.class, t2.subject());
        assertInstanceOf(IriAst.class, t2.predicate());
        assertInstanceOf(VarAst.class, t2.object());
        assertTrue(((IriAst) t2.subject()).raw().startsWith("_:"), "subject should be a blank node");
        assertEquals("vcard:familyName", ((IriAst) t2.predicate()).raw());
        assertEquals("fname", ((VarAst) t2.object()).name());

        // --- WHERE: two UNION patterns
        GroupGraphPatternAst where = construct.whereClause();
        assertEquals(2, where.patterns().size());
        assertInstanceOf(UnionAst.class, where.patterns().get(0));
        assertInstanceOf(UnionAst.class, where.patterns().get(1));

        UnionAst union1 = (UnionAst) where.patterns().get(0);
        UnionAst union2 = (UnionAst) where.patterns().get(1);
        assertNotNull(union1.left());
        assertNotNull(union1.right());
        assertNotNull(union2.left());
        assertNotNull(union2.right());
    }
}
