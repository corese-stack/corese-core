package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;

public class RdfXmlParser extends DefaultHandler implements RDFParser {

    private final Model model;
    private final RDFFormat format = RDFFormats.RDF_XML;
    private final ValueFactory factory;
    private String baseURI;
    private Resource currentSubject;
    private Statement statement;

    private StringBuilder characters = new StringBuilder();
    private final Deque<Statement> statementStack = new ArrayDeque<>();

    public RdfXmlParser(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }

    // used for my play test class
    public RdfXmlParser() {
        this(new CoreseModel(), new CoreseAdaptedValueFactory());
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

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        characters.setLength(0);

        if (isRdfRDF(uri, localName)) {
            return; // skip root element
        }

        // nodeElement
        if (currentSubject == null) {
            currentSubject = extractSubject(attrs);
            return;
        }

        // propertyElement → create statement and push it
        IRI predicate = factory.createIRI(qName); // TODO: resolve properly
        var resourceAttr = attrs.getValue(RDF.type.getNamespace(), "resource");

        if (resourceAttr != null) {
            Value object = factory.createLiteral(resourceAttr);
            Statement stmt = factory.createStatement(currentSubject, predicate, object);
            model.add(stmt);
            return;
        }

        // literal content will be handled in endElement
        Statement stub = factory.createStatement(currentSubject, predicate, null);
        statementStack.push(stub);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!statementStack.isEmpty()) {
            Statement stmt = statementStack.pop();
            String content = characters.toString().trim();
            if (!content.isEmpty()) {
                Value literal = factory.createLiteral(content);
                Statement complete = factory.createStatement(
                        stmt.getSubject(), stmt.getPredicate(), literal
                );
                model.add(complete);
            }
        } else {
            // end of nodeElement
            currentSubject = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    private Resource extractSubject(Attributes attrs) {
        String about = attrs.getValue(RDF.type.getNamespace(), "about");
        if (about != null) return factory.createIRI(about);

        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");
        if (nodeID != null) return factory.createBNode("_:" + nodeID);

        String id = attrs.getValue(RDF.type.getNamespace(), "ID");
        if (id != null) return factory.createIRI("#" + id);

        return factory.createBNode();
    }

    private boolean isRdfRDF(String uri, String localName) {
        return RDF.type.getNamespace().equals(uri) && "RDF".equals(localName);
    }

    private void emitTripleString(String subject, String predicate, String object) {
        System.out.printf("Triple: <%s> <%s> %s%n", subject, predicate, object);
    }


    private void emitTriple(Resource subj, IRI pred, Value obj,  Resource context) {
        this.statement = factory.createStatement(subj, pred, obj, context);
    }
}
