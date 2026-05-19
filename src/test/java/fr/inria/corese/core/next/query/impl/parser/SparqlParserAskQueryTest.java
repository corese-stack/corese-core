package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlParserAskQueryTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("should parse a basic ASK WHERE {?s ?p ?o} query")
    public void shouldParseBasicAskQueryTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                ASK WHERE {
                  ?s ?p ?o .
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
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
    @DisplayName("should parse a short form ASK {uri ?p ?o} query")
    public void shouldParseShortAskQueryTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                ASK {
                  <https://ns.inria.fr/corese> ?p ?o .
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size(), "WHERE clause should contain 1 pattern (BGP)");

        PatternAst p0 = where.patterns().getFirst();
        assertInstanceOf(BgpAst.class, p0, "First pattern should be a BGP");

        BgpAst bgp = (BgpAst) p0;
        assertEquals(1, bgp.triples().size(), "BGP should contain 1 triple");

        TriplePatternAst t = bgp.triples().getFirst();

        assertInstanceOf(IriAst.class, t.subject());
        assertInstanceOf(VarAst.class, t.predicate());
        assertInstanceOf(VarAst.class, t.object());

        assertEquals("<https://ns.inria.fr/corese>", ((IriAst)t.subject()).raw());
        assertEquals("p", ((VarAst) t.predicate()).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }

    @Test
    @DisplayName("Insertion of a comment before the query should not change the treatment of the query")
    public void shouldIgnoreCommentBeforeQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                # This is a test comment
                ASK {
                    ?s a ?c ;
                        ?p ?o .
                }
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("<" + RDF.type.getIRI().stringValue() + ">", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }

    @Test
    @DisplayName("Insertion of a comment after the query should not change the treatment of the query")
    public void shouldIgnoreCommentAfterQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK {
                    ?s a ?c ;
                        ?p ?o .
                }
                # This is a test comment
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("<" + RDF.type.getIRI().stringValue() + ">", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }

    @Test
    @DisplayName("Insertion of a comment in the middle of the query should not change the treatment of the query")
    public void shouldIgnoreCommentInTheMiddleOfQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK {
                    ?s a ?c ;
                # This is a test comment
                        ?p ?o .
                }
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("<" + RDF.type.getIRI().stringValue() + ">", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK
                FROM <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(1, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(0, queryAst.datasetClause().namedGraphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK
                FROM <http://ns.inria.fr/graph1>
                FROM <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(3, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[1]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[2]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(0, queryAst.datasetClause().namedGraphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromNamedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK
                FROM NAMED <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(1, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(0, queryAst.datasetClause().graphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleNamedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK
                FROM NAMED <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM NAMED <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(3, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[1]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[2]);
        assertEquals(0, queryAst.datasetClause().graphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleMixedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK
                FROM <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(2, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[1]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(1, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
    }

    @Test
    @DisplayName("should parse ASK with a SELECT subquery inside WHERE")
    public void shouldParseAskWithSubqueryInWhere() {
        SparqlParser parser = newParserDefault();
        QueryAst ast = parser.parse("""
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                ASK WHERE {
                  {
                    SELECT ?s WHERE {
                      ?s a foaf:Person
                    }
                  }
                }
                """);

        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst ask = (AskQueryAst) ast;
        GroupGraphPatternAst outerWhere = ask.whereClause();

        assertEquals(1, outerWhere.patterns().size());
        assertInstanceOf(GroupGraphPatternAst.class, outerWhere.patterns().getFirst());

        GroupGraphPatternAst wrapperGroup = (GroupGraphPatternAst) outerWhere.patterns().getFirst();
        assertEquals(1, wrapperGroup.patterns().size());
        assertInstanceOf(SubQueryAst.class, wrapperGroup.patterns().getFirst());

        SubQueryAst subQuery = (SubQueryAst) wrapperGroup.patterns().getFirst();
        assertInstanceOf(SelectQueryAst.class, subQuery.query());

        SelectQueryAst inner = (SelectQueryAst) subQuery.query();
        assertNotNull(inner.whereClause());
        assertEquals(1, inner.whereClause().patterns().size());
        assertInstanceOf(BgpAst.class, inner.whereClause().patterns().getFirst());
    }

    @Test
    @DisplayName("Ask short form with limit")
    public void shortformWithLimit() {
        SparqlParser parser = newParserDefault();
        String query = """
                ASK  {
                    ?s ?p ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(query);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertInstanceOf(AskQueryAst.class, queryAst);
        AskQueryAst askQueryAst = (AskQueryAst) queryAst;
        assertNotNull(askQueryAst.solutionModifier());
        assertEquals(10L, askQueryAst.solutionModifier().limit());
    }
}
