package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Reader;

public class RdfXmlParser extends DefaultHandler implements RDFParser {

    private final Model model;
    private final RDFFormat format = RDFFormats.RDF_XML;
    private final ValueFactory factory;
    private String baseURI;

    public RdfXmlParser(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }

    @Override
    public RDFFormat getRDFFormat() {
        return format;
    }

    @Override
    public void parse(InputStream in) {
        parse(in, null);
    }

    @Override
    public void parse(InputStream in, String baseURI) {
        this.baseURI = baseURI;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(in, this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RDF/XML input stream", e);
        }
    }

    @Override
    public void parse(Reader reader) {
        parse(reader, null);
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        this.baseURI = baseURI;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            InputSource inputSource = new InputSource(reader);
            saxParser.parse(inputSource, this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RDF/XML input stream", e);
        }
    }

    // SAX: element start
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

    }

    // SAX: element end
    @Override
    public void endElement(String uri, String localName, String qName) {
        System.out.println("End: " + qName);
    }
}
