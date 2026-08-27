package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.spi.io.IOConstants;
import fr.inria.corese.core.next.data.spi.io.parser.AbstractRDFParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SAX-based W3C RDF 1.1 XML Syntax parser implementation.
 *
 * <p>This parser implements the complete W3C RDF 1.1 XML Syntax grammar (W3C Recommendation 25 February 2014)
 * using an event-driven SAX element frame state machine. It handles scoped {@code xml:base} resolution,
 * {@code xml:lang} inheritance, typed and anonymous node elements, property elements,
 * {@code rdf:parseType} modes (Resource, Collection, Literal), property attributes, and reification.</p>
 *
 * <p>RDF statements generated during parsing are added to the provided {@link Model}
 * using the supplied {@link ValueFactory}.</p>
 */
public class RDFXMLParser extends AbstractRDFParser {

    private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final String RDF_XML_LITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";

    private static final String ATTR_ABOUT = "about";
    private static final String ATTR_ID = "ID";
    private static final String ATTR_NODE_ID = "nodeID";
    private static final String ATTR_BAG_ID = "bagID";
    private static final String ATTR_PARSE_TYPE = "parseType";
    private static final String ATTR_RESOURCE = "resource";
    private static final String ATTR_DATATYPE = "datatype";
    private static final String ATTR_ABOUT_EACH = "aboutEach";
    private static final String ATTR_ABOUT_EACH_PREFIX = "aboutEachPrefix";
    private static final String ATTR_LI = "li";
    private static final String ATTR_TYPE = "type";
    private static final String ELEMENT_DESCRIPTION = "Description";
    private static final String ELEMENT_RDF = "RDF";

    private static final Set<String> FORBIDDEN_NODE_ELEMENT_NAMES = Set.of(
            ELEMENT_RDF, ATTR_ID, ATTR_ABOUT, ATTR_BAG_ID, ATTR_PARSE_TYPE,
            ATTR_RESOURCE, ATTR_NODE_ID, ATTR_DATATYPE, ATTR_LI,
            ATTR_ABOUT_EACH, ATTR_ABOUT_EACH_PREFIX
    );

    private static final Set<String> FORBIDDEN_PROPERTY_ELEMENT_NAMES = Set.of(
            ELEMENT_RDF, ATTR_ID, ATTR_ABOUT, ATTR_BAG_ID, ATTR_PARSE_TYPE,
            ATTR_RESOURCE, ATTR_NODE_ID, ATTR_DATATYPE, ELEMENT_DESCRIPTION,
            ATTR_ABOUT_EACH, ATTR_ABOUT_EACH_PREFIX
    );

    /**
     * Constructs an RDFXMLParser with default options.
     *
     * @param model the model to which statements are added
     * @param factory the factory used to create RDF terms
     */
    public RDFXMLParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFXMLParserOptions.Builder().build());
    }

    /**
     * Constructs an RDFXMLParser with the specified parsing configuration options.
     *
     * @param model the model to which statements are added
     * @param factory the factory used to create RDF terms
     * @param config the RDF parsing options
     */
    public RDFXMLParser(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFXML;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingException {
        Objects.requireNonNull(in, "InputStream cannot be null");
        try {
            InputSource is = new InputSource(in);
            is.setEncoding(StandardCharsets.UTF_8.name());
            parseInputSource(is, baseURI);
        } catch (Exception e) {
            if (e instanceof ParsingException pe) throw pe;
            throw new ParsingException("RDF/XML parsing error: " + e.getMessage(), e);
        }
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingException {
        Objects.requireNonNull(reader, "Reader cannot be null");
        try {
            InputSource is = new InputSource(reader);
            parseInputSource(is, baseURI);
        } catch (Exception e) {
            if (e instanceof ParsingException pe) throw pe;
            throw new ParsingException("RDF/XML parsing error: " + e.getMessage(), e);
        }
    }

    /**
     * Configures a secure SAXParser and executes the streaming parsing on the given InputSource.
     *
     * @param inputSource the XML input source to parse
     * @param baseURI the base URI for relative URI resolution
     * @throws ParsingException if a syntactic or structural error is encountered
     */
    private void parseInputSource(InputSource inputSource, String baseURI) throws ParsingException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setFeature("http://xml.org/sax/features/namespaces", true);
            spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

            SAXParser saxParser = spf.newSAXParser();
            NativeRdfXmlHandler handler = new NativeRdfXmlHandler(getModel(), getValueFactory(), baseURI != null ? baseURI : "");
            saxParser.parse(inputSource, handler);
        } catch (SAXException e) {
            if (e.getException() instanceof ParsingException pe) {
                throw pe;
            }
            throw new ParsingException("RDF/XML parsing syntax error: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof ParsingException pe) throw pe;
            throw new ParsingException("RDF/XML parser error: " + e.getMessage(), e);
        }
    }

    /**
     * Types of element frames in the parser stack state machine.
     */
    private enum FrameType {
        RDF_ROOT,
        NODE_ELEMENT,
        PROPERTY_ELEMENT,
        PARSE_TYPE_RESOURCE,
        PARSE_TYPE_COLLECTION,
        PARSE_TYPE_LITERAL
    }

    /**
     * ElementFrame maintains scoped state (base URI, language, subject/predicate context, attributes)
     * during the hierarchical traversal of the XML tree.
     */
    private static class ElementFrame {
        final ElementFrame parent;
        final FrameType type;

        String baseURI;
        String lang;

        // For NODE_ELEMENT & PARSE_TYPE_RESOURCE:
        Resource subject;
        int liIndex = 1;

        // For PROPERTY_ELEMENT:
        IRI predicate;
        String rdfID;
        String datatype;
        String resourceAttr;
        String nodeIDAttr;
        boolean hasChildElements = false;

        // For PARSE_TYPE_COLLECTION:
        List<Resource> collectionItems = new ArrayList<>();

        // For literal character content:
        StringBuilder text = new StringBuilder();

        // For PARSE_TYPE_LITERAL:
        StringBuilder xmlContent = new StringBuilder();

        // Property attributes on property elements (emptyPropertyElt production):
        Map<IRI, String> propertyAttributes = new LinkedHashMap<>();
        String propertyTypeAttr = null;

        ElementFrame(ElementFrame parent, FrameType type, String baseURI, String lang) {
            this.parent = parent;
            this.type = type;
            this.baseURI = baseURI;
            this.lang = lang;
        }
    }

    /**
     * DefaultHandler implementation managing W3C RDF 1.1 XML grammar state transitions.
     */
    private static class NativeRdfXmlHandler extends DefaultHandler {
        private final Model model;
        private final ValueFactory factory;
        private final String initialBaseURI;
        private final Deque<ElementFrame> stack = new ArrayDeque<>();
        private final Set<String> declaredIDs = new HashSet<>();
        private final Map<String, String> bnodeMap = new HashMap<>();

        private final IRI rdfType;
        private final IRI rdfStatement;
        private final IRI rdfSubject;
        private final IRI rdfPredicate;
        private final IRI rdfObject;
        private final IRI rdfFirst;
        private final IRI rdfRest;
        private final IRI rdfNil;

        NativeRdfXmlHandler(Model model, ValueFactory factory, String initialBaseURI) {
            this.model = model;
            this.factory = factory;
            this.initialBaseURI = initialBaseURI;

            this.rdfType = factory.createIRI(RDF_NS + "type");
            this.rdfStatement = factory.createIRI(RDF_NS + "Statement");
            this.rdfSubject = factory.createIRI(RDF_NS + "subject");
            this.rdfPredicate = factory.createIRI(RDF_NS + "predicate");
            this.rdfObject = factory.createIRI(RDF_NS + "object");
            this.rdfFirst = factory.createIRI(RDF_NS + "first");
            this.rdfRest = factory.createIRI(RDF_NS + "rest");
            this.rdfNil = factory.createIRI(RDF_NS + "nil");
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (stack.isEmpty()) return;
            ElementFrame current = stack.peek();

            if (current.type == FrameType.PARSE_TYPE_LITERAL) {
                current.xmlContent.append(escapeXmlChars(ch, start, length));
            } else {
                current.text.append(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            try {
                handleStartElement(uri, localName, qName, attrs);
            } catch (ParsingException e) {
                throw new SAXException(e);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            try {
                handleEndElement(qName);
            } catch (ParsingException e) {
                throw new SAXException(e);
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        private void handleStartElement(String uri, String localName, String qName, Attributes attrs) throws ParsingException {
            ElementFrame parent = stack.peek();

            if (parent != null && parent.type == FrameType.PARSE_TYPE_LITERAL) {
                serializeLiteralElement(parent, qName, attrs);
                return;
            }

            String currentBase = resolveScopedBase(parent, attrs);
            String currentLang = resolveScopedLang(parent, attrs);

            validateAttributes(attrs);
            dispatchStartElement(parent, uri, localName, qName, attrs, currentBase, currentLang);
        }

        private void serializeLiteralElement(ElementFrame parent, String qName, Attributes attrs) {
            parent.xmlContent.append("<").append(qName);
            for (int i = 0; i < attrs.getLength(); i++) {
                parent.xmlContent.append(" ").append(attrs.getQName(i)).append("=\"")
                        .append(escapeXmlAttr(attrs.getValue(i))).append("\"");
            }
            parent.xmlContent.append(">");
            stack.push(new ElementFrame(parent, FrameType.PARSE_TYPE_LITERAL, parent.baseURI, parent.lang));
        }

        private String resolveScopedBase(ElementFrame parent, Attributes attrs) {
            String currentBase = parent != null ? parent.baseURI : initialBaseURI;
            String xmlBase = getXmlAttribute(attrs, "base");
            if (xmlBase != null) {
                int hash = xmlBase.indexOf('#');
                if (hash >= 0) {
                    xmlBase = xmlBase.substring(0, hash);
                }
                currentBase = resolveURI(xmlBase, currentBase);
            }
            return currentBase;
        }

        private String resolveScopedLang(ElementFrame parent, Attributes attrs) {
            String currentLang = parent != null ? parent.lang : null;
            String xmlLang = getXmlAttribute(attrs, "lang");
            if (xmlLang != null) {
                currentLang = xmlLang.isEmpty() ? null : xmlLang;
            }
            return currentLang;
        }

        private void dispatchStartElement(ElementFrame parent, String uri, String localName, String qName,
                                          Attributes attrs, String currentBase, String currentLang) throws ParsingException {
            if (parent == null) {
                if (RDF_NS.equals(uri) && ELEMENT_RDF.equals(localName)) {
                    stack.push(new ElementFrame(null, FrameType.RDF_ROOT, currentBase, currentLang));
                } else {
                    startNodeElement(null, uri, localName, qName, attrs, currentBase, currentLang);
                }
                return;
            }

            switch (parent.type) {
                case RDF_ROOT -> {
                    if (RDF_NS.equals(uri) && ELEMENT_RDF.equals(localName)) {
                        throw new ParsingException("Nested rdf:RDF elements are forbidden.");
                    }
                    startNodeElement(parent, uri, localName, qName, attrs, currentBase, currentLang);
                }
                case NODE_ELEMENT, PARSE_TYPE_RESOURCE ->
                        startPropertyElement(parent, uri, localName, qName, attrs, currentBase, currentLang);
                case PROPERTY_ELEMENT -> {
                    parent.hasChildElements = true;
                    startNodeElement(parent, uri, localName, qName, attrs, currentBase, currentLang);
                }
                case PARSE_TYPE_COLLECTION ->
                        startNodeElement(parent, uri, localName, qName, attrs, currentBase, currentLang);
                default -> throw new ParsingException("Unexpected XML element <" + qName + "> in state " + parent.type);
            }
        }

        private void startNodeElement(ElementFrame parent, String uri, String localName, String qName,
                                      Attributes attrs, String baseURI, String lang) throws ParsingException {
            validateNodeElementName(uri, localName);

            ElementFrame frame = new ElementFrame(parent, FrameType.NODE_ELEMENT, baseURI, lang);
            Resource subject = extractSubject(qName, attrs, baseURI);
            frame.subject = subject;

            if (!(RDF_NS.equals(uri) && ELEMENT_DESCRIPTION.equals(localName))) {
                IRI typeIri = factory.createIRI(expandQName(uri, localName, qName));
                emitTriple(subject, rdfType, typeIri);
            }

            emitNodeAttributes(frame, attrs);
            linkNodeElementToParent(parent, subject);
            stack.push(frame);
        }

        private Resource extractSubject(String qName, Attributes attrs, String baseURI) throws ParsingException {
            String about = attrs.getValue(RDF_NS, ATTR_ABOUT);
            String id = attrs.getValue(RDF_NS, ATTR_ID);
            String nodeID = attrs.getValue(RDF_NS, ATTR_NODE_ID);
            String bagID = attrs.getValue(RDF_NS, ATTR_BAG_ID);

            int idCount = (about != null ? 1 : 0) + (id != null ? 1 : 0) + (nodeID != null ? 1 : 0) + (bagID != null ? 1 : 0);
            if (idCount > 1) {
                throw new ParsingException("Multiple subject-identifying attributes on <" + qName + ">");
            }

            if (about != null) {
                return factory.createIRI(resolveURI(about, baseURI));
            } else if (id != null) {
                validateNCName(id, "rdf:ID");
                String fullId = resolveURI("#" + id, baseURI);
                if (!declaredIDs.add(fullId)) {
                    throw new ParsingException("Duplicate rdf:ID '" + id + "' in document.");
                }
                return factory.createIRI(fullId);
            } else if (nodeID != null) {
                validateNCName(nodeID, "rdf:nodeID");
                String bnodeId = bnodeMap.computeIfAbsent(nodeID, k -> factory.createBNode().getID());
                return factory.createBNode(bnodeId);
            } else {
                return factory.createBNode();
            }
        }

        private void linkNodeElementToParent(ElementFrame parent, Resource subject) {
            if (parent != null && parent.type == FrameType.PROPERTY_ELEMENT) {
                Resource parentSubject = parent.parent.subject;
                emitTriple(parentSubject, parent.predicate, subject);
                if (parent.rdfID != null) {
                    emitReification(parent.rdfID, parentSubject, parent.predicate, subject, parent.baseURI);
                }
            } else if (parent != null && parent.type == FrameType.PARSE_TYPE_COLLECTION) {
                parent.collectionItems.add(subject);
            }
        }

        private void startPropertyElement(ElementFrame parent, String uri, String localName, String qName,
                                          Attributes attrs, String baseURI, String lang) throws ParsingException {
            validatePropertyElementName(uri, localName);

            IRI predicate = resolvePropertyPredicate(parent, uri, localName, qName);
            String parseType = attrs.getValue(RDF_NS, ATTR_PARSE_TYPE);
            String resource = attrs.getValue(RDF_NS, ATTR_RESOURCE);
            String nodeID = attrs.getValue(RDF_NS, ATTR_NODE_ID);
            String datatype = attrs.getValue(RDF_NS, ATTR_DATATYPE);
            String id = attrs.getValue(RDF_NS, ATTR_ID);

            validatePropertyAttributes(qName, resource, nodeID, parseType, datatype);

            if (id != null) {
                validateNCName(id, "rdf:ID");
                String fullId = resolveURI("#" + id, baseURI);
                if (!declaredIDs.add(fullId)) {
                    throw new ParsingException("Duplicate rdf:ID '" + id + "' in document.");
                }
            }

            if (parseType != null) {
                handleParseTypeProperty(parent, parseType, predicate, id, baseURI, lang);
                return;
            }

            ElementFrame frame = new ElementFrame(parent, FrameType.PROPERTY_ELEMENT, baseURI, lang);
            frame.predicate = predicate;
            frame.rdfID = id;
            frame.datatype = datatype;
            frame.resourceAttr = resource;
            frame.nodeIDAttr = nodeID;
            extractEmptyPropertyAttributes(frame, attrs);

            stack.push(frame);
        }

        private IRI resolvePropertyPredicate(ElementFrame parent, String uri, String localName, String qName) {
            if (RDF_NS.equals(uri) && ATTR_LI.equals(localName)) {
                int n = parent.liIndex++;
                return factory.createIRI(RDF_NS + "_" + n);
            }
            return factory.createIRI(expandQName(uri, localName, qName));
        }

        private void validatePropertyAttributes(String qName, String resource, String nodeID, String parseType, String datatype) throws ParsingException {
            if (resource != null && nodeID != null) {
                throw new ParsingException("rdf:resource and rdf:nodeID cannot coexist on <" + qName + ">");
            }
            if (resource != null && parseType != null) {
                throw new ParsingException("rdf:resource and rdf:parseType cannot coexist on <" + qName + ">");
            }
            if (nodeID != null && parseType != null) {
                throw new ParsingException("rdf:nodeID and rdf:parseType cannot coexist on <" + qName + ">");
            }
            if (resource != null && datatype != null) {
                throw new ParsingException("rdf:resource and rdf:datatype cannot coexist on <" + qName + ">");
            }
            if (nodeID != null && datatype != null) {
                throw new ParsingException("rdf:nodeID and rdf:datatype cannot coexist on <" + qName + ">");
            }
        }

        private void handleParseTypeProperty(ElementFrame parent, String parseType, IRI predicate, String id,
                                             String baseURI, String lang) throws ParsingException {
            switch (parseType) {
                case "Resource" -> {
                    ElementFrame frame = new ElementFrame(parent, FrameType.PARSE_TYPE_RESOURCE, baseURI, lang);
                    frame.predicate = predicate;
                    frame.rdfID = id;
                    Resource bNode = factory.createBNode();
                    frame.subject = bNode;
                    emitTriple(parent.subject, predicate, bNode);
                    if (id != null) {
                        emitReification(id, parent.subject, predicate, bNode, baseURI);
                    }
                    stack.push(frame);
                }
                case "Collection" -> {
                    ElementFrame frame = new ElementFrame(parent, FrameType.PARSE_TYPE_COLLECTION, baseURI, lang);
                    frame.predicate = predicate;
                    frame.rdfID = id;
                    stack.push(frame);
                }
                case "Literal" -> {
                    ElementFrame frame = new ElementFrame(parent, FrameType.PARSE_TYPE_LITERAL, baseURI, lang);
                    frame.predicate = predicate;
                    frame.rdfID = id;
                    stack.push(frame);
                }
                default -> throw new ParsingException("Invalid rdf:parseType value '" + parseType + "'");
            }
        }

        private void extractEmptyPropertyAttributes(ElementFrame frame, Attributes attrs) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aUri = attrs.getURI(i);
                String aLocal = attrs.getLocalName(i);
                String aQName = attrs.getQName(i);
                String aVal = attrs.getValue(i);

                if (isSyntaxOrXmlAttribute(aUri, aLocal, aQName)) continue;

                if (RDF_NS.equals(aUri) && ATTR_TYPE.equals(aLocal)) {
                    frame.propertyTypeAttr = aVal;
                } else {
                    IRI propIri = factory.createIRI(expandQName(aUri, aLocal, aQName));
                    frame.propertyAttributes.put(propIri, aVal);
                }
            }
        }

        private void handleEndElement(String qName) throws ParsingException {
            ElementFrame frame = stack.pop();

            switch (frame.type) {
                case PARSE_TYPE_LITERAL -> handleEndLiteral(frame, qName);
                case PARSE_TYPE_COLLECTION -> handleEndCollection(frame);
                case PROPERTY_ELEMENT -> handleEndProperty(frame);
                default -> {
                    // Node elements and root frames require no trailing triple emission
                }
            }
        }

        private void handleEndLiteral(ElementFrame frame, String qName) {
            if (frame.parent != null && frame.parent.type == FrameType.PARSE_TYPE_LITERAL) {
                frame.parent.xmlContent.append("</").append(qName).append(">");
                return;
            }
            String xml = frame.xmlContent.toString();
            Literal lit = factory.createLiteral(xml, factory.createIRI(RDF_XML_LITERAL));
            emitTriple(frame.parent.subject, frame.predicate, lit);
            if (frame.rdfID != null) {
                emitReification(frame.rdfID, frame.parent.subject, frame.predicate, lit, frame.baseURI);
            }
        }

        private void handleEndCollection(ElementFrame frame) {
            Resource listHead = createRdfList(frame.collectionItems);
            emitTriple(frame.parent.subject, frame.predicate, listHead);
            if (frame.rdfID != null) {
                emitReification(frame.rdfID, frame.parent.subject, frame.predicate, listHead, frame.baseURI);
            }
        }

        private void handleEndProperty(ElementFrame frame) throws ParsingException {
            if (frame.hasChildElements) {
                return;
            }
            Resource parentSubject = frame.parent.subject;

            if (frame.resourceAttr != null) {
                Resource obj = factory.createIRI(resolveURI(frame.resourceAttr, frame.baseURI));
                emitTriple(parentSubject, frame.predicate, obj);
                emitPropertyAttributesOnObject(obj, frame);
                if (frame.rdfID != null) {
                    emitReification(frame.rdfID, parentSubject, frame.predicate, obj, frame.baseURI);
                }
            } else if (frame.nodeIDAttr != null) {
                validateNCName(frame.nodeIDAttr, "rdf:nodeID");
                String bnodeId = bnodeMap.computeIfAbsent(frame.nodeIDAttr, k -> factory.createBNode().getID());
                Resource obj = factory.createBNode(bnodeId);
                emitTriple(parentSubject, frame.predicate, obj);
                emitPropertyAttributesOnObject(obj, frame);
                if (frame.rdfID != null) {
                    emitReification(frame.rdfID, parentSubject, frame.predicate, obj, frame.baseURI);
                }
            } else if (!frame.propertyAttributes.isEmpty() || frame.propertyTypeAttr != null) {
                Resource bNode = factory.createBNode();
                emitTriple(parentSubject, frame.predicate, bNode);
                emitPropertyAttributesOnObject(bNode, frame);
                if (frame.rdfID != null) {
                    emitReification(frame.rdfID, parentSubject, frame.predicate, bNode, frame.baseURI);
                }
            } else {
                emitLiteralProperty(parentSubject, frame);
            }
        }

        private void emitLiteralProperty(Resource parentSubject, ElementFrame frame) {
            String rawText = frame.text.toString();
            Literal lit;
            if (frame.datatype != null) {
                lit = factory.createLiteral(rawText, factory.createIRI(resolveURI(frame.datatype, frame.baseURI)));
            } else if (frame.lang != null) {
                lit = factory.createLiteral(rawText, frame.lang);
            } else {
                lit = factory.createLiteral(rawText);
            }
            emitTriple(parentSubject, frame.predicate, lit);
            if (frame.rdfID != null) {
                emitReification(frame.rdfID, parentSubject, frame.predicate, lit, frame.baseURI);
            }
        }

        private void emitNodeAttributes(ElementFrame frame, Attributes attrs) throws ParsingException {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aUri = attrs.getURI(i);
                String aLocal = attrs.getLocalName(i);
                String aQName = attrs.getQName(i);
                String aVal = attrs.getValue(i);

                if (isSyntaxOrXmlAttribute(aUri, aLocal, aQName)) continue;

                if (RDF_NS.equals(aUri) && ATTR_TYPE.equals(aLocal)) {
                    IRI typeIri = factory.createIRI(resolveURI(aVal, frame.baseURI));
                    emitTriple(frame.subject, rdfType, typeIri);
                } else {
                    IRI propIri = factory.createIRI(expandQName(aUri, aLocal, aQName));
                    Literal lit = frame.lang != null ? factory.createLiteral(aVal, frame.lang) : factory.createLiteral(aVal);
                    emitTriple(frame.subject, propIri, lit);
                }
            }
        }

        private void emitPropertyAttributesOnObject(Resource obj, ElementFrame frame) {
            if (frame.propertyTypeAttr != null) {
                IRI typeIri = factory.createIRI(resolveURI(frame.propertyTypeAttr, frame.baseURI));
                emitTriple(obj, rdfType, typeIri);
            }
            for (Map.Entry<IRI, String> entry : frame.propertyAttributes.entrySet()) {
                Literal lit = frame.lang != null ? factory.createLiteral(entry.getValue(), frame.lang) : factory.createLiteral(entry.getValue());
                emitTriple(obj, entry.getKey(), lit);
            }
        }

        private Resource createRdfList(List<Resource> items) {
            if (items.isEmpty()) {
                return rdfNil;
            }
            Resource head = factory.createBNode();
            Resource current = head;
            for (int i = 0; i < items.size(); i++) {
                Resource item = items.get(i);
                emitTriple(current, rdfFirst, item);

                if (i < items.size() - 1) {
                    Resource next = factory.createBNode();
                    emitTriple(current, rdfRest, next);
                    current = next;
                } else {
                    emitTriple(current, rdfRest, rdfNil);
                }
            }
            return head;
        }

        private void emitTriple(Resource s, IRI p, fr.inria.corese.core.next.data.api.term.Value o) {
            model.add(factory.createStatement(s, p, o));
        }

        private void emitReification(String id, Resource s, IRI p, fr.inria.corese.core.next.data.api.term.Value o, String baseURI) {
            IRI stmtUri = factory.createIRI(resolveURI("#" + id, baseURI));
            model.add(factory.createStatement(stmtUri, rdfType, rdfStatement));
            model.add(factory.createStatement(stmtUri, rdfSubject, s));
            model.add(factory.createStatement(stmtUri, rdfPredicate, p));
            model.add(factory.createStatement(stmtUri, rdfObject, o));
        }

        private String getXmlAttribute(Attributes attrs, String localName) {
            String val = attrs.getValue(XML_NS, localName);
            if (val == null) {
                val = attrs.getValue("xml:" + localName);
            }
            return val;
        }

        private boolean isSyntaxOrXmlAttribute(String uri, String localName, String qName) {
            if (RDF_NS.equals(uri)) {
                return switch (localName) {
                    case ATTR_ABOUT, ATTR_ID, ATTR_NODE_ID, ATTR_BAG_ID, ATTR_RESOURCE, ATTR_PARSE_TYPE, ATTR_DATATYPE, ATTR_ABOUT_EACH, ATTR_ABOUT_EACH_PREFIX, ATTR_LI -> true;
                    default -> false;
                };
            }
            if (XML_NS.equals(uri) || "http://www.w3.org/2000/xmlns/".equals(uri)) {
                return true;
            }
            if (qName != null) {
                String lowerQ = qName.toLowerCase();
                if (lowerQ.startsWith("xml:") || lowerQ.startsWith("xmlns:") || lowerQ.equals("xmlns") || lowerQ.startsWith("xml")) {
                    return true;
                }
            }
            return localName != null && localName.toLowerCase().startsWith("xml");
        }

        private void validateAttributes(Attributes attrs) throws ParsingException {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aUri = attrs.getURI(i);
                String aLocal = attrs.getLocalName(i);

                if (RDF_NS.equals(aUri)) {
                    if (ATTR_ABOUT_EACH.equals(aLocal) || ATTR_ABOUT_EACH_PREFIX.equals(aLocal)) {
                        throw new ParsingException("rdf:" + aLocal + " is an obsolete term removed from RDF 1.1.");
                    }
                    if (ATTR_LI.equals(aLocal)) {
                        throw new ParsingException("rdf:li is not allowed as an attribute.");
                    }
                    if (ATTR_BAG_ID.equals(aLocal)) {
                        validateNCName(attrs.getValue(i), "rdf:bagID");
                    }
                }
            }
        }

        private void validateNodeElementName(String uri, String localName) throws ParsingException {
            if (RDF_NS.equals(uri) && FORBIDDEN_NODE_ELEMENT_NAMES.contains(localName)) {
                throw new ParsingException("rdf:" + localName + " cannot be used as a node element name.");
            }
        }

        private void validatePropertyElementName(String uri, String localName) throws ParsingException {
            if (RDF_NS.equals(uri) && FORBIDDEN_PROPERTY_ELEMENT_NAMES.contains(localName)) {
                throw new ParsingException("rdf:" + localName + " cannot be used as a property element name.");
            }
        }

        private void validateNCName(String val, String attrName) throws ParsingException {
            if (val == null || val.isEmpty() || val.contains(":") || val.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
                throw new ParsingException("Invalid NCName for " + attrName + ": '" + val + "'");
            }
            char first = val.charAt(0);
            if (!Character.isLetter(first) && first != '_') {
                throw new ParsingException("Invalid NCName starting character for " + attrName + ": '" + val + "'");
            }
            for (int i = 1; i < val.length(); i++) {
                char c = val.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_') {
                    throw new ParsingException("Invalid NCName character in " + attrName + ": '" + val + "'");
                }
            }
        }

        private String expandQName(String uri, String localName, String qName) {
            return (uri != null && !uri.isEmpty()) ? uri + localName : qName;
        }

        private String resolveURI(String rel, String base) {
            if (rel == null) return base;
            if (base == null || base.isEmpty()) return rel;
            if (rel.isEmpty()) {
                int hash = base.indexOf('#');
                return hash >= 0 ? base.substring(0, hash) : base;
            }
            if (rel.startsWith("#")) {
                int hash = base.indexOf('#');
                String baseNoHash = hash >= 0 ? base.substring(0, hash) : base;
                return baseNoHash + rel;
            }
            if (rel.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                return rel;
            }
            try {
                return URI.create(base).resolve(rel).toString();
            } catch (Exception e) {
                return rel;
            }
        }

        private String escapeXmlChars(char[] ch, int start, int length) {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < start + length; i++) {
                char c = ch[i];
                if (c == '&') sb.append("&amp;");
                else if (c == '<') sb.append("&lt;");
                else if (c == '>') sb.append("&gt;");
                else sb.append(c);
            }
            return sb.toString();
        }

        private String escapeXmlAttr(String val) {
            if (val == null) return "";
            return val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
