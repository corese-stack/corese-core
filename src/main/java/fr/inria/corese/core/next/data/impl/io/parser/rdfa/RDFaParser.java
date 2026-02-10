package fr.inria.corese.core.next.data.impl.io.parser.rdfa;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.data.io.IOOptions;
import fr.inria.corese.core.next.data.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.data.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.data.impl.io.parser.rdfa.model.*;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;
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

    private String baseIri = SerializationConstants.getDefaultBaseURI();

    private SAXParser saxParser;
    private SAXParserFactory saxParserFactory;

    /**
     * An index of IRI prefixes
     */
    private Map<String, IRI> iriMappings = new HashMap<>();

    /**
     * Buffer/Pile of local value to adapt the parsing algorithm to SAX processing
     */
    private final LinkedList<RDFaProcessingContext> processingContexts = new LinkedList<>();

    public RDFaParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
        // Initializing the iri mappings with the default prefixes as defined by https://www.w3.org/TR/rdfa-core/#xmlrdfaconformance
        for (RDFaInitialPrefixes prefixObject : RDFaInitialPrefixes.values()) {
            this.addIriMapping(prefixObject.getPrefix(), getValueFactory().createIRI(prefixObject.getNamespace()));
        }

        this.saxParserFactory = SAXParserFactory.newInstance();
        try {
            this.saxParser = this.saxParserFactory.newSAXParser();
        } catch (SAXException | ParserConfigurationException e) {
            throw new ParsingErrorException("Unexpected error during XML+RDFa parser creation: " + e.getMessage(), e);
        }
        this.setConfig(config);
    }

    @Override
    public void setConfig(IOOptions options) {
        super.setConfig(options);
        if (options instanceof BaseIRIOptions baseIRIOptions) {
            this.baseIri = baseIRIOptions.getBaseIRI();
        }

        if(options instanceof RDFaParserOptions rdfaOptions) {
            rdfaOptions.getSAXFeatures().forEach((featureUri, value) -> {
                try {
                    this.saxParserFactory.setFeature(featureUri, value);
                } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
                    throw new ParsingErrorException("Failed setting the SAX feature " + featureUri + " from the parser's options", e);
                }
            });
            rdfaOptions.getSAXProperties().forEach((propertyUri, value) -> {
                try {
                    this.saxParser.setProperty(propertyUri, value);
                } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                    throw new ParsingErrorException("Failed setting the SAX property " + propertyUri + " from the parser's options", e);
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
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setValidating(false);

            SAXParser saxParser = factory.newSAXParser();
            InputSource inputSource = new InputSource(reader);
            saxParser.parse(inputSource, new XMLSaxHandler());
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse XML+RDFa input stream: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during XML+RDFa parsing: " + e.getMessage(), e);
        }
    }


    private void addPrefix(String prefix, String uri) {
        IRI prefixIRI = getValueFactory().createIRI(uri);
        this.addIriMapping(prefix, prefixIRI);
    }

    /**
     * Handles character data between XML elements
     * Accumulate the characters in all local values in the pile
     */
    private void handleCharacters(char[] ch, int start, int length) {
        for (RDFaProcessingContext value : this.processingContexts) {
            value.addCharacters(ch, start, length);
        }
    }

    private void clearAllCharactersBuffers() {
        for (RDFaProcessingContext value : this.processingContexts) {
            value.clearCharacters();
        }
    }

    /*
     *  The algorithm in <a href="https://www.w3.org/TR/rdfa-core/#s_sequence">W3C recommendation</a> is based on DOM processing, but this implementation is made in SAX.
     * To reconcile both approaches, the "local values" are stored in a pile of ProcessingContext. The IRI mapping are shared independently.
     * All operations except the ones that create literals are done ine this function.
     */
    private void startProcessElement(String uri, String localName, String qName, Attributes attrs) {

        // 1 First, the local values are initialized
        RDFaProcessingContext processingContext;
        if(this.processingContexts.size() > 1) { // Not a root element
            processingContext = new RDFaProcessingContext(currentProcessingContext().getEvaluationContext());
            processingContext.setRootElement(false);
            this.setIriMappings(this.getIriMappings());
            // 13. Next, all elements that are children of the current element are processed using the rules described here, using a new evaluation context, initialized as follows:
            // If the skip element flag is 'true' then the new evaluation context is a copy of the current context that was passed in to this level of processing, with the language and list of IRI mappings values replaced with the local values;
            if (this.currentProcessingContext().isSkipElement()) {
                processingContext.setEvaluationContext(new RDFaEvaluationContext(currentProcessingContext().getEvaluationContext()));
                processingContext.getEvaluationContext().setLanguage(this.currentProcessingContext().getCurrentLanguage());
                // Otherwise, the values are:
            } else {
                Resource oldParentSubject = currentProcessingContext().getEvaluationContext().getParentSubjectResource();
                // the base is set to the base value of the current evaluation context;
                processingContext.setEvaluationContext(new RDFaEvaluationContext(currentProcessingContext().getEvaluationContext().getBaseIri()));
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
            throw new ParsingErrorException("Start process element "+ qName +" is not paired with the right context" + this.currentProcessingContext());
        }

        // HTML-specific base element
        if (qName.equals(BASE_TAG)
                && isAttributePresent(RDFaAttributes.HREF)) {
            Resource resourceBase = getAttributeValueResource(RDFaAttributes.HREF);
            if (resourceBase.isIRI()) {
                currentProcessingContext().getEvaluationContext().setBaseIri((IRI) resourceBase);
            }
        }

        // 2. The current element is examined for any change to the default vocabulary via @vocab. If @vocab is present and contains a value, the local default vocabulary is updated according to the section on CURIE and IRI Processing. If the value is empty, then the local default vocabulary MUST be reset to the Host Language defined default (if any).
        if (isAttributePresent(RDFaAttributes.VOCAB)
                && !getAttributeStringValue(RDFaAttributes.VOCAB).isEmpty()) {
            this.currentProcessingContext().setDefaultVocabulary(getAttributeStringValue(RDFaAttributes.VOCAB));
        }

        // 3. The current element is examined for IRI mappings and these are added to the local list of IRI mappings. Note that an IRI mapping will simply overwrite any current mapping in the list that has the same name;
        this.currentProcessingContext().getElementAttributes().forEach((String attribute, String attributeValue) -> {
            if (attribute.startsWith(XMLNS_PREFIX + ":")) {
                String prefixName = attribute.replace(XMLNS_PREFIX + ":", "");

                if (prefixName.contains("_")) {
                    throw new ParsingErrorException("Prefix '" + prefixName + "' contains underscore character which is not allowed in xmlns declaration");
                }

                IRI prefixNamespace = getValueFactory().createIRI(attributeValue, "");
                this.addIriMapping(prefixName, prefixNamespace);
            }
        });
        if (isAttributePresent(RDFaAttributes.PREFIX)
                && !getAttributeStringValue(RDFaAttributes.PREFIX).isEmpty()) {
            String prefixDeclaration = getAttributeStringValue(RDFaAttributes.PREFIX);
            this.addIriMappings(getPrefixesFromDeclaration(prefixDeclaration));
        }

        // 4. The current element is also parsed for any language information, and if present, current language is set accordingly;
        // Host Languages that incorporate RDFa MAY provide a mechanism for specifying the natural language of an element and its contents (e.g., XML provides the general-purpose XML attribute @xml:lang).
        if (isAttributePresent(RDFaAttributes.LANG_ALT)
                && !getAttributeStringValue(RDFaAttributes.LANG_ALT).isEmpty()) {
            this.currentProcessingContext().setCurrentLanguage(getAttributeStringValue(RDFaAttributes.LANG_ALT));
        } else if (isAttributePresent(RDFaAttributes.LANG)
                && !getAttributeStringValue(RDFaAttributes.LANG).isEmpty()) {
            this.currentProcessingContext().setCurrentLanguage(getAttributeStringValue(RDFaAttributes.LANG));
        }

        // 5. If the current element contains no @rel or @rev attribute, then the next step is to establish a value for new subject. This step has two possible alternatives.
        if (!isAttributePresent(RDFaAttributes.REL)
                && !isAttributePresent(RDFaAttributes.REV)) {
            // 5.1. If the current element contains the @property attribute, but does not contain either the @content or @datatype attributes, then
            if (isAttributePresent(RDFaAttributes.PROPERTY)
                    && !getAttributeStringValue(RDFaAttributes.PROPERTY).trim().isEmpty()
                    && !isAttributePresent(RDFaAttributes.CONTENT)
                    && !isAttributePresent(RDFaAttributes.DATATYPE)
                    && ((isAttributePresent(RDFaAttributes.ABOUT)
                            && ! getAttributeStringValue(RDFaAttributes.ABOUT).isEmpty())
                        || this.currentProcessingContext().isRootElement()
                        || currentProcessingContext().getEvaluationContext().getParentObjectResource() != null)) {
                // new subject is set to the resource obtained from the first match from the following rule:
                // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                if (isAttributePresent(RDFaAttributes.ABOUT)) {
                    this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
                    // otherwise, if the element is the root element of the document, then act as if there is an empty @about present, and process it according to the rule for @about, above;
                } else if (this.currentProcessingContext().isRootElement()) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getBaseIri());
                    // otherwise, if parent object is present, new subject is set to the value of parent object.
                } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                }
                // If @typeof is present then typed resource is set to the resource obtained from the first match from the following rules:
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                    if (isAttributePresent(RDFaAttributes.ABOUT)) {
                        this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
                        // otherwise, if the element is the root element of the document, then act as if there is an empty @about present and process it according to the previous rule;
                    } else if (this.currentProcessingContext().isRootElement()) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.currentProcessingContext().setTypedResource(emptyAboutResource.get());
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                        }
                        // otherwise,
                    } else {
                        // by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                        if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.RESOURCE));
                            // otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.HREF)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.HREF));
                            // otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.SRC)) {
                            this.currentProcessingContext().setTypedResource(getAttributeValueResource(RDFaAttributes.SRC));
                            // otherwise, the value of typed resource is set to a newly created bnode.
                        } else {
                            this.currentProcessingContext().setTypedResource(getValueFactory().createBNode());
                        }
                        // The value of the current object resource is then set to the value of typed resource.
                        this.currentProcessingContext().setCurrentObjectResource(this.currentProcessingContext().getTypedResource());
                    }
                }
                // 5.2. otherwise:
            } else {
                // If the element contains an @about, @href, @src, or @resource attribute, new subject is set to the resource obtained as follows:
                if (isAttributePresent(RDFaAttributes.ABOUT)
                        || isAttributePresent(RDFaAttributes.HREF)
                        || isAttributePresent(RDFaAttributes.SRC)
                        || isAttributePresent(RDFaAttributes.RESOURCE)) {
                    // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                    if (isAttributePresent(RDFaAttributes.ABOUT)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
                        // otherwise, by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                    } else if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.RESOURCE));
                        // otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                    } else if (isAttributePresent(RDFaAttributes.HREF)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.HREF));
                        // otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing.
                    } else if (isAttributePresent(RDFaAttributes.SRC)) {
                        this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.SRC));
                    }
                    // otherwise, if no resource is provided by a resource attribute, then the first match from the following rules will apply:
                } else {
                    // if the element is the root element of the document, then act as if there is an empty @about present, and process it according to the rule for @about, above;
                    if (this.currentProcessingContext().isRootElement()) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.currentProcessingContext().setNewSubject(emptyAboutResource.get());
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate newSubject from empty CURIE");
                        }
                        // otherwise, if @typeof is present, then new subject is set to be a newly created bnode;
                    } else if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                        this.currentProcessingContext().setNewSubject(getValueFactory().createBNode());
                        // otherwise, if parent object is present, new subject is set to the value of parent object. Additionally, if @property is not present then the skip element flag is set to 'true'.
                    } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                        this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                        if (!isAttributePresent(RDFaAttributes.PROPERTY)) {
                            this.currentProcessingContext().setSkipElement(true);
                        }
                    }
                }
                // Finally, if @typeof is present, set the typed resource to the value of new subject.
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
                }
            }
        }

        // 6. If the current element does contain a @rel or @rev attribute, then the next step is to establish both a value for new subject and a value for current object resource:
        if (isAttributePresent(RDFaAttributes.REL)
                || isAttributePresent(RDFaAttributes.REV)) {
            if (isAttributePresent(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setNewSubject(getAttributeValueResource(RDFaAttributes.ABOUT));
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                this.currentProcessingContext().setTypedResource(this.currentProcessingContext().getNewSubject());
            }
            if (this.currentProcessingContext().getNewSubject() == null) {
                if (this.currentProcessingContext().isRootElement()) {
                    Optional<Resource> emptyAboutResource = resolveStringResource("");
                    if (emptyAboutResource.isPresent()) {
                        this.currentProcessingContext().setTypedResource(emptyAboutResource.get());
                    } else {
                        throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                    }
                } else if (currentProcessingContext().getEvaluationContext().getParentObjectResource() != null) {
                    this.currentProcessingContext().setNewSubject(currentProcessingContext().getEvaluationContext().getParentObjectResource());
                }
            }
            if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.RESOURCE));
            } else if (isAttributePresent(RDFaAttributes.HREF)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.HREF));
            } else if (isAttributePresent(RDFaAttributes.SRC)) {
                this.currentProcessingContext().setCurrentObjectResource(getAttributeValueResource(RDFaAttributes.SRC));
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setCurrentObjectResource(this.getValueFactory().createBNode());
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)
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
                this.getModel().add(this.currentProcessingContext().getTypedResource(), RDF.type.getIRI(), (IRI) typeRes);
            }
        }

        // 8. If in any of the previous steps a new subject was set to a non-null value different from the parent object;
        if (this.currentProcessingContext().getNewSubject() != null && this.currentProcessingContext().getNewSubject() != currentProcessingContext().getEvaluationContext().getParentObjectResource()) {
            this.currentProcessingContext().setListMappings(new HashMap<>());
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
                            throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.REL.getName()));
                        }
                    }
                }
                if (isAttributePresent(RDFaAttributes.REV)) {
                    List<Resource> revResourceList = getAttributeValueResourceList(RDFaAttributes.REV);
                    for(Resource revResource: revResourceList) {
                        if (!revResource.isIRI()) {
                            throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + getAttributeStringValue(RDFaAttributes.REV));
                        }
                        if (!this.currentProcessingContext().getCurrentObjectResource().isResource()) {
                            throw new ParsingErrorException("object resource expected to be a resource but was " + this.currentProcessingContext().getCurrentObjectResource());
                        }
                        this.getModel().add(this.currentProcessingContext().getCurrentObjectResource(), (IRI) revResource, this.currentProcessingContext().getNewSubject());
                    }
                }
            }
        }

        // 10. If however current object resource was set to null, but there are predicates present, then they must be stored as incomplete triples, pending the discovery of a subject that can be used as the object. Also, current object resource should be set to a newly created bnode (so that the incomplete triples have a subject to connect to if they are ultimately turned into triples);
        if (this.currentProcessingContext().getCurrentObjectResource() == null
                && (isAttributePresent(RDFaAttributes.REL)
        ) || isAttributePresent(RDFaAttributes.REV)) {
            if (this.currentProcessingContext().getIncompleteStatements() == null) {
                this.currentProcessingContext().setIncompleteStatements(new HashSet<>());
            }
            this.currentProcessingContext().setCurrentObjectResource(getValueFactory().createBNode());
            if (isAttributePresent(RDFaAttributes.REL)) {
                List<Resource> relList = getAttributeValueResourceList(RDFaAttributes.REL);
                for(Resource relResource : relList) {
                    if (isAttributePresent(RDFaAttributes.INLIST)) {
                        if (!this.currentProcessingContext().getListMappings().containsKey((IRI) relResource)) {
                            this.currentProcessingContext().addListMappings((IRI) relResource, new HashSet<>());
                        }
                        this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) relResource, RDFaIncompleteStatement.Direction.NONE));
                    } else {
                        this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) relResource, RDFaIncompleteStatement.Direction.FORWARD));
                    }
                }
            } else if (isAttributePresent(RDFaAttributes.REV)) {
                if (!getAttributeValueResource(RDFaAttributes.REV).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.REV.getName()));
                }
                List<Resource> revList = getAttributeValueResourceList(RDFaAttributes.REV);
                for (Resource revRes : revList) {
                    this.currentProcessingContext().addIncompleteStatement(new RDFaIncompleteStatement((IRI) revRes, RDFaIncompleteStatement.Direction.BACKWARD));
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
                    this.currentProcessingContext().addListMapping(incompleteStatement.getPredicate(), this.currentProcessingContext().getNewSubject());
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.FORWARD) {
                    this.getModel().add(currentProcessingContext().getEvaluationContext().getParentSubjectResource(), incompleteStatement.getPredicate(), this.currentProcessingContext().getNewSubject());
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.BACKWARD) {
                    this.getModel().add(this.currentProcessingContext().getNewSubject(), incompleteStatement.getPredicate(), currentProcessingContext().getEvaluationContext().getParentSubjectResource());
                }
            }
        }

        Map<IRI, Set<Value>> oldListMappings = currentProcessingContext().getEvaluationContext().getListMappings();

        // 14. Finally, if there is one or more mapping in the local list mapping, list triples are generated as follows:
        for (Map.Entry<IRI, Set<Value>> listMapping : this.currentProcessingContext().getListMappings().entrySet()) {
            IRI propertyIRI = listMapping.getKey();
            Set<Value> propertyList = listMapping.getValue();

            if (!oldListMappings.containsKey(propertyIRI)) {
                if (propertyList.isEmpty()) {
                    getModel().add(this.currentProcessingContext().getNewSubject(), propertyIRI, RDF.nil.getIRI());
                } else {
                    ArrayList<BNode> bnodes = new ArrayList<>();
                    for (int i = 0; i < propertyList.size(); i++) {
                        bnodes.add(getValueFactory().createBNode());
                    }
                    int bnodeIndex = 0;
                    for (Value listElement : propertyList) {
                        BNode elementNode = bnodes.get(bnodeIndex);
                        Resource nextElementNode = RDF.nil.getIRI();
                        if (bnodeIndex < bnodes.size() - 1) {
                            nextElementNode = bnodes.get(bnodeIndex + 1);
                        }
                        getModel().add(elementNode, RDF.first.getIRI(), listElement);
                        getModel().add(elementNode, RDF.rest.getIRI(), nextElementNode);

                        bnodeIndex++;
                    }
                    getModel().add(this.currentProcessingContext().getNewSubject(), propertyIRI, bnodes.getFirst());
                }
            }
        }

    }

    /*
     * Ths function will apply the operations for the creation of literals using the character buffer and remove the current top processing context from the pile.
     */
    private void endProcessElement(String uri, String localName, String qName) {
        if(! this.currentProcessingContext().getElementName().equals(qName)) {
            throw new ParsingErrorException("End process element "+ qName +" is not paired with the right context" + this.currentProcessingContext());
        }

        // 11. The next step of the iteration is to establish any current property value;
        if (isAttributePresent(RDFaAttributes.PROPERTY) ) {
            String propertyValue = getAttributeStringValue(RDFaAttributes.PROPERTY);
            if (propertyValue == null || propertyValue.trim().isEmpty()) {
                throw new ParsingErrorException("@property attribute cannot be empty");
            }
            List<Resource> propertyIRIList = getAttributeValueResourceList(RDFaAttributes.PROPERTY);
            // as a typed literal if @datatype is present, does not have an empty value according to the section on CURIE and IRI Processing, and is not set to XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#.
            // The actual literal is either the value of @content (if present) or a string created by concatenating the value of all descendant text nodes, of the current element in turn. The final string includes the datatype IRI, as described in [RDF-SYNTAX-GRAMMAR], which will have been obtained according to the section on CURIE and IRI Processing.
            if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && getAttributeValueResource(RDFaAttributes.DATATYPE).isIRI()
                    && getAttributeValueResource(RDFaAttributes.DATATYPE) != RDF.XMLLiteral.getIRI()) {
                IRI datatypeIRI = (IRI) getAttributeValueResource(RDFaAttributes.DATATYPE);
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = getAttributeStringValue(RDFaAttributes.CONTENT);
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                } else {
                    String contentString = this.currentProcessingContext().getCharacters().trim();
                    if(! contentString.isEmpty()) {
                        this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                        this.clearAllCharactersBuffers();
                    }
                }
                //  otherwise, as a plain literal if @datatype is present but has an empty value according to the section on CURIE and IRI Processing.
                // The actual literal is either the value of @content (if present) or a string created by concatenating the value of all descendant text nodes, of the current element in turn.
            } else if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && getAttributeStringValue(RDFaAttributes.DATATYPE).isEmpty()) {
                IRI datatypeIRI = (IRI) getAttributeValueResource(RDFaAttributes.DATATYPE);
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.CONTENT.getName());
                    this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                } else {
                        String contentString = this.currentProcessingContext().getCharacters().trim();
                    if(! contentString.isEmpty()) {
                        this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                        this.clearAllCharactersBuffers();
                    }
                }
                // otherwise, as an XML literal if @datatype is present and is set to XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#.
                // The value of the XML literal is a string created by serializing to text, all nodes that are descendants of the current element, i.e., not including the element itself, and giving it a datatype of XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#. The format of the resulting serialized content is as defined in Exclusive XML Canonicalization Version 1.0 [XML-EXC-C14N].

                // otherwise, as a plain literal using the value of @content if @content is present.
            } else if (isAttributePresent(RDFaAttributes.CONTENT)) {
                String contentString = this.currentProcessingContext().getElementAttributes().get(RDFaAttributes.CONTENT.getName());
                this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                //  otherwise, if the @rel, @rev, and @content attributes are not present, as a resource obtained from one of the following:
                //    by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                //    otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                //    otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing.
            } else if (!isAttributePresent(RDFaAttributes.REL)
                    && !isAttributePresent(RDFaAttributes.REV)
                    && !isAttributePresent(RDFaAttributes.CONTENT)
                    && (isAttributePresent(RDFaAttributes.RESOURCE)
                    || isAttributePresent(RDFaAttributes.HREF)
                    || isAttributePresent(RDFaAttributes.SRC)
            )) {
                if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.RESOURCE));
                } else if (isAttributePresent(RDFaAttributes.HREF)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.HREF));
                } else if (isAttributePresent(RDFaAttributes.SRC)) {
                    this.currentProcessingContext().setCurrentPropertyValue(getAttributeValueResource(RDFaAttributes.SRC));
                }
                // otherwise, if @typeof is present and @about is not, the value of typed resource.
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.currentProcessingContext().setCurrentPropertyValue(this.currentProcessingContext().getTypedResource());
                // otherwise as a plain literal.
            } else {
                String contentString = this.currentProcessingContext().getCharacters().trim();
                if(! contentString.isEmpty()) {
                    // Additionally, if there is a value for current language then the value of the plain literal should include this language information, as described in [RDF-SYNTAX-GRAMMAR]. The actual literal is either the value of @content (if present) or a string created by concatenating the text content of each of the descendant elements of the current element in document order.
                    if (this.currentProcessingContext().getCurrentLanguage() != null
                            && !this.currentProcessingContext().getCurrentLanguage().isEmpty()) {
                        this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, this.currentProcessingContext().getCurrentLanguage()));
                    } else {
                        this.currentProcessingContext().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                    }
                    this.clearAllCharactersBuffers();
                }
            }

            // The current property value is then used with each predicate as follows:
            // If the element also includes the @inlist attribute, the current property value is added to the local list mapping as follows:
            for(Resource propertyIRIResource: propertyIRIList) {
                if (!propertyIRIResource.isIRI()) {
                    throw new ParsingErrorException("Property must be an IRI, got: " + propertyIRIResource + ". Blank nodes are not allowed as predicates.");
                }
                IRI propertyIRI = (IRI) propertyIRIResource;
                if(this.currentProcessingContext().getCurrentPropertyValue() != null) {
                    if (isAttributePresent(RDFaAttributes.INLIST)) {
                        // if the local list mapping does not contain a list associated with the predicate IRI, instantiate a new list and add to local list mappings
                        if (!this.currentProcessingContext().getListMappings().containsKey(propertyIRI)) {
                            this.currentProcessingContext().addListMappings(propertyIRI, new HashSet<>());
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

        this.processingContexts.pop();
    }

    /**
     * Internal SAX handler that delegates to the parser's methods
     */
    private class XMLSaxHandler extends DefaultHandler {
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
            startProcessElement(uri, localName, qName, attrs);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            endProcessElement(uri, localName, qName);
        }

        @Override
        public void error(SAXParseException e) {
            throw new ParsingErrorException("Failed to parse XML+RDFa: " + e.getMessage(), e);
        }

        @Override
        public void fatalError(SAXParseException e) {
            throw new ParsingErrorException("Failed to parse XML+RDFa: " + e.getMessage(), e);
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
            throw new ParsingErrorException("Error during prefix extraction of " + declaration);
        }

        int numberOfPairs = prefixArray.length / 2;
        for(int pairNumber = 0; pairNumber < numberOfPairs; pairNumber++) {
            String prefix = prefixArray[pairNumber * 2];
            String namespaceString = prefixArray[pairNumber * 2 + 1];

            if(!prefix.endsWith(":")) {
                throw new ParsingErrorException("Expecting namespace prefix declaration to end with \":\", got " + prefix + " in declaration " + declaration);
            }

            prefix = prefix.replaceAll(":$", "");

            if (prefix.contains("_") && !prefix.equals("_")) {
                throw new ParsingErrorException("Prefix '" + prefix + "' contains underscore character which is not allowed in declaration: " + declaration);
            }

            if (namespaceString == null || namespaceString.trim().isEmpty()) {
                throw new ParsingErrorException("Namespace for prefix '" + prefix + "' cannot be empty in declaration: " + declaration);
            }

            // AJOUT: Résoudre les IRIs relatives
            IRI namespace;
            if (IRIUtils.isStandardIRI(namespaceString)) {
                // IRI absolue
                namespace = getValueFactory().createIRI(namespaceString);
            } else {
                // IRI relative - résoudre par rapport à la base
                String baseIriString = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue();
                String resolvedIRI = baseIriString + namespaceString;
                namespace = getValueFactory().createIRI(resolvedIRI);
            }

            result.put(prefix, namespace);
        }

        return result;
    }

    private List<Resource> resolveWhitespaceSeparatedList(String rawList) {
        ArrayList<Resource> result = new ArrayList<>();
        String[] rawResourceList = rawList.trim().split("\\s+");

        for(String rawResource : rawResourceList) {
            if (rawResource.isEmpty()) {
                continue;
            }
            Optional<Resource> resourceOptional = resolveStringResource(rawResource);
            resourceOptional.ifPresent(result::add);
        }

        return result;
    }

    private Resource getAttributeValueResource(RDFaAttributes attribute) {
        String attributeValue = this.currentProcessingContext().getElementAttributes().get(attribute.getName());
        Optional<Resource> resourceResolution = resolveStringResource(attributeValue);
        if (resourceResolution.isPresent()) {
            return resourceResolution.get();
        } else {
            throw new ParsingErrorException("Could not parse @" + attribute.getName() + " value: " + attributeValue);
        }
    }

    private List<Resource> getAttributeValueResourceList(RDFaAttributes attribute) {
        String attributeValue = this.currentProcessingContext().getElementAttributes().get(attribute.getName());
        return resolveWhitespaceSeparatedList(attributeValue);
    }

    private boolean isAttributePresent(RDFaAttributes attribute) {
        return this.currentProcessingContext().getElementAttributes().get(attribute.getName()) != null;
    }

    private String getAttributeStringValue(RDFaAttributes attribute) {
        return this.currentProcessingContext().getElementAttributes().get(attribute.getName());
    }

    /**
     * Convenience accessor to the top of the processing contexts pile
     *
     */
    private RDFaProcessingContext currentProcessingContext() {
        return this.processingContexts.getFirst();
    }

    /**
     * Resolves the string representation of a resource found in attributes of an element, be it an IRI, <a href="https://www.w3.org/TR/rdfa-core/#s_curieprocessing">CURIE</a> or relative URI
     *
     * @param stringResource the resource as stored in the attribute of the HTML element
     * @return the full IRI if it is a relative IRI, full IRI or CURIE, nothing otherwise
     */
    protected Optional<Resource> resolveStringResource(String stringResource) {
        String resultString = stringResource;
        if (resultString.startsWith("[") && resultString.endsWith("]")) {
            resultString = resultString.replaceFirst("\\[", "");
            resultString = resultString.replaceFirst("]", "");
        }


        if (stringUriIsCURIE(resultString)) { // CURIE
            int colonIndex = resultString.indexOf(":");
            String prefixString = resultString.substring(0, colonIndex);
            String localNameString = resultString.substring(colonIndex + 1);
            // Basic resolution following https://www.w3.org/TR/rdfa-syntax/#s_convertingcurietouri
            if (this.hasIriMapping(prefixString)) {
                IRI namespaceIRI = this.getIriMapping(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (this.getIriMappings().containsKey(prefixString)) {
                IRI namespaceIRI = this.getIriMappings().get(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (prefixString.isEmpty()) { // CURIE is relative to the base URI
                return Optional.of(this.getValueFactory().createIRI(currentProcessingContext().getEvaluationContext().getBaseIri().stringValue(), localNameString));
            } else {
                throw new ParsingErrorException("CURIE " + stringResource + " uses unknown prefix among " + this.getIriMappings().keySet() + " and " + this.getIriMappings().keySet());
            }
        } else if (IRIUtils.isStandardIRI(resultString)) {  // Full IRI
            return Optional.of(this.getValueFactory().createIRI(resultString));

        } else if (resultString.startsWith("_:")) {  // Blank Node
            int colonIndex = resultString.indexOf(":");
            String localNameString = resultString.substring(colonIndex + 1);
            return Optional.of(this.getValueFactory().createBNode(localNameString));
        } else if (IRIUtils.isStandardIRI(currentProcessingContext().getEvaluationContext().getBaseIri().stringValue() + resultString)) {
            String concatenatedRelativeUri = currentProcessingContext().getEvaluationContext().getBaseIri().stringValue() + resultString;
            return Optional.of(this.getValueFactory().createIRI(concatenatedRelativeUri));
        } else if(this.currentProcessingContext().getEvaluationContext().getTermMapping(resultString) != null) {
            return Optional.of(this.currentProcessingContext().getEvaluationContext().getTermMapping(resultString));
        }
        return Optional.empty();
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

        if (stringIri.contains("://")) {
            return false;
        }

        String potentialScheme = stringIri.substring(0, colonIndex).toLowerCase();

        return !potentialScheme.equals("http") &&
                !potentialScheme.equals("https") &&
                !potentialScheme.equals("ftp") &&
                !potentialScheme.equals("ftps") &&
                !potentialScheme.equals("mailto") &&
                !potentialScheme.equals("tel") &&
                !potentialScheme.equals("urn") &&
                !potentialScheme.equals("data") &&
                !potentialScheme.equals("file") &&
                !potentialScheme.equals("ssh") &&
                !potentialScheme.equals("git") &&
                !potentialScheme.equals("news") &&
                !potentialScheme.equals("nntp") &&
                !potentialScheme.equals("irc") &&
                !potentialScheme.equals("ldap");
    }

    private RDFaEvaluationContext getNewContext(IRI baseIRI) {
        RDFaEvaluationContext result = new RDFaEvaluationContext(baseIRI);
        initializeEvaluationContextMappings(result);
        return result;
    }

    private void initializeEvaluationContextMappings(RDFaEvaluationContext context) {
        // <a href="https://www.w3.org/2011/rdfa-context/rdfa-1.1">https://www.w3.org/2011/rdfa-context/rdfa-1.1</a> sets a list of predefined terms mappings for RDFa contexts.
        context.addTermMapping("describedby", getValueFactory().createIRI("http://www.w3.org/2007/05/powder-s#describedby"));
        context.addTermMapping("license", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#license"));
        context.addTermMapping("role", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#role"));
    }

    private Map<String, IRI> getIriMappings() {
        return iriMappings;
    }

    private void setIriMappings(Map<String, IRI> iriMappings) {
        this.iriMappings = iriMappings;
    }

    private boolean hasIriMapping(String prefix) {
        return this.iriMappings.containsKey(prefix);
    }

    /**
     * @param prefix the prefix WITHOUT ":"
     * @return the IRI associated to the prefix in this context
     */
    private IRI getIriMapping(String prefix) {
        return this.iriMappings.get(prefix);
    }

    private void addIriMapping(String prefix, IRI prefixIri) {
        this.iriMappings.put(prefix, prefixIri);
    }

    private void addIriMappings(Map<String, IRI> otherMappings) {
        if(otherMappings != null) {
            this.iriMappings.putAll(otherMappings);
        }
    }

}
