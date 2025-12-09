package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;
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
public class RDFXMLParserTest {
    private static final Logger logger = LoggerFactory.getLogger(RDFXMLParserTest.class);

    /**
     * Helper method to parse an RDF/XML string into an RDF model.
     *
     * @param rdfXml The RDF/XML content as a string.
     * @return The resulting {@link Model} populated by the parser.
     * @throws Exception if an error occurs during parsing or I/O.
     */
    private Model parseRdfXml(String rdfXml) throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        try (InputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8))) {
            RDFXMLParser parser = new RDFXMLParser(model, valueFactory);
            parser.parse(inputStream);
        }
        return model;
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
    public void testNodeElementsWithIRIs() throws Exception {
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
    public void testBasicRdfParsing() throws Exception {
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
    @Test
    public void testExample3CompleteDescriptionOfAllGraphPaths() throws Exception {
        String rdfXml = """
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
                """.trim();
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(5, model.size(), "Expected five RDF statements");
    }

    /**
     * Tests RDF/XML file using multiple property elements on a node element.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample4UsingMultiplePropertyElements() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests RDF/XML with empty property elements.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample5EmptyPropertyElements() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a RDF/XML file with replacing property elements with string literal content into property attributes.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample6ReplacingPropertyElementsWithStringLiteral() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a complete RDF/XML.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample7CompleteRDFXML() throws Exception {
        String rdfXml = """
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
                """.trim();
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a complete example of xml:lang.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample8CompleteExampleXmlLang() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(6, model.size(), "Expected six RDF statements");
    }

    /**
     * Tests a complete example of rdf:datatype.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample11CompleteExamplerdfDatatype() throws Exception {
        String rdfXml = """
                <?xml version="1.0"?>
                                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                                xmlns:ex="http://example.org/stuff/1.0/">
                
                                      <rdf:Description rdf:about="http://example.org/item01">
                                        <ex:size rdf:datatype="http://www.w3.org/2001/XMLSchema#int">123</ex:size>
                                      </rdf:Description>
                
                                    </rdf:RDF>
                """.trim();
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(1, model.size(), "Expected one RDF statement");
    }

    /**
     * Tests a complete RDF/XML file with a description of graph using rdf:nodeID.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample12CompleteRDFXMLUsingRdfNodeID() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a RDF/XML file with a complete example using rdf:parseType=Resource.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample13CompleteExampleUsingRdfparseTypeResource() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a RDF/XML file with a complete example of property attributes on an empty property element.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample14CompleteExampleOfPropertyAttributesOnAnEmptyPropertyElement() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:ex="http://example.org/stuff/1.0/">

  <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar"
            dc:title="RDF 1.2 XML Syntax">
    <ex:editor ex:fullName="Dave Beckett" />
            <!-- Note the ex:homePage property has been ignored for this example -->
  </rdf:Description>

</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(3, model.size(), "Expected three RDF statements");
    }

    /**
     * Tests a RDF/XML file with a complete example with rdf:type.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample15CompleteExampleWithRdfType() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:ex="http://example.org/stuff/1.0/">

  <rdf:Description rdf:about="http://example.org/thing">
    <rdf:type rdf:resource="http://example.org/stuff/1.0/Document"/>
    <dc:title>A marvelous thing</dc:title>
  </rdf:Description>
</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(2, model.size(), "Expected two RDF statements");
    }

    /**
     * Tests a RDF/XML file with a complete example using a typed node element to replace an rdf:type.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample16CompleteExampleUsingATypedNodeElementToReplaceAnRdfType() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:ex="http://example.org/stuff/1.0/">

  <ex:Document rdf:about="http://example.org/thing">
    <dc:title>A marvelous thing</dc:title>
  </ex:Document>

</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(2, model.size(), "Expected two RDF statements");
    }

    /**
     * Tests an XML/RDF file using rdf:ID and xml:base.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample17CompleteExampleUsingRdfIDAndXmlbase() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:ex="http://example.org/stuff/1.0/"
            xml:base="http://example.org/here/">

  <rdf:Description rdf:ID="snack">
    <ex:prop rdf:resource="fruit/apple"/>
  </rdf:Description>

</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(1, model.size(), "Expected one RDF statement");
    }

    /**
     * Tests a complex example using RDF list properties.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample18ComplexExampleUsingRdfListProperties() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

  <rdf:Seq rdf:about="http://example.org/favourite-fruit">
    <rdf:_1 rdf:resource="http://example.org/banana"/>
    <rdf:_2 rdf:resource="http://example.org/apple"/>
    <rdf:_3 rdf:resource="http://example.org/pear"/>
  </rdf:Seq>

</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a complete example using rdf:li.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample19CompleteExampleUsingRdfliProperties() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

  <rdf:Seq rdf:about="http://example.org/favourite-fruit">
    <rdf:li rdf:resource="http://example.org/banana"/>
    <rdf:li rdf:resource="http://example.org/apple"/>
    <rdf:li rdf:resource="http://example.org/pear"/>
  </rdf:Seq>

</rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(4, model.size(), "Expected four RDF statements");
    }

    /**
     * Tests a complete example of a RDF collection.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample20CompleteExampleOfARdfCollectionOfNodes() throws Exception {
        String rdfXml = """
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
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(7, model.size(), "Expected seven RDF statements");
    }

    /**
     * Tests a complete example of rdf:ID reifying a property element.
     *
     * @throws Exception If parsing fails.
     */
    @Test
    public void testExample21CompleteExampleOfRdfID() throws Exception {
        String rdfXml = """
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:ex="http://example.org/stuff/1.0/"
            xml:base="http://example.org/triples/">
  <rdf:Description rdf:about="http://example.org/">
    <ex:prop rdf:ID="triple1">blah</ex:prop>
  </rdf:Description>

</rdf:RDF>
                """.trim();
        Model model = parseRdfXml(rdfXml);
        printModel(model);
        assertEquals(1, model.size(), "Expected one RDF statement");
    }
}