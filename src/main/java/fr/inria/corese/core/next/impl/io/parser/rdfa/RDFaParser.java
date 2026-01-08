package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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

    private RDFaEvaluationContext currentContext = null;

    /**
     * Buffer/Pile of local value to adapt the parsing algorithm to SAX processing
     */
    private final LinkedList<RDFaLocalValues> localValuePile = new LinkedList<>();

    /**
     * Buffer for accumulating character data between start and end tags.
     */
    private StringBuilder characters = new StringBuilder();

    private boolean isRootElement = true;
    private Attributes currentElementAttributes = null;

    public RDFaParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }


    @Override
    public void parse(InputStream in) {
        if (getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            String baseIRI = baseIRIOptions.getBaseIRI();
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseIRI);
        } else {
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
        }
    }

    @Override
    public void parse(InputStream in, String baseURIString) {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURIString);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFA;
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        try {
            this.currentContext = getNewContext(getValueFactory().createIRI(baseURI));
            this.currentContext.setParentSubjectResource(this.currentContext.getBaseIri());
            this.currentContext.setParentObjectResource(null);
            this.currentContext.setLanguage(null);

            this.currentContext.setDefaultVocabulary(null);

            SAXParserFactory factory = SAXParserFactory.newInstance();
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
        this.currentContext.addIriMapping(prefix, prefixIRI);
    }

    /**
     * Handles character data between XML elements
     */
    private void handleCharacters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    private void startProcessElement(String uri, String localName, String qName, Attributes attrs) {
        this.currentElementAttributes = attrs;

        // 1 First, the local values are initialized
        this.localValuePile.add(new RDFaLocalValues(this.currentContext));

        logger.info("START {} {}", qName, this.localValuePile.getFirst());

        // HTML-specific base element
        if (qName.equals(BASE_TAG)
                && isAttributePresent(RDFaAttributes.HREF)) {
            Resource resourceBase = getAttributeResourceValue(RDFaAttributes.HREF);
            if (resourceBase.isIRI()) {
                this.currentContext.setBaseIri((IRI) resourceBase);
            }
        }

        // 2. The current element is examined for any change to the default vocabulary via @vocab. If @vocab is present and contains a value, the local default vocabulary is updated according to the section on CURIE and IRI Processing. If the value is empty, then the local default vocabulary MUST be reset to the Host Language defined default (if any).
        if (isAttributePresent(RDFaAttributes.VOCAB)
                && !getAttributeStringValue(RDFaAttributes.VOCAB).isEmpty()) {
            this.localValuePile.getFirst().setDefaultVocabulary(getAttributeStringValue(RDFaAttributes.VOCAB));
        }

        logger.info("2 {} {}", qName, this.localValuePile.getFirst());

        // 3. The current element is examined for IRI mappings and these are added to the local list of IRI mappings. Note that an IRI mapping will simply overwrite any current mapping in the list that has the same name;
        for (int i = 0; i < this.currentElementAttributes.getLength(); i++) {
            String attribute = this.currentElementAttributes.getQName(i);
            logger.info("3 {} attribute: {}", qName, attribute);
            if (attribute.startsWith(XMLNS_PREFIX)) {
                String attributeValue = this.currentElementAttributes.getValue(i);
                String prefixName = attribute.replace(XMLNS_PREFIX + ":", "");
                logger.info("3 {} {} : {}", qName, prefixName, attributeValue);
                IRI prefixNamespace = getValueFactory().createIRI(attributeValue, "");
                this.localValuePile.getFirst().addIRIMappings(prefixName, prefixNamespace);
            }
        }
        logger.info("3: {} local uri mappings {}", qName, this.localValuePile.getFirst().getIRIMappings());
        if (isAttributePresent(RDFaAttributes.PREFIX)
                && !getAttributeStringValue(RDFaAttributes.PREFIX).isEmpty()) {
            String prefixDeclaration = getAttributeStringValue(RDFaAttributes.PREFIX);
            String prefixName = getPrefixFromDeclaration(prefixDeclaration);
            IRI prefixIRI = getPrefixIriFromDeclaration(prefixDeclaration);
            this.localValuePile.getFirst().addIRIMappings(prefixName, prefixIRI);
        }

        logger.info("3 {} {}", qName, this.localValuePile.getFirst());

        // 4. The current element is also parsed for any language information, and if present, current language is set accordingly;
        // Host Languages that incorporate RDFa MAY provide a mechanism for specifying the natural language of an element and its contents (e.g., XML provides the general-purpose XML attribute @xml:lang).
        if (isAttributePresent(RDFaAttributes.LANG_ALT)
                && !getAttributeStringValue(RDFaAttributes.LANG_ALT).isEmpty()) {
            this.localValuePile.getFirst().setCurrentLanguage(getAttributeStringValue(RDFaAttributes.LANG_ALT));
        }

        logger.info("4 {} {}", qName, this.localValuePile.getFirst());

        // 5. If the current element contains no @rel or @rev attribute, then the next step is to establish a value for new subject. This step has two possible alternatives.
        if (!isAttributePresent(RDFaAttributes.REL)
                && !isAttributePresent(RDFaAttributes.REV)) {
            // 5.1. If the current element contains the @property attribute, but does not contain either the @content or @datatype attributes, then
            if (isAttributePresent(RDFaAttributes.PROPERTY)
                    && !getAttributeStringValue(RDFaAttributes.PROPERTY).isEmpty()
                    && !isAttributePresent(RDFaAttributes.CONTENT)
                    && !isAttributePresent(RDFaAttributes.DATATYPE)
                    && (isAttributePresent(RDFaAttributes.ABOUT)
                    || isRootElement
                    || this.currentContext.getParentObjectResource() != null)) {
                // new subject is set to the resource obtained from the first match from the following rule:
                // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                if (isAttributePresent(RDFaAttributes.ABOUT)) {
                    this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.ABOUT));
                    logger.info("5.1 About {}", this.localValuePile.getFirst().getNewSubject());
                    // otherwise, if the element is the root element of the document, then act as if there is an empty @about present, and process it according to the rule for @about, above;
                } else if (isRootElement) {
                    this.localValuePile.getFirst().setNewSubject(this.currentContext.getBaseIri());
                    logger.info("5.1 Root element {}", this.localValuePile.getFirst().getNewSubject());
                    // otherwise, if parent object is present, new subject is set to the value of parent object.
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.localValuePile.getFirst().setNewSubject(this.currentContext.getParentObjectResource());
                    logger.info("5.1 context parent object {}", this.localValuePile.getFirst().getNewSubject());
                }
                // If @typeof is present then typed resource is set to the resource obtained from the first match from the following rules:
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                    if (isAttributePresent(RDFaAttributes.ABOUT)) {
                        this.localValuePile.getFirst().setTypedResource(this.localValuePile.getFirst().getNewSubject());
                        // otherwise, if the element is the root element of the document, then act as if there is an empty @about present and process it according to the previous rule;
                    } else if (isRootElement) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.localValuePile.getFirst().setTypedResource(emptyAboutResource.get());
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                        }
                        // otherwise,
                    } else {
                        // by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                        if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                            this.localValuePile.getFirst().setTypedResource(getAttributeResourceValue(RDFaAttributes.RESOURCE));
                            // otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.HREF)) {
                            this.localValuePile.getFirst().setTypedResource(getAttributeResourceValue(RDFaAttributes.HREF));
                            // otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.SRC)) {
                            this.localValuePile.getFirst().setTypedResource(getAttributeResourceValue(RDFaAttributes.SRC));
                            // otherwise, the value of typed resource is set to a newly created bnode.
                        } else {
                            this.localValuePile.getFirst().setTypedResource(getValueFactory().createBNode());
                        }
                        // The value of the current object resource is then set to the value of typed resource.
                        this.localValuePile.getFirst().setCurrentObjectResource(this.localValuePile.getFirst().getTypedResource());
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
                        this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.ABOUT));
                        logger.info("5.2 about {}", this.localValuePile.getFirst().getNewSubject());
                    // otherwise, by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                    } else if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                        this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.RESOURCE));
                        logger.info("5.2 resource {}", this.localValuePile.getFirst().getNewSubject());
                    // otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                    } else if (isAttributePresent(RDFaAttributes.HREF)) {
                        this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.HREF));
                        logger.info("5.2 href {}", this.localValuePile.getFirst().getNewSubject());
                    // otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing.
                    } else if (isAttributePresent(RDFaAttributes.SRC)) {
                        this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.SRC));
                        logger.info("5.2 src {}", this.localValuePile.getFirst().getNewSubject());
                    }
                // otherwise, if no resource is provided by a resource attribute, then the first match from the following rules will apply:
                } else {
                    // if the element is the root element of the document, then act as if there is an empty @about present, and process it according to the rule for @about, above;
                    if (isRootElement) {
                        logger.info("RootElement {}", qName);
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.localValuePile.getFirst().setNewSubject(emptyAboutResource.get());
                            logger.info("5.2 rootElement {}", this.localValuePile.getFirst());
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate newSubject from empty CURIE");
                        }
                    // otherwise, if @typeof is present, then new subject is set to be a newly created bnode;
                    } else if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                        this.localValuePile.getFirst().setNewSubject(getValueFactory().createBNode());
                        logger.info("5.2 typeOf {}", this.localValuePile.getFirst());
                    // otherwise, if parent object is present, new subject is set to the value of parent object. Additionally, if @property is not present then the skip element flag is set to 'true'.
                    } else if (this.currentContext.getParentObjectResource() != null) {
                        this.localValuePile.getFirst().setNewSubject(this.currentContext.getParentObjectResource());
                        logger.info("5.2 parent object resource {}", this.localValuePile.getFirst());
                        if (!isAttributePresent(RDFaAttributes.PROPERTY)) {
                            this.localValuePile.getFirst().setSkipElement(true);
                        }
                    }
                }
                // Finally, if @typeof is present, set the typed resource to the value of new subject.
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    this.localValuePile.getFirst().setTypedResource(this.localValuePile.getFirst().getNewSubject());
                }
            }
        }

        logger.info("5 {} {}", qName, this.localValuePile.getFirst());

        // 6. If the current element does contain a @rel or @rev attribute, then the next step is to establish both a value for new subject and a value for current object resource:
        if (isAttributePresent(RDFaAttributes.REL)
                || isAttributePresent(RDFaAttributes.REV)) {
            if (isAttributePresent(RDFaAttributes.ABOUT)) {
                this.localValuePile.getFirst().setNewSubject(getAttributeResourceValue(RDFaAttributes.ABOUT));
                logger.info("6 about newSubject: {}", this.localValuePile.getFirst().getNewSubject());
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                this.localValuePile.getFirst().setTypedResource(this.localValuePile.getFirst().getNewSubject());
                logger.info("6 typeof newSubject: {}", this.localValuePile.getFirst().getNewSubject());
            }
            if (this.localValuePile.getFirst().getNewSubject() == null) {
                if (isRootElement) {
                    Optional<Resource> emptyAboutResource = resolveStringResource("");
                    if (emptyAboutResource.isPresent()) {
                        this.localValuePile.getFirst().setTypedResource(emptyAboutResource.get());
                    } else {
                        throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                    }
                    logger.info("6 root element typed resource: {}", this.localValuePile.getFirst().getTypedResource());
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.localValuePile.getFirst().setNewSubject(this.currentContext.getParentObjectResource());
                    logger.info("6 parent object resource not null: {}", this.currentContext.getParentObjectResource());
                }
            }
            if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                this.localValuePile.getFirst().setCurrentObjectResource(getAttributeResourceValue(RDFaAttributes.RESOURCE));
                logger.info("6 resource CurrentObjectResource: {}", this.localValuePile.getFirst().getCurrentObjectResource());
            } else if (isAttributePresent(RDFaAttributes.HREF)) {
                this.localValuePile.getFirst().setCurrentObjectResource(getAttributeResourceValue(RDFaAttributes.HREF));
                logger.info("6 href CurrentObjectResource: {}", this.localValuePile.getFirst().getCurrentObjectResource());
            } else if (isAttributePresent(RDFaAttributes.SRC)) {
                this.localValuePile.getFirst().setCurrentObjectResource(getAttributeResourceValue(RDFaAttributes.SRC));
                logger.info("6 src CurrentObjectResource: {}", this.localValuePile.getFirst().getCurrentObjectResource());
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.localValuePile.getFirst().setCurrentObjectResource(this.getValueFactory().createBNode());
                logger.info("6 typeof CurrentObjectResource: {}", this.localValuePile.getFirst().getCurrentObjectResource());
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)
                    && (this.localValuePile.getFirst().getCurrentObjectResource() == null
                        || this.localValuePile.getFirst().getCurrentObjectResource().isResource())) {
                this.localValuePile.getFirst().setTypedResource(this.localValuePile.getFirst().getCurrentObjectResource());
                logger.info("6 typed resource: {}", this.localValuePile.getFirst().getTypedResource());
            }
        }

        logger.info("6 {} {}", qName, this.localValuePile.getFirst());

        // 7. If in any of the previous steps a typed resource was set to a non-null value, it is now used to provide a subject for type values;
        if (this.localValuePile.getFirst().getTypedResource() != null) {
            Resource typeIri = getAttributeResourceValue(RDFaAttributes.TYPEOF);
            this.getModel().add(this.localValuePile.getFirst().getTypedResource(), RDF.type.getIRI(), typeIri);
        }

        logger.info("7 {} {}", qName, this.localValuePile.getFirst());

        // 8. If in any of the previous steps a new subject was set to a non-null value different from the parent object;
        if (this.localValuePile.getFirst().getNewSubject() != null && this.localValuePile.getFirst().getNewSubject() != this.currentContext.getParentObjectResource()) {
            this.localValuePile.getFirst().setListMappings(new HashMap<>());
        }

        logger.info("8 {} {}", qName, this.localValuePile.getFirst());

        // 9. If in any of the previous steps a current object resource was set to a non-null value, it is now used to generate triples and add entries to the local list mapping:
        if (this.localValuePile.getFirst().getCurrentObjectResource() != null) {
            if (isAttributePresent(RDFaAttributes.INLIST)
                    && isAttributePresent(RDFaAttributes.REL)) {
                IRI relResource = (IRI) getAttributeResourceValue(RDFaAttributes.REL);
                this.localValuePile.getFirst().addListMapping(relResource, this.localValuePile.getFirst().getCurrentObjectResource());
            }
            if (!isAttributePresent(RDFaAttributes.INLIST)) {
                if (isAttributePresent(RDFaAttributes.REL)) {
                    Resource relResource = getAttributeResourceValue(RDFaAttributes.REL);
                    if (relResource.isIRI()) {
                        this.getModel().add(this.localValuePile.getFirst().getNewSubject(), (IRI) relResource, this.localValuePile.getFirst().getCurrentObjectResource());
                    } else {
                        throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                    }
                }
                if (isAttributePresent(RDFaAttributes.REV)) {
                    Resource revResource = getAttributeResourceValue(RDFaAttributes.REV);
                    if (!revResource.isIRI()) {
                        throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + getAttributeStringValue(RDFaAttributes.REV));
                    }
                    if (!this.localValuePile.getFirst().getCurrentObjectResource().isResource()) {
                        throw new ParsingErrorException("object resource expected to be a resource but was " + this.localValuePile.getFirst().getCurrentObjectResource());
                    }
                    this.getModel().add((Resource) this.localValuePile.getFirst().getCurrentObjectResource(), (IRI) revResource, this.localValuePile.getFirst().getNewSubject());
                }
            }
        }

        logger.info("9 {} {}", qName, this.localValuePile.getFirst());

        // 10. If however current object resource was set to null, but there are predicates present, then they must be stored as incomplete triples, pending the discovery of a subject that can be used as the object. Also, current object resource should be set to a newly created bnode (so that the incomplete triples have a subject to connect to if they are ultimately turned into triples);
        if (this.localValuePile.getFirst().getCurrentObjectResource() == null
                && (isAttributePresent(RDFaAttributes.REL)
        )           || isAttributePresent(RDFaAttributes.REV)) {
            if(this.localValuePile.getFirst().getIncompleteStatements() == null) {
                this.localValuePile.getFirst().setIncompleteStatements(new HashSet<>());
            }
            this.localValuePile.getFirst().setCurrentObjectResource(getValueFactory().createBNode());
            if (isAttributePresent(RDFaAttributes.REL)) {
                if (!getAttributeResourceValue(RDFaAttributes.REL).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                }
                IRI relIRI = (IRI) getAttributeResourceValue(RDFaAttributes.REL);
                if (isAttributePresent(RDFaAttributes.INLIST)) {
                    if (!this.localValuePile.getFirst().getListMappings().containsKey(relIRI)) {
                        this.localValuePile.getFirst().addListMappings(relIRI, new HashSet<>());
                    }
                    this.localValuePile.getFirst().addIncompleteStatement(new RDFaIncompleteStatement(relIRI, RDFaIncompleteStatement.Direction.NONE));
                } else {
                    this.localValuePile.getFirst().addIncompleteStatement(new RDFaIncompleteStatement(relIRI, RDFaIncompleteStatement.Direction.FORWARD));
                }
            } else if (isAttributePresent(RDFaAttributes.REV)) {
                if (!getAttributeResourceValue(RDFaAttributes.REV).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()));
                }
                IRI revIRI = (IRI) getAttributeResourceValue(RDFaAttributes.REV);
                this.localValuePile.getFirst().addIncompleteStatement(new RDFaIncompleteStatement(revIRI, RDFaIncompleteStatement.Direction.BACKWARD));
            }
        }

        logger.info("10 {} {}", qName, this.localValuePile.getFirst());

        // 11. The next step of the iteration is to establish any current property value;
        if (isAttributePresent(RDFaAttributes.PROPERTY)) {
            IRI propertyIRI = (IRI) getAttributeResourceValue(RDFaAttributes.PROPERTY);
            // as a typed literal if @datatype is present, does not have an empty value according to the section on CURIE and IRI Processing, and is not set to XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#.
            // The actual literal is either the value of @content (if present) or a string created by concatenating the value of all descendant text nodes, of the current element in turn. The final string includes the datatype IRI, as described in [RDF-SYNTAX-GRAMMAR], which will have been obtained according to the section on CURIE and IRI Processing.
            if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && getAttributeResourceValue(RDFaAttributes.DATATYPE).isIRI()
                    && getAttributeResourceValue(RDFaAttributes.DATATYPE) != RDF.XMLLiteral.getIRI()) {
                IRI datatypeIRI = (IRI) getAttributeResourceValue(RDFaAttributes.DATATYPE);
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = getAttributeStringValue(RDFaAttributes.CONTENT);
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, datatypeIRI));
                } else {
                    String contentString = this.characters.toString().trim();
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                    this.characters = new StringBuilder();
                }
                //  otherwise, as a plain literal if @datatype is present but has an empty value according to the section on CURIE and IRI Processing.
                // The actual literal is either the value of @content (if present) or a string created by concatenating the value of all descendant text nodes, of the current element in turn.
            } else if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && getAttributeStringValue(RDFaAttributes.DATATYPE).isEmpty()) {
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                } else {
                    String contentString = this.characters.toString().trim();
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                    this.characters = new StringBuilder();
                }
                // otherwise, as an XML literal if @datatype is present and is set to XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#.
                // The value of the XML literal is a string created by serializing to text, all nodes that are descendants of the current element, i.e., not including the element itself, and giving it a datatype of XMLLiteral in the vocabulary http://www.w3.org/1999/02/22-rdf-syntax-ns#. The format of the resulting serialized content is as defined in Exclusive XML Canonicalization Version 1.0 [XML-EXC-C14N].
                //} else if (this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()) != null
                //        && getAttributeResourceValue( RDFaAttributes.DATATYPE).isIRI()
                //        && getAttributeResourceValue( RDFaAttributes.DATATYPE) == RDF.XMLLiteral.getIRI()) {
                // otherwise, as a plain literal using the value of @content if @content is present.
            } else if (isAttributePresent(RDFaAttributes.CONTENT)) {
                String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
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
                    this.localValuePile.getFirst().setCurrentPropertyValue(getAttributeResourceValue(RDFaAttributes.RESOURCE));
                } else if (isAttributePresent(RDFaAttributes.HREF)) {
                    this.localValuePile.getFirst().setCurrentPropertyValue(getAttributeResourceValue(RDFaAttributes.HREF));
                } else if (isAttributePresent(RDFaAttributes.SRC)) {
                    this.localValuePile.getFirst().setCurrentPropertyValue(getAttributeResourceValue(RDFaAttributes.SRC));
                }
                // otherwise, if @typeof is present and @about is not, the value of typed resource.
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.localValuePile.getFirst().setCurrentPropertyValue(this.localValuePile.getFirst().getTypedResource());
                // otherwise as a plain literal.
            } else {
                String contentString = this.characters.toString().trim();
                // Additionally, if there is a value for current language then the value of the plain literal should include this language information, as described in [RDF-SYNTAX-GRAMMAR]. The actual literal is either the value of @content (if present) or a string created by concatenating the text content of each of the descendant elements of the current element in document order.
                if (this.localValuePile.getFirst().getCurrentLanguage() != null
                        && !this.localValuePile.getFirst().getCurrentLanguage().isEmpty()) {
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString, this.localValuePile.getFirst().getCurrentLanguage()));
                } else {
                    this.localValuePile.getFirst().setCurrentPropertyValue(getValueFactory().createLiteral(contentString));
                }
                this.characters = new StringBuilder();
            }

            // The current property value is then used with each predicate as follows:
            // If the element also includes the @inlist attribute, the current property value is added to the local list mapping as follows:
            if (isAttributePresent(RDFaAttributes.INLIST)) {
                // if the local list mapping does not contain a list associated with the predicate IRI, instantiate a new list and add to local list mappings
                if (!this.localValuePile.getFirst().getListMappings().containsKey(propertyIRI)) {
                    this.localValuePile.getFirst().addListMappings(propertyIRI, new HashSet<>());
                }
                // add the current property value to the list associated with the predicate IRI in the local list mapping
                this.localValuePile.getFirst().addListMapping(propertyIRI, this.localValuePile.getFirst().getCurrentPropertyValue());
                // Otherwise the current property value is used to generate a triple as follows:
                // subject new subject
                // predicate full IRI
                // object current property value
            } else {
                this.getModel().add(this.localValuePile.getFirst().getNewSubject(), propertyIRI, this.localValuePile.getFirst().getCurrentPropertyValue());
            }
        }

        logger.info("11 {} {}", qName, this.localValuePile.getFirst());

        // 12. If the skip element flag is 'false', and new subject was set to a non-null value, then any incomplete triples within the current context should be completed:
        if (!this.localValuePile.getFirst().isSkipElement()
                && this.localValuePile.getFirst().getNewSubject() != null) {
            if(this.localValuePile.getFirst().getIncompleteStatements() == null) {
                this.localValuePile.getFirst().setIncompleteStatements(new HashSet<>());
            }
            for (RDFaIncompleteStatement incompleteStatement : this.currentContext.getIncompleteStatement()) {
                if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.NONE) {
                    this.localValuePile.getFirst().addListMapping(incompleteStatement.getPredicate(), this.localValuePile.getFirst().getNewSubject());
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.FORWARD) {
                    this.getModel().add(this.currentContext.getParentSubjectResource(), incompleteStatement.getPredicate(), this.localValuePile.getFirst().getNewSubject());
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.BACKWARD) {
                    this.getModel().add(this.localValuePile.getFirst().getNewSubject(), incompleteStatement.getPredicate(), this.currentContext.getParentSubjectResource());
                }
            }
        }

        logger.info("12 {} {}", qName, this.localValuePile.getFirst());

        // 13. Next, all elements that are children of the current element are processed using the rules described here, using a new evaluation context, initialized as follows:
        // If the skip element flag is 'true' then the new evaluation context is a copy of the current context that was passed in to this level of processing, with the language and list of IRI mappings values replaced with the local values;
        if (this.localValuePile.getFirst().isSkipElement()) {
            this.currentContext = new RDFaEvaluationContext(this.currentContext);
            this.currentContext.clearIriMappings();
            initializeNewContext(this.currentContext);
            this.currentContext.setLanguage(this.localValuePile.getFirst().getCurrentLanguage());
            this.currentContext.setIriMappings(this.localValuePile.getFirst().getIRIMappings());
            // Otherwise, the values are:
        } else {
            Resource oldParentSubject = this.currentContext.getParentSubjectResource();
            // the base is set to the base value of the current evaluation context;
            this.currentContext = new RDFaEvaluationContext(this.currentContext.getBaseIri());
            initializeNewContext(this.currentContext);
            // the parent subject is set to the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
            this.currentContext.setParentSubjectResource(this.localValuePile.getFirst().getNewSubject());
            // the parent object is set to value of current object resource, if non-null, or the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
            if (this.localValuePile.getFirst().getCurrentObjectResource() != null) {
                logger.info("13 parent object resource = current object resource {}", this.localValuePile.getFirst().getCurrentObjectResource());
                this.currentContext.setParentObjectResource(this.localValuePile.getFirst().getCurrentObjectResource());
            } else if (this.localValuePile.getFirst().getNewSubject() != null) {
                this.currentContext.setParentObjectResource(this.localValuePile.getFirst().getNewSubject());
                logger.info("13 parent object resource = new subject {}", this.localValuePile.getFirst().getNewSubject());
            } else {
                this.currentContext.setParentObjectResource(oldParentSubject);
            }
            logger.info("13 context parent object resource: {}", this.currentContext.getParentObjectResource());
            // the list of IRI mappings is set to the local list of IRI mappings;
            this.currentContext.setIriMappings(this.localValuePile.getFirst().getIRIMappings());
            // the list of incomplete triples is set to the local list of incomplete triples;
            this.currentContext.setIncompleteStatements(this.localValuePile.getFirst().getIncompleteStatements());
            // the list mapping is set to the local list mapping;
            this.currentContext.setListMappings(this.localValuePile.getFirst().getListMappings());
            // language is set to the value of current language.
            this.currentContext.setLanguage(this.localValuePile.getFirst().getCurrentLanguage());
            // the default vocabulary is set to the value of the local default vocabulary.
            this.currentContext.setDefaultVocabulary(this.localValuePile.getFirst().getDefaultVocabulary());
        }

        logger.info("13 {} {}", qName, this.localValuePile.getFirst());

        this.isRootElement = false;
    }

    private void endProcessElement(String uri, String localName, String qName) {
        Map<IRI, Set<Value>> oldListMappings = this.currentContext.getListMappings();

        // 14. Finally, if there is one or more mapping in the local list mapping, list triples are generated as follows:
        for (Map.Entry<IRI, Set<Value>> listMapping : this.localValuePile.getFirst().getListMappings().entrySet()) {
            IRI propertyIRI = listMapping.getKey();
            Set<Value> propertyList = listMapping.getValue();

            if (!oldListMappings.containsKey(propertyIRI)) {
                if (propertyList.isEmpty()) {
                    getModel().add(this.localValuePile.getFirst().getNewSubject(), propertyIRI, RDF.nil.getIRI());
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
                    getModel().add(this.localValuePile.getFirst().getNewSubject(), propertyIRI, bnodes.getFirst());
                }
            }
        }

        logger.info("14 {} newSubject: {}, currentObjectResource: {}, currentPropertyValue: {}, typedResource: {}, skip: {}", qName, this.localValuePile.getFirst().getNewSubject(), this.localValuePile.getFirst().getCurrentObjectResource(), this.localValuePile.getFirst().getCurrentPropertyValue(), this.localValuePile.getFirst().getTypedResource(), this.localValuePile.getFirst().isSkipElement());

        this.localValuePile.pop();

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

    private String getPrefixFromDeclaration(String declaration) {
        String[] prefixArray = declaration.split(": ");
        if (prefixArray.length != 2) {
            throw new ParsingErrorException("Error during prefix extraction of " + declaration);
        }
        return prefixArray[0].toLowerCase();
    }

    private IRI getPrefixIriFromDeclaration(String declaration) {
        String[] prefixArray = declaration.split(": ");
        if (prefixArray.length != 2) {
            throw new ParsingErrorException("Error during prefix extraction of " + declaration);
        }
        return getValueFactory().createIRI(prefixArray[1].toLowerCase());
    }

    private Resource getAttributeResourceValue(RDFaAttributes attribute) {
        String attributeValue = this.currentElementAttributes.getValue(attribute.getName());
        Optional<Resource> resourceResolution = resolveStringResource(attributeValue);
        if (resourceResolution.isPresent()) {
            return resourceResolution.get();
        } else {
            throw new ParsingErrorException("Could not parse @" + attribute.getName() + " value: " + attributeValue);
        }
    }

    private boolean isAttributePresent(RDFaAttributes attribute) {
        return this.currentElementAttributes.getValue(attribute.getName()) != null;
    }

    private String getAttributeStringValue(RDFaAttributes attribute) {
        return this.currentElementAttributes.getValue(attribute.getName());
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
            if (currentContext.hasIriMapping(prefixString)) {
                IRI namespaceIRI = currentContext.getIriMapping(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (this.localValuePile.getFirst().getIRIMappings().containsKey(prefixString)) {
                IRI namespaceIRI = this.localValuePile.getFirst().getIRIMappings().get(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (prefixString.isEmpty()) { // CURIE is relative to the base URI
                return Optional.of(this.getValueFactory().createIRI(currentContext.getBaseIri().stringValue(), localNameString));
            } else {
                throw new ParsingErrorException("CURIE " + stringResource + " uses unknown prefix among " + this.currentContext.getIriMappings() + " and " + this.localValuePile.getFirst().getIRIMappings());
            }
        } else if (IRIUtils.isStandardIRI(resultString)) {  // Full IRI
            return Optional.of(this.getValueFactory().createIRI(resultString));

        } else if (resultString.startsWith("_:")) {  // Blank Node
            int colonIndex = resultString.indexOf(":");
            String localNameString = resultString.substring(colonIndex + 1);
            return Optional.of(this.getValueFactory().createBNode(localNameString));
        } else if (IRIUtils.isStandardIRI(currentContext.getBaseIri().stringValue() + resultString)) {
            String concatenatedRelativeUri = currentContext.getBaseIri().stringValue() + resultString;
            return Optional.of(this.getValueFactory().createIRI(concatenatedRelativeUri));
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
        return colonIndex > -1 && !stringIri.contains("://") && !stringIri.startsWith("_:") && !stringIri.startsWith("[_:");
    }

    private RDFaEvaluationContext getNewContext(IRI baseIRI) {
        RDFaEvaluationContext result = new RDFaEvaluationContext(baseIRI);
        initializeNewContext(result);
        return result;
    }

    private void initializeNewContext(RDFaEvaluationContext context) {
        // Initializing the iri mappings with the default prefixes as defined by https://www.w3.org/TR/rdfa-core/#xmlrdfaconformance
        for (RDFaInitialPrefixes prefixObject : RDFaInitialPrefixes.values()) {
            context.addIriMapping(prefixObject.getPrefix(), getValueFactory().createIRI(prefixObject.getName()));
        }

        // <a href="https://www.w3.org/2011/rdfa-context/rdfa-1.1">https://www.w3.org/2011/rdfa-context/rdfa-1.1</a> sets a list of predefined terms mappings for RDFa contexts.
        context.addTermMapping("describedby", getValueFactory().createIRI("http://www.w3.org/2007/05/powder-s#describedby"));
        context.addTermMapping("license", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#license"));
        context.addTermMapping("role", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#role"));
    }

    private String debugAttributesToString() {
        StringBuilder sb = new StringBuilder();

        if (this.currentElementAttributes != null) {
            for (int i = 0; i < this.currentElementAttributes.getLength(); i++) {
                String attributeLocalName = this.currentElementAttributes.getQName(i);
                String attributeValue = this.currentElementAttributes.getValue(i);
                sb.append(attributeLocalName).append(" : ").append(attributeValue).append(" ");
            }
        }

        return sb.toString();
    }
}
