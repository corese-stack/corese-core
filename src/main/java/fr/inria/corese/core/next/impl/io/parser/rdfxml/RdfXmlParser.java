package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.impl.common.literal.XSD;
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
import java.util.Optional;

import fr.inria.corese.core.next.impl.common.vocabulary.RDF;

public class RdfXmlParser extends DefaultHandler implements RDFParser {

    private final Model model;
    private final RDFFormat format = RDFFormats.RDF_XML;
    private final ValueFactory factory;

    private StringBuilder characters = new StringBuilder();

    private String baseURI;

    private Statement statement;

    private final Deque<Resource> subjectStack = new ArrayDeque<>();
    private final Deque<IRI> predicateStack = new ArrayDeque<>();
    private final Deque<String> langStack = new ArrayDeque<>();
    private final Deque<String> datatypeStack = new ArrayDeque<>();

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
        // Ignore rdf:RDF root element
        if (isRdfRDF(uri, localName)) return;
        characters.setLength(0);

        // Handle datatype
        String datatype = attrs.getValue(RDF.type.getNamespace(), "datatype");
        if (datatype != null) {
            datatypeStack.push(datatype);
        }


        // Handle xml:lang
        String xmlLang = attrs.getValue("xml:lang");
        if (xmlLang != null) {
            langStack.push(xmlLang);
        }

        // Handle xml:base
        String xmlBase = attrs.getValue("xml:base");
        if (xmlBase != null) {
            baseURI = xmlBase;
        }

        // Handle RDF containers
        if (isContainer(localName, uri)) {
            Resource subject = extractSubject(attrs);
            subjectStack.push(subject);
            inContainer = true;
            liIndex = 1;
            return;
        }

        // Handle container items: rdf:li → rdf:_n
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
                    model.add(factory.createStatement(
                            subjectStack.peek(),
                            predicate,
                            factory.createIRI(resolveAgainstBase(resource))
                    ));
                }
                return;
            }
        }

        // Handle <rdf:Description> (typed or untyped)
        if (isDescription(localName, uri) || isNodeElement(attrs)) {
            Resource newSubject = extractSubject(attrs);
            // If this <rdf:Description> or typed element is the object of a property
            if (!predicateStack.isEmpty() && !subjectStack.isEmpty()) {
                Resource parent = subjectStack.peek();
                IRI predicate = predicateStack.pop(); // consume the predicate
                model.add(factory.createStatement(parent, predicate, newSubject));
            }

            subjectStack.push(newSubject);

            // If it's a typed node (e.g., <ex:Document>), add rdf:type triple
            if (!isDescription(localName, uri)) {
                IRI typeIRI = factory.createIRI(expandQName(uri, localName, qName));
                model.add(factory.createStatement(
                        newSubject,
                        factory.createIRI(RDF.type.getIRI().stringValue()),
                        typeIRI
                ));
            }

            // Handle property attributes
            for (int i = 0; i < attrs.getLength(); i++) {
                String attrURI = attrs.getURI(i);
                String attrLocal = attrs.getLocalName(i);
                String attrQName = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (isSyntaxAttribute(attrURI, attrLocal, attrQName)) continue;

                IRI pred = factory.createIRI(expandQName(attrURI, attrLocal, attrQName));
                model.add(factory.createStatement(newSubject, pred, factory.createLiteral(value)));
            }

            return;
        }

        // Handle regular property elements (e.g., <ex:editor>)
        IRI predicate = factory.createIRI(expandQName(uri, localName, qName));
        predicateStack.push(predicate);

        // Handle rdf:resource object (IRI)
        String resource = attrs.getValue("rdf:resource");
        if (resource != null) {
            model.add(factory.createStatement(
                    subjectStack.peek(),
                    predicate,
                    factory.createIRI(resolveAgainstBase(resource))
            ));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String text = characters.toString().trim();
        characters.setLength(0);

        // Always pop lang/datatype if pushed
        if (!langStack.isEmpty()) langStack.pop();
        String datatypeUri = !datatypeStack.isEmpty() ? datatypeStack.pop() : null;

        // Property literal
        if (!predicateStack.isEmpty() && !text.isEmpty()) {
            IRI predicate = predicateStack.pop();
            Resource subject = subjectStack.peek();

            Value literal;

            if (datatypeUri != null && !datatypeUri.isBlank()) {
                Optional<XSD> known = fromURI(datatypeUri);

                if (known.isPresent()) {
                    // normalized datatype
                    IRI normalizedDatatype = known.get().getIRI();
                    literal = factory.createLiteral(text, normalizedDatatype);
                } else {
                    // fallback datatype
                    System.err.printf("[Warning] Unknown datatype: %s%n", datatypeUri);
                    IRI fallbackDatatype = factory.createIRI(datatypeUri);
                    literal = factory.createLiteral(text, fallbackDatatype);
                }
            } else {
                // no datatype – use language tag if any
                String lang = langStack.isEmpty() ? null : langStack.peek();
                literal = (lang != null && !lang.equals("__NO_LANG__"))
                        ? factory.createLiteral(text, lang)
                        : factory.createLiteral(text);
            }

            model.add(factory.createStatement(subject, predicate, literal));
            return;
        }

        // Clean up stray predicates
        if (!predicateStack.isEmpty()) {
            predicateStack.pop();
        }

        // Handle containers
        if (isContainer(localName, uri)) {
            if (!subjectStack.isEmpty()) subjectStack.pop();
            inContainer = false;
            liIndex = 1;
            return;
        }

        // Handle end of rdf:Description
        if (isDescription(localName, uri)) {
            if (!subjectStack.isEmpty()) subjectStack.pop();
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    private Resource extractSubject(Attributes attrs) {
        String about = attrs.getValue(RDF.type.getNamespace(), "about");
        if (about != null) return factory.createIRI(resolveAgainstBase(about));

        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");
        if (nodeID != null) return factory.createBNode("_:" + nodeID);

        String id = attrs.getValue(RDF.type.getNamespace(), "ID");
        if (id != null) return factory.createIRI(resolveAgainstBase("#" + id));

        // Default to blank node
        return factory.createBNode();
    }

    private String resolveAgainstBase(String iri) {
        if (iri == null) return null;
        if (baseURI == null || iri.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            // Absolute IRI or no base, return as-is
            return iri;
        }

        try {
            return new java.net.URI(baseURI).resolve(iri).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve IRI: " + iri + " against base: " + baseURI, e);
        }
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

    private boolean isNodeElement(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "about") != null ||
                attrs.getValue(RDF.type.getNamespace(), "nodeID") != null ||
                attrs.getValue(RDF.type.getNamespace(), "ID") != null;
    }

    public Optional<XSD> fromURI(String uri) {
        for (XSD xsd : XSD.values()) {
            if (xsd.getIRI().stringValue().equals(uri)) {
                return Optional.of(xsd);
            }
        }
        return Optional.empty();
    }
}
