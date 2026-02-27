package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test if we parse the Basic Graph Pattern (BGP)
 */
public class SparqlParserBgpTest {

    /**
     * create a Default Sparql Parser with default Config
     * @return SparqlParser
     */
    private SparqlParser newParserDefault() {
        return new SparqlParser(new SparqlParserOptions.Builder().build());
    }

    /**
     *  create a Sparql Parser with Option
     * @param failFast option to make the Parsing fail if it encountered an error
     * @param collectErrors option to collect error encountered when parsing
     * @return SparqlParser
     */
    private SparqlParser newParser(boolean failFast, boolean collectErrors) {
        return new SparqlParser(new SparqlParserOptions.Builder()
                .failFast(failFast)
                .collectErrors(collectErrors)
                .build());
    }

    @Test
    void shouldParseSingleTriplePatternInBgp() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size(), "WHERE should contain 1 pattern (BGP)");

        PatternAst p0 = where.patterns().getFirst();
        assertInstanceOf(BgpAst.class, p0, "First pattern should be a BGP");

        BgpAst bgp = (BgpAst) p0;
        assertEquals(1, bgp.triples().size(), "BGP should contain 1 triple");

        TriplePatternAst t = bgp.triples().getFirst();

        assertInstanceOf(VarAst.class, t.subject());
        assertInstanceOf(VarAst.class, t.predicate());
        assertInstanceOf(VarAst.class, t.object());

        assertEquals("s", ((VarAst) t.subject()).name());
        assertEquals("p", ((VarAst) t.predicate()).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }

    @Test
    void shouldParsePropertyListSemicolonAndCommaIntoMultipleTriples() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT * WHERE {
                  ?s a foaf:Person ;
                     foaf:name ?n , ?n2 .
                }
                """);

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size());

        BgpAst bgp = (BgpAst) where.patterns().getFirst();

        // Expected triples:
        // (?s, a, foaf:Person)
        // (?s, foaf:name, ?n)
        // (?s, foaf:name, ?n2)
        assertEquals(3, bgp.triples().size());

        TriplePatternAst t0 = bgp.triples().getFirst();
        assertEquals("s", ((VarAst) t0.subject()).name());
        assertInstanceOf(IriAst.class, t0.predicate());
        assertEquals("a", ((IriAst) t0.predicate()).raw());

        TriplePatternAst t1 = bgp.triples().get(1);
        assertEquals("s", ((VarAst) t1.subject()).name());
        assertInstanceOf(IriAst.class, t1.predicate());
        assertEquals("foaf:name", ((IriAst) t1.predicate()).raw());
        assertEquals("n", ((VarAst) t1.object()).name());

        TriplePatternAst t2 = bgp.triples().get(2);
        assertEquals("s", ((VarAst) t2.subject()).name());
        assertEquals("foaf:name", ((IriAst) t2.predicate()).raw());
        assertEquals("n2", ((VarAst) t2.object()).name());
    }

    @Test
    void shouldParseRdfLiteralWithLanguageTag() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example/p> "salut"@fr .
                }
                """);

        BgpAst bgp = (BgpAst) ast.whereClause().patterns().get(0);
        assertEquals(1, bgp.triples().size());

        TriplePatternAst t = bgp.triples().getFirst();
        assertInstanceOf(LiteralAst.class, t.object());

        LiteralAst lit = (LiteralAst) t.object();
        assertEquals("\"salut\"", lit.lexical()); // tu conserves les guillemets (string_().getText())
        assertEquals("fr", lit.lang());
        assertNull(lit.datatype());
    }

    @Test
    void shouldParseRdfLiteralWithDatatype() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                SELECT * WHERE {
                  ?s <http://example/p> "12"^^xsd:integer .
                }
                """);

        BgpAst bgp = (BgpAst) ast.whereClause().patterns().getFirst();
        TriplePatternAst t = bgp.triples().getFirst();

        assertInstanceOf(LiteralAst.class, t.object());
        LiteralAst lit = (LiteralAst) t.object();

        assertEquals("\"12\"", lit.lexical());
        assertNull(lit.lang());
        assertEquals("xsd:integer", lit.datatype());
    }

    @Test
    void failFastTrueShouldThrowQuerySyntaxExceptionOnInvalidQuery() {
        SparqlParser parser = newParser(true, true);

        // Syntax error: missing object after predicate
        assertThrows(QuerySyntaxException.class, () ->
                parser.parse("SELECT * WHERE { ?s ?p . }")
        );
    }

    @Test
    void failFastFalseCollectErrorsTrueShouldThrowQueryExceptionWithCollectedErrors() {
        SparqlParser parser = newParser(false, true);

        QueryException ex = assertThrows(QueryException.class, () ->
                parser.parse("SELECT * WHERE { ?s ?p . }")
        );

        assertTrue(ex.getMessage().contains("Syntax error"), "Should contain syntax error message");

    }

    @Test
    void shouldParseEmptyWhereGroup() {
        SparqlParser parser = newParserDefault();
        QueryAst ast = parser.parse("SELECT * WHERE { }");

        assertNotNull(ast.whereClause());
        // selon ton builder: TriplesBlock absent => pas de BgpAst, donc patterns vide
        assertEquals(0, ast.whereClause().patterns().size());
    }

}
