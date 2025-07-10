package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class RdfxmlParserTest {

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

        // Prepare input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));

        // Set up the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        // Provide an explicit model
        CoreseModel model = new CoreseModel();
        RdfXmlParser handler = new RdfXmlParser(model);

        // Parse the input
        saxParser.parse(inputStream, handler);

        // Assert or inspect the result
        assertEquals(2, model.size(), "Expected two RDF statements");

        model.statements().forEach(stmt -> {
            System.out.println(stmt);
        });
    }


}
