package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the RDFXMLParser class.
 * These tests verify the parser's ability to correctly parse RDF/XML
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
class RDFXMLParserTest extends ParserTestBase {
    private static final Logger logger = LoggerFactory.getLogger(RDFXMLParserTest.class);

    /**
     * Helper method to parse an RDF/XML string into an RDF model.
     *
     * @param rdfXml The RDF/XML content as a string.
     * @return The resulting {@link Model} populated by the parser.
     * @throws Exception if an error occurs during parsing or I/O.
     */
    private Model parseRdfXml(String rdfXml) throws Exception {
        Model coreseModel = createTestModel();

        try (InputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8))) {
            RDFXMLParser parser = new RDFXMLParser(coreseModel, valueFactory);
            parser.parse(inputStream);
        }
        return coreseModel;
    }

    /**
     * Helper method to print the model.
     *
     * @param model The model to print.
     */
    private void printModel(Model model) {
        model.forEach(stmt -> {
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

    /**
     * Tests node elements with IRIs.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    void testNodeElementsWithIRIs() throws Exception {
        String rdfXml = """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/">
                <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                  <ex:editor>
                    <rdf:Description>
                      <ex:homePage>
                        <rdf:Description rdf:about="http://purl.org/net/dajobe/">
                        </rdf:Description>
                      </ex:homePage>
                    </rdf:Description>
                  </ex:editor>
                </rdf:Description>
                </rdf:RDF>
                """;

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(2, model.size(), "Expected two RDF statements");
    }

    /**
     * Tests a basic RDF/XML file.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    void testBasicRdfParsing() throws Exception {
        String rdfXml = """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/">
                  <rdf:Description rdf:about="http://www.example.org/index.html">
                    <ex:creator>John Smith</ex:creator>
                    <ex:date>2025-07-07</ex:date>
                  </rdf:Description>
                </rdf:RDF>
                """;
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(2, model.size(), "Expected two RDF statements");
    }

    /**
     * Tests a RDF/XML file with complete description of all graph paths.
     *
     * @throws Exception If parsing fails.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRdfXmlW3CExamples")
    void testRdfXmlW3CExamples(String exampleName, String rdfXml, int expectedSize) throws Exception {
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(expectedSize, model.size(), "Statement count mismatch for " + exampleName);
    }

    private static Stream<Arguments> provideRdfXmlW3CExamples() {
        return Stream.of(
            Arguments.of(
                "Example 3: Complete description of all graph paths",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/"
                         xmlns:dc="http://purl.org/dc/terms/">
                    <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                      <ex:editor>
                        <rdf:Description>
                          <ex:homePage>
                            <rdf:Description rdf:about="http://purl.org/net/dajobe/">
                            </rdf:Description>
                          </ex:homePage>
                        </rdf:Description>
                      </ex:editor>
                    </rdf:Description>
                    <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                      <ex:editor>
                        <rdf:Description>
                          <ex:fullName>Dave Beckett</ex:fullName>
                        </rdf:Description>
                      </ex:editor>
                    </rdf:Description>
                    <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                      <dc:title>RDF 1.2 XML Syntax</dc:title>
                    </rdf:Description>
                </rdf:RDF>
                """.trim(),
                5
            ),
            Arguments.of(
                "Example 4: Using multiple property elements",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/"
                         xmlns:dc="http://purl.org/dc/terms/">
                <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                  <ex:editor>
                    <rdf:Description>
                      <ex:homePage>
                        <rdf:Description rdf:about="http://purl.org/net/dajobe/">
                        </rdf:Description>
                      </ex:homePage>
                      <ex:fullName>Dave Beckett</ex:fullName>
                    </rdf:Description>
                  </ex:editor>
                  <dc:title>RDF 1.2 XML Syntax</dc:title>
                </rdf:Description>
                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 5: Empty property elements",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/"
                         xmlns:dc="http://purl.org/dc/terms/">
                <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                  <ex:editor>
                    <rdf:Description>
                      <ex:homePage rdf:resource="http://purl.org/net/dajobe/"/>
                      <ex:fullName>Dave Beckett</ex:fullName>
                    </rdf:Description>
                  </ex:editor>
                  <dc:title>RDF 1.2 XML Syntax</dc:title>
                </rdf:Description>
                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 6: Replacing property elements with string literal content into property attributes",
                """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/"
                         xmlns:dc="http://purl.org/dc/terms/">
                <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
                           dc:title="RDF 1.2 XML Syntax">
                  <ex:editor>
                    <rdf:Description ex:fullName="Dave Beckett">
                      <ex:homePage rdf:resource="http://purl.org/net/dajobe/"/>
                    </rdf:Description>
                  </ex:editor>
                </rdf:Description>
                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 7: Complete RDF/XML",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
                             dc:title="RDF 1.2 XML Syntax">
                    <ex:editor>
                      <rdf:Description ex:fullName="Dave Beckett">
                        <ex:homePage rdf:resource="http://purl.org/net/dajobe/" />
                      </rdf:Description>
                    </ex:editor>
                  </rdf:Description>
                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 8: Complete example xml:lang",
                """
                <?xml version="1.0" encoding="utf-8"?>
                          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                   xmlns:dc="http://purl.org/dc/elements/1.1/">

                            <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                              <dc:title>RDF 1.2 XML Syntax</dc:title>
                              <dc:title xml:lang="en">RDF 1.2 XML Syntax</dc:title>
                              <dc:title xml:lang="en-US">RDF 1.2 XML Syntax</dc:title>
                            </rdf:Description>

                            <rdf:Description rdf:about="http://example.org/buecher/baum" xml:lang="de">
                              <dc:title>Der Baum</dc:title>
                              <dc:description>Das Buch ist außergewöhnlich</dc:description>
                              <dc:title xml:lang="en">The Tree</dc:title>
                            </rdf:Description>

                          </rdf:RDF>
                """.trim(),
                6
            ),
            Arguments.of(
                "Example 11: Complete example rdf:datatype",
                """
                <?xml version="1.0"?>
                                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                                xmlns:ex="http://example.org/stuff/1.0/">

                                      <rdf:Description rdf:about="http://example.org/item01">
                                        <ex:size rdf:datatype="http://www.w3.org/2001/XMLSchema#int">123</ex:size>
                                      </rdf:Description>

                                    </rdf:RDF>
                """.trim(),
                1
            ),
            Arguments.of(
                "Example 12: Complete RDF/XML using rdf:nodeID",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
                             dc:title="RDF 1.2 XML Syntax">
                    <ex:editor rdf:nodeID="abc"/>
                  </rdf:Description>

                  <rdf:Description rdf:nodeID="abc" ex:fullName="Dave Beckett">
                    <ex:homePage rdf:resource="http://purl.org/net/dajobe/"/>
                  </rdf:Description>

                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 13: Complete example using rdf:parseType=Resource",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">
                  <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
                                   dc:title="RDF 1.2 XML Syntax">
                    <ex:editor rdf:parseType="Resource">
                      <ex:fullName>Dave Beckett</ex:fullName>
                      <ex:homePage rdf:resource="http://purl.org/net/dajobe/"/>
                    </ex:editor>
                  </rdf:Description>
                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 14: Complete example of property attributes on an empty property element",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
                            dc:title="RDF 1.2 XML Syntax">
                    <ex:editor ex:fullName="Dave Beckett" />
                  </rdf:Description>

                </rdf:RDF>
                """.trim(),
                3
            ),
            Arguments.of(
                "Example 15: Complete example with rdf:type",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <rdf:Description rdf:about="http://example.org/thing">
                    <rdf:type rdf:resource="http://example.org/stuff/1.0/Document"/>
                    <dc:title>A marvelous thing</dc:title>
                  </rdf:Description>
                </rdf:RDF>
                """.trim(),
                2
            ),
            Arguments.of(
                "Example 16: Complete example using a typed node element to replace an rdf:type",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <ex:Document rdf:about="http://example.org/thing">
                    <dc:title>A marvelous thing</dc:title>
                  </ex:Document>

                </rdf:RDF>
                """.trim(),
                2
            ),
            Arguments.of(
                "Example 17: Complete example using rdf:ID and xml:base",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:ex="http://example.org/stuff/1.0/"
                            xml:base="http://example.org/here/">

                  <rdf:Description rdf:ID="snack">
                    <ex:prop rdf:resource="fruit/apple"/>
                  </rdf:Description>

                </rdf:RDF>
                """.trim(),
                1
            ),
            Arguments.of(
                "Example 18: Complex example using RDF list properties",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

                  <rdf:Seq rdf:about="http://example.org/favourite-fruit">
                    <rdf:_1 rdf:resource="http://example.org/banana"/>
                    <rdf:_2 rdf:resource="http://example.org/apple"/>
                    <rdf:_3 rdf:resource="http://example.org/pear"/>
                  </rdf:Seq>

                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 19: Complete example using rdf:li properties",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

                  <rdf:Seq rdf:about="http://example.org/favourite-fruit">
                    <rdf:li rdf:resource="http://example.org/banana"/>
                    <rdf:li rdf:resource="http://example.org/apple"/>
                    <rdf:li rdf:resource="http://example.org/pear"/>
                  </rdf:Seq>

                </rdf:RDF>
                """.trim(),
                4
            ),
            Arguments.of(
                "Example 20: Complete example of a RDF collection of nodes",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:ex="http://example.org/stuff/1.0/">

                  <rdf:Description rdf:about="http://example.org/basket">
                    <ex:hasFruit rdf:parseType="Collection">
                      <rdf:Description rdf:about="http://example.org/banana"/>
                      <rdf:Description rdf:about="http://example.org/apple"/>
                      <rdf:Description rdf:about="http://example.org/pear"/>
                    </ex:hasFruit>
                  </rdf:Description>

                </rdf:RDF>
                """.trim(),
                7
            ),
            Arguments.of(
                "Example 21: Complete example of rdf:ID",
                """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            xmlns:ex="http://example.org/stuff/1.0/"
                            xml:base="http://example.org/triples/">
                  <rdf:Description rdf:about="http://example.org/">
                    <ex:prop rdf:ID="triple1">blah</ex:prop>
                  </rdf:Description>

                </rdf:RDF>
                """.trim(),
                5
            )
        );
    }
}