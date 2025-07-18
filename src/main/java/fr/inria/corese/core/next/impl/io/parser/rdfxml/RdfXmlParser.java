package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.io.parser.rdfxml.context.RdfXmlContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Optional;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RdfXmlUtils.*;

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
public class RdfXmlParser  extends DefaultHandler implements RDFParser {

    /** RDF/XML format identifier for this parser. */
    private final RDFFormat format = RDFFormats.RDF_XML;

    /** Buffer for accumulating character data between start and end tags. */
    private StringBuilder characters = new StringBuilder();

    /** Shared state across SAX callbacks. */
    private RdfXmlContext ctx;

    /**
     * Creates a new parser with a target RDF model and factory.
     *
     * @param model   the RDF model to populate
     * @param factory the RDF value factory for term creation
     */
    public RdfXmlParser(Model model, ValueFactory factory) {
        this.ctx = new RdfXmlContext(model, factory);
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
        ctx.baseURI = baseURI;
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
        ctx.baseURI = baseURI;
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
    public void characters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    /**
     * Handles opening of an XML element.
     * Identifies node elements, container constructs, properties,
     * and special parseType attributes, updating the parsing context accordingly.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        // Skip the top-level rdf:RDF wrapper element
        if (RdfXmlUtils.isRdfRDF(uri, localName)) return;

        // Reset character buffer
        characters.setLength(0);

        // Handle xml:base (change base URI dynamically)
        String xmlBase = attrs.getValue("xml:base");
        if (xmlBase != null) {
            ctx.baseURI = xmlBase;
        }

        // Handle xml:lang
        String xmlLang = attrs.getValue("xml:lang");
        if (xmlLang != null) {
            ctx.langStack.push(xmlLang);
        }

        // Handle rdf:datatype (applies to property literal values)
        String datatype = attrs.getValue(RDF.type.getNamespace(), "datatype");
        if (datatype != null) {
            ctx.datatypeStack.push(datatype);
        }

        // --- RDF Container Element ---
        if (RdfXmlUtils.isContainer(localName, uri)) {
            Resource subject = RdfXmlUtils.extractSubject(attrs, ctx.factory, ctx.baseURI);
            ctx.subjectStack.push(subject);
            ctx.inContainer = true;
            ctx.liIndex = 1;

            IRI typeIRI = ctx.factory.createIRI(RdfXmlUtils.expandQName(uri, localName, qName));
            ctx.model.add(ctx.factory.createStatement(subject, RDF.type.getIRI(), typeIRI));
            return;
        }

        // --- Container Items (rdf:li, rdf:_n) ---
        if (ctx.inContainer && RDF.type.getNamespace().equals(uri)) {
            String pred = null;
            if ("li".equals(localName)) {
                pred = RDF.type.getNamespace() + "_" + ctx.liIndex++;
            } else if (localName.matches("_\\d+")) {
                pred = RDF.type.getNamespace() + localName;
            }

            if (pred != null) {
                IRI predicate = ctx.factory.createIRI(pred);
                String resource = attrs.getValue("rdf:resource");
                if (resource != null) {
                    ctx.model.add(ctx.factory.createStatement(
                            ctx.subjectStack.peek(),
                            predicate,
                            ctx.factory.createIRI(RdfXmlUtils.resolveAgainstBase(resource, ctx.baseURI))
                    ));
                }
                return;
            }
        }

        // --- parseType="Collection" ---
        String parseType = attrs.getValue(RDF.type.getNamespace(), "parseType");
        if ("Collection".equals(parseType)) {
            IRI predicate = ctx.factory.createIRI(RdfXmlUtils.expandQName(uri, localName, qName));
            ctx.predicateStack.push(predicate);
            ctx.collectionSubject = ctx.subjectStack.peek();
            ctx.collectionPredicate = predicate;
            ctx.collectionBuilder = new ArrayList<>();
            ctx.inCollection = true;
            return;
        }

        // --- Inside Collection: Collect rdf:Description Items ---
        if (ctx.inCollection && RdfXmlUtils.isDescription(localName, uri)) {
            Resource item = RdfXmlUtils.extractSubject(attrs, ctx.factory, ctx.baseURI);
            ctx.collectionBuilder.add(item);
            ctx.suppressSubject = true;
            return;
        }

        // --- Node Element: rdf:Description or typed node ---
        boolean isNode = RdfXmlUtils.isDescription(localName, uri)
                || (ctx.subjectStack.isEmpty() && RdfXmlUtils.isNodeElement(attrs));

        if (isNode) {
            Resource newSubject = RdfXmlUtils.extractSubject(attrs, ctx.factory, ctx.baseURI);

            // If current node is object of a property
            if (!ctx.predicateStack.isEmpty() && !ctx.subjectStack.isEmpty()) {
                Resource parent = ctx.subjectStack.peek();
                IRI predicate = ctx.predicateStack.pop();
                ctx.model.add(ctx.factory.createStatement(parent, predicate, newSubject));
            }

            ctx.subjectStack.push(newSubject);

            // Emit rdf:type for typed node elements
            if (!RdfXmlUtils.isDescription(localName, uri)) {
                IRI typeIRI = ctx.factory.createIRI(RdfXmlUtils.expandQName(uri, localName, qName));
                ctx.model.add(ctx.factory.createStatement(newSubject, RDF.type.getIRI(), typeIRI));
            }

            // Handle non-RDF attributes as property triples
            for (int i = 0; i < attrs.getLength(); i++) {
                String attrURI = attrs.getURI(i);
                String attrLocal = attrs.getLocalName(i);
                String attrQName = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (RdfXmlUtils.isSyntaxAttribute(attrURI, attrLocal, attrQName)) continue;

                IRI pred = ctx.factory.createIRI(RdfXmlUtils.expandQName(attrURI, attrLocal, attrQName));
                ctx.model.add(ctx.factory.createStatement(newSubject, pred, ctx.factory.createLiteral(value)));
            }

            return;
        }

        // --- Property Element (e.g., <ex:name>) ---
        IRI predicate = ctx.factory.createIRI(RdfXmlUtils.expandQName(uri, localName, qName));
        ctx.predicateStack.push(predicate);

        // --- Property Resource/Object reference ---
        String resource = attrs.getValue(RDF.type.getNamespace(), "resource");
        String nodeID = attrs.getValue(RDF.type.getNamespace(), "nodeID");

        if (resource != null || nodeID != null) {
            Resource object = resource != null
                    ? ctx.factory.createIRI(RdfXmlUtils.resolveAgainstBase(resource, ctx.baseURI))
                    : ctx.factory.createBNode("_:" + nodeID);

            ctx.model.add(ctx.factory.createStatement(
                    ctx.subjectStack.peek(),
                    predicate,
                    object
            ));

            ctx.predicateStack.pop(); // already used
            return;
        }

        // --- parseType="Resource": create blank node ---
        if ("Resource".equals(parseType)) {
            Resource parent = ctx.subjectStack.peek();
            Resource bnode = ctx.factory.createBNode();
            ctx.model.add(ctx.factory.createStatement(parent, predicate, bnode));
            ctx.subjectStack.push(bnode);
            return;
        }

        // --- Inline property attributes: Create blank node with attributes ---
        boolean hasNonSyntaxAttributes = false;
        for (int i = 0; i < attrs.getLength(); i++) {
            String attrURI = attrs.getURI(i);
            String attrLocal = attrs.getLocalName(i);
            String attrQName = attrs.getQName(i);
            if (!RdfXmlUtils.isSyntaxAttribute(attrURI, attrLocal, attrQName)) {
                hasNonSyntaxAttributes = true;
                break;
            }
        }

        if (hasNonSyntaxAttributes) {
            Resource parent = ctx.subjectStack.peek();
            Resource bnode = ctx.factory.createBNode();
            ctx.model.add(ctx.factory.createStatement(parent, predicate, bnode));

            for (int i = 0; i < attrs.getLength(); i++) {
                String attrURI = attrs.getURI(i);
                String attrLocal = attrs.getLocalName(i);
                String attrQName = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (RdfXmlUtils.isSyntaxAttribute(attrURI, attrLocal, attrQName)) continue;

                IRI attrPred = ctx.factory.createIRI(RdfXmlUtils.expandQName(attrURI, attrLocal, attrQName));
                ctx.model.add(ctx.factory.createStatement(bnode, attrPred, ctx.factory.createLiteral(value)));
            }
            ctx.predicateStack.pop(); // already emitted
        }
    }

    /**
     * Handles the end of an XML element, emitting a literal or cleaning up context stacks.
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        String text = characters.toString().trim();
        characters.setLength(0);

        if (!ctx.predicateStack.isEmpty() && !text.isEmpty()) {
            IRI predicate = ctx.predicateStack.pop();
            Resource subject = ctx.subjectStack.peek();
            String datatypeUri = ctx.datatypeStack.isEmpty() ? null : ctx.datatypeStack.pop();
            emitLiteral(subject, predicate, text, datatypeUri);
            return;
        }
        cleanEndElement(uri, localName);
    }

    /**
     * Emits a literal statement (optionally typed or language-tagged).
     */
    private void emitLiteral(Resource subject, IRI predicate, String text, String datatypeUri) {
        Value literal;
        if (datatypeUri != null && !datatypeUri.isEmpty()) {
            Optional<XSD> known = RdfXmlUtils.resolveDatatype(datatypeUri);
            IRI dtype = known.map(XSD::getIRI).orElseGet(() -> {
                System.err.printf("[Warning] Unknown datatype: %s%n", datatypeUri);
                return ctx.factory.createIRI(datatypeUri);
            });
            literal = ctx.factory.createLiteral(text, dtype);
        } else {
            String lang = ctx.langStack.isEmpty() ? null : ctx.langStack.peek();
            literal = (lang != null && !lang.equals("__NO_LANG__"))
                    ? ctx.factory.createLiteral(text, lang)
                    : ctx.factory.createLiteral(text);
        }
        ctx.model.add(ctx.factory.createStatement(subject, predicate, literal));
    }

    /**
     * Cleans up stacks and handles closing of collections, containers, and resource blocks.
     */
    private void cleanEndElement(String uri, String localName) {
        if (!ctx.langStack.isEmpty()) ctx.langStack.pop();
        if (!ctx.predicateStack.isEmpty()) ctx.predicateStack.pop();
        if (RdfXmlUtils.isContainer(localName, uri)) {
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
        if (ctx.inCollection && RdfXmlUtils.isDescription(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) ctx.subjectStack.pop();
            return;
        }
        if (RdfXmlUtils.isDescription(localName, uri)) {
            if (!ctx.subjectStack.isEmpty()) ctx.subjectStack.pop();
        }
        if (!ctx.subjectStack.isEmpty() && !ctx.predicateStack.isEmpty()) {
            ctx.predicateStack.pop();
            ctx.subjectStack.pop();
        }
    }
}