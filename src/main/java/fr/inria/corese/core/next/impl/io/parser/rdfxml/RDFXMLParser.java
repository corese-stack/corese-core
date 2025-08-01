package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfxml.context.RdfXmlContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLUtils.*;

/**
 * SAX-based RDF/XML parser using a shared parsing context ({@link RdfXmlContext}).
 *
 * <p>This parser processes RDF/XML documents using the SAX streaming API.
 * It tracks RDF constructs (resources, properties, literals, containers, collections)
 * using an explicit stack-based context, and supports features like xml:lang,
 * rdf:datatype, rdf:parseType, and property attributes.</p>
 *
 * <p>The parser adds RDF statements to the provided {@link Model} using
 * the supplied {@link ValueFactory}. This parser supports nested nodes,
 * blank nodes, typed nodes, and RDF collections.</p>
 */
public class RDFXMLParser extends AbstractRDFParser {

    /** RDF/XML format identifier for this parser. */
    private final RDFFormat format = RDFFormat.RDFXML;

    /** Buffer for accumulating character data between start and end tags. */
    private StringBuilder characters = new StringBuilder();

    /** Shared state across SAX callbacks. */
    private RdfXmlContext ctx;

    private final RDFXMLStatementEmitter emitter;

    /**
     * Creates a new parser with a target RDF model and factory.
     *
     * @param model   the RDF model to populate
     * @param factory the RDF value factory for term creation
     */
    public RDFXMLParser(Model model, ValueFactory factory) {
        this(model, factory, null);
    }

    /**
     * Creates a new parser with a target RDF model, factory, and configuration options.
     *
     * @param model   the RDF model to populate
     * @param factory the RDF value factory for term creation
     * @param config  optional configuration options for the parser
     */
    public RDFXMLParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
        this.ctx = new RdfXmlContext(getModel(), getValueFactory());
        this.emitter = new RDFXMLStatementEmitter(model, factory);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return format;
    }

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    public void parse(Reader reader) throws ParsingErrorException {
        parse(reader, null);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        ctx.baseURI = baseURI;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            InputSource inputSource = new InputSource(reader);
            saxParser.parse(inputSource, new RdfXmlSaxHandler());
        }
        catch (IOException e) {
            throw new ParsingErrorException("Failed to parse RDF/XML input stream: " + e.getMessage() , e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during RDF/XML parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Internal SAX handler that delegates to the parser's methods
     */
    private class RdfXmlSaxHandler extends DefaultHandler {

        @Override
        public void characters(char[] ch, int start, int length) {
            RDFXMLParser.this.handleCharacters(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            RDFXMLParser.this.handleStartElement(uri, localName, qName, attrs);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            RDFXMLParser.this.handleEndElement(uri, localName, qName);
        }
    }

    /**
     * Handles character data between XML elements
     */
    private void handleCharacters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    /**
     * Handles opening of an XML element.
     * Identifies node elements, container constructs, properties,
     * and special parseType attributes, updating the parsing context accordingly.
     */
    private void handleStartElement(String uri, String localName, String qName, Attributes attrs) {

        // Skip the top-level rdf:RDF wrapper element
        if (RDFXMLUtils.isRdfRDF(uri, localName)) return;

        // Reset character buffer
        characters.setLength(0);

        // Handle xml:base (change base URI dynamically)
        updateBase(attrs);

        // Handle xml:lang
        updateLang(attrs);

        // Handle rdf:datatype (applies to property literal values)
        updateDatatype(attrs);

        if (processContainerElement(localName, uri, qName, attrs)) return;
        if (processCollectionStart(localName, uri, qName, attrs)) return;
        if (processCollectionItem(localName, uri, attrs)) return;
        if (processNodeElement(localName, uri, qName, attrs)) return;
        processPropertyElement(localName, uri, qName, attrs);
    }

    /**
     * Handles the end of an XML element, emitting a literal or cleaning up context stacks.
     */
    private void handleEndElement(String uri, String localName, String qName) {
        String text = characters.toString().trim();
        characters.setLength(0);

        if (!ctx.predicateStack.isEmpty() && !text.isEmpty()) {
            IRI predicate = ctx.predicateStack.pop();
            Resource subject = ctx.subjectStack.peek();
            String datatypeUri = ctx.datatypeStack.isEmpty() ? null : ctx.datatypeStack.pop();
            //emitLiteral(subject, predicate, text, datatypeUri);
            String lang = ctx.langStack.isEmpty() ? null : ctx.langStack.peek();
            emitter.emitLiteral(subject, predicate, text, datatypeUri, lang);
            return;
        }
        cleanEndElement(uri, localName);
    }

    /**
     * Updates the base URI for IRI resolution using the xml:base attribute if present.
     *
     * @param attrs the XML attributes of the current element
     */
    private void updateBase(Attributes attrs) {
        String xmlBase = attrs.getValue("xml:base");
        if (xmlBase != null) ctx.baseURI = xmlBase;
    }

    /**
     * Updates the language context using the xml:lang attribute if present.
     * The language value is pushed onto a stack to support nested scope.
     *
     * @param attrs the XML attributes of the current element
     */
    private void updateLang(Attributes attrs) {
        String xmlLang = attrs.getValue("xml:lang");
        if (xmlLang != null) ctx.langStack.push(xmlLang);
    }


    /**
     * Updates the datatype context using the rdf:datatype attribute if present.
     * The datatype URI is pushed onto a stack to support nested scope.
     *
     * @param attrs the XML attributes of the current element
     */
    private void updateDatatype(Attributes attrs) {
        String datatype = attrs.getValue(RDF.type.getNamespace(), "datatype");
        if (datatype != null) {
            ctx.datatypeStack.push(datatype);
        }
    }

    /**
     * Processes the start of an RDF collection indicated by parseType="Collection".
     * Initializes the internal collection structures and returns true if this is a collection.
     *
     * @param localName the local name of the element
     * @param uri       the namespace URI
     * @param qName     the qualified name
     * @param attrs     the attributes of the element
     * @return true if this element starts a collection, false otherwise
     */
    private boolean processCollectionStart(String localName, String uri, String qName, Attributes attrs) {
        if (!"Collection".equals(getParseType(attrs))) return false;
        IRI predicate = ctx.factory.createIRI(RDFXMLUtils.expandQName(uri, localName, qName));
        prepareCollection(predicate);
        return true;
    }

    /**
     * Prepares internal context to collect RDF list elements for a collection.
     *
     * @param predicate the predicate that points to the collection
     */
    private void prepareCollection(IRI predicate) {
        ctx.predicateStack.push(predicate);
        ctx.collectionSubject = ctx.subjectStack.peek();
        ctx.collectionPredicate = predicate;
        ctx.collectionBuilder = new ArrayList<>();
        ctx.inCollection = true;
    }

    /**
     * Processes an item inside an RDF collection. Adds the extracted subject to the collection list.
     *
     * @param localName the local name of the element
     * @param uri       the namespace URI
     * @param attrs     the attributes of the element
     * @return true if the element is processed as a collection item, false otherwise
     */
    private boolean processCollectionItem(String localName, String uri, Attributes attrs) {
        if (!ctx.inCollection || !RDFXMLUtils.isDescription(localName, uri)) return false;

        Resource item = extractSubject(attrs, ctx.factory, ctx.baseURI);
        ctx.collectionBuilder.add(item);
        ctx.suppressSubject = true;

        return true;
    }

    /**
     * Processes RDF container elements like rdf:Bag, rdf:Seq, and code rdf:Alt,
     * as well as container items like rdf:li and rdf:_n.
     *
     * @param localName the local name of the element
     * @param uri       the namespace URI
     * @param qName     the qualified name
     * @param attrs     the attributes of the element
     * @return true if the element is a container or container item, false otherwise
     */
    private boolean processContainerElement(String localName, String uri, String qName, Attributes attrs) {
        // --- RDF Container Element ---
        if (isContainer(localName, uri)) {
            Resource subject = extractSubject(attrs, ctx.factory, ctx.baseURI);
            ctx.subjectStack.push(subject);
            ctx.inContainer = true;
            ctx.liIndex = 1;
            emitter.emitType(subject, expandQName(uri, localName, qName));

            return true;
        }

        // --- Container Items (rdf:li, rdf:_n) ---

        if (ctx.inContainer && RDF.type.getNamespace().equals(uri)) {
            String pred = switch (localName) {
                case "li" -> RDF.type.getNamespace() + "_" + ctx.liIndex++;
                default -> localName.matches("_\\d+") ? RDF.type.getNamespace() + localName : null;
            };

            if (pred != null) {
                IRI predicate = ctx.factory.createIRI(pred);
                String resource = attrs.getValue("rdf:resource");
                if (resource != null) {
                    emitter.emitResourceTriple(ctx.subjectStack.peek(), predicate, resource, ctx.baseURI);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Processes an RDF node element such as rdf:Description or a typed node.
     * Handles subject creation, optional rdf:type triple emission, and property attributes.
     *
     * @param localName the local name of the element
     * @param uri       the namespace URI
     * @param qName     the qualified name
     * @param attrs     the element's attributes
     * @return true if the element is processed as an RDF node, false otherwise
     */
    private boolean processNodeElement(String localName, String uri, String qName, Attributes attrs) {
        boolean isNode = isDescription(localName, uri)
                || (ctx.subjectStack.isEmpty() && RDFXMLUtils.isNodeElement(attrs));

        if (!isNode) return false;

        Resource newSubject = RDFXMLUtils.extractSubject(attrs, ctx.factory, ctx.baseURI);

        // Add triple if nested in another node as object
        if (!ctx.predicateStack.isEmpty() && !ctx.subjectStack.isEmpty()) {
            Resource parent = ctx.subjectStack.peek();
            IRI predicate = ctx.predicateStack.pop();
            emitter.emitTriple(parent, predicate, newSubject);
        }

        ctx.subjectStack.push(newSubject);

        // Emit rdf:type if typed node
        if (!isDescription(localName, uri)) {
           emitter.emitType(newSubject, expandQName(uri, localName, qName));
        }

        // Handle non-syntax attributes
        emitter.emitPropertyAttributes(newSubject, attrs);
        return true;
    }

    /**
     * Processes an RDF property element and emits triples accordingly.
     * Handles {@code rdf:resource}, {@code rdf:nodeID}, {@code parseType="Resource"},
     * and inline property attributes.
     *
     * @param localName the local name of the property element
     * @param uri       the namespace URI
     * @param qName     the qualified name
     * @param attrs     the element's attributes
     *
     * @return true if the element is processed as an RDF property element, false otherwise
     */
    private boolean processPropertyElement(String localName, String uri, String qName, Attributes attrs) {
        IRI predicate = ctx.factory.createIRI(RDFXMLUtils.expandQName(uri, localName, qName));
        ctx.predicateStack.push(predicate);

        String resource = attrs.getValue(RDF.type.getNamespace(), "resource");
        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");

        if (resource != null) {
            emitter.emitResourceTriple(ctx.subjectStack.peek(), predicate, resource, ctx.baseURI);
            ctx.predicateStack.pop();
            return true;
        }

        if (nodeID != null) {
            emitter.emitBNodeTriple(ctx.subjectStack.peek(), predicate, nodeID);
            ctx.predicateStack.pop();
            return true;
        }

        // parseType="Resource"
        String parseType = getParseType(attrs);
        if ("Resource".equals(parseType)) {
            Resource bnode = emitBnodePredicateObject(predicate);
            ctx.subjectStack.push(bnode);
            return true;
        }

        // Inline attributes
        if (hasNonSyntaxAttributes(attrs)) {
            Resource bnode = emitBnodePredicateObject(predicate);
            emitter.emitPropertyAttributes(bnode, attrs);
            ctx.predicateStack.pop();
            return true;
        }
        return false;
    }

    /**
     * Checks if the given attributes contain any non-syntax (i.e., user-defined) attributes.
     *
     * @param attrs the XML attributes to inspect
     * @return true if at least one attribute is not a reserved RDF or XML syntax attribute
     */
    private boolean hasNonSyntaxAttributes(Attributes attrs) {
        for (int i = 0; i < attrs.getLength(); i++) {
            if (!isSyntaxAttribute(attrs.getURI(i), attrs.getLocalName(i), attrs.getQName(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Emits a blank node as the object of the current predicate and links it to the subject.
     *
     * @param predicate the predicate of the triple
     * @return the newly created blank node
     */
    private Resource emitBnodePredicateObject(IRI predicate) {
        Resource parent = ctx.subjectStack.peek();
        Resource bnode = ctx.factory.createBNode();
        emitter.emitTriple(parent, predicate, bnode);
        return bnode;
    }



    /**
     * Cleans up parsing context stacks when an XML end element is encountered.
     * @param uri        the namespace URI of the element
     * @param localName  the local name of the element
     */
    private void cleanEndElement(String uri, String localName) {
        if (!ctx.langStack.isEmpty()) ctx.langStack.pop();
        if (!ctx.predicateStack.isEmpty()) ctx.predicateStack.pop();
        if (RDFXMLUtils.isContainer(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) ctx.subjectStack.pop();
            ctx.inContainer = false;
            ctx.liIndex = 1;
            return;
        }
        if (ctx.inCollection && localName.equals(ctx.collectionPredicate.getLocalName())) {
            Resource listHead = createRdfCollection(ctx.collectionBuilder, ctx.model, ctx.factory);
            ctx.model.add(ctx.factory.createStatement(ctx.collectionSubject, ctx.collectionPredicate, listHead));
            ctx.inCollection = false;
            ctx.collectionBuilder.clear();
            return;
        }
        if (ctx.inCollection && RDFXMLUtils.isDescription(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) ctx.subjectStack.pop();
            return;
        }
        if (RDFXMLUtils.isDescription(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) ctx.subjectStack.pop();
        }
        if (!ctx.subjectStack.isEmpty() && !ctx.predicateStack.isEmpty()) {
            ctx.subjectStack.pop();
            ctx.predicateStack.pop();
        }
    }
}