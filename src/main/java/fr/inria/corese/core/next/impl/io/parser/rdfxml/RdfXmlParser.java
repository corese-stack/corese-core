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
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;

public class RdfXmlParser extends DefaultHandler implements RDFParser {

    private final Model model;
    private final RDFFormat format = RDFFormats.RDF_XML;
    private final ValueFactory factory;

    private StringBuilder characters = new StringBuilder();

    private String baseURI;
    private Resource currentSubject;
    private Statement statement;

    private final Deque<Statement> statementStack = new ArrayDeque<>();
    private final Deque<Resource> subjectStack = new ArrayDeque<>();
    private final Deque<IRI> predicateStack = new ArrayDeque<>();


    private boolean inContainer = false;
    private int liIndex = 1;

    public RdfXmlParser(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }

    // used for test class and can be removed
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

        // Ignore rdf:RDF
        if (isRdfRDF(uri, localName)) return;

        // Handle container elements: rdf:Seq, rdf:Bag, rdf:Alt
        if (isContainer(localName, uri)) {
            Resource subject = extractSubject(attrs);
            subjectStack.push(subject);
            inContainer = true;
            liIndex = 1;
            return;
        }

        // Handle container children: rdf:li → rdf:_n
        if (inContainer && RDF.type.getNamespace().equals(uri)) {
            String pred = null;
            if ("li".equals(localName)) {
                pred = RDF.type.getNamespace() + "_" + liIndex++;
            } else if (localName.matches("_\\d+")) {
                pred = RDF.type.getNamespace() + localName;
            }

            if (pred != null) {
                IRI predicate = factory.createIRI(pred);
                String resource = attrs.getValue("rdf:resource");
                if (resource != null) {
                    model.add(factory.createStatement(subjectStack.peek(), predicate, factory.createIRI(resource)));
                }
                return;
            }
        }

        // Handle <rdf:Description>
        if (isDescription(localName, uri)) {
            Resource subject = extractSubject(attrs);

            if (!predicateStack.isEmpty() && !subjectStack.isEmpty()) {
                Resource parent = subjectStack.peek();
                IRI predicate = predicateStack.peek();
                model.add(factory.createStatement(parent, predicate, subject));
            }

            subjectStack.push(subject);

            for (int i = 0; i < attrs.getLength(); i++) {
                String attrURI = attrs.getURI(i);
                String attrLocal = attrs.getLocalName(i);
                String attrQName = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (isSyntaxAttribute(attrURI, attrLocal, attrQName)) {
                    continue; // skip core syntax attributes
                }

                IRI pred = factory.createIRI(expandQName(attrURI, attrLocal, attrQName));
                model.add(factory.createStatement(subject, pred, factory.createLiteral(value)));
            }

            return;
        }


        // Handle regular property elements
        IRI predicate = factory.createIRI(expandQName(uri, localName, qName));
        predicateStack.push(predicate);

        // Check for rdf:resource
        String resource = attrs.getValue("rdf:resource");
        if (resource != null) {
            model.add(factory.createStatement(subjectStack.peek(), predicate, factory.createIRI(resource)));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String text = characters.toString().trim();
        characters.setLength(0);

        if (isContainer(localName, uri)) {
            subjectStack.pop();
            inContainer = false;
            liIndex = 1;
            return;
        }

        if (isDescription(localName, uri)) {
            subjectStack.pop();
            return;
        }

        // Closing a property element with literal content
        if (!predicateStack.isEmpty() && !text.isEmpty()) {
            IRI predicate = predicateStack.pop();
            Resource subject = subjectStack.peek();
            model.add(factory.createStatement(subject, predicate, factory.createLiteral(text)));

        } else if (!predicateStack.isEmpty()) {
            predicateStack.pop(); // still clean up
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    private Resource extractSubject(Attributes attrs) {
        String about = attrs.getValue("rdf:about");
        if (about != null) return factory.createIRI(about);

        String nodeID = attrs.getValue("rdf:nodeID");
        if (nodeID != null) return factory.createBNode("_:" + nodeID);

        String id = attrs.getValue("rdf:ID");
        if (id != null) return factory.createIRI("#" + id);

        return factory.createBNode();
    }

    private boolean isRdfRDF(String uri, String localName) {
        return RDF.type.equals(uri) && "RDF".equals(localName);
    }

    private boolean isDescription(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) && "Description".equals(localName);
    }

    private boolean isContainer(String localName, String uri) {
        return RDF.type.getNamespace().equals(uri) &&
                ("Seq".equals(localName) || "Bag".equals(localName) || "Alt".equals(localName));
    }

    private String expandQName(String uri, String localName, String qName) {
        return (uri != null && !uri.isEmpty()) ? uri + localName : qName;
    }

    private boolean isSyntaxAttribute(String uri, String localName, String qName) {
        if (uri != null && RDF.type.getNamespace().equals(uri)) {
            return switch (localName) {
                case "about", "ID", "nodeID", "resource", "parseType", "datatype" -> true;
                default -> false;
            };
        }
        return qName.startsWith("xml:");
    }

    private void emitTripleString(String subject, String predicate, String object) {
        System.out.printf("Triple: <%s> <%s> %s%n", subject, predicate, object);
    }

    private void emitTriple(Resource subj, IRI pred, Value obj,  Resource context) {
        this.statement = factory.createStatement(subj, pred, obj, context);
    }
}
