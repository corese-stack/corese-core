package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DESCRIBE query parsing via {@link SparqlParser}.
 */
class SparqlParserDescribeTest extends AbstractSparqlParserFeatureTest {

    private SparqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SparqlParser();
    }



    @Nested
    @DisplayName("DESCRIBE ?s WHERE { ?s ?p ?o }")
    class DescribeVariableWithWhere {

        @Test
        @DisplayName("Produces a DescribeQueryAst")
        void producesDescribeQueryAst() {
            QueryAst result = parser.parse("DESCRIBE ?s WHERE { ?s ?p ?o }");
            assertInstanceOf(DescribeQueryAst.class, result);
        }

        @Test
        @DisplayName("Described list contains one variable")
        void describedListContainsOneVariable() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s WHERE { ?s ?p ?o }");

            assertEquals(1, result.described().size());
            assertInstanceOf(VarAst.class, result.described().getFirst());
        }

        @Test
        @DisplayName("Variable name is 's'")
        void variableNameIsS() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s WHERE { ?s ?p ?o }");

            VarAst describedVar = (VarAst) result.described().getFirst();
            assertEquals("s", describedVar.name());
        }

        @Test
        @DisplayName("WHERE clause contains one BGP")
        void whereClauseContainsOneBgp() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s WHERE { ?s ?p ?o }");

            assertEquals(1, result.whereClause().patterns().size());
            assertInstanceOf(BgpAst.class, result.whereClause().patterns().getFirst());
        }

        @Test
        @DisplayName("BGP contains one triple ?s ?p ?o")
        void bgpContainsOneTriple() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s WHERE { ?s ?p ?o }");

            BgpAst bgp = (BgpAst) result.whereClause().patterns().getFirst();
            assertEquals(1, bgp.triples().size());

            TriplePatternAst triple = bgp.triples().getFirst();
            assertEquals("s", ((VarAst) triple.subject()).name());
            assertEquals("p", ((VarAst) simplePredicateTerm(triple)).name());
            assertEquals("o", ((VarAst) triple.object()).name());
        }
    }

    @Nested
    @DisplayName("DESCRIBE * WHERE { ?s ?p ?o }")
    class DescribeAll {

        @Test
        @DisplayName("isDescribeAll() is true")
        void isDescribeAllIsTrue() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE * WHERE { ?s ?p ?o }");

            assertTrue(result.isDescribeAll());
        }

        @Test
        @DisplayName("described list is empty")
        void describedListIsEmpty() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE * WHERE { ?s ?p ?o }");

            assertTrue(result.described().isEmpty());
        }

        @Test
        @DisplayName("WHERE clause contains a BGP")
        void whereClauseContainsBgp() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE * WHERE { ?s ?p ?o }");

            assertFalse(result.whereClause().patterns().isEmpty());
            assertInstanceOf(BgpAst.class, result.whereClause().patterns().getFirst());
        }
    }

    @Nested
    @DisplayName("DESCRIBE ?s ?p WHERE { ?s ?p ?o }")
    class DescribeMultipleVariables {

        @Test
        @DisplayName("Described list contains two variables")
        void describedListContainsTwoVariables() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s ?p WHERE { ?s ?p ?o }");

            assertEquals(2, result.described().size());
        }

        @Test
        @DisplayName("Variable names are 's' and 'p' in order")
        void variableNamesAreInOrder() {
            DescribeQueryAst result = (DescribeQueryAst) parser.parse(
                    "DESCRIBE ?s ?p WHERE { ?s ?p ?o }");

            assertEquals("s", ((VarAst) result.described().get(0)).name());
            assertEquals("p", ((VarAst) result.described().get(1)).name());
        }
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    void fromGraphs() {
        SparqlParser queryParser = newParserDefault();
        String commentedQuery = """
                DESCRIBE ?s
                FROM <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        SparqlQueryAst queryAst = (SparqlQueryAst) queryParser.parse(commentedQuery);
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
    void fromMultipleGraphs() {
        SparqlParser queryParser = newParserDefault();
        String commentedQuery = """
                DESCRIBE ?s
                FROM <http://ns.inria.fr/graph1>
                FROM <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        SparqlQueryAst queryAst = (SparqlQueryAst) queryParser.parse(commentedQuery);
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
    void fromNamedGraphs() {
        SparqlParser queryParser = newParserDefault();
        String commentedQuery = """
                DESCRIBE ?s
                FROM NAMED <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        SparqlQueryAst queryAst = (SparqlQueryAst) queryParser.parse(commentedQuery);
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
    void fromMultipleNamedGraphs() {
        SparqlParser queryParser = newParserDefault();
        String commentedQuery = """
                DESCRIBE ?s
                FROM NAMED <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM NAMED <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        SparqlQueryAst queryAst = (SparqlQueryAst) queryParser.parse(commentedQuery);
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
    void fromMultipleMixedGraphs() {
        SparqlParser queryParser = newParserDefault();
        String commentedQuery = """
                DESCRIBE ?s
                FROM <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                }
               """;
        SparqlQueryAst queryAst = (SparqlQueryAst) queryParser.parse(commentedQuery);
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
    @DisplayName("DESCRIBE should parse solution modifiers")
    void describeShouldParseSolutionModifiers() {
        QueryAst ast = parser.parse("""
                DESCRIBE ?s
                WHERE {
                    ?s ?p ?o
                }
                ORDER BY ?o
                LIMIT 10
                OFFSET 5
                """);

        assertInstanceOf(DescribeQueryAst.class, ast);
        DescribeQueryAst describe = (DescribeQueryAst) ast;

        assertNotNull(describe.solutionModifier());
        assertEquals(1, describe.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, describe.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class, describe.solutionModifier().orderBy().getFirst().expression());
        assertEquals("o", ((VarAst) describe.solutionModifier().orderBy().getFirst().expression()).name());
        assertEquals(10L, describe.solutionModifier().limit());
        assertEquals(5L, describe.solutionModifier().offset());
    }
}
