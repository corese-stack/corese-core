package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.apache.jena.base.Sys;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RdfxmlParserTest {
    @Test
    public void testNodeElementsWithIRIs() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        String rdfXml = """
                <?xml version="1.0"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:ex="http://example.org/stuff/1.0/">
                <rdf:Description rdf:about="http://www.w3.org/TR/rdf-syntax-grammar">
                  <ex:editor>
                    <rdf:Description>
                      <ex:homePage>
                        <rdf:Description rdf:about="http://purl.org/net/dajobe">
                        </rdf:Description>
                      </ex:homePage>
                    </rdf:Description>
                  </ex:editor>
                </rdf:Description>
                </rdf:RDF>
                """;


        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected two RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });

    }

    @Test
    public void testBasicRdfParsing() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected two RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample3CompleteDescriptionOfAllGraphPaths() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        String inTriG = """
                @prefix dcterms: <http://purl.org/dc/terms/> .
                
                {
                	<http://www.w3.org/TR/rdf-syntax-grammar> <http://example.org/stuff/1.0/editor> _:b15, _:b16 ;
                		dcterms:title "RDF 1.2 XML Syntax" .
                
                	_:b15 <http://example.org/stuff/1.0/homePage> <http://purl.org/net/dajobe/> .
                
                	_:b16 <http://example.org/stuff/1.0/fullName> "Dave Beckett" .
                }
                """;

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(5, model.size(), "Expected five RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample4UsingMultiplePropertyElements() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        String rdfXml = """
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(5, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample5EmptyPropertyElements() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        String rdfXml = """
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(5, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample6ReplacingPropertyElementsWithStringLiteral() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(5, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample7CompleteRDFXML() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(5, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample8CompleteExampleXmlLang() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(8, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample11CompleteExamplerdfDatatype() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        String rdfXml = """
                <?xml version="1.0"?>
                                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                                xmlns:ex="http://example.org/stuff/1.0/">
                
                                      <rdf:Description rdf:about="http://example.org/item01">
                                        <ex:size rdf:datatype="http://www.w3.org/2001/XMLSchema#int">123</ex:size>
                                      </rdf:Description>
                
                                    </rdf:RDF>
                """.trim();

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample12CompleteRDFXMLUsingRdfNodeID() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(6, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample13CompleteExampleUsingRdfparseTypeResource() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(4, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample14CompleteExampleOfPorpertyAttributesOnAnEmptyPropertyElement() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample15CompleteExampleWithRdfType() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(3, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample16CompleteExampleUsingATypedNodeElementToReplaceAnRdfType() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample17CompleteExampleUsingRdfIDAndXmlbase() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected four RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample18ComplexExampleUsingRdfListProperties() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(3, model.size(), "Expected three RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample19ComplexExampleUsingRdfliProperties() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(3, model.size(), "Expected three RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample20CompleteExampleOfARdfCollectionOfNodes() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(4, model.size(), "Expected three RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }

    @Test
    public void testExample21CompleteExampleOfRdfID() throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        RdfXmlParser handler = new RdfXmlParser(model, valueFactory);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(3, model.size(), "Expected three RDF statements");

        model.stream().forEach(stmt -> {
            System.out.println(stmt.toString());
        });
    }
}