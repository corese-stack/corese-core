package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SPARQL {@code SERVICE} graph pattern parsing.
 *
 * <p>Exercises the full pipeline: ANTLR grammar → {@code SparqlListener} → {@code ServiceFeature}
 * → {@code SparqlAstBuilder} → {@link ServiceAst}.
 */
class SparqlParserServiceTest extends AbstractSparqlParserFeatureTest {

    @Nested
    @DisplayName("Basic SERVICE with IRI endpoint")
    class BasicServiceIri {

        @Test
        @DisplayName("Should parse SERVICE with a plain IRI endpoint")
        void shouldParseServiceWithIriEndpoint() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://dbpedia.org/sparql> {
                        ?s a <http://dbpedia.org/ontology/Person> .
                      }
                    }
                    """);

            assertNotNull(ast);
            assertInstanceOf(SelectQueryAst.class, ast);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(1, where.patterns().size());

            assertInstanceOf(ServiceAst.class, where.patterns().getFirst());

            ServiceAst service = (ServiceAst) where.patterns().getFirst();
            assertFalse(service.silent(), "SILENT must be false when keyword absent");
            assertInstanceOf(IriAst.class, service.endpoint());
            assertEquals("<https://dbpedia.org/sparql>", ((IriAst) service.endpoint()).raw());
        }

        @Test
        @DisplayName("Service body should contain a BGP with the triple pattern")
        void serviceBodyShouldContainBgp() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://dbpedia.org/sparql> {
                        ?s a <http://dbpedia.org/ontology/Person> .
                      }
                    }
                    """);

            ServiceAst service = (ServiceAst) ast.whereClause().patterns().getFirst();
            GroupGraphPatternAst body = service.pattern();
            assertEquals(1, body.patterns().size());
            assertInstanceOf(BgpAst.class, body.patterns().getFirst());

            BgpAst bgp = (BgpAst) body.patterns().getFirst();
            assertEquals(1, bgp.triples().size());
            assertEquals(new VarAst("s"), bgp.triples().getFirst().subject());
            assertEquals(new IriAst("<" + RDF.type.getIRI().stringValue() + ">"), bgp.triples().getFirst().predicate());
            assertEquals(new IriAst("<http://dbpedia.org/ontology/Person>"), bgp.triples().getFirst().object());
        }
    }

    @Nested
    @DisplayName("SERVICE SILENT")
    class ServiceSilent {

        @Test
        @DisplayName("SILENT keyword sets the silent flag to true")
        void shouldParseSilentService() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE SILENT <https://example.org/sparql> {
                        ?s ?p ?o .
                      }
                    }
                    """);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(1, where.patterns().size());
            assertInstanceOf(ServiceAst.class, where.patterns().getFirst());

            ServiceAst service = (ServiceAst) where.patterns().getFirst();
            assertTrue(service.silent(), "SILENT keyword must set silent=true");
        }

        @Test
        @DisplayName("SERVICE without SILENT keeps silent=false")
        void shouldParseNonSilentService() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://example.org/sparql> {
                        ?s ?p ?o .
                      }
                    }
                    """);

            ServiceAst service = (ServiceAst) ast.whereClause().patterns().getFirst();
            assertFalse(service.silent());
        }
    }

    @Nested
    @DisplayName("SERVICE with variable endpoint")
    class ServiceVariable {

        @Test
        @DisplayName("SERVICE with a variable endpoint is parsed correctly")
        void shouldParseServiceWithVariableEndpoint() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      ?endpoint a <http://example.org/Service> .
                      SERVICE ?endpoint {
                        ?s ?p ?o .
                      }
                    }
                    """);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(2, where.patterns().size());
            assertInstanceOf(BgpAst.class, where.patterns().get(0));
            assertInstanceOf(ServiceAst.class, where.patterns().get(1));

            ServiceAst service = (ServiceAst) where.patterns().get(1);
            assertInstanceOf(VarAst.class, service.endpoint());
            assertEquals("endpoint", ((VarAst) service.endpoint()).name());
        }
    }

    @Nested
    @DisplayName("SERVICE mixed with other patterns")
    class ServiceMixed {

        @Test
        @DisplayName("BGP before and after SERVICE are preserved in order")
        void bgpBeforeAndAfterService() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      ?s a <http://example.org/Type> .
                      SERVICE <https://remote.org/sparql> {
                        ?s <http://example.org/prop> ?val .
                      }
                      FILTER(?val > 0)
                    }
                    """);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(3, where.patterns().size());
            assertInstanceOf(BgpAst.class, where.patterns().get(0));
            assertInstanceOf(ServiceAst.class, where.patterns().get(1));
            assertInstanceOf(FilterAst.class, where.patterns().get(2));
        }

        @Test
        @DisplayName("SERVICE combined with OPTIONAL in the same WHERE clause")
        void serviceAndOptional() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://remote.org/sparql> {
                        ?s a <http://xmlns.com/foaf/0.1/Person> .
                      }
                      OPTIONAL { ?s <http://xmlns.com/foaf/0.1/name> ?name . }
                    }
                    """);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(2, where.patterns().size());
            assertInstanceOf(ServiceAst.class, where.patterns().get(0));
            assertInstanceOf(OptionalAst.class, where.patterns().get(1));
        }

        @Test
        @DisplayName("Nested SERVICE inside OPTIONAL is handled")
        void nestedServiceInsideOptional() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      ?s a <http://example.org/Type> .
                      OPTIONAL {
                        SERVICE <https://remote.org/sparql> {
                          ?s <http://example.org/prop> ?val .
                        }
                      }
                    }
                    """);

            GroupGraphPatternAst where = ast.whereClause();
            assertEquals(2, where.patterns().size());
            assertInstanceOf(BgpAst.class, where.patterns().get(0));

            OptionalAst optional = (OptionalAst) where.patterns().get(1);
            GroupGraphPatternAst optionalBody = (GroupGraphPatternAst) optional.ast();
            assertEquals(1, optionalBody.patterns().size());
            assertInstanceOf(ServiceAst.class, optionalBody.patterns().getFirst());
        }
    }

    @Nested
    @DisplayName("SERVICE with multiple triples in body")
    class ServiceMultipleTriples {

        @Test
        @DisplayName("SERVICE body with multiple triples in the same BGP")
        void multipleTriplesSameBgp() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://dbpedia.org/sparql> {
                        ?s a <http://dbpedia.org/ontology/Person> ;
                           <http://dbpedia.org/ontology/birthDate> ?date .
                      }
                    }
                    """);

            ServiceAst service = (ServiceAst) ast.whereClause().patterns().getFirst();
            GroupGraphPatternAst body = service.pattern();
            BgpAst bgp = (BgpAst) body.patterns().getFirst();
            assertEquals(2, bgp.triples().size());
        }

        @Test
        @DisplayName("SERVICE body with FILTER is parsed correctly")
        void serviceBodyWithFilter() {
            SparqlParser parser = newParserDefault();

            QueryAst ast = parser.parse("""
                    SELECT *
                    WHERE {
                      SERVICE <https://remote.org/sparql> {
                        ?s <http://example.org/age> ?age .
                        FILTER(?age > 18)
                      }
                    }
                    """);

            ServiceAst service = (ServiceAst) ast.whereClause().patterns().getFirst();
            GroupGraphPatternAst body = service.pattern();
            assertEquals(2, body.patterns().size());
            assertInstanceOf(BgpAst.class, body.patterns().get(0));
            assertInstanceOf(FilterAst.class, body.patterns().get(1));
        }
    }
}
