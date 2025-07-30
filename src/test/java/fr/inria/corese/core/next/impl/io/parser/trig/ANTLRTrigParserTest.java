package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the ANTLRTrigParser class.
 * These tests verify the parser's ability to correctly parse Trig
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
class ANTLRTrigParserTest {

    private static final Logger logger = LoggerFactory.getLogger(ANTLRTrigParserTest.class);

    /**
     * helper method to parse trig data into corese model
     *
     * @param trigData a string of rdf data in trig format
     * @param baseURI the base uri
     * @return Corese rdf model
     * @throws Exception
     */

    private Model parseFromString(String trigData, String baseURI) throws Exception {
        Model model = new CoreseModel();
        ValueFactory factory = new CoreseAdaptedValueFactory();
        RDFParser parser = new ANTLRTrigParser(model, factory);
        parser.parse(new StringReader(trigData), baseURI);
        return model;
    }

    /**
     * Helper method to print the model.
     * @param model
     */
    private void printModel(Model model) {
        model.stream().forEach(stmt -> {
            Value obj = stmt.getObject();
            String subjectString = stmt.getSubject().stringValue();
            String predicateString = stmt.getPredicate().stringValue();

            if (obj instanceof Literal literal) {
                String label = String.valueOf(literal.getLabel());
                String languageTag = literal.getLanguage().orElse(null);

                if (languageTag != null) {
                    logger.debug("({}, {}, \"{}\"@{})",
                            subjectString,
                            predicateString,
                            label,
                            languageTag);
                } else {
                    logger.debug("({}, {}, \"{}\")",
                            subjectString,
                            predicateString,
                            label);
                }
            } else {
                logger.debug("({}, {}, {})",
                        subjectString,
                        predicateString,
                        obj.stringValue());
            }
        });
    }

    @Test
    void testNamedGraphParsing() throws Exception {
        String trig = """
                @prefix ex: <http://example.org/> 
                ex:Graph1 {
                  ex:Alice ex:knows ex:Bob .
                }""".trim();

        Model model = parseFromString(trig, null);
        printModel(model);
        assertEquals(1, model.size());

        assertEquals(1, model.getNamespaces().size());

        assertEquals(1, model.contexts().size());
    }

    @Test
    void testDocumentThatContainsOneGraphExample1() throws Exception {
        String trig = """
                # This document encodes one graph.
                @prefix ex: <http://www.example.org/vocabulary#> .
                @prefix : <http://www.example.org/exampleDocument#> .
                
                :G1 { :Monica a ex:Person ;
                              ex:name "Monica Murphy" ;
                              ex:homepage <http://www.monicamurphy.org> ;
                              ex:email <mailto:monica@monicamurphy.org> ;
                              ex:hasSkill ex:Management ,
                                          ex:Programming . }
                """.trim();

        Model model = parseFromString(trig, null);
        printModel(model);

        assertEquals(6, model.size());

        assertEquals(2, model.getNamespaces().size());

        assertEquals(1, model.contexts().size());
    }

    @Test
    void testDocumentThatContainsTwoGraphExample() throws Exception {
        String trig = """
                # This document contains a same data as the
                # previous example.
                
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix dc: <http://purl.org/dc/terms/> .
                @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                
                # default graph - no {} used.
                <http://example.org/bob> dc:publisher "Bob" .
                <http://example.org/alice> dc:publisher "Alice" .
                
                # GRAPH keyword to highlight a named graph
                # Abbreviation of triples using ;
                GRAPH <http://example.org/bob>
                {
                   [] foaf:name "Bob" ;
                      foaf:mbox <mailto:bob@oldcorp.example.org> ;
                      foaf:knows _:b .
                }
                
                GRAPH <http://example.org/alice>
                {
                    _:b foaf:name "Alice" ;
                        foaf:mbox <mailto:alice@work.example.org>
                }
                """.trim();

        Model model = parseFromString(trig, null);
        printModel(model);

        assertEquals(7, model.size());

        assertEquals(3, model.getNamespaces().size());

        assertEquals(3, model.contexts().size());
    }

    @Test
    void testNestedBlankNodesWithSharedIdentifiers() throws Exception {
        String trig = """
                @prefix ex: <http://example.org/> .
                
                GRAPH ex:graph1 {
                  ex:Alice ex:knows [
                    ex:name "Bob" ;
                    ex:knows [
                      ex:name "Charlie"
                    ]
                  ] ;
                  ex:email "alice@example.org" .
                }
                """.trim();
        Model model = parseFromString(trig, null);
        printModel(model);

        assertEquals(5, model.size());

    }
}