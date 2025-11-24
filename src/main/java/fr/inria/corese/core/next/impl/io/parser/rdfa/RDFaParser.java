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
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
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
public class RDFaParser extends AbstractRDFParser {

    private static final String REL_ATTR = "rel";
    private static final String REV_ATTR = "rev";
    private static final String CONTENT_ATTR = "content";
    private static final String HREF_ATTR = "href";
    private static final String SRC_ATTR = "src";
    private static final String ABOUT_ATTR = "about";
    private static final String PROPERTY_ATTR = "property";
    private static final String RESOURCE_ATTR = "resource";
    private static final String DATATYPE_ATTR = "datatype";
    private static final String TYPEOF_ATTR = "typeof";
    private static final String LANG_ATTR = "xml:lang";

    private static final String XMLNS_PREFIX = "xmlns";

    public RDFaParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    public RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFa;
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
            Iterator<Element> baseElementIterator = document.stream().filter(element -> element.nameIs("base")).iterator();
            while (baseElementIterator.hasNext()) {
                Element baseElement = baseElementIterator.next();
                Attribute baseElementHrefAttribute = baseElement.attribute("href");
                if (baseElementHrefAttribute != null) {
                    String baseIriString = baseElementHrefAttribute.getValue();
                    baseIriFromXml = getValueFactory().createIRI(baseIriString);
                }
            }

            baseIri = this.getValueFactory().createIRI(baseIriFromXml.stringValue());
        }

        for (Element element : document.children()) {
            processElement(element, new RDFaEvaluationContext(baseIri));
        }
    }

    /**
     * @param element     Current element
     * @param context     Active context
     * @param recursive   Processing generally continues recursively through the entire tree of elements available. However, if an author indicates that some branch of the tree should be treated as an XML literal, no further processing should take place on that branch, and setting this flag to false would have that effect.
     * @param skipElement Flag thet indicates whether the [current element] can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     * @see <a href="https://www.w3.org/TR/rdfa-syntax/#s_rdfaindetail">RDFa processing in details<a/>
     */
    private void processElement(Element element, RDFaEvaluationContext context, boolean recursive, boolean skipElement) {

        // 1. First, the local values are initialized
        Resource newSubject = null;
        Resource currentObject = null;
        Literal currentObjectLiteral = null;
        Map<String, IRI> currentMappings = context.uriMappings();
        Set<RDFaIncompleteStatement> incompleteStatementSet = new HashSet<>();
        String language = context.getLanguage();

        // 2. Next the [current element] is parsed for [URI mapping]s and these are added to the [local list of URI mappings]. Note that a [URI mapping] will simply overwrite any current mapping in the list that has the same name;
        // Looking for namespace declarations
        // Namespace declaration are done using the XML namespace declaration mechanism, that can be seen as an attributes prefixed by "xmlns" and looks like this: "xmlns:prefix=namespace"
        Iterator<Attribute> itAttribute = element.attributes().iterator();
        while(itAttribute.hasNext()) {
            Attribute attribute = itAttribute.next();
            if (attribute.getKey().startsWith(XMLNS_PREFIX)) {
                String prefixName = attribute.localName();
                IRI prefixNamespace = getValueFactory().createIRI(attribute.getValue(), "");
                context.addUriMapping(prefixName, prefixNamespace);
            }
        }

        // 3. The [current element] is also parsed for any language information, and if present, [current language] is set accordingly;
        if (element.attribute(LANG_ATTR) != null) {
            String langString = element.attr(LANG_ATTR);
            language = langString;
        }

        // 4. If the [current element] contains no @rel or @rev attribute, then the next step is to establish a value for [new subject]. Any of the attributes that can carry a resource can set [new subject];
        if(element.attribute(REL_ATTR) == null && element.attribute(REV_ATTR) == null) {
            // [new subject] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(ABOUT_ATTR) != null) { // by using the URI from @about, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, ABOUT_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(SRC_ATTR) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, SRC_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(RESOURCE_ATTR) != null) { // otherwise, by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RESOURCE_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(HREF_ATTR) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, HREF_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.nameIs("body") || element.nameIs("head")) { // if the element is the head or body element then act as if there is an empty @about present, and process it according to the rule for @about, above;
                newSubject = context.baseIri();
            } else if (element.attribute(TYPEOF_ATTR) != null) { // if @typeof is present, obtained according to the section on CURIE and URI Processing, then [new subject] is set to be a newly created [bnode].
                    newSubject = this.getValueFactory().createBNode();
            } else if (context.parentObjectResource() != null) { // otherwise, if [parent object] is present, [new subject] is set to the value of [parent object]. Additionally, if @property is not present then the [skip element] flag is set to 'true';
                    newSubject = context.parentObjectResource();
                    if(element.attribute(PROPERTY_ATTR) == null) {
                        skipElement = true;
                    }
            }
        } else {
            // [new subject] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(ABOUT_ATTR) != null) { // by using the URI from @about, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, ABOUT_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.attribute(SRC_ATTR) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, SRC_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                }
            } else if (element.nameIs("body") || element.nameIs("head")) { // if the element is the head or body element then act as if there is an empty @about present, and process it according to the rule for @about, above;
                newSubject = context.baseIri();
            } else if (element.attribute(TYPEOF_ATTR) != null) { // if @typeof is present, obtained according to the section on CURIE and URI Processing, then [new subject] is set to be a newly created [bnode].
                newSubject = this.getValueFactory().createBNode();
            } else if(context.parentObjectResource() != null) { // otherwise, if [parent object] is present, [new subject] is set to that.
                newSubject = context.parentObjectResource();
            }

            // Then the [current object resource] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(RESOURCE_ATTR) != null) { // by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newObjectResource =  getResourceFromElementAttribute(element, RESOURCE_ATTR, context);
                if (newObjectResource.isPresent()) {
                    currentObject = newObjectResource.get();
                }
            } else if (element.attribute(HREF_ATTR) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newObjectResource =  getResourceFromElementAttribute(element, RESOURCE_ATTR, context);
                if (newObjectResource.isPresent()) {
                    currentObject = newObjectResource.get();
                }
            }
        }

        // 6. If in any of the previous steps a [new subject] was set to a non-null value, it is now used to provide a subject for type values;
        if(newSubject != null) {
            if(element.attribute(TYPEOF_ATTR) != null) { // One or more 'types' for the [new subject] can be set by using @typeof. If present, the attribute must contain one or more URIs, obtained according to the section on URI and CURIE Processing, each of which is used to generate a triple as follows:
                Optional<Resource> typeIri = getResourceFromElementAttribute(element, TYPEOF_ATTR, context);
                if (typeIri.isPresent()) {
                    Statement stat = this.getValueFactory().createStatement(newSubject, RDF.type.getIRI(), typeIri.get());
                    this.getModel().add(stat);
                } else {
                    throw new ParsingErrorException("Typeof statement uses unknown type " + element.attr(TYPEOF_ATTR));
                }
            }
        }

        // 7. If in any of the previous steps a [current object resource] was set to a non-null value, it is now used to generate triples:
        if (currentObject != null && (element.attribute(REL_ATTR) != null || element.attribute(REV_ATTR) != null)) {
            if(element.attribute(REL_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    this.getModel().add(newSubject, property, currentObject);
                }
            }
            if(element.attribute(REV_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI() && currentObject.isResource()) {
                    IRI property = (IRI) propertyOpt.get();
                    this.getModel().add(currentObject, property, newSubject);
                }
            }
        }

        // 8. If however [current object resource] was set to null, but there are predicates present, then they must be stored as [incomplete triple]s, pending the discovery of a subject that can be used as the object. Also, [current object resource] should be set to a newly created [bnode];
        if (currentObject == null && (element.attribute(REL_ATTR) != null || element.attribute(REV_ATTR) != null)) {
            currentObject = getValueFactory().createBNode();
            if(element.attribute(REL_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    incompleteStatementSet.add(statement);
                }
            }
            if(element.attribute(REV_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI() && currentObject.isResource()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    statement.setBackward();
                    incompleteStatementSet.add(statement);
                }
            }
        }

        // 9. The next step of the iteration is to establish any [current object literal];
        if(element.attribute(PROPERTY_ATTR) != null) { // Predicates for the [current object literal] can be set by using @property. If present, one or more URIs are obtained according to the section on CURIE and URI Processing, and then the actual literal value is obtained as follows:
            Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, PROPERTY_ATTR, context);
            if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                IRI property = (IRI)propertyOpt.get();

                IRI datatype = null;
                if(element.attribute(DATATYPE_ATTR) != null && ! element.attr(DATATYPE_ATTR).isEmpty()) {
                    Optional<Resource> datatypeOpt = getResourceFromElementAttribute(element, DATATYPE_ATTR, context);
                    if(datatypeOpt.isPresent() && datatypeOpt.get().isIRI() && ! datatypeOpt.get().equals(RDF.XMLLiteral.getIRI())) {
                        datatype = (IRI) datatypeOpt.get();
                    }
                }
                String value = element.text();
                if(element.attribute(CONTENT_ATTR) != null) {
                    value = element.attr(CONTENT_ATTR);
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
                this.getModel().add(context.parentSubjectResource(), statement.getPredicate(), newSubject);
            } else if (statement.isBackward()){
                this.getModel().add(newSubject, statement.getPredicate(), context.parentSubjectResource());
            }
        }

        // 11. If the [recurse] flag is 'true', all elements that are children of the [current element] are processed using the rules described here, using a new [evaluation context],
        if(recursive) {
            if(skipElement) {
                RDFaEvaluationContext newContext = new RDFaEvaluationContext(context);
                newContext.setLanguage(language);
                newContext.uriMappings(currentMappings);
                context = newContext;
            } else {
                context = new RDFaEvaluationContext(context.baseIri());
                if(newSubject != null) {
                    context.parentObjectResource(newSubject);
                }
                if(currentObject != null) {
                    context.parentObjectResource(currentObject);
                }
                context.uriMappings(currentMappings);
                context.incompleteStatements(incompleteStatementSet);
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
    private void processElement(Element element, RDFaEvaluationContext context) {
        processElement(element, context, true, false);
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        InputStream inputStream = new ReaderInputStream(reader, StandardCharsets.UTF_8);
        parse(inputStream , baseURI);
    }

    /**
     * Resolves the string representation of a resource found in attributes of an element, be it an IRI, <ahref="https://www.w3.org/TR/rdfa-syntax/#s_curieprocessing">CURIE</a> or relative URI
     *
     * @param stringResource the resource as stored in the attribute of the HTML element
     * @param context        the context of the element evalation
     * @return the full IRI if it is a relative IRI, full IRI or CURIE, nothing otherwise
     */
    private Optional<Resource> resolveStringResource(String stringResource, RDFaEvaluationContext context) {
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
            if (context.hasUriMapping(prefixString)) {
                IRI namespaceIRI = context.uriMapping(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (prefixString.isEmpty()) { // CURIE is relative to the base URI
                return Optional.of(this.getValueFactory().createIRI(context.baseIri().stringValue(), localNameString));
            } else {
                throw new ParsingErrorException("CURIE " + stringResource + " uses unknown prefix");
            }
        } else if (IRIUtils.isStandardIRI(resultString)) {  // Full IRI
            return Optional.of(this.getValueFactory().createIRI(resultString));

        } else if (resultString.startsWith("_:")) {  // Blank Node
            int colonIndex = resultString.indexOf(":");
            String localNameString = resultString.substring(colonIndex + 1);
            return Optional.of(this.getValueFactory().createBNode(localNameString));
        } else if (IRIUtils.isStandardIRI(context.baseIri().stringValue() + resultString)) {
            String concatenatedRelativeUri = context.baseIri().stringValue() + resultString;
            return Optional.of(getValueFactory().createIRI(concatenatedRelativeUri));
        }
        return Optional.empty();
    }

    /**
     * Equivalent to test if it has a colon, and it is not a blank node
     *
     * @param stringIri
     * @return
     */
    private boolean stringUriIsCURIE(String stringIri) {
        int colonIndex = stringIri.indexOf(":");
        return colonIndex > -1 && !stringIri.contains("://") && !stringIri.startsWith("_:") && !stringIri.startsWith("[_:");
    }

    private Optional<Resource> getResourceFromElementAttribute(Element element, String attributeName, RDFaEvaluationContext context) {
        if (element.attribute(attributeName) != null) { // otherwise, by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
            String newSubjectString = element.attr(attributeName);
            return resolveStringResource(newSubjectString, context);
        }
        return Optional.empty();
    }
}
