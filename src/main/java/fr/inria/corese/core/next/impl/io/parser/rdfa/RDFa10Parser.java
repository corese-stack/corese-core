package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.common.IOConstants;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFa10EvaluationContext;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaAttributes;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaEvaluationContext;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import org.apache.commons.io.input.ReaderInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RDFa parser. This parser will load the RDF data stored as RDFa in an HTML page. Its inner implementation is based on the jsoup library. It loads the html page as DOM and process it following the <a href="https://www.w3.org/TR/rdfa-syntax/#sec_5.5.">recommended algorithm in the RDFa recommendation.</a>
 */
public class RDFa10Parser extends AbstractRDFaParser {

    private static final String BASE_TAG = "base";

    private static final String XMLNS_PREFIX = "xmlns";

    public RDFa10Parser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFa10Parser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFa_1_0;
    }

    @Override
    public void parse(InputStream in) {
        if(getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            String baseIRI = baseIRIOptions.getBaseIRI();
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseIRI);
        } else {
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
        }
    }

    @Override
    public void parse(InputStream in, String baseURIString) {
        try {
            Document document = Jsoup.parse(in, null, baseURIString);

            IRI baseIri = getValueFactory().createIRI(baseURIString);
            processDocument(document, baseIri);
        } catch (Exception e) {
            throw new ParsingErrorException("Error during parsing of HTML document", e);
        }
    }

    /**
     * Intermediary function to configure the processing of a document using some basic HTML traversal to determine if a baseIri has been defined in the document.
     * If the baseIri in argument is the Corese default base IRI, the value stored in the document is used instead.
     *
     * @param document Jsoup HTML document to be processed
     * @param baseIri  An IRI object
     */
    private void processDocument(Document document, IRI baseIri) {
        // If the base Iri in argument is not the default baseIri, then we take it, else we use the one in the document
        if (baseIri.stringValue().equals(IOConstants.getDefaultBaseURI())) {
            // Looking for the <base> node in the document
            IRI baseIriFromXml = baseIri;
            Iterator<Element> baseElementIterator = document.stream().filter(element -> element.nameIs(BASE_TAG)).iterator();
            while (baseElementIterator.hasNext()) {
                Element baseElement = baseElementIterator.next();
                Attribute baseElementHrefAttribute = baseElement.attribute(RDFaAttributes.HREF.getName());
                if (baseElementHrefAttribute != null) {
                    String baseIriString = baseElementHrefAttribute.getValue();
                    baseIriFromXml = getValueFactory().createIRI(baseIriString);
                }
            }

            baseIri = this.getValueFactory().createIRI(baseIriFromXml.stringValue());
        }

        for (Element element : document.children()) {
            processElement(element, new RDFa10EvaluationContext(baseIri));
        }
    }

    /**
     * @param element     Current element
     * @param context     Active context
     * @param recursive   Processing generally continues recursively through the entire tree of elements available. However, if an author indicates that some branch of the tree should be treated as an XML literal, no further processing should take place on that branch, and setting this flag to false would have that effect.
     * @param skipElement Flag thet indicates whether the [current element] can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     * @see <a href="https://www.w3.org/TR/rdfa-syntax/#s_rdfaindetail">RDFa processing in details<a/>
     */
    private void processElement(Element element, fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFa10EvaluationContext context, boolean recursive, boolean skipElement) {

        // 1. First, the local values are initialized
        Resource newSubject = null;
        Resource currentObject = null;
        Literal currentObjectLiteral = null;
        Map<String, IRI> currentMappings = context.getIriMappings();
        Set<RDFaIncompleteStatement> incompleteStatementSet = new HashSet<>();
        String language = context.getLanguage();

        // 2. Next the [current element] is parsed for [URI mapping]s and these are added to the [local list of URI mappings]. Note that a [URI mapping] will simply overwrite any current mapping in the list that has the same name;
        // Looking for namespace declarations
        // Namespace declaration are done using the XML namespace declaration mechanism, that can be seen as an attributes prefixed by "xmlns" and looks like this: "xmlns:prefix=namespace"
        for (Attribute attribute : element.attributes()) {
            if (attribute.getKey().startsWith(XMLNS_PREFIX)) {
                String prefixName = attribute.localName();
                IRI prefixNamespace = getValueFactory().createIRI(attribute.getValue(), "");
                context.addIriMapping(prefixName, prefixNamespace);
            }
        }

        // 3. The [current element] is also parsed for any language information, and if present, [current language] is set accordingly;
        if (element.attribute(RDFaAttributes.LANG.getName()) != null) {
            language = element.attr(RDFaAttributes.LANG.getName());
        }

        // 4. If the [current element] contains no @rel or @rev attribute, then the next step is to establish a value for [new subject]. Any of the attributes that can carry a resource can set [new subject];
        if(element.attribute(RDFaAttributes.REL.getName()) == null && element.attribute(RDFaAttributes.REV.getName()) == null) {
            // [new subject] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(RDFaAttributes.ABOUT.getName()) != null) { // by using the URI from @about, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.ABOUT.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(RDFaAttributes.SRC.getName()) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.SRC.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(RDFaAttributes.RESOURCE.getName()) != null) { // otherwise, by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.RESOURCE.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(RDFaAttributes.HREF.getName()) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.HREF.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.nameIs("body") || element.nameIs("head")) { // if the element is the head or body element then act as if there is an empty @about present, and process it according to the rule for @about, above;
                newSubject = context.getBaseIri();
            } else if (element.attribute(RDFaAttributes.TYPEOF.getName()) != null) { // if @typeof is present, obtained according to the section on CURIE and URI Processing, then [new subject] is set to be a newly created [bnode].
                    newSubject = this.getValueFactory().createBNode();
            } else if (context.getParentObjectResource() != null) { // otherwise, if [parent object] is present, [new subject] is set to the value of [parent object]. Additionally, if @property is not present then the [skip element] flag is set to 'true';
                    newSubject = context.getParentObjectResource();
                    if(element.attribute(RDFaAttributes.PROPERTY.getName()) == null) {
                        skipElement = true;
                    }
            }
        } else {
            // [new subject] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(RDFaAttributes.ABOUT.getName()) != null) { // by using the URI from @about, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.ABOUT.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(RDFaAttributes.SRC.getName()) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RDFaAttributes.SRC.getName(), context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.nameIs("body") || element.nameIs("head")) { // if the element is the head or body element then act as if there is an empty @about present, and process it according to the rule for @about, above;
                newSubject = context.getBaseIri();
            } else if (element.attribute(RDFaAttributes.TYPEOF.getName()) != null) { // if @typeof is present, obtained according to the section on CURIE and URI Processing, then [new subject] is set to be a newly created [bnode].
                newSubject = this.getValueFactory().createBNode();
            } else if(context.getParentObjectResource() != null) { // otherwise, if [parent object] is present, [new subject] is set to that.
                newSubject = context.getParentObjectResource();
            }

            // Then the [current object resource] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(RDFaAttributes.RESOURCE.getName()) != null) { // by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newObjectResource =  getResourceFromElementAttribute(element, RDFaAttributes.RESOURCE.getName(), context);
                if (newObjectResource.isPresent()) {
                    currentObject = newObjectResource.get();
                }
            } else if (element.attribute(RDFaAttributes.HREF.getName()) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newObjectResource =  getResourceFromElementAttribute(element, RDFaAttributes.RESOURCE.getName(), context);
                if (newObjectResource.isPresent()) {
                    currentObject = newObjectResource.get();
                }
            }
        }

        // 6. If in any of the previous steps a [new subject] was set to a non-null value, it is now used to provide a subject for type values;
        if(newSubject != null) {
            if(element.attribute(RDFaAttributes.TYPEOF.getName()) != null) { // One or more 'types' for the [new subject] can be set by using @typeof. If present, the attribute must contain one or more URIs, obtained according to the section on URI and CURIE Processing, each of which is used to generate a triple as follows:
                Optional<Resource> typeIri = getResourceFromElementAttribute(element, RDFaAttributes.TYPEOF.getName(), context);
                if (typeIri.isPresent()) {
                    Statement stat = this.getValueFactory().createStatement(newSubject, RDF.type.getIRI(), typeIri.get());
                    this.getModel().add(stat);
                } else {
                    throw new ParsingErrorException("Typeof statement uses unknown type " + element.attr(RDFaAttributes.TYPEOF.getName()));
                }
            }
        }

        // 7. If in any of the previous steps a [current object resource] was set to a non-null value, it is now used to generate triples:
        if (currentObject != null && (element.attribute(RDFaAttributes.REL.getName()) != null || element.attribute(RDFaAttributes.REV.getName()) != null)) {
            if(element.attribute(RDFaAttributes.REL.getName()) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, RDFaAttributes.REL.getName(), context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    this.getModel().add(newSubject, property, currentObject);
                }
            }
            if(element.attribute(RDFaAttributes.REV.getName()) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, RDFaAttributes.REL.getName(), context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI() && currentObject.isResource()) {
                    IRI property = (IRI) propertyOpt.get();
                    this.getModel().add(currentObject, property, newSubject);
                }
            }
        }

        // 8. If however [current object resource] was set to null, but there are predicates present, then they must be stored as [incomplete triple]s, pending the discovery of a subject that can be used as the object. Also, [current object resource] should be set to a newly created [bnode];
        if (currentObject == null && (element.attribute(RDFaAttributes.REL.getName()) != null || element.attribute(RDFaAttributes.REV.getName()) != null)) {
            currentObject = getValueFactory().createBNode();
            if(element.attribute(RDFaAttributes.REL.getName()) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, RDFaAttributes.REL.getName(), context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    incompleteStatementSet.add(statement);
                }
            }
            if(element.attribute(RDFaAttributes.REV.getName()) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, RDFaAttributes.REL.getName(), context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI() && currentObject.isResource()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    statement.setBackward();
                    incompleteStatementSet.add(statement);
                }
            }
        }

        // 9. The next step of the iteration is to establish any [current object literal];
        if(element.attribute(RDFaAttributes.PROPERTY.getName()) != null) { // Predicates for the [current object literal] can be set by using @property. If present, one or more URIs are obtained according to the section on CURIE and URI Processing, and then the actual literal value is obtained as follows:
            Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, RDFaAttributes.PROPERTY.getName(), context);
            if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                IRI property = (IRI)propertyOpt.get();

                IRI datatype = null;
                if(element.attribute(RDFaAttributes.DATATYPE.getName()) != null && ! element.attr(RDFaAttributes.DATATYPE.getName()).isEmpty()) {
                    Optional<Resource> datatypeOpt = getResourceFromElementAttribute(element, RDFaAttributes.DATATYPE.getName(), context);
                    if(datatypeOpt.isPresent() && datatypeOpt.get().isIRI() && ! datatypeOpt.get().equals(RDF.XMLLiteral.getIRI())) {
                        datatype = (IRI) datatypeOpt.get();
                    }
                }
                String value = element.text();
                if(element.attribute(RDFaAttributes.CONTENT.getName()) != null) {
                    value = element.attr(RDFaAttributes.CONTENT.getName());
                }
                if(datatype != null) {
                    currentObjectLiteral = this.getValueFactory().createLiteral(value, datatype);
                    recursive = false;
                } else if(language != null) {
                    currentObjectLiteral = this.getValueFactory().createLiteral(value, language);
                } else {
                    currentObjectLiteral = this.getValueFactory().createLiteral(value);
                }
                this.getModel().add(newSubject, property, currentObjectLiteral);
            }
        }

        // 10. If the [skip element] flag is 'false', and [new subject] was set to a non-null value, then any [incomplete triple]s within the current context should be completed:
        Iterator<RDFaIncompleteStatement> itStat = context.getIncompleteStatementIterator();
        while(itStat.hasNext()) {
            RDFaIncompleteStatement statement = itStat.next();
            if(statement.isForward()) {
                this.getModel().add(context.getParentSubjectResource(), statement.getPredicate(), newSubject);
            } else if (statement.isBackward()){
                this.getModel().add(newSubject, statement.getPredicate(), context.getParentSubjectResource());
            }
        }

        // 11. If the [recurse] flag is 'true', all elements that are children of the [current element] are processed using the rules described here, using a new [evaluation context],
        if(recursive) {
            if(skipElement) {
                RDFa10EvaluationContext newContext = new fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFa10EvaluationContext(context);
                newContext.setLanguage(language);
                newContext.setIriMappings(currentMappings);
                context = newContext;
            } else {
                context = new RDFa10EvaluationContext(context.getBaseIri());
                if(newSubject != null) {
                    context.setParentObjectResource(newSubject);
                }
                if(currentObject != null) {
                    context.setParentObjectResource(currentObject);
                }
                context.setIriMappings(currentMappings);
                context.setIncompleteStatements(incompleteStatementSet);
                context.setLanguage(language);
            }

            for (Element child : element.children()) {
                processElement(child, context, recursive, skipElement);
            }
        }
    }

    /**
     * Surcharge function that initialize the flags and subject and objet to their initial values for processing
     *
     * @param element HTML element
     * @param context current evaluation context
     */
    private void processElement(Element element, fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFa10EvaluationContext context) {
        processElement(element, context, true, false);
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        InputStream inputStream = new ReaderInputStream(reader, StandardCharsets.UTF_8);
        parse(inputStream , baseURI);
    }

    private Optional<Resource> getResourceFromElementAttribute(Element element, String attributeName, RDFaEvaluationContext context) {
        if (element.attribute(attributeName) != null) { // otherwise, by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
            String newSubjectString = element.attr(attributeName);
            return resolveStringResource(newSubjectString, context);
        }
        return Optional.empty();
    }
}
