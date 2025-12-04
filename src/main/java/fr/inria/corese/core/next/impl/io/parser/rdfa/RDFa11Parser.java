package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.impl.common.vocabulary.RDFa;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFa11EvaluationContext;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaAttributes;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaInitialPrefixes;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
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
public class RDFa11Parser extends AbstractRDFaParser {

    private static final Logger logger = LoggerFactory.getLogger(RDFa11Parser.class);

    private static final String BASE_TAG = "base";
    private static final String XMLNS_PREFIX = "xmlns";

    private RDFa11EvaluationContext currentContext = null;

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
    private Map<String, IRI> localTermMappings = null;
    private String localDefaultVocabulary = null;

    private boolean isRootElement = true;
    private Attributes currentElementAttributes = null;

    private Model parsingModel = new CoreseModel();

    public RDFa11Parser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFa11Parser(Model model, ValueFactory factory, IOOptions config) {
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
        return RDFFormat.RDFa_1_1;
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        try {
            this.currentContext = new RDFa11EvaluationContext(getValueFactory().createIRI(baseURI));
            this.currentContext.setParentSubjectResource(this.currentContext.getBaseIri());
            this.currentContext.setParentObjectResource(null);
            this.currentContext.setLanguage(null);

            // Initializing the iri mappings with the default prefixes as defined by https://www.w3.org/TR/rdfa-core/#xmlrdfaconformance
            for (RDFaInitialPrefixes prefixObject : RDFaInitialPrefixes.values()) {
                currentContext.addIriMapping(prefixObject.getPrefix(), getValueFactory().createIRI(prefixObject.getName()));
            }

            // <a href="https://www.w3.org/2011/rdfa-context/rdfa-1.1">https://www.w3.org/2011/rdfa-context/rdfa-1.1</a> sets a list of predefined terms mappings for RDFa contexts.
            this.currentContext.addTermMapping("describedby", getValueFactory().createIRI("http://www.w3.org/2007/05/powder-s#describedby"));
            this.currentContext.addTermMapping("license", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#license"));
            this.currentContext.addTermMapping("role", getValueFactory().createIRI("http://www.w3.org/1999/xhtml/vocab#role"));

            this.currentContext.setDefaultVocabulary(null);


            skipElement = false;
            newSubject = null;
            currentObjectResource = null;
            typedResource = null;
            localIRIMappings = new HashMap<>();
            localIncompleteStatements = new HashSet<>();
            localListMappings = this.currentContext.getListMappings();
            currentLanguage = this.currentContext.getLanguage();
            localTermMappings = this.currentContext.getTermMappings();
            localDefaultVocabulary = this.currentContext.getDefaultVocabulary();

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
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
    }

    private void endProcessElement(String uri, String localName, String qName) {
        String currentElementName = qName;

        // The current element is examined for any change to the default vocabulary via @vocab. If @vocab is present and contains a value, the local default vocabulary is updated according to the section on CURIE and IRI Processing. If the value is empty, then the local default vocabulary MUST be reset to the Host Language defined default (if any).
        if (this.currentElementAttributes.getValue(RDFaAttributes.VOCAB.getName()) != null
                && !this.currentElementAttributes.getValue(RDFaAttributes.VOCAB.getName()).isEmpty()) {
            String vocabValue = this.currentElementAttributes.getValue(RDFaAttributes.VOCAB.getName());
            localDefaultVocabulary = vocabValue;
            parsingModel.add(this.currentContext.getBaseIri(), RDFa.usesVocabulary.getIRI(), getValueFactory().createLiteral(vocabValue));
        }

        // The current element is examined for IRI mappings and these are added to the local list of IRI mappings. Note that an IRI mapping will simply overwrite any current mapping in the list that has the same name;
        for (int i = 0; i < this.currentElementAttributes.getLength(); i++) {
            String attribute = this.currentElementAttributes.getQName(i);
            if (attribute.startsWith(XMLNS_PREFIX)) {
                String attributeValue = this.currentElementAttributes.getValue(i);
                String prefixName = this.currentElementAttributes.getLocalName(i);
                IRI prefixNamespace = getValueFactory().createIRI(attributeValue, "");
                localIRIMappings.put(prefixName, prefixNamespace);
            }
        }
        if (this.currentElementAttributes.getValue(RDFaAttributes.PREFIX.getName()) != null
                && !this.currentElementAttributes.getValue(RDFaAttributes.PREFIX.getName()).isEmpty()) {
            String prefixDeclaration = this.currentElementAttributes.getValue(RDFaAttributes.PREFIX.getName());
            String prefixName = getPrefixFromDeclaration(prefixDeclaration);
            IRI prefixIRI = getPrefixIriFromDeclaration(prefixDeclaration);
            localIRIMappings.put(prefixName, prefixIRI);
        }

        // If the current element contains no @rel or @rev attribute, then the next step is to establish a value for new subject. This step has two possible alternatives.
        if (this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) == null
                && this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()) == null) {
            // If the current element contains the @property attribute, but does not contain either the @content or @datatype attributes, then
            if (this.currentElementAttributes.getValue(RDFaAttributes.PROPERTY.getName()) != null
                    && !this.currentElementAttributes.getValue(RDFaAttributes.PROPERTY.getName()).isEmpty()
                    && this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName()) == null
                    && this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()) == null) {
                if (this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) != null) {
                    this.newSubject = getResourceFromElementAttribute(RDFaAttributes.ABOUT);
                } else if (isRootElement) {
                    this.newSubject = this.currentContext.getBaseIri();
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.newSubject = this.currentContext.getParentObjectResource();
                }
                if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null) {
                    if (this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) != null) {
                        this.typedResource = this.newSubject;
                    } else if (isRootElement) {
                        this.typedResource = resolveStringResource("", this.currentContext).get();
                    } else {
                        if (this.currentElementAttributes.getValue(RDFaAttributes.RESOURCE.getName()) != null) {
                            this.newSubject = getResourceFromElementAttribute(RDFaAttributes.RESOURCE);
                        } else if (this.currentElementAttributes.getValue(RDFaAttributes.HREF.getName()) != null) {
                            this.newSubject = getResourceFromElementAttribute( RDFaAttributes.HREF);
                        } else if (this.currentElementAttributes.getValue(RDFaAttributes.SRC.getName()) != null) {
                            this.newSubject = getResourceFromElementAttribute( RDFaAttributes.SRC);
                        } else {
                            this.typedResource = getValueFactory().createBNode();
                        }
                        this.currentObjectResource = this.typedResource;
                    }
                }
                // otherwise:
            } else {
                if (this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) != null
                        && this.currentElementAttributes.getValue(RDFaAttributes.HREF.getName()) != null
                        && this.currentElementAttributes.getValue(RDFaAttributes.SRC.getName()) != null
                        && this.currentElementAttributes.getValue(RDFaAttributes.RESOURCE.getName()) != null) {
                    if (this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) != null) {
                        this.newSubject = getResourceFromElementAttribute( RDFaAttributes.ABOUT);
                    } else if (this.currentElementAttributes.getValue(RDFaAttributes.RESOURCE.getName()) != null) {
                        this.newSubject = getResourceFromElementAttribute( RDFaAttributes.RESOURCE);
                    } else if (this.currentElementAttributes.getValue(RDFaAttributes.HREF.getName()) != null) {
                        this.newSubject = getResourceFromElementAttribute( RDFaAttributes.HREF);
                    } else if (this.currentElementAttributes.getValue(RDFaAttributes.SRC.getName()) != null) {
                        this.newSubject = getResourceFromElementAttribute( RDFaAttributes.SRC);
                    }
                } else {
                    if (isRootElement) {
                        this.newSubject = resolveStringResource("", this.currentContext).get();
                    } else if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null) {
                        this.newSubject = getValueFactory().createBNode();
                    } else if (this.currentContext.getParentObjectResource() != null) {
                        this.newSubject = this.currentContext.getParentObjectResource();
                        if (this.currentElementAttributes.getValue(RDFaAttributes.PROPERTY.getName()) == null) {
                            skipElement = true;
                        }
                    }
                    if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null) {
                        this.typedResource = this.newSubject;
                    }
                }
            }
        }

        //  If the current element does contain a @rel or @rev attribute, then the next step is to establish both a value for new subject and a value for current object resource:
        if (this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) != null
                && this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()) != null) {
            if (this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) != null) {
                this.newSubject = getResourceFromElementAttribute( RDFaAttributes.ABOUT);
            }
            if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null) {
                this.typedResource = this.newSubject;
            }
            if (this.newSubject == null) {
                if (isRootElement) {
                    this.typedResource = resolveStringResource("", this.currentContext).get();
                } else if (this.currentContext.getParentObjectResource() != null) {
                    this.newSubject = this.currentContext.getParentObjectResource();
                }
            }
            if (this.currentElementAttributes.getValue(RDFaAttributes.RESOURCE.getName()) != null) {
                this.currentObjectResource = getResourceFromElementAttribute( RDFaAttributes.RESOURCE);
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.HREF.getName()) != null) {
                this.currentObjectResource = getResourceFromElementAttribute( RDFaAttributes.HREF);
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.SRC.getName()) != null) {
                this.currentObjectResource = getResourceFromElementAttribute( RDFaAttributes.SRC);
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null
                    && this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) == null) {
                this.currentObjectResource = this.getValueFactory().createBNode();
            }
            if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null
                    && this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) == null
                    && (this.currentObjectResource == null || this.currentObjectResource.isResource())) {
                this.typedResource = (Resource) this.currentObjectResource;
            }
        }

        //  If in any of the previous steps a typed resource was set to a non-null value, it is now used to provide a subject for type values;
        if (this.typedResource != null) {
            Resource typeIri = getResourceFromElementAttribute( RDFaAttributes.TYPEOF);
            this.getModel().add(this.typedResource, RDF.type.getIRI(), typeIri);
        }

        //  If in any of the previous steps a new subject was set to a non-null value different from the parent object;
        if (this.newSubject != null && this.newSubject != this.currentContext.getParentObjectResource()) {
            this.localListMappings = new HashMap<>();
        }

        //  If in any of the previous steps a current object resource was set to a non-null value, it is now used to generate triples and add entries to the local list mapping:
        if (this.currentObjectResource != null) {
            if (this.currentElementAttributes.getValue(RDFaAttributes.INLIST.getName()) != null
                    && this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) != null) {
                IRI relResource = (IRI) getResourceFromElementAttribute( RDFaAttributes.REL);
                if (!localListMappings.containsKey(relResource)) {
                    this.localListMappings.put(relResource, new HashSet<>());
                }
                this.localListMappings.get(relResource).add(this.currentObjectResource);
            }
            if (this.currentElementAttributes.getValue(RDFaAttributes.INLIST.getName()) == null) {
                if (this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) != null) {
                    Resource relResource = getResourceFromElementAttribute( RDFaAttributes.REL);
                    if (relResource.isIRI()) {
                        this.getModel().add(newSubject, (IRI) relResource, currentObjectResource);
                    } else {
                        throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                    }
                }
                if (this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()) != null) {
                    Resource revResource = getResourceFromElementAttribute( RDFaAttributes.REV);
                    if (!revResource.isIRI()) {
                        throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()));
                    }
                    if (!currentObjectResource.isResource()) {
                        throw new ParsingErrorException("object resource expected to be a resource but was " + currentObjectResource);
                    }
                    this.getModel().add((Resource) currentObjectResource, (IRI) revResource, newSubject);
                }
            }
        }

        //  If however current object resource was set to null, but there are predicates present, then they must be stored as incomplete triples, pending the discovery of a subject that can be used as the object. Also, current object resource should be set to a newly created bnode (so that the incomplete triples have a subject to connect to if they are ultimately turned into triples);
        if (this.currentObjectResource == null) {
            this.currentObjectResource = getValueFactory().createBNode();
            if (this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) != null) {
                if(! getResourceFromElementAttribute( RDFaAttributes.REL).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rel expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()));
                }
                IRI relIRI = (IRI) getResourceFromElementAttribute( RDFaAttributes.REL);
                if(this.currentElementAttributes.getValue(RDFaAttributes.INLIST.getName()) != null) { // TODO: Step to be double checked, standard unclear
                    if (!localListMappings.containsKey(relIRI)) {
                        this.localListMappings.put(relIRI, new HashSet<>());
                    }
                    this.localListMappings.get(relIRI).add(this.currentObjectResource);
                } else {
                    this.localIncompleteStatements.add(new RDFaIncompleteStatement(relIRI, RDFaIncompleteStatement.Direction.FORWARD));
                }
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()) != null) {
                if(! getResourceFromElementAttribute( RDFaAttributes.REV).isIRI()) {
                    throw new ParsingErrorException("Value of attribute @rev expected to be an IRI but was " + this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()));
                }
                IRI revIRI = (IRI) getResourceFromElementAttribute( RDFaAttributes.REV);
                this.localIncompleteStatements.add(new RDFaIncompleteStatement(revIRI, RDFaIncompleteStatement.Direction.BACKWARD));
            }
        }

        // The next step of the iteration is to establish any current property value;
        if(this.currentElementAttributes.getValue(RDFaAttributes.PROPERTY.getName()) != null) {
            IRI propertyIRI = (IRI) getResourceFromElementAttribute(RDFaAttributes.PROPERTY);
            Value currentPropertyValue = null;
            if (this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()) != null
                    && getResourceFromElementAttribute(RDFaAttributes.DATATYPE).isIRI()
                    && getResourceFromElementAttribute(RDFaAttributes.DATATYPE) != RDF.XMLLiteral.getIRI()) {
                IRI datatypeIRI = (IRI) getResourceFromElementAttribute(RDFaAttributes.DATATYPE);
                if (this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName()) != null) {
                    String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                    currentPropertyValue = getValueFactory().createLiteral(contentString, datatypeIRI);
                } else {
                    String contentString = this.characters.toString().trim();
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                    this.characters = new StringBuilder();
                }
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()) != null
                    && this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()).isEmpty()) {
                if (this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName()) != null) {
                    String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                } else {
                    String contentString = this.characters.toString().trim();
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                    this.characters = new StringBuilder();
                }
            //} else if (this.currentElementAttributes.getValue(RDFaAttributes.DATATYPE.getName()) != null
            //        && getResourceFromElementAttribute( RDFaAttributes.DATATYPE).isIRI()
            //        && getResourceFromElementAttribute( RDFaAttributes.DATATYPE) == RDF.XMLLiteral.getIRI()) {
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName()) != null) {
                String contentString = this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName());
                currentPropertyValue = getValueFactory().createLiteral(contentString);
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.REL.getName()) == null
                    && this.currentElementAttributes.getValue(RDFaAttributes.REV.getName()) == null
                    && this.currentElementAttributes.getValue(RDFaAttributes.CONTENT.getName()) == null) {
                if(this.currentElementAttributes.getValue(RDFaAttributes.RESOURCE.getName()) != null) {
                    currentPropertyValue = getResourceFromElementAttribute(RDFaAttributes.RESOURCE);
                } else if(this.currentElementAttributes.getValue(RDFaAttributes.HREF.getName()) != null) {
                    currentPropertyValue = getResourceFromElementAttribute(RDFaAttributes.HREF);
                } else if(this.currentElementAttributes.getValue(RDFaAttributes.SRC.getName()) != null) {
                    currentPropertyValue = getResourceFromElementAttribute(RDFaAttributes.SRC);
                }
            } else if (this.currentElementAttributes.getValue(RDFaAttributes.TYPEOF.getName()) != null
                    && this.currentElementAttributes.getValue(RDFaAttributes.ABOUT.getName()) == null) {
                currentPropertyValue = typedResource;
            } else {
                String contentString = this.characters.toString().trim();
                if(this.currentLanguage != null
                        && ! this.currentLanguage.isEmpty()) {
                    currentPropertyValue = getValueFactory().createLiteral(contentString, this.currentLanguage);
                } else {
                    currentPropertyValue = getValueFactory().createLiteral(contentString);
                }
                this.characters = new StringBuilder();
            }

            if(this.currentElementAttributes.getValue(RDFaAttributes.INLIST.getName()) != null) {
                if(! this.localListMappings.containsKey(propertyIRI)) {
                    this.localListMappings.put(propertyIRI, new HashSet<>());
                }
                this.localListMappings.get(propertyIRI).add(currentPropertyValue);
            } else {
                this.getModel().add(this.newSubject, propertyIRI, currentPropertyValue);
            }
        }

        //  If the skip element flag is 'false', and new subject was set to a non-null value, then any incomplete triples within the current context should be completed:
        if(! skipElement
                && newSubject != null) {
            for(RDFaIncompleteStatement incompleteStatement : this.currentContext.getIncompleteStatement()) {
                if(incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.NONE) {
                    localListMappings.get(incompleteStatement.getPredicate()).add(newSubject);
                } else if(incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.FORWARD) {
                    this.getModel().add(currentContext.getParentSubjectResource(), incompleteStatement.getPredicate(), newSubject);
                } else if(incompleteStatement.getDirection() == RDFaIncompleteStatement.Direction.BACKWARD) {
                    this.getModel().add(newSubject, incompleteStatement.getPredicate(), currentContext.getParentSubjectResource());
                }
            }
        }

        //  Next, all elements that are children of the current element are processed using the rules described here, using a new evaluation context, initialized as follows:
        Map<IRI, Set<Value>> oldListMappings = currentContext.getListMappings();
        if(skipElement) {
            currentContext = new RDFa11EvaluationContext(this.currentContext);
            currentContext.setLanguage(currentLanguage);
            currentContext.setIriMappings(localIRIMappings);
        } else {
            Resource oldParentSubject = this.currentContext.getParentSubjectResource();
            currentContext = new RDFa11EvaluationContext(this.currentContext.getBaseIri());
            currentContext.setParentSubjectResource(newSubject);
            if(currentObjectResource != null) {
                currentContext.setParentObjectResource(currentObjectResource);
            } if (newSubject != null) {
                currentContext.setParentObjectResource(newSubject);
            } else {
                currentContext.setParentObjectResource(oldParentSubject);
            }
            currentContext.setIriMappings(localIRIMappings);
            currentContext.setIncompleteStatements(localIncompleteStatements);
            currentContext.setListMappings(localListMappings);
            currentContext.setLanguage(currentLanguage);
            currentContext.setDefaultVocabulary(localDefaultVocabulary);
        }

        // Finally, if there is one or more mapping in the local list mapping, list triples are generated as follows:
        for(Map.Entry<IRI, Set<Value>> listMapping : localListMappings.entrySet()) {
            IRI propertyIRI = listMapping.getKey();
            Set<Value> propertyList = listMapping.getValue();

            if(!oldListMappings.containsKey(propertyIRI)) {
                if(propertyList.isEmpty()) {
                    getModel().add(newSubject, propertyIRI, RDF.nil.getIRI());
                } else {
                    ArrayList<BNode> bnodes = new ArrayList<>();
                    for(int i = 0; i < propertyList.size(); i++) {
                        bnodes.add(getValueFactory().createBNode());
                    }
                    int bnodeIndex = 0;
                    for(Value listElement : propertyList) {
                        BNode elementNode = bnodes.get(bnodeIndex);
                        Resource nextElementNode = RDF.nil.getIRI();
                        if(bnodeIndex < bnodes.size() - 1) {
                            nextElementNode = bnodes.get(bnodeIndex + 1);
                        }
                        getModel().add(elementNode, RDF.first.getIRI(), listElement);
                        getModel().add(elementNode, RDF.rest.getIRI(), nextElementNode);

                        bnodeIndex++;
                    }
                    getModel().add(newSubject, propertyIRI, bnodes.getFirst());
                }
            }
        }

        isRootElement = false;
    }

    /**
     * Internal SAX handler that delegates to the parser's methods
     */
    private class XMLSaxHandler extends DefaultHandler {
        @Override
        public void characters(char[] ch, int start, int length) {
            RDFa11Parser.this.handleCharacters(ch, start, length);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            RDFa11Parser.this.addPrefix(prefix, uri);
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

    private Resource getResourceFromElementAttribute(RDFaAttributes attribute) {
        String attributeValue = this.currentElementAttributes.getValue(attribute.getName());
        if (resolveStringResource(attributeValue, this.currentContext).isPresent()) {
            return resolveStringResource(attributeValue, this.currentContext).get();
        } else {
            throw new ParsingErrorException("Could not parse @" + attribute.getName() + " value: " + attributeValue);
        }
    }
}
