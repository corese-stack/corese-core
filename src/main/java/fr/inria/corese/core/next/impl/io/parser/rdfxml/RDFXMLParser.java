package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfxml.context.RDFXMLContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLUtils.*;

/**
 * SAX-based RDF/XML parser implementation.
 *
 * <p>This parser utilizes the SAX streaming API combined with an explicit
 * stack-based context ({@link RDFXMLContext}) to correctly track nested RDF
 * structures, including resources, property elements, literals, containers,
 * and collections. It fully supports standard RDF/XML features such as
 * xml:lang, rdf:datatype, rdf:parseType, and property attributes.</p>
 *
 * <p>RDF statements generated during parsing are added to the provided {@link Model}
 * using the supplied {@link ValueFactory}.</p>
 */
public class RDFXMLParser extends AbstractRDFParser {

    /** RDF/XML format identifier for this parser. */
    private final RDFFormat format = RDFFormat.RDFXML;

    /** Buffer for accumulating character data between start and end tags. */
    private final StringBuilder characters = new StringBuilder();

    /*** The shared state/context for tracking namespaces, base URI, and RDF node stacks.*/
    private final RDFXMLContext ctx;

    /**
     * Utility class responsible for creating and adding triples to the Model.
     */
    private final RDFXMLStatementEmitter emitter;

    /*** Stores rdf:ID values to detect and prevent duplicate definitions.*/
    private final Set<String> usedIDs = new HashSet<>();

    /*** Tracks the counter (rdf:_1, rdf:_2, ...) for each active RDF container (Bag, Seq, Alt).*/
    private final Map<Resource, Integer> containerCounters = new HashMap<>();
    private int rdfDepth = 0;

    private String lastElementQName = null;
    private String lastElementRdfId = null;

    public RDFXMLParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFXMLParserOptions.Builder().build());
    }

    /**
     * Constructs an RDFXMLParser with specified options.
     * @param model The model to which triples will be added.
     * @param factory The factory used to create RDF values.
     * @param config The IO configuration options.
     */
    public RDFXMLParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
        this.ctx = new RDFXMLContext(getModel(), getValueFactory());
        this.emitter = new RDFXMLStatementEmitter(model, factory);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return format;
    }


    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        // Initialize context and state for a new parse operation
        ctx.baseURI = baseURI;
        usedIDs.clear();
        containerCounters.clear();
        rdfDepth = 0;  // Reset depth counter for each document

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);

            SAXParser saxParser = factory.newSAXParser();
            InputSource inputSource = new InputSource(reader);
            // Parse the input using the custom handler
            saxParser.parse(inputSource, new RdfXmlSaxHandler());
        } catch (SAXException e) {
            // Unpack custom ParsingErrorException if it was wrapped in SAXException
            if (e.getCause() instanceof ParsingErrorException) {
                throw (ParsingErrorException) e.getCause();
            }
            throw new ParsingErrorException("Unexpected error during RDF/XML parsing: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse RDF/XML input stream: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during RDF/XML parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Internal SAX handler that delegates processing logic to the parser's methods.
     * This wrapper catches exceptions thrown by the core logic and wraps them
     * in {@link SAXException} to stop the SAX parser.
     */
    private class RdfXmlSaxHandler extends DefaultHandler {
        @Override
        public void characters(char[] ch, int start, int length) {
            RDFXMLParser.this.handleCharacters(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            try {
                // Delegates start element processing
                RDFXMLParser.this.handleStartElement(uri, localName, qName, attrs);
            } catch (ParsingErrorException e) {
                // Re-throw as SAXException to halt parsing process
                throw new SAXException(e);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            try {
                // Delegates end element processing
                RDFXMLParser.this.handleEndElement(uri, localName);
            } catch (ParsingErrorException e) {
                // Re-throw as SAXException to halt parsing process
                throw new SAXException(e);
            }
        }
    }

    /**
     * Handles character data between XML elements
     */
    private void handleCharacters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    private void handleStartElement(String uri, String localName, String qName, Attributes attrs)
            throws ParsingErrorException {

        if (qName.equals(lastElementQName)) {
            String rdfId = attrs.getValue(RDF.type.getNamespace(), "ID");
            if (rdfId != null && rdfId.equals(lastElementRdfId)) {
                return;
            }
            lastElementRdfId = rdfId;
        } else {
            lastElementQName = qName;
            lastElementRdfId = attrs.getValue(RDF.type.getNamespace(), "ID");
        }

        // Check for the top-level <rdf:RDF> element
        if (RDFXMLUtils.isRdfRDF(uri, localName)) {
            rdfDepth++;
            if (rdfDepth > 1) {
                throw new ParsingErrorException(
                        "rdf:RDF cannot be used as a node element. Nested rdf:RDF elements are not allowed.");
            }
            return;
        }

        // Clear the character buffer at the start of a new element.
        characters.setLength(0);

        // Update context based on XML/RDF syntax attributes
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
     * Handles the end of an XML element, processing accumulated literal content or cleaning up context stacks.
     */
    private void handleEndElement(String uri, String localName) throws ParsingErrorException {
        // Handle <rdf:RDF> closing tag cleanup.
        if (RDFXMLUtils.isRdfRDF(uri, localName)) {
            rdfDepth--;
            return;
        }

        String text = characters.toString().trim();
        characters.setLength(0);

        if (!ctx.predicateStack.isEmpty() && !text.isEmpty()) {
            IRI predicate = ctx.predicateStack.pop();

            // CRITICAL FIX: Ensure a subject exists before creating a triple.
            if (ctx.subjectStack.isEmpty()) {
                throw new ParsingErrorException(
                        "Cannot emit literal: no subject available for predicate " + predicate);
            }

            Resource subject = ctx.subjectStack.peek();
            // Datatype is popped, but lang is peeked (lang applies to parent node scope).
            String datatypeUri = ctx.datatypeStack.isEmpty() ? null : ctx.datatypeStack.pop();
            String lang = ctx.langStack.isEmpty() ? null : ctx.langStack.peek();
            emitter.emitLiteral(subject, predicate, text, datatypeUri, lang);

            cleanEndElement(uri, localName);
            return;
        }

        if (!ctx.predicateStack.isEmpty()) {
            ctx.predicateStack.pop();
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
        if (datatype != null) ctx.datatypeStack.push(datatype);
    }

    private boolean processCollectionStart(String localName, String uri, String qName, Attributes attrs)
            throws ParsingErrorException {
        String parseType = getParseType(attrs);
        // Checks for rdf:parseType="Collection"
        if (!"Collection".equals(parseType)) return false;

        RDFXMLUtils.validateParseType(parseType);

        // Initialize collection state
        ctx.inCollection = true;
        ctx.collectionBuilder = new ArrayList<>();
        // The subject of the collection statement is the current resource on the subject stack
        ctx.collectionSubject = ctx.subjectStack.peek();
        // The predicate is the property element that contains the collection
        ctx.collectionPredicate = ctx.factory.createIRI(expandQName(uri, localName, qName));
        return true;
    }

    private boolean processCollectionItem(String localName, String uri, Attributes attrs)
            throws ParsingErrorException {
        // Checks if we are inside a collection and the current element is a node element (rdf:Description or typed node).
        if (!ctx.inCollection || !RDFXMLUtils.isDescription(localName, uri)) return false;

        // Extracts the resource URI or blank node ID for the collection item.
        Resource item = extractSubject(attrs, ctx.factory, ctx.baseURI, usedIDs);
        ctx.collectionBuilder.add(item);
        ctx.suppressSubject = true;

        return true;
    }

    private boolean processContainerElement(String localName, String uri, String qName, Attributes attrs)
            throws ParsingErrorException {

        if (isContainer(localName, uri)) {
            Resource subject = extractSubject(attrs, ctx.factory, ctx.baseURI, usedIDs);
            ctx.subjectStack.push(subject);
            ctx.inContainer = true;
            containerCounters.put(subject, 0);
            emitter.emitType(subject, expandQName(uri, localName, qName));
            return true;
        }

        if (RDF.type.getNamespace().equals(uri)) {
            if ("li".equals(localName)) {
                Resource currentContainer = ctx.subjectStack.isEmpty() ? null : ctx.subjectStack.peek();
                if (currentContainer == null) {
                    throw new ParsingErrorException("Container item found without a container subject");
                }

                // Generate rdf:_n predicate
                int counter = containerCounters.getOrDefault(currentContainer, 0) + 1;
                containerCounters.put(currentContainer, counter);
                String pred = RDF.type.getNamespace() + "_" + counter;

                IRI predicate = ctx.factory.createIRI(pred);
                String resource = attrs.getValue(RDF.type.getNamespace(), "resource");


                if (resource != null) {
                    emitter.emitResourceTriple(currentContainer, predicate, resource, ctx.baseURI);
                } else {
                    ctx.predicateStack.push(predicate);
                }
                return true;
            }

            // Handle explicit rdf:_n (container membership properties)
            if (localName.matches("_\\d+")) {
                if (ctx.inContainer) {
                    Resource currentContainer = ctx.subjectStack.isEmpty() ? null : ctx.subjectStack.peek();
                    if (currentContainer == null) {
                        throw new ParsingErrorException("Container item found without a container subject");
                    }

                    String pred = RDF.type.getNamespace() + localName;
                    IRI predicate = ctx.factory.createIRI(pred);
                    String resource = attrs.getValue(RDF.type.getNamespace(), "resource");

                    // If rdf:resource is present, emit a resource triple directly
                    if (resource != null) {
                        emitter.emitResourceTriple(currentContainer, predicate, resource, ctx.baseURI);
                    } else {
                        // Otherwise, push the rdf:_n predicate for the following object
                        ctx.predicateStack.push(predicate);
                    }
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private boolean processNodeElement(String localName, String uri, String qName, Attributes attrs)
            throws ParsingErrorException {
        boolean hasParentSubject = !ctx.subjectStack.isEmpty();

        if (hasParentSubject) {
            String resource = attrs.getValue(RDF.type.getNamespace(), "resource");
            String nodeIDAttr = attrs.getValue(RDF.type.getNamespace(), "nodeID");

            if (resource != null || (nodeIDAttr != null && !isDescription(localName, uri))) {
                return false;
            }
        }

        boolean isNode = isDescription(localName, uri)
                || hasNodeIdentifyingAttributes(attrs)
                || ctx.subjectStack.isEmpty();

        if (!isNode) return false;

        // Validation required for a Node Element name.
        RDFXMLUtils.validateNodeElementName(uri, localName);


        Resource newSubject = RDFXMLUtils.extractSubject(attrs, ctx.factory, ctx.baseURI, usedIDs);

        // If a predicate is pending, the new subject is the object of the pending triple.
        if (!ctx.predicateStack.isEmpty() && !ctx.subjectStack.isEmpty()) {
            Resource parent = ctx.subjectStack.peek();
            IRI predicate = ctx.predicateStack.pop();
            emitter.emitTriple(parent, predicate, newSubject);
        }

        // Push the new resource as the active subject.
        ctx.subjectStack.push(newSubject);

        // If the element is not rdf:Description, it is a typed node, so emit rdf:type.
        if (!isDescription(localName, uri)) {
            emitter.emitType(newSubject, expandQName(uri, localName, qName));
        }

        // Emit any property attributes defined on the node element (e.g., properties not using sub-elements).
        emitter.emitPropertyAttributes(newSubject, attrs);
        return true;
    }

    private void processPropertyElement(String localName, String uri, String qName, Attributes attrs)
            throws ParsingErrorException {

        // CRITICAL FIX: Validate property element name against RDF/XML constraints.
        RDFXMLUtils.validatePropertyElementName(uri, localName);

        // Determine the predicate URI and push it to the stack (for potential literal content).
        IRI predicate = ctx.factory.createIRI(RDFXMLUtils.expandQName(uri, localName, qName));
        ctx.predicateStack.push(predicate);

        // Extract RDF syntax attributes.
        String resource = attrs.getValue(RDF.type.getNamespace(), "resource");
        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");
        String parseType = attrs.getValue(RDF.type.getNamespace(), "parseType");
        String bagID = attrs.getValue(RDF.type.getNamespace(), "bagID");

        // Validate rdf:bagID usage (only allowed on Node Elements).
        if (bagID != null) {
            throw new ParsingErrorException(
                    "rdf:bagID cannot be used on property elements. " +
                            "It can only be used on node elements (typed nodes or rdf:Description).");
        }

        // Validate mutually exclusive attributes (resource, nodeID, parseType).
        if (resource != null && nodeID != null) {
            throw new ParsingErrorException(
                    "Both rdf:resource and rdf:nodeID cannot be present on the same property element");
        }

        if (resource != null && parseType != null) {
            throw new ParsingErrorException(
                    "rdf:resource and rdf:parseType cannot be used together on the same property element");
        }

        if (nodeID != null && parseType != null) {
            throw new ParsingErrorException(
                    "rdf:nodeID and rdf:parseType cannot be used together on the same property element");
        }

        // --- Case 1: Property Element with rdf:resource (Object is a Resource) ---
        if (resource != null) {
            if (ctx.subjectStack.isEmpty()) {
                throw new ParsingErrorException("Property element with rdf:resource has no subject");
            }

            // CRITICAL FIX: rdf:resource cannot coexist with rdf:datatype on property elements.
            String datatype = attrs.getValue(RDF.type.getNamespace(), "datatype");
            if (datatype != null) {
                throw new ParsingErrorException(
                        "rdf:resource and rdf:datatype cannot be used together on the same property element");
            }

            // Emit the S-P-O triple where O is the resource URI.
            emitter.emitResourceTriple(ctx.subjectStack.peek(), predicate, resource, ctx.baseURI);

            // Emit property attributes as properties of the parent subject
            if (hasRealPropertyAttributes(attrs)) {
                emitter.emitPropertyAttributes(ctx.subjectStack.peek(), attrs);
            }

            // Pop the predicate, as the triple is now complete.
            ctx.predicateStack.pop();
            return;
        }

        // --- Case 2: Property Element with rdf:nodeID (Object is a Blank Node) ---
        if (nodeID != null) {
            if (ctx.subjectStack.isEmpty()) {
                throw new ParsingErrorException("Property element with rdf:nodeID has no subject");
            }

            // CRITICAL FIX: Validate nodeID format (must be valid NCName).
            if (RDFXMLUtils.isInvalidXMLName(nodeID, false)) {
                throw new ParsingErrorException(
                        "rdf:nodeID value '" + nodeID + "' is not a valid NCName. " +
                                "NCNames cannot contain colons and must start with a letter or underscore.");
            }

            // rdf:nodeID cannot coexist with rdf:datatype on property elements.
            String datatype = attrs.getValue(RDF.type.getNamespace(), "datatype");
            if (datatype != null) {
                throw new ParsingErrorException(
                        "rdf:nodeID and rdf:datatype cannot be used together on the same property element");
            }

            // Emit the S-P-O triple where O is the blank node specified by nodeID.
            emitter.emitBNodeTriple(ctx.subjectStack.peek(), predicate, nodeID);

            // Emit property attributes (if any) as properties of the parent subject.
            if (hasRealPropertyAttributes(attrs)) {
                emitter.emitPropertyAttributes(ctx.subjectStack.peek(), attrs);
            }

            // Pop the predicate, as the triple is now complete.
            ctx.predicateStack.pop();
            return;
        }

        // --- Case 3: Property Element with rdf:parseType="Resource" ---
        if (parseType != null) {
            RDFXMLUtils.validateParseType(parseType);

            if ("Resource".equals(parseType)) {
                // Creates a new blank node, emits S-P-BNode triple, and makes BNode the new subject.
                Resource bnode = emitBnodePredicateObject(predicate);
                ctx.subjectStack.push(bnode);
                return;
            }
        }

        // --- Case 4: Property Element with attributes only (Abbreviated form for anonymous Blank Node) ---
        if (hasNonSyntaxAttributes(attrs)) {
            // Creates a new blank node (O), emits S-P-BNode triple, and adds attributes as properties of BNode.
            Resource bnode = emitBnodePredicateObject(predicate);
            emitter.emitPropertyAttributes(bnode, attrs);
            // Triple is complete, so pop the predicate.
            ctx.predicateStack.pop();
        }
    }

    private boolean hasRealPropertyAttributes(Attributes attrs) {
        // Checks if an element has non-RDF-syntax attributes that should be treated as properties.
        for (int i = 0; i < attrs.getLength(); i++) {
            String attrURI = attrs.getURI(i);
            String attrLocal = attrs.getLocalName(i);
            String attrQName = attrs.getQName(i);

            // Skip attributes defining RDF/XML syntax (rdf:ID, rdf:about, rdf:resource, etc.)
            if (isSyntaxAttribute(attrURI, attrLocal, attrQName)) {
                continue;
            }

            // Skip XML-namespace attributes (xml:lang, xml:base, etc.)
            // Check both URI and QName prefix because SAX parsers may return empty URI
            if ("http://www.w3.org/XML/1998/namespace".equals(attrURI) ||
                    (attrQName != null && attrQName.startsWith("xml:"))) {
                continue;
            }

            return true;
        }
        return false;
    }

    private boolean hasNonSyntaxAttributes(Attributes attrs) {
        for (int i = 0; i < attrs.getLength(); i++) {
            if (!isSyntaxAttribute(attrs.getURI(i), attrs.getLocalName(i), attrs.getQName(i))) {
                return true;
            }
        }
        return false;
    }

    private Resource emitBnodePredicateObject(IRI predicate) throws ParsingErrorException {
        // Creates a new anonymous blank node and emits a triple from the current subject using the given predicate.
        if (ctx.subjectStack.isEmpty()) {
            throw new ParsingErrorException("Cannot create blank node object: no subject available");
        }
        Resource parent = ctx.subjectStack.peek();
        Resource bnode = ctx.factory.createBNode();
        emitter.emitTriple(parent, predicate, bnode);
        return bnode;
    }

    /**
     * Checks if attributes contain node-identifying attributes (rdf:about, rdf:ID, rdf:bagID).
     * This is used to distinguish Node Elements from Property Elements without explicit rdf:resource/rdf:nodeID.
     */
    private boolean hasNodeIdentifyingAttributes(Attributes attrs) {
        return attrs.getValue(RDF.type.getNamespace(), "about") != null ||
                attrs.getValue(RDF.type.getNamespace(), "ID") != null ||
                attrs.getValue(RDF.type.getNamespace(), "bagID") != null;
    }

    private void cleanEndElement(String uri, String localName) {
        if (!ctx.langStack.isEmpty()) ctx.langStack.pop();

        if (RDFXMLUtils.isContainer(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) {
                Resource container = ctx.subjectStack.pop();
                containerCounters.remove(container);
            }
            ctx.inContainer = false;
            return;
        }

        if (ctx.inCollection && ctx.collectionPredicate != null
                && localName.equals(ctx.collectionPredicate.getLocalName())) {
            Resource listHead = createRdfCollection(ctx.collectionBuilder, ctx.model, ctx.factory);
            ctx.model.add(ctx.factory.createStatement(ctx.collectionSubject, ctx.collectionPredicate, listHead));

            ctx.inCollection = false;
            ctx.collectionBuilder.clear();
            return;
        }

        if (ctx.inCollection && RDFXMLUtils.isDescription(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) {
                ctx.subjectStack.pop();
            }
            return;
        }

        if (RDFXMLUtils.isDescription(localName, uri) || RDFXMLUtils.isRdfNodeElementType(uri, localName)) {
            if (!ctx.subjectStack.isEmpty()) {
                ctx.subjectStack.pop();
            }
        }
    }
}