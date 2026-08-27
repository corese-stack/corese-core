package fr.inria.corese.core.next.data.impl.io.parser.rdfa;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.spi.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.impl.io.parser.rdfa.model.*;
import fr.inria.corese.core.next.data.spi.io.IOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SAX-based RDFa 1.1 parser.
 *
 * <p>This parser processes XML+RDFa documents (XHTMl, SVG, etc.) using the SAX streaming API.
 * It follows the <a href="https://www.w3.org/TR/rdfa-core/#s_sequence">W3C recommendation</a>, using buffers to replace DOM traversal.</p>
 *
 * <p>
 * This parser does NOT support vocabulary expansion
 * </p>
 */
public class RDFaParser extends AbstractRDFParser {

    private static final Logger logger = LoggerFactory.getLogger(RDFaParser.class);

    private static final String BASE_TAG = "base";
    private static final String XMLNS_PREFIX = "xmlns";

    private String baseIri = IOConstants.getDefaultBaseURI();
    private String documentBaseIri;

    private final SAXParser saxParser;
    private final SAXParserFactory saxParserFactory;

    /**
     * Buffer/Pile of local value to adapt the parsing algorithm to SAX processing
     */
    private final LinkedList<RDFaProcessingContext> processingContexts = new LinkedList<>();

    public RDFaParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFaParser(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
        this.saxParserFactory = SAXParserFactory.newInstance();
        try {
            this.saxParser = this.saxParserFactory.newSAXParser();
        } catch (SAXException | ParserConfigurationException e) {
            throw new ParsingException("Unexpected error during XML+RDFa parser creation: " + e.getMessage(), e);
        }
        this.setConfig(config);
    }

    @Override
    public void setConfig(RDFParsingOptions options) {
        super.setConfig(options);
        if (options instanceof BaseIRIOptions baseIRIOptions) {
            this.baseIri = baseIRIOptions.getBaseIRI();
        }

        if(options instanceof RDFaParserOptions rdfaOptions) {
            rdfaOptions.getSAXFeatures().forEach((featureUri, value) -> {
                try {
                    this.saxParserFactory.setFeature(featureUri, value);
                } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
                    throw new ParsingException("Failed setting the SAX feature " + featureUri + " from the parser's options", e);
                }
            });
            rdfaOptions.getSAXProperties().forEach((propertyUri, value) -> {
                try {
                    this.saxParser.setProperty(propertyUri, value);
                } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                    throw new ParsingException("Failed setting the SAX property " + propertyUri + " from the parser's options", e);
                }
            });
            this.saxParserFactory.setSchema(rdfaOptions.getSchema());
        }
    }


    @Override
    public void parse(InputStream in) {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), this.baseIri);
    }

    @Override
    public void parse(InputStream in, String baseURIString) {
        this.baseIri = baseURIString;
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURIString);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFA;
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        try {
            this.baseIri = baseURI;
            this.documentBaseIri = baseURI;
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setValidating(false);

            SAXParser parser = factory.newSAXParser();
            InputSource inputSource = new InputSource(reader);
            parser.parse(inputSource, new XMLSaxHandler());
        } catch (IOException e) {
            throw new ParsingException("Failed to parse XML+RDFa input stream: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingException("Unexpected error during XML+RDFa parsing: " + e.getMessage(), e);
        }
    }


    @SuppressWarnings("java:S3398")
    private void addPrefix(String prefix, String uri) {
        IRI prefixIRI = getValueFactory().createIRI(uri);
        this.addIriMapping(prefix, prefixIRI);
    }

    /**
     * Handles character data between XML elements
     * Accumulate the characters in all local values in the pile
     */
    @SuppressWarnings("java:S3398")
    private void handleCharacters(char[] ch, int start, int length) {
        for (RDFaProcessingContext value : this.processingContexts) {
            value.addCharacters(ch, start, length);
            if (value.isXmlLiteralProperty()) {
                value.appendXmlLiteralContent(escapeXmlText(new String(ch, start, length)));
            }
        }
    }

    private String escapeXmlText(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void clearAllCharactersBuffers() {
        if (!this.processingContexts.isEmpty()) {
            this.currentProcessingContext().clearCharacters();
        }
    }

    /*
     *  The algorithm in <a href="https://www.w3.org/TR/rdfa-core/#s_sequence">W3C recommendation</a> is based on DOM processing, but this implementation is made in SAX.
     * To reconcile both approaches, the "local values" are stored in a pile of ProcessingContext. The IRI mapping are shared independently.
     * All operations except the ones that create literals are done ine this function.
     */
    @SuppressWarnings({"java:S3776", "java:S3398", "java:S125"})
    private void startProcessElement(String qName, Attributes attrs) {

        // 1 First, the local values are initialized
        RDFaProcessingContext processingContext;
        if (!this.processingContexts.isEmpty()) { // Not a root element
            processingContext = new RDFaProcessingContext(currentProcessingContext().getEvaluationContext());
            processingContext.setNamespaceDeclarations(currentProcessingContext().getNamespaceDeclarations());
            processingContext.setRootElement(false);
            // 13. Next, all elements that are children of the current element are processed using the rules described here, using a new evaluation context, initialized as follows:
            // If the skip element flag is 'true' then the new evaluation context is a copy of the current context that was passed in to this level of processing, with the language and list of IRI mappings values replaced with the local values;
            if (this.currentProcessingContext().isSkipElement()) {
                processingContext.setEvaluationContext(new RDFaEvaluationContext(currentProcessingContext().getEvaluationContext()));
                processingContext.getEvaluationContext().setLanguage(this.currentProcessingContext().getCurrentLanguage());
                // Otherwise, the values are:
            } else {
                Resource oldParentSubject = currentProcessingContext().getEvaluationContext().getParentSubjectResource();
                // the base is set to the base value of the current evaluation context;
                processingContext.setEvaluationContext(new RDFaEvaluationContext(currentProcessingContext().getEvaluationContext()));
                // the parent subject is set to the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
                processingContext.getEvaluationContext().setParentSubjectResource(this.currentProcessingContext().getNewSubject());
                // the parent object is set to value of current object resource, if non-null, or the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
                if (this.currentProcessingContext().getCurrentObjectResource() != null) {
                    processingContext.getEvaluationContext().setParentObjectResource(this.currentProcessingContext().getCurrentObjectResource());
                } else if (this.currentProcessingContext().getNewSubject() != null) {
                    processingContext.getEvaluationContext().setParentObjectResource(this.currentProcessingContext().getNewSubject());
                } else {
                    processingContext.getEvaluationContext().setParentObjectResource(oldParentSubject);
                }
                // the list of incomplete triples is set to the local list of incomplete triples;
                processingContext.getEvaluationContext().setIncompleteStatements(this.currentProcessingContext().getIncompleteStatements());
                // the list mapping is set to the local list mapping;
                processingContext.getEvaluationContext().setListMappings(this.currentProcessingContext().getListMappings());
                // language is set to the value of current language.
                processingContext.getEvaluationContext().setLanguage(this.currentProcessingContext().getCurrentLanguage());
                // the default vocabulary is set to the value of the local default vocabulary.
                processingContext.getEvaluationContext().setDefaultVocabulary(this.currentProcessingContext().getDefaultVocabulary());
            }
        } else {
            // This is the start of the document
            RDFaEvaluationContext startingContext = getNewContext(getValueFactory().createIRI(this.baseIri));
            initializeEvaluationContextMappings(startingContext);
            startingContext.setParentSubjectResource(startingContext.getBaseIri());
            startingContext.setParentObjectResource(null);
            startingContext.setLanguage(null);
            startingContext.setDefaultVocabulary(null);
            processingContext = new RDFaProcessingContext(startingContext);
            processingContext.setRootElement(true);
        }
        processingContext.setElementName(qName);
        processingContext.setElementAttributes(attrs);
        this.processingContexts.addFirst(processingContext);
        if(! this.currentProcessingContext().getElementName().equals(qName)) {
            throw new ParsingException("Start process element "+ qName +" is not paired with the right context" + this.currentProcessingContext());
        }

        // XML-specific xml:base attribute or a base element.
        String xmlBase = this.currentProcessingContext().getElementAttributes().get("xml:base");
        if (xmlBase != null && !xmlBase.isEmpty()) {
            String currentBase = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
            String resolvedBase = java.net.URI.create(currentBase).resolve(xmlBase).toString();
            currentProcessingContext().getEvaluationContext().setBaseIri(getValueFactory().createIRI(resolvedBase));
        } else if (qName.equalsIgnoreCase(BASE_TAG) && isAttributePresent(RDFaAttributes.HREF)) {
            Resource resourceBase = getAttributeValueResource(RDFaAttributes.HREF);
            if (resourceBase != null && resourceBase.isIRI()) {
                String baseStr = resourceBase.stringValue();
                int fragIdx = baseStr.indexOf('#');
                if (fragIdx >= 0) {
                    baseStr = baseStr.substring(0, fragIdx);
                }
                IRI newBase = getValueFactory().createIRI(baseStr);
                this.baseIri = newBase.stringValue();
                for (RDFaProcessingContext ctx : this.processingContexts) {
                    ctx.getEvaluationContext().setBaseIri(newBase);
                }

                // In HTML, the base element also updates the implicit document
                // resource tracked by the active processing contexts. For XML
                // hosts it only affects the resolution of relative IRIs.
                if (isHtmlDocument()) {
                    for (RDFaProcessingContext ctx : this.processingContexts) {
                        if (ctx.getNewSubject() != null && ctx.getNewSubject().isIRI()
                                && (this.documentBaseIri == null || ctx.getNewSubject().stringValue().equals(this.documentBaseIri))) {
                            ctx.setNewSubject(newBase);
                        }
                        if (ctx.getEvaluationContext().getParentSubjectResource() != null
                                && ctx.getEvaluationContext().getParentSubjectResource().isIRI()
                                && (this.documentBaseIri == null || ctx.getEvaluationContext().getParentSubjectResource().stringValue().equals(this.documentBaseIri))) {
                            ctx.getEvaluationContext().setParentSubjectResource(newBase);
                        }
                        if (ctx.getEvaluationContext().getParentObjectResource() != null
                                && ctx.getEvaluationContext().getParentObjectResource().isIRI()
                                && (this.documentBaseIri == null || ctx.getEvaluationContext().getParentObjectResource().stringValue().equals(this.documentBaseIri))) {
                            ctx.getEvaluationContext().setParentObjectResource(newBase);
                        }
                    }
                }
            }
        }

        // 2. The current element is examined for any change to the default vocabulary via @vocab.
        if (isAttributePresent(RDFaAttributes.VOCAB)) {
            String vocabString = getAttributeStringValue(RDFaAttributes.VOCAB);
            if (vocabString != null && !vocabString.trim().isEmpty()) {
                String vocab = vocabString.trim();
                this.currentProcessingContext().setDefaultVocabulary(vocab);
                this.currentProcessingContext().getEvaluationContext().setDefaultVocabulary(vocab);
                this.getModel().add(
                    currentProcessingContext().getEvaluationContext().getBaseIri(),
                    getValueFactory().createIRI("http://www.w3.org/ns/rdfa#usesVocabulary"),
                    getValueFactory().createIRI(vocab)
                );
            } else {
                this.currentProcessingContext().setDefaultVocabulary("");
                this.currentProcessingContext().getEvaluationContext().setDefaultVocabulary("");
            }
        }

        // 3. The current element is examined for IRI mappings and these are added to the local list of IRI mappings. Note that an IRI mapping will simply overwrite any current mapping in the list that has the same name;
        this.currentProcessingContext().getElementAttributes().forEach((String attribute, String attributeValue) -> {
            if (attribute.equals(XMLNS_PREFIX)) {
                this.currentProcessingContext().addNamespaceDeclaration("", attributeValue);
            }
            if (attribute.startsWith(XMLNS_PREFIX + ":")) {
                String prefixName = attribute.replace(XMLNS_PREFIX + ":", "");

                if (prefixName.contains("_") || prefixName.isEmpty()) {
                    return;
                }

                IRI prefixNamespace;
                if (IRIUtils.isStandardIRI(attributeValue)) {
                    prefixNamespace = getValueFactory().createIRI(attributeValue);
                } else {
                    String baseToUse = this.documentBaseIri != null ? this.documentBaseIri : currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
                    String resolvedIRI = java.net.URI.create(baseToUse).resolve(attributeValue).toString();
                    prefixNamespace = getValueFactory().createIRI(resolvedIRI);
                }
                this.addIriMapping(prefixName, prefixNamespace);
                this.currentProcessingContext().addNamespaceDeclaration(prefixName, prefixNamespace.stringValue());
            }
        });
        if (isAttributePresent(RDFaAttributes.PREFIX)
                && !getAttributeStringValue(RDFaAttributes.PREFIX).isEmpty()) {
            String prefixDeclaration = getAttributeStringValue(RDFaAttributes.PREFIX);
            Map<String, IRI> prefixMappings = getPrefixesFromDeclaration(prefixDeclaration);
            this.addIriMappings(prefixMappings);
            prefixMappings.forEach((prefix, namespace) -> this.currentProcessingContext()
                    .addNamespaceDeclaration(prefix, namespace.stringValue()));
        }

        // 4. The current element is also parsed for any language information, and if present, current language is set accordingly;
        // Host Languages that incorporate RDFa MAY provide a mechanism for specifying the natural language of an element and its contents (e.g., XML provides the general-purpose XML attribute @xml:lang).
        if (isAttributePresent(RDFaAttributes.LANG_ALT)) {
            String langVal = getAttributeStringValue(RDFaAttributes.LANG_ALT);
            this.currentProcessingContext().setCurrentLanguage(langVal);
            this.currentProcessingContext().getEvaluationContext().setLanguage(langVal);
        } else if (isAttributePresent(RDFaAttributes.LANG)) {
            String langVal = getAttributeStringValue(RDFaAttributes.LANG);
            this.currentProcessingContext().setCurrentLanguage(langVal);
            this.currentProcessingContext().getEvaluationContext().setLanguage(langVal);
        }

        // 5. If the current element contains no @rel or @rev attribute, then the next step is to establish a value for new subject. This step has two possible alternatives.
        if (!isAttributePresent(RDFaAttributes.REL)
                && !isAttributePresent(RDFaAttributes.REV)) {
            // 5.1. If the current element contains the @property attribute, but does not contain either the @content or @datatype attributes, then
            if (isAttributePresent(RDFaAttributes.PROPERTY)
                    && !getAttributeStringValue(RDFaAttributes.PROPERTY).trim().isEmpty()
                    && !isAttributePresent(RDFaAttributes.CONTENT)
                    && !isAttributePresent(RDFaAttributes.DATATYPE)
                    && (hasValidResourceAttribute(RDFaAttributes.ABOUT)
                        || this.currentProcessingContext().isRootElement()
                        || currentProcessingContext().getEvaluationContext().getParentObjectResource() != null)) {
                // new subject is set to the resource obtained from the first match from the following rule:
                if (hasValidResourceAttribute(RDFaAttributes.ABOUT)) {
                    this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
                } else if (this.currentProcessingContext().isRootElement()) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getBaseIri());
                } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                }
                // If @typeof is present then typed resource is set to the resource obtained from the first match from the following rules:
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    if (hasValidResourceAttribute(RDFaAttributes.ABOUT)) {
                        this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
                    } else if (this.currentProcessingContext().isRootElement()) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.currentProcessingContext().setTypedResource(emptyAboutResource.get());
                        }
                    } else {
                        if (hasValidResourceAttribute(RDFaAttributes.RESOURCE)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.RESOURCE));
                        } else if (hasValidResourceAttribute(RDFaAttributes.HREF)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.HREF));
                        } else if (hasValidResourceAttribute(RDFaAttributes.SRC)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.SRC));
                        } else {
                            this.currentProcessingContext().setTypedResource(getValueFactory().createBNode());
                        }
                        this.currentProcessingContext().setCurrentObjectResource(this.currentProcessingContext().getTypedResource());
                    }
                }
                // 5.2. otherwise:
            } else {
                if (hasValidResourceAttribute(RDFaAttributes.ABOUT)
                        || hasValidResourceAttribute(RDFaAttributes.HREF)
                        || hasValidResourceAttribute(RDFaAttributes.SRC)
                        || hasValidResourceAttribute(RDFaAttributes.RESOURCE)) {
                    if (hasValidResourceAttribute(RDFaAttributes.ABOUT)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
                    } else if (hasValidResourceAttribute(RDFaAttributes.RESOURCE)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.RESOURCE));
                    } else if (hasValidResourceAttribute(RDFaAttributes.HREF)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.HREF));
                    } else if (hasValidResourceAttribute(RDFaAttributes.SRC)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.SRC));
                    }
                } else {
                    if (this.currentProcessingContext().isRootElement()) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.currentProcessingContext().setNewSubject(emptyAboutResource.get());
                        }
                    } else if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                        if (qName.equalsIgnoreCase("head") || qName.equalsIgnoreCase("body")) {
                            this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getBaseIri());
                        } else {
                            this.currentProcessingContext().setNewSubject(getValueFactory().createBNode());
                        }
                    } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                        this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                        if (!isAttributePresent(RDFaAttributes.PROPERTY)) {
                            this.currentProcessingContext().setSkipElement(true);
                        }
                    }
                }
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
                }
            }
        }

        // 6. If the current element does contain a @rel or @rev attribute
        if (isAttributePresent(RDFaAttributes.REL)
                || isAttributePresent(RDFaAttributes.REV)) {
            if (hasValidResourceAttribute(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
            }
            if (hasValidResourceAttribute(RDFaAttributes.ABOUT) && isAttributePresent(RDFaAttributes.TYPEOF)) {
                this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
            }
            if (this.currentProcessingContext().getNewSubject() == null) {
                if (this.currentProcessingContext().isRootElement()) {
                    Optional<Resource> emptyAboutResource = resolveStringResource("");
                    if (emptyAboutResource.isPresent()) {
                        this.currentProcessingContext().setNewSubject(emptyAboutResource.get());
                    }
                } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                }
            }
            if (hasValidResourceAttribute(RDFaAttributes.RESOURCE)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.RESOURCE));
            } else if (hasValidResourceAttribute(RDFaAttributes.HREF)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.HREF));
            } else if (hasValidResourceAttribute(RDFaAttributes.SRC)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.SRC));
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !hasValidResourceAttribute(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setCurrentObjectResource(this.getValueFactory().createBNode());
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !hasValidResourceAttribute(RDFaAttributes.ABOUT)
                    && (this.currentProcessingContext().getCurrentObjectResource() == null
                    || this.currentProcessingContext().getCurrentObjectResource().isResource())) {
                this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getCurrentObjectResource());
            }
        }

        // 7. If in any of the previous steps a typed resource was set to a non-null value, it is now used to provide a subject for type values;
        if (this.currentProcessingContext().getTypedResource() != null
                && isAttributePresent(RDFaAttributes.TYPEOF)) {
            List<Resource> typeList = getAttributeValueResourceList(RDFaAttributes.TYPEOF);
            for(Resource typeRes : typeList) {
                this.getModel().add(this.currentProcessingContext().getTypedResource(), RDF.type.getIRI(), typeRes);
            }
        }

        // 8. If in any of the previous steps a new subject was set to a non-null value different from the parent subject;
        Resource parentSubj = currentProcessingContext().getEvaluationContext().getParentSubjectResource();
        if (this.currentProcessingContext().getNewSubject() != null && !this.currentProcessingContext().getNewSubject().equals(parentSubj)) {
            Map<IRI, List<Value>> freshListMappings = new HashMap<>();
            this.currentProcessingContext().setListMappings(freshListMappings);
            this.currentProcessingContext().getEvaluationContext().setListMappings(freshListMappings);
        }

        // 9. If in any of the previous steps a current object resource was set to a non-null value, it is now used to generate triples and add entries to the local list mapping:
        if (this.currentProcessingContext().getCurrentObjectResource() != null) {
            if (isAttributePresent(RDFaAttributes.INLIST)
                    && isAttributePresent(RDFaAttributes.REL)) {
                List<Resource> relResourceList = getAttributeValueResourceList(RDFaAttributes.REL);
                for(Resource relResource: relResourceList) {
                    this.currentProcessingContext().addListMapping((IRI) relResource, this.currentProcessingContext().getCurrentObjectResource());
                }
            }
            if (!isAttributePresent(RDFaAttributes.INLIST)) {
                if (isAttributePresent(RDFaAttributes.REL)) {
                    List<Resource> relResourceList = getAttributeValueResourceList(RDFaAttributes.REL);
                    for(Resource relResource: relResourceList) {
                        if (relResource.isIRI()) {
                            this.getModel().add(this.currentProcessingContext().getNewSubject(), (IRI) relResource, this.currentProcessingContext().getCurrentObjectResource());
                        } else {
                            throw new ParsingException("Value of attribute @rel expected to be an IRI but was " + this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.REL.getName()));
                        }
                    }
                }
                if (isAttributePresent(RDFaAttributes.REV)) {
                    List<Resource> revResourceList = getAttributeValueResourceList(RDFaAttributes.REV);
                    for(Resource revResource: revResourceList) {
                        if (!revResource.isIRI()) {
                            throw new ParsingException("Value of attribute @rev expected to be an IRI but was " + getAttributeStringValue(RDFaAttributes.REV));
                        }
                        if (!this.currentProcessingContext().getCurrentObjectResource().isResource()) {
                            throw new ParsingException("object resource expected to be a resource but was " + this.currentProcessingContext().getCurrentObjectResource());
                        }
                        this.getModel().add(this.currentProcessingContext().getCurrentObjectResource(), (IRI) revResource, this.currentProcessingContext().getNewSubject());
                    }
                }
            }
        }

        // 10. If however current object resource was set to null, but there are predicates present, then they must be stored as incomplete triples, pending the discovery of a subject that can be used as the object. Also, current object resource should be set to a newly created bnode (so that the incomplete triples have a subject to connect to if they are ultimately turned into triples);
        if (this.currentProcessingContext().getCurrentObjectResource() == null
                && (isAttributePresent(RDFaAttributes.REL) || isAttributePresent(RDFaAttributes.REV))) {
            if (this.currentProcessingContext().getIncompleteStatements() == null) {
                this.currentProcessingContext().setIncompleteStatements(new HashSet<>());
            }
            this.currentProcessingContext().setCurrentObjectResource(getValueFactory().createBNode());
            if (isAttributePresent(RDFaAttributes.REL)) {
                List<Resource> relList = getAttributeValueResourceList(RDFaAttributes.REL);
                for(Resource relResource : relList) {
                    if (isAttributePresent(RDFaAttributes.INLIST)) {
                        List<Value> targetList = this.currentProcessingContext().getListMappings().computeIfAbsent((IRI) relResource, k -> new ArrayList<>());
                        this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) relResource, RDFaIncompleteStatement.Direction.NONE, targetList));
                    } else {
                        this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) relResource, RDFaIncompleteStatement.Direction.FORWARD));
                    }
                }
            }
            if (isAttributePresent(RDFaAttributes.REV)) {
                List<Resource> revList = getAttributeValueResourceList(RDFaAttributes.REV);
                for (Resource revRes : revList) {
                    if (revRes != null && revRes.isIRI()) {
                        this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) revRes, RDFaIncompleteStatement.Direction.BACKWARD));
                    }
                }
            }
        }

        // 12. If the skip element flag is 'false', and new subject was set to a non-null value, then any incomplete triples within the current context should be completed:
        if (!this.currentProcessingContext().isSkipElement()
                && this.currentProcessingContext().getNewSubject() != null) {
            if (this.currentProcessingContext().getIncompleteStatements() == null) {
                this.currentProcessingContext().setIncompleteStatements(new HashSet<>());
            }
            for (RDFaIncompleteStatement incompleteStatement : currentProcessingContext().getEvaluationContext().getIncompleteStatements()) {
                if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.NONE) {
                    if (incompleteStatement.getTargetList() != null) {
                        incompleteStatement.getTargetList().add(this.currentProcessingContext().getNewSubject());
                    } else {
                        currentProcessingContext().getEvaluationContext().getListMappings().computeIfAbsent(incompleteStatement.getPredicate(), k -> new ArrayList<>()).add(this.currentProcessingContext().getNewSubject());
                    }
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.FORWARD) {
                    this.getModel().add(currentProcessingContext().getEvaluationContext().getParentSubjectResource(), incompleteStatement.getPredicate(), this.currentProcessingContext().getNewSubject());
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.BACKWARD) {
                    this.getModel().add(this.currentProcessingContext().getNewSubject(), incompleteStatement.getPredicate(), currentProcessingContext().getEvaluationContext().getParentSubjectResource());
                }
            }
        }
    }

    /*
     * Ths function will apply the operations for the creation of literals using the character buffer and remove the current top processing context from the pile.
     */
    @SuppressWarnings({"java:S3776", "java:S3398"})
    private void endProcessElement(String qName) {
        if(! this.currentProcessingContext().getElementName().equals(qName)) {
            throw new ParsingException("End process element "+ qName +" is not paired with the right context" + this.currentProcessingContext());
        }

        // 11. The next step of the iteration is to establish any current property value;
        if (isAttributePresent(RDFaAttributes.PROPERTY)) {
            String propertyValue = getAttributeStringValue(RDFaAttributes.PROPERTY);
            if (propertyValue != null && !propertyValue.trim().isEmpty()) {
                List<Resource> propertyIRIList = getAttributeValueResourceList(RDFaAttributes.PROPERTY);
            Resource datatypeRes = (isAttributePresent(RDFaAttributes.DATATYPE) && !getAttributeStringValue(RDFaAttributes.DATATYPE).trim().isEmpty())
                    ? getAttributeValueResource(RDFaAttributes.DATATYPE)
                    : null;

            if (datatypeRes != null && datatypeRes.isIRI() && !datatypeRes.equals(RDF.XMLLiteral.getIRI())) {
                IRI datatypeIRI = (IRI) datatypeRes;
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = getAttributeStringValue(RDFaAttributes.CONTENT);
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                } else {
                    String contentString = this.currentProcessingContext().getCharacters();
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                    this.clearAllCharactersBuffers();
                }
            } else if (datatypeRes != null && datatypeRes.equals(RDF.XMLLiteral.getIRI())) {
                this.currentProcessingContext().setCurrentPropertyValue(
                        getValueFactory().createLiteral(this.currentProcessingContext().getXmlLiteralContent(), RDF.XMLLiteral.getIRI()));
            } else if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && (getAttributeStringValue(RDFaAttributes.DATATYPE).trim().isEmpty() || datatypeRes == null)) {
                String contentString;
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    contentString = this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.CONTENT.getName());
                } else {
                    contentString = this.currentProcessingContext().getCharacters();
                    this.clearAllCharactersBuffers();
                }
                if (this.currentProcessingContext().getCurrentLanguage() != null
                        && !this.currentProcessingContext().getCurrentLanguage().isEmpty()) {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, this.currentProcessingContext().getCurrentLanguage()));
                } else {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                }
                // otherwise, as an XML literal if @datatype is present and is set to XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#.
                // The value of the XML literal is a string created by serializing to text, all nodes that are descendants of the current element, i.e., not including the element itself, and giving it a datatype of XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#. The format of the resulting serialized content is as defined in Exclusive XML Canonicalization Version 1.0 [XML-EXC-C14N].

                // otherwise, as a plain literal using the value of @content if @content is present.
            } else if (isAttributePresent(RDFaAttributes.CONTENT)) {
                String contentString = this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.CONTENT.getName());
                if (this.currentProcessingContext().getCurrentLanguage() != null
                        && !this.currentProcessingContext().getCurrentLanguage().isEmpty()) {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, this.currentProcessingContext().getCurrentLanguage()));
                } else {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                }
                //  otherwise, if the @rel, @rev, and @content attributes are not present, as a resource obtained from one of the following:
                //    by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                //    otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                //    otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing.
            } else if (!isAttributePresent(RDFaAttributes.REL)
                    && !isAttributePresent(RDFaAttributes.REV)
                    && !isAttributePresent(RDFaAttributes.CONTENT)
                    && (hasValidResourceAttribute(RDFaAttributes.RESOURCE)
                    || hasValidResourceAttribute(RDFaAttributes.HREF)
                    || hasValidResourceAttribute(RDFaAttributes.SRC)
            )) {
                if (hasValidResourceAttribute(RDFaAttributes.RESOURCE)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.RESOURCE));
                } else if (hasValidResourceAttribute(RDFaAttributes.HREF)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.HREF));
                } else if (hasValidResourceAttribute(RDFaAttributes.SRC)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.SRC));
                }
                // otherwise, if @typeof is present and @about is not, the value of typed resource.
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setCurrentPropertyValue(this.currentProcessingContext().getTypedResource());
                // otherwise as a plain literal.
            } else {
                String contentString = this.currentProcessingContext().getCharacters();
                if (this.currentProcessingContext().getCurrentLanguage() != null
                        && !this.currentProcessingContext().getCurrentLanguage().isEmpty()) {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, this.currentProcessingContext().getCurrentLanguage()));
                } else {
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                }
                this.clearAllCharactersBuffers();
            }

            // The current property value is then used with each predicate as follows:
            // If the element also includes the @inlist attribute, the current property value is added to the local list mapping as follows:
            for(Resource propertyIRIResource: propertyIRIList) {
                if (!propertyIRIResource.isIRI()) {
                    continue;
                }
                IRI propertyIRI = (IRI) propertyIRIResource;
                if(this.currentProcessingContext().getCurrentPropertyValue() != null) {
                    if (isAttributePresent(RDFaAttributes.INLIST)) {
                        // if the local list mapping does not contain a list associated with the predicate IRI, instantiate a new list and add to local list mappings
                        if (!this.currentProcessingContext().getListMappings().containsKey(propertyIRI)) {
                            this.currentProcessingContext().addListMappings(propertyIRI, new ArrayList<>());
                        }
                        // add the current property value to the list associated with the predicate IRI in the local list mapping
                        this.currentProcessingContext().addListMapping(propertyIRI, this.currentProcessingContext().getCurrentPropertyValue());
                        // Otherwise the current property value is used to generate a triple as follows:
                        // subject new subject
                        // predicate full IRI
                        // object current property value
                    } else {
                        Statement statement = getValueFactory().createStatement(this.currentProcessingContext().getNewSubject(), propertyIRI, this.currentProcessingContext().getCurrentPropertyValue());
                        this.getModel().add(statement);
                    }
                }
            }
            }
        }

        // 14. Finally, if there is one or more mapping in the local list mapping, list triples are generated as follows:
        Resource parentSubj = currentProcessingContext().getEvaluationContext().getParentSubjectResource();
        boolean isListOwner = this.currentProcessingContext().isRootElement()
                || parentSubj == null
                || (this.currentProcessingContext().getNewSubject() != null && !this.currentProcessingContext().getNewSubject().equals(parentSubj));

        if (isListOwner) {
            for (Map.Entry<IRI, List<Value>> listMapping : this.currentProcessingContext().getListMappings().entrySet()) {
                IRI propertyIRI = listMapping.getKey();
                List<Value> propertyList = listMapping.getValue();

                if (propertyList.isEmpty()) {
                    getModel().add(this.currentProcessingContext().getNewSubject(), propertyIRI, RDF.nil.getIRI());
                } else {
                    List<BNode> bnodes = new ArrayList<>();
                    for (int i = 0; i < propertyList.size(); i++) {
                        bnodes.add(getValueFactory().createBNode());
                    }
                    for (int i = 0; i < propertyList.size(); i++) {
                        BNode elementNode = bnodes.get(i);
                        Resource nextElementNode = (i < propertyList.size() - 1) ? bnodes.get(i + 1) : RDF.nil.getIRI();
                        getModel().add(elementNode, RDF.first.getIRI(), propertyList.get(i));
                        getModel().add(elementNode, RDF.rest.getIRI(), nextElementNode);
                    }
                    getModel().add(this.currentProcessingContext().getNewSubject(), propertyIRI, bnodes.getFirst());
                }
            }
        }

        this.processingContexts.pop();
    }

    /**
     * Internal SAX handler that delegates to the parser's methods
     */
    private class XMLSaxHandler extends DefaultHandler {
        private String serializeStartElement(String qName, Attributes attrs, Map<String, String> namespaceDeclarations) {
            StringBuilder serialized = new StringBuilder("<").append(qName);
            for (int index = 0; index < attrs.getLength(); index++) {
                serialized.append(' ')
                        .append(attrs.getQName(index))
                        .append("=\"")
                        .append(escapeXmlAttribute(attrs.getValue(index)))
                        .append('"');
            }
            namespaceDeclarations.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(declaration -> serialized.append(' ')
                            .append(declaration.getKey().isEmpty() ? XMLNS_PREFIX : XMLNS_PREFIX + ':' + declaration.getKey())
                            .append("=\"")
                            .append(escapeXmlAttribute(declaration.getValue()))
                            .append('"'));
            return serialized.append('>').toString();
        }

        private String escapeXmlAttribute(String value) {
            return escapeXmlText(value).replace("\"", "&quot;").replace("\t", "&#x9;").replace("\n", "&#xA;").replace("\r", "&#xD;");
        }

        private void appendXmlLiteralStartElement(String qName, Attributes attrs) {
            for (RDFaProcessingContext context : RDFaParser.this.processingContexts) {
                if (context.isXmlLiteralProperty()) {
                    context.appendXmlLiteralContent(serializeStartElement(qName, attrs, context.getNamespaceDeclarations()));
                }
            }
        }

        private void appendXmlLiteralEndElement(String qName) {
            String serializedElement = "</" + qName + ">";
            for (RDFaProcessingContext context : RDFaParser.this.processingContexts) {
                if (context != RDFaParser.this.currentProcessingContext() && context.isXmlLiteralProperty()) {
                    context.appendXmlLiteralContent(serializedElement);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            RDFaParser.this.handleCharacters(ch, start, length);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            RDFaParser.this.addPrefix(prefix, uri);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            appendXmlLiteralStartElement(qName, attrs);
            startProcessElement(qName, attrs);
            RDFaParser.this.currentProcessingContext().setXmlLiteralProperty(
                    isAttributePresent(RDFaAttributes.DATATYPE)
                    && RDF.XMLLiteral.getIRI().equals(getAttributeValueResource(RDFaAttributes.DATATYPE)));
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            appendXmlLiteralEndElement(qName);
            endProcessElement(qName);
        }

        @Override
        public void error(SAXParseException e) {
            throw new ParsingException("Failed to parse XML+RDFa: " + e.getMessage(), e);
        }

        @Override
        public void fatalError(SAXParseException e) {
            throw new ParsingException("Failed to parse XML+RDFa: " + e.getMessage(), e);
        }

        @Override
        public void warning(SAXParseException e) {
            logger.warn("Warning during parsing of XML+RDFa: ", e);
        }
    }

    private Map<String, IRI> getPrefixesFromDeclaration(String declaration) {
        String[] prefixArray = declaration.trim().split("\\s+");
        HashMap<String, IRI> result = new HashMap<>();

        if (prefixArray.length % 2 != 0) {
            throw new ParsingException("Error during prefix extraction of " + declaration);
        }

        int numberOfPairs = prefixArray.length / 2;
        for (int pairNumber = 0; pairNumber < numberOfPairs; pairNumber++) {
            parseSinglePrefixDeclaration(prefixArray[pairNumber * 2], prefixArray[pairNumber * 2 + 1], declaration, result);
        }

        return result;
    }

    private void parseSinglePrefixDeclaration(String prefixRaw, String namespaceString, String declaration, Map<String, IRI> result) {
        if (!prefixRaw.endsWith(":")) {
            throw new ParsingException("Expecting namespace prefix declaration to end with \":\", got " + prefixRaw + " in declaration " + declaration);
        }
        String prefix = prefixRaw.replaceAll(":$", "").trim();
        if ((prefix.isEmpty() && !isHtmlDocument()) || prefix.startsWith("_")) {
            return;
        }
        if (namespaceString == null || namespaceString.trim().isEmpty()) {
            throw new ParsingException("Namespace for prefix '" + prefix + "' cannot be empty in declaration: " + declaration);
        }
        result.put(prefix, resolveNamespaceIRI(namespaceString));
    }

    private IRI resolveNamespaceIRI(String namespaceString) {
        if (IRIUtils.isStandardIRI(namespaceString)) {
            return getValueFactory().createIRI(namespaceString);
        }
        String baseToUse = this.documentBaseIri != null
                ? this.documentBaseIri
                : currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
        String resolvedIRI = java.net.URI.create(baseToUse).resolve(namespaceString).toString();
        return getValueFactory().createIRI(resolvedIRI);
    }

    private boolean isTermOrCurieAttribute(RDFaAttributes attribute) {
        return attribute == RDFaAttributes.PROPERTY
                || attribute == RDFaAttributes.REL
                || attribute == RDFaAttributes.REV
                || attribute == RDFaAttributes.TYPEOF
                || attribute == RDFaAttributes.DATATYPE;
    }

    private Optional<Resource> resolveAttributeValue(RDFaAttributes attribute, String val) {
        if (isTermOrCurieAttribute(attribute)) {
            return resolveTermOrCurieOrAbsUri(val);
        }
        return resolveUriOrSafeCurie(val);
    }

    private List<Resource> resolveWhitespaceSeparatedList(RDFaAttributes attribute, String attributeValue) {
        if (attributeValue == null) {
            return Collections.emptyList();
        }
        String[] values = attributeValue.trim().split("\\s+");
        List<Resource> result = new ArrayList<>();
        for (String singleVal : values) {
            Optional<Resource> res = resolveAttributeValue(attribute, singleVal);
            res.ifPresent(result::add);
        }
        return result;
    }

    private Resource getAttributeValueResource(RDFaAttributes attribute) {
        String attributeValue = this.currentProcessingContext().getElementAttributes().get(attribute.getName());
        if (attributeValue == null) {
            return null;
        }
        Optional<Resource> resourceResolution = resolveAttributeValue(attribute, attributeValue);
        return resourceResolution.orElse(null);
    }

    private boolean hasValidResourceAttribute(RDFaAttributes attribute) {
        String attributeValue = this.currentProcessingContext().getElementAttributes().get(attribute.getName());
        if (attributeValue == null) {
            return false;
        }
        return resolveAttributeValue(attribute, attributeValue).isPresent();
    }

    private List<Resource> getAttributeValueResourceList(RDFaAttributes attribute) {
        String attributeValue = this.currentProcessingContext().getElementAttributes().get(attribute.getName());
        return resolveWhitespaceSeparatedList(attribute, attributeValue);
    }

    private boolean isAttributePresent(RDFaAttributes attribute) {
        return this.currentProcessingContext().getElementAttributes().get(attribute.getName()) != null;
    }

    private String getAttributeStringValue(RDFaAttributes attribute) {
        return this.currentProcessingContext().getElementAttributes().get(attribute.getName());
    }

    /**
     * Convenience accessor to the top of the processing contexts pile
     */
    private RDFaProcessingContext currentProcessingContext() {
        return this.processingContexts.getFirst();
    }

    private boolean isHtmlDocument() {
        return !this.processingContexts.isEmpty() && this.processingContexts.getLast().getElementName().equalsIgnoreCase("html");
    }

    /**
     * Resolves attributes that require TERMorCURIEorAbsURI (@property, @rel, @rev, @typeof, @datatype)
     */
    protected Optional<Resource> resolveTermOrCurieOrAbsUri(String stringResource) {
        if (stringResource == null) {
            return Optional.empty();
        }
        String resultString = stringResource.trim();
        if (resultString.isEmpty() || resultString.startsWith("_:")) {
            return Optional.empty();
        }

        if (resultString.startsWith("[") && resultString.endsWith("]")) {
            return resolveSafeCurie(resultString);
        }

        if (!resultString.contains(":")) {
            return resolveTerm(resultString);
        }

        Optional<Resource> curie = resolveCurie(resultString);
        if (curie.isPresent()) {
            return curie;
        }

        int colonIndex = resultString.indexOf(":");
        String prefixString = resultString.substring(0, colonIndex);
        if (IRIUtils.isStandardIRI(resultString) && isValidIRIScheme(prefixString)) {
            return Optional.of(this.getValueFactory().createIRI(resultString));
        }

        return Optional.empty();
    }

    /**
     * Resolves attributes that require URIorSafeCURIE (@about, @resource, @href, @src)
     */
    protected Optional<Resource> resolveUriOrSafeCurie(String stringResource) {
        if (stringResource == null) {
            return Optional.empty();
        }
        String resultString = stringResource.trim();

        if (resultString.startsWith("[") && resultString.endsWith("]")) {
            return resolveSafeCurie(resultString);
        }

        if (resultString.startsWith("_:")) {
            return Optional.of(this.getValueFactory().createBNode(resultString.substring(2)));
        }

        Optional<Resource> curie = resolveCurie(resultString);
        if (curie.isPresent()) {
            return curie;
        }

        if (resultString.contains(":")) {
            String prefixString = resultString.substring(0, resultString.indexOf(":"));
            if (IRIUtils.isStandardIRI(resultString) && isValidIRIScheme(prefixString)) {
                return Optional.of(this.getValueFactory().createIRI(resultString));
            }
        }

        return resolveSameDocumentOrRelativeUri(resultString);
    }

    private Optional<Resource> resolveSafeCurie(String resultString) {
        String safeContent = resultString.substring(1, resultString.length() - 1).trim();
        if (safeContent.isEmpty()) {
            return Optional.empty();
        }
        if (safeContent.startsWith("_:")) {
            return Optional.of(getValueFactory().createBNode(safeContent.substring(2)));
        }
        Optional<Resource> curie = resolveCurie(safeContent);
        if (curie.isPresent()) {
            return curie;
        }
        if (IRIUtils.isStandardIRI(safeContent) && isValidIRIScheme(safeContent.split(":")[0])) {
            return Optional.of(this.getValueFactory().createIRI(safeContent));
        }
        return Optional.empty();
    }

    private Optional<Resource> resolveCurie(String resultString) {
        if (!resultString.contains(":")) {
            return Optional.empty();
        }
        int colonIndex = resultString.indexOf(":");
        String prefixString = resultString.substring(0, colonIndex);
        String localNameString = resultString.substring(colonIndex + 1);
        if (this.hasIriMapping(prefixString)) {
            IRI namespaceIRI = this.getIriMapping(prefixString);
            return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
        }
        return Optional.empty();
    }

    private Optional<Resource> resolveTerm(String resultString) {
        if (this.currentProcessingContext().getDefaultVocabulary() != null
                && !this.currentProcessingContext().getDefaultVocabulary().isEmpty()) {
            return Optional.of(getValueFactory().createIRI(this.currentProcessingContext().getDefaultVocabulary() + resultString));
        }
        IRI termIRI = this.currentProcessingContext().getEvaluationContext().getTermMapping(resultString);
        if (termIRI == null) {
            termIRI = this.currentProcessingContext().getEvaluationContext().getTermMapping(resultString.toLowerCase(Locale.ROOT));
        }
        return Optional.ofNullable(termIRI);
    }

    private Optional<Resource> resolveSameDocumentOrRelativeUri(String resultString) {
        if (resultString.isEmpty()) {
            return Optional.of(currentProcessingContext().getEvaluationContext().getBaseIri());
        }
        if (resultString.startsWith("#")) {
            String base = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
            int hash = base.indexOf('#');
            String baseNoHash = hash >= 0 ? base.substring(0, hash) : base;
            return Optional.of(this.getValueFactory().createIRI(baseNoHash + resultString));
        }
        if (resultString.startsWith("?")) {
            String base = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
            int hash = base.indexOf('#');
            String baseNoHash = hash >= 0 ? base.substring(0, hash) : base;
            int query = baseNoHash.indexOf('?');
            String baseNoQuery = query >= 0 ? baseNoHash.substring(0, query) : baseNoHash;
            return Optional.of(this.getValueFactory().createIRI(baseNoQuery + resultString));
        }
        try {
            String base = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
            String resolved = java.net.URI.create(base).resolve(resultString).normalize().toString();
            return Optional.of(this.getValueFactory().createIRI(resolved));
        } catch (Exception e) {
            return Optional.of(this.getValueFactory().createIRI(currentProcessingContext().getEvaluationContext().getBaseIri().stringValue() + resultString));
        }
    }

    protected Optional<Resource> resolveStringResource(String stringResource) {
        return resolveUriOrSafeCurie(stringResource);
    }

    /**
     * Equivalent to test if it contains a colon, and it is not a blank node
     *
     * @param stringIri Attribute or text value
     * @return true if it is a valid CURIE
     */
    protected boolean stringUriIsCURIE(String stringIri) {
        int colonIndex = stringIri.indexOf(":");

        if (colonIndex == -1) {
            return false;
        }

        if (stringIri.startsWith("_:") || stringIri.startsWith("[_:")) {
            return false;
        }

        String potentialPrefix = stringIri.substring(0, colonIndex);

        if (this.hasIriMapping(potentialPrefix) || this.getIriMappings().containsKey(potentialPrefix)) {
            return true;
        }

        return !isValidIRIScheme(potentialPrefix);
    }

    /**
     * Determines whether the given string is a syntactically valid IRI scheme
     * as defined by RFC 3986.
     *
     * @param potentialScheme the string to evaluate as a potential IRI scheme
     * @return {@code true} if the string is a valid IRI scheme, {@code false} otherwise
     */
    private boolean isValidIRIScheme(String potentialScheme) {
        if (potentialScheme == null || potentialScheme.isEmpty()) {
            return false;
        }

        if (!Character.isLetter(potentialScheme.charAt(0))) {
            return false;
        }

        for (int i = 1; i < potentialScheme.length(); i++) {
            char c = potentialScheme.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }

        return true;
    }

    private RDFaEvaluationContext getNewContext(IRI baseIRI) {
        RDFaEvaluationContext result = new RDFaEvaluationContext(baseIRI);
        initializeEvaluationContextMappings(result);
        return result;
    }

    private void initializeEvaluationContextMappings(RDFaEvaluationContext context) {
        // Standard XHTML vocabulary terms
        String[] xhvTerms = {
            "alternate", "appendix", "cite", "bookmark", "contents", "chapter", "copyright",
            "first", "glossary", "help", "icon", "index", "last", "license", "meta", "next",
            "prev", "previous", "role", "section", "start", "stylesheet", "subsection", "top", "up", "p3pv1"
        };
        for (String term : xhvTerms) {
            context.addTermMapping(term, getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#" + term));
        }
        context.addTermMapping("describedby", getValueFactory().createIRI("http://www.w3.org/2007/05/powder-s#describedby"));

        // Initial context predefined prefixes
        for (RDFaInitialPrefixes prefixObject : RDFaInitialPrefixes.values()) {
            context.addIriMapping(prefixObject.getPrefix(), getValueFactory().createIRI(prefixObject.getNamespace()));
        }
        // Default empty prefix
        context.addIriMapping("", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#"));
    }

    private Map<String, IRI> getIriMappings() {
        return currentProcessingContext().getEvaluationContext().getIriMappings();
    }

    private boolean hasIriMapping(String prefix) {
        return currentProcessingContext().getEvaluationContext().hasIriMapping(prefix);
    }

    /**
     * @param prefix the prefix WITHOUT ":"
     * @return the IRI associated to the prefix in this context
     */
    private IRI getIriMapping(String prefix) {
        return currentProcessingContext().getEvaluationContext().getIriMapping(prefix);
    }

    private void addIriMapping(String prefix, IRI prefixIri) {
        currentProcessingContext().getEvaluationContext().addIriMapping(prefix, prefixIri);
    }

    private void addIriMappings(Map<String, IRI> otherMappings) {
        currentProcessingContext().getEvaluationContext().addIriMappings(otherMappings);
    }

}
