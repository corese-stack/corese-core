package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class RdfxmlParserTest {

    @Test
    public void testBasicRdfParsing() throws Exception {
        String rdfXml = "" +
                "<?xml version=\"1.0\"?>" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
                "         xmlns:ex=\"http://example.org/stuff/1.0/\">" +
                "  <rdf:Description rdf:about=\"http://www.example.org/index.html\">" +
                "    <ex:creator>John Smith</ex:creator>" +
                "    <ex:date>2025-07-07</ex:date>" +
                "  </rdf:Description>" +
                "</rdf:RDF>";

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
        RdfXmlParser handler = new RdfXmlParser();

        saxParser.parse(inputStream, handler);
    }


}
