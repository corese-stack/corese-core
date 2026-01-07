package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaAttributes;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaEvaluationContext;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaInitialPrefixes;
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
     * Buffer for accumulating character data between start and end tags.
     */
    private StringBuilder characters = new StringBuilder();

    // Local context
    private boolean skipElement = false;
    private Resource newSubject = null;
    private Resource currentObjectResource = null;
    private Resource typedResource = null;
    private Map<String, IRI> localIRIMappings = null;
    private Set<RDFaIncompleteStatement> localIncompleteStatements = null;
    private Map<IRI, Set<Value>> localListMappings = null;
    private String currentLanguage = null;
    private Value currentPropertyValue = null;
    private Map<String, IRI> localTermMappings = null;
    private String localDefaultVocabulary = null;

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
        logger.info("{} {}", qName, debugAttributesToString());

        this.characters = new StringBuilder();

        this.skipElement = false;
        this.newSubject = null;
        this.currentObjectResource = null;
        this.typedResource = null;
        this.localIRIMappings = this.currentContext.getIriMappings();
        this.localIncompleteStatements = null;
        this.localListMappings = this.currentContext.getListMappings();
        this.currentLanguage = this.currentContext.getLanguage();
        this.localTermMappings = this.currentContext.getTermMappings();
        this.localDefaultVocabulary = this.currentContext.getDefaultVocabulary();

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
            this.localDefaultVocabulary = getAttributeStringValue(RDFaAttributes.VOCAB);
        }

        // 3. The current element is examined for IRI mappings and these are added to the local list of IRI mappings. Note that an IRI mapping will simply overwrite any current mapping in the list that has the same name;
        for (int i = 0; i < this.currentElementAttributes.getLength(); i++) {
            String attribute = this.currentElementAttributes.getQName(i);
            if (attribute.startsWith(XMLNS_PREFIX)) {
                String attributeValue = this.currentElementAttributes.getValue(i);
                String prefixName = attribute.replace(XMLNS_PREFIX + ":", "");
                IRI prefixNamespace = getValueFactory().createIRI(attributeValue, "");
                this.localIRIMappings.put(prefixName, prefixNamespace);
            }
        }
        if (isAttributePresent(RDFaAttributes.PREFIX)
                && !getAttributeStringValue(RDFaAttributes.PREFIX).isEmpty()) {
            String prefixDeclaration = getAttributeStringValue(RDFaAttributes.PREFIX);
            String prefixName = getPrefixFromDeclaration(prefixDeclaration);
            IRI prefixIRI = getPrefixIriFromDeclaration(prefixDeclaration);
            this.localIRIMappings.put(prefixName, prefixIRI);
        }

        // 4. The current element is also parsed for any language information, and if present, current language is set accordingly;
        // Host Languages that incorporate RDFa MAY provide a mechanism for specifying the natural language of an element and its contents (e.g., XML provides the general-purpose XML attribute @xml:lang).
        if (isAttributePresent(RDFaAttributes.LANG_ALT)
                && !getAttributeStringValue(RDFaAttributes.LANG_ALT).isEmpty()) {
            this.currentLanguage = getAttributeStringValue(RDFaAttributes.LANG_ALT);
        }

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
                    this.newSubject = getAttributeResourceValue(RDFaAttributes.ABOUT);
                    // otherwise, if the element is the root element of the document, then act as if there is an empty @about present, and process it according to the rule for @about, above;
                } else if (isRootElement) {
                    this.newSubject = this.currentContext.getBaseIri();
                    // otherwise, if parent object is present, new subject is set to the value of parent object.
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.newSubject = this.currentContext.getParentObjectResource();
                }
                // If @typeof is present then typed resource is set to the resource obtained from the first match from the following rules:
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    // by using the resource from @about, if present, obtained according to the section on CURIE and IRI Processing;
                    if (isAttributePresent(RDFaAttributes.ABOUT)) {
                        this.typedResource = this.newSubject;
                        // otherwise, if the element is the root element of the document, then act as if there is an empty @about present and process it according to the previous rule;
                    } else if (isRootElement) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.typedResource = emptyAboutResource.get();
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                        }
                        // otherwise,
                    } else {
                        // by using the resource from @resource, if present, obtained according to the section on CURIE and IRI Processing;
                        if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                            this.typedResource = getAttributeResourceValue(RDFaAttributes.RESOURCE);
                            // otherwise, by using the IRI from @href, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.HREF)) {
                            this.typedResource = getAttributeResourceValue(RDFaAttributes.HREF);
                            // otherwise, by using the IRI from @src, if present, obtained according to the section on CURIE and IRI Processing;
                        } else if (isAttributePresent(RDFaAttributes.SRC)) {
                            this.typedResource = getAttributeResourceValue(RDFaAttributes.SRC);
                            // otherwise, the value of typed resource is set to a newly created bnode.
                        } else {
                            this.typedResource = getValueFactory().createBNode();
                        }
                        // The value of the current object resource is then set to the value of typed resource.
                        this.currentObjectResource = this.typedResource;
                    }
                }
                logger.info("{}", this.newSubject);
                // 5.2. otherwise:
            } else {
                if (isAttributePresent(RDFaAttributes.ABOUT)
                        || isAttributePresent(RDFaAttributes.HREF)
                        || isAttributePresent(RDFaAttributes.SRC)
                        || isAttributePresent(RDFaAttributes.RESOURCE)) {
                    if (isAttributePresent(RDFaAttributes.ABOUT)) {
                        this.newSubject = getAttributeResourceValue(RDFaAttributes.ABOUT);
                        logger.info("{}", this.newSubject);
                    } else if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                        this.newSubject = getAttributeResourceValue(RDFaAttributes.RESOURCE);
                        logger.info("{}", this.newSubject);
                    } else if (isAttributePresent(RDFaAttributes.HREF)) {
                        this.newSubject = getAttributeResourceValue(RDFaAttributes.HREF);
                        logger.info("{}", this.newSubject);
                    } else if (isAttributePresent(RDFaAttributes.SRC)) {
                        this.newSubject = getAttributeResourceValue(RDFaAttributes.SRC);
                        logger.info("{}", this.newSubject);
                    } else {
                        logger.info("No subject retrieved");
                    }
                } else {
                    if (isRootElement) {
                        Optional<Resource> emptyAboutResource = resolveStringResource("");
                        if (emptyAboutResource.isPresent()) {
                            this.newSubject = emptyAboutResource.get();
                        } else {
                            throw new ParsingErrorException("Expected to be able to generate newSubject from empty CURIE");
                        }
                    } else if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                        this.newSubject = getValueFactory().createBNode();
                        logger.info("{}", this.newSubject);
                    } else if (this.currentContext.getParentObjectResource() != null) {
                        this.newSubject = this.currentContext.getParentObjectResource();
                        if (!isAttributePresent(RDFaAttributes.PROPERTY)) {
                            skipElement = true;
                        }
                    }
                }
                if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                    this.typedResource = this.newSubject;
                }
            }
        }

        // 6. If the current element does contain a @rel or @rev attribute, then the next step is to establish both a value for new subject and a value for current object resource:
        if (isAttributePresent(RDFaAttributes.REL)
                || isAttributePresent(RDFaAttributes.REV)) {
            if (isAttributePresent(RDFaAttributes.ABOUT)) {
                this.newSubject = getAttributeResourceValue(RDFaAttributes.ABOUT);
                logger.info("{}", this.newSubject);
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)) {
                this.typedResource = this.newSubject;
            }
            if (this.newSubject == null) {
                if (isRootElement) {
                    Optional<Resource> emptyAboutResource = resolveStringResource("");
                    if (emptyAboutResource.isPresent()) {
                        this.typedResource = emptyAboutResource.get();
                    } else {
                        throw new ParsingErrorException("Expected to be able to generate typedResource from empty CURIE");
                    }
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.newSubject = this.currentContext.getParentObjectResource();
                }
            }
            if (isAttributePresent(RDFaAttributes.RESOURCE)) {
                this.currentObjectResource = getAttributeResourceValue(RDFaAttributes.RESOURCE);
            } else if (isAttributePresent(RDFaAttributes.HREF)) {
                this.currentObjectResource = getAttributeResourceValue(RDFaAttributes.HREF);
            } else if (isAttributePresent(RDFaAttributes.SRC)) {
                this.currentObjectResource = getAttributeResourceValue(RDFaAttributes.SRC);
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                this.currentObjectResource = this.getValueFactory().createBNode();
            }
            if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)
                    && (this.currentObjectResource == null || this.currentObjectResource.isResource())) {
                this.typedResource = this.currentObjectResource;
            }
        }
        logger.info("{} : subject {}", qName, this.newSubject);

        // 7. If in any of the previous steps a typed resource was set to a non-null value, it is now used to provide a subject for type values;
        if (this.typedResource != null) {
            Resource typeIri = getAttributeResourceValue(RDFaAttributes.TYPEOF);
            this.getModel().add(this.typedResource, RDF.type.getIRI(), typeIri);
        }

        // 8. If in any of the previous steps a new subject was set to a non-null value different from the parent object;
        if (this.newSubject != null && this.newSubject != this.currentContext.getParentObjectResource()) {
            this.localListMappings = new HashMap<>();
        }

        // 9. If in any of the previous steps a current object resource was set to a non-null value, it is now used to generate triples and add entries to the local list mapping:
        if (this.currentObjectResource != null) {
            if (isAttributePresent(RDFaAttributes.INLIST)
                    && isAttributePresent(RDFaAttributes.REL)) {
                IRI relResource = (IRI) getAttributeResourceValue(RDFaAttributes.REL);
                if (!localListMappings.containsKey(relResource)) {
                    this.localListMappings.put(relResource, new HashSet<>());
                }
                this.localListMappings.get(relResource).add(this.currentObjectResource);
            }
            if (!isAttributePresent(RDFaAttributes.INLIST)) {
                if (isAttributePresent(RDFaAttributes.REL)) {
                    Resource relResource = getAttributeResourceValue(RDFaAttributes.REL);
                    if (relResource.isIRI()) {
                        this.getModel().add(newSubject, (IRI) relResource, currentObjectResource);
                    } else {
                        throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                    }
                }
                if (isAttributePresent(RDFaAttributes.REV)) {
                    Resource revResource = getAttributeResourceValue(RDFaAttributes.REV);
                    if (!revResource.isIRI()) {
                        throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + getAttributeStringValue(RDFaAttributes.REV));
                    }
                    if (!currentObjectResource.isResource()) {
                        throw new ParsingErrorException("object resource expected to be a resource but was " + currentObjectResource);
                    }
                    this.getModel().add((Resource) currentObjectResource, (IRI) revResource, newSubject);
                }
            }
        }

        // 10. If however current object resource was set to null, but there are predicates present, then they must be stored as incomplete triples, pending the discovery of a subject that can be used as the object. Also, current object resource should be set to a newly created bnode (so that the incomplete triples have a subject to connect to if they are ultimately turned into triples);
        if (this.currentObjectResource == null) {
            if(this.localIncompleteStatements == null) {
                this.localIncompleteStatements = new HashSet<>();
            }
            this.currentObjectResource = getValueFactory().createBNode();
            if (isAttributePresent(RDFaAttributes.REL)) {
                if (!getAttributeResourceValue(RDFaAttributes.REL).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                }
                IRI relIRI = (IRI) getAttributeResourceValue(RDFaAttributes.REL);
                if (isAttributePresent(RDFaAttributes.INLIST)) {
                    if (!localListMappings.containsKey(relIRI)) {
                        this.localListMappings.put(relIRI, new HashSet<>());
                    }
                    this.localIncompleteStatements.add(new RDFaIncompleteStatement(relIRI, RDFaIncompleteStatement.Direction.NONE));
                } else {
                    this.localIncompleteStatements.add(new RDFaIncompleteStatement(relIRI, RDFaIncompleteStatement.Direction.FORWARD));
                }
            } else if (isAttributePresent(RDFaAttributes.REV)) {
                if (!getAttributeResourceValue(RDFaAttributes.REV).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()));
                }
                IRI revIRI = (IRI) getAttributeResourceValue(RDFaAttributes.REV);
                this.localIncompleteStatements.add(new RDFaIncompleteStatement(revIRI, RDFaIncompleteStatement.Direction.BACKWARD));
            }
        }

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
                    currentPropertyValue = getValueFactory().createLiteral(contentString, datatypeIRI);
                } else {
                    String contentString = this.characters.toString().trim();
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                    this.characters = new StringBuilder();
                }
                //  otherwise, as a plain literal if @datatype is present but has an empty value according to the section on CURIE and IRI Processing.
                // The actual literal is either the value of @content (if present) or a string created by concatenating the value of all descendant text nodes, of the current element in turn.
            } else if (isAttributePresent(RDFaAttributes.DATATYPE)
                    && getAttributeStringValue(RDFaAttributes.DATATYPE).isEmpty()) {
                if (isAttributePresent(RDFaAttributes.CONTENT)) {
                    String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                } else {
                    String contentString = this.characters.toString().trim();
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
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
                currentPropertyValue = getValueFactory().createLiteral(contentString);
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
                    currentPropertyValue = getAttributeResourceValue(RDFaAttributes.RESOURCE);
                } else if (isAttributePresent(RDFaAttributes.HREF)) {
                    currentPropertyValue = getAttributeResourceValue(RDFaAttributes.HREF);
                } else if (isAttributePresent(RDFaAttributes.SRC)) {
                    currentPropertyValue = getAttributeResourceValue(RDFaAttributes.SRC);
                }
                // otherwise, if @typeof is present and @about is not, the value of typed resource.
            } else if (isAttributePresent(RDFaAttributes.TYPEOF)
                    && !isAttributePresent(RDFaAttributes.ABOUT)) {
                currentPropertyValue = typedResource;
                // otherwise as a plain literal.
            } else {
                String contentString = this.characters.toString().trim();
                // Additionally, if there is a value for current language then the value of the plain literal should include this language information, as described in [RDF-SYNTAX-GRAMMAR]. The actual literal is either the value of @content (if present) or a string created by concatenating the text content of each of the descendant elements of the current element in document order.
                if (this.currentLanguage != null
                        && !this.currentLanguage.isEmpty()) {
                    currentPropertyValue = getValueFactory().createLiteral(contentString, this.currentLanguage);
                } else {
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                }
                this.characters = new StringBuilder();
            }

            // The current property value is then used with each predicate as follows:
            // If the element also includes the @inlist attribute, the current property value is added to the local list mapping as follows:
            if (isAttributePresent(RDFaAttributes.INLIST)) {
                // if the local list mapping does not contain a list associated with the predicate IRI, instantiate a new list and add to local list mappings
                if (!this.localListMappings.containsKey(propertyIRI)) {
                    this.localListMappings.put(propertyIRI, new HashSet<>());
                }
                // add the current property value to the list associated with the predicate IRI in the local list mapping
                this.localListMappings.get(propertyIRI).add(currentPropertyValue);
                // Otherwise the current property value is used to generate a triple as follows:
                // subject new subject
                // predicate full IRI
                // object current property value
            } else {
                this.getModel().add(this.newSubject, propertyIRI, this.currentPropertyValue);
            }
        }

        // 12. If the skip element flag is 'false', and new subject was set to a non-null value, then any incomplete triples within the current context should be completed:
        if (!this.skipElement
                && this.newSubject != null) {
            if(this.localIncompleteStatements == null) {
                this.localIncompleteStatements = new HashSet<>();
            }
            for (RDFaIncompleteStatement incompleteStatement : this.currentContext.getIncompleteStatement()) {
                if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.NONE) {
                    localListMappings.get(incompleteStatement.getPredicate()).add(this.newSubject);
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.FORWARD) {
                    this.getModel().add(this.currentContext.getParentSubjectResource(), incompleteStatement.getPredicate(), this.newSubject);
                } else if (incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.BACKWARD) {
                    this.getModel().add(this.newSubject, incompleteStatement.getPredicate(), this.currentContext.getParentSubjectResource());
                }
            }
        }

        // 13. Next, all elements that are children of the current element are processed using the rules described here, using a new evaluation context, initialized as follows:
        // If the skip element flag is 'true' then the new evaluation context is a copy of the current context that was passed in to this level of processing, with the language and list of IRI mappings values replaced with the local values;
        if (this.skipElement) {
            this.currentContext = new RDFaEvaluationContext(this.currentContext);
            this.currentContext.clearIriMappings();
            initializeNewContext(this.currentContext);
            this.currentContext.setLanguage(this.currentLanguage);
            this.currentContext.addIriMappings(this.localIRIMappings);
            // Otherwise, the values are:
        } else {
            Resource oldParentSubject = this.currentContext.getParentSubjectResource();
            // the base is set to the base value of the current evaluation context;
            this.currentContext = new RDFaEvaluationContext(this.currentContext.getBaseIri());
            initializeNewContext(this.currentContext);
            // the parent subject is set to the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
            this.currentContext.setParentSubjectResource(this.newSubject);
            // the parent object is set to value of current object resource, if non-null, or the value of new subject, if non-null, or the value of the parent subject of the current evaluation context;
            if (this.currentObjectResource != null) {
                this.currentContext.setParentObjectResource(this.currentObjectResource);
            } else if (this.newSubject != null) {
                this.currentContext.setParentObjectResource(this.newSubject);
            } else {
                this.currentContext.setParentObjectResource(oldParentSubject);
            }
            // the list of IRI mappings is set to the local list of IRI mappings;
            this.currentContext.addIriMappings(this.localIRIMappings);
            // the list of incomplete triples is set to the local list of incomplete triples;
            this.currentContext.setIncompleteStatements(this.localIncompleteStatements);
            // the list mapping is set to the local list mapping;
            this.currentContext.setListMappings(this.localListMappings);
            // language is set to the value of current language.
            this.currentContext.setLanguage(this.currentLanguage);
            // the default vocabulary is set to the value of the local default vocabulary.
            this.currentContext.setDefaultVocabulary(this.localDefaultVocabulary);
        }

        this.isRootElement = false;
    }

    private void endProcessElement(String uri, String localName, String qName) {
        Map<IRI, Set<Value>> oldListMappings = this.currentContext.getListMappings();

        // 14. Finally, if there is one or more mapping in the local list mapping, list triples are generated as follows:
        for (Map.Entry<IRI, Set<Value>> listMapping : this.localListMappings.entrySet()) {
            IRI propertyIRI = listMapping.getKey();
            Set<Value> propertyList = listMapping.getValue();

            if (!oldListMappings.containsKey(propertyIRI)) {
                if (propertyList.isEmpty()) {
                    getModel().add(this.newSubject, propertyIRI, RDF.nil.getIRI());
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
                    getModel().add(this.newSubject, propertyIRI, bnodes.getFirst());
                }
            }
        }

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
            } else if (localIRIMappings.containsKey(prefixString)) {
                IRI namespaceIRI = localIRIMappings.get(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (prefixString.isEmpty()) { // CURIE is relative to the base URI
                return Optional.of(this.getValueFactory().createIRI(currentContext.getBaseIri().stringValue(), localNameString));
            } else {
                logger.info("{} context mappings", currentContext.getIriMappings().size());
                logger.info("{} local mappings", localIRIMappings.size());
                throw new ParsingErrorException("CURIE " + stringResource + " uses unknown prefix");
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
