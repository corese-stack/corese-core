package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

public class RDFaParser extends AbstractRDFParser {

    private static final Logger logger = LoggerFactory.getLogger(RDFaParser.class);

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
        if (baseIri.stringValue().equals(ParserConstants.getDefaultBaseURI())) {
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
            ;

            baseIri = this.getValueFactory().createIRI(baseIriFromXml.stringValue());
        }

        for (Element element : document.children()) {
            processElement(element, new RDFaEvaluationContext(baseIri), baseIri);
        }
    }

    /**
     *
     * @param element     Current element
     * @param context     Active context
     * @param recursive   Processing generally continues recursively through the entire tree of elements available. However, if an author indicates that some branch of the tree should be treated as an XML literal, no further processing should take place on that branch, and setting this flag to false would have that effect.
     * @param skipElement Flag thet indicates whether the [current element] can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     * @see <a href="https://www.w3.org/TR/rdfa-syntax/#s_rdfaindetail">RDFa processing in details<a/>
     */
    private void processElement(Element element, RDFaEvaluationContext context, boolean recursive, boolean skipElement) {
        logger.debug("processElement({}, {}, ...)", element, context);

        Resource newSubject = null;
        Value currentObject = null;
        Map<String, IRI> currentMappings = context.uriMappings();
        Set<RDFaIncompleteStatement> incompleteStatementSet = new HashSet<>();
        String language = context.getLanguage();

        // Looking for namespace declarations
        // Namespace declaration are done using the XML namespace declaration mechanism, that can be seen as an attributes prefixed by "xmlns" and looks like this: "xmlns:prefix=namespace"
        element.attributes().forEach(attribute -> {
            logger.debug("Looking at attribute {}", attribute.getKey());
            if (attribute.getKey().startsWith(XMLNS_PREFIX)) {
                String prefixName = attribute.localName();
                String prefixNamespace = attribute.getValue();
                logger.debug("Prefix found {} = {}", prefixName, prefixNamespace);
                context.addUriMapping(prefixName, getValueFactory().createIRI(prefixNamespace));
            }
        });

        if (element.attribute(LANG_ATTR) != null) {
            String langString = element.attr(LANG_ATTR);
            language = langString;
        }

        if(element.attribute(REL_ATTR) == null && element.attribute(REV_ATTR) == null) {
            // [new subject] is set to the URI obtained from the first match from the following rules:
            if (element.attribute(ABOUT_ATTR) != null) { // by using the URI from @about, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, ABOUT_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                    logger.debug("@about found: {}", newSubjectResource.get().stringValue());
                }
            } else if (element.attribute(SRC_ATTR) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, SRC_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                    logger.debug("@src found: {}", newSubjectResource.get().stringValue());
                }
            } else if (element.attribute(RESOURCE_ATTR) != null) { // otherwise, by using the URI from @resource, if present, obtained according to the section on CURIE and URI Processing;
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, RESOURCE_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                    logger.debug("@resource found: {}", newSubjectResource.get().stringValue());
                }
            } else if (element.attribute(HREF_ATTR) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, HREF_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                    logger.debug("href found: {}", newSubjectResource.get());
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
                    logger.debug("@about found: {}", newSubjectResource.get());
                }
            } else if (element.attribute(SRC_ATTR) != null) { // otherwise, by using the URI from @src, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newSubjectResource = getResourceFromElementAttribute(element, SRC_ATTR, context);
                if (newSubjectResource.isPresent()) {
                    newSubject = newSubjectResource.get();
                    logger.debug("@src found: {}", newSubjectResource.get());
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
                    logger.debug("@resource found: {}", newObjectResource.get().stringValue());
                }
            } else if (element.attribute(HREF_ATTR) != null) { // otherwise, by using the URI from @href, if present, obtained according to the section on CURIE and URI Processing.
                Optional<Resource> newObjectResource =  getResourceFromElementAttribute(element, RESOURCE_ATTR, context);
                if (newObjectResource.isPresent()) {
                    currentObject = newObjectResource.get();
                    logger.debug("href found: {}", newObjectResource.get().stringValue());
                }
            }
        }

        if (newSubject != null)
            logger.debug("New subject resolved to {}", newSubject.stringValue());
        if(currentObject != null)
            logger.debug("New object resolved to {}", currentObject.stringValue());

        // If in any of the previous steps a [new subject] was set to a non-null value, it is now used to provide a subject for type values;
        if(newSubject != null) {
            if(element.attribute(TYPEOF_ATTR) != null) { // One or more 'types' for the [new subject] can be set by using @typeof. If present, the attribute must contain one or more URIs, obtained according to the section on URI and CURIE Processing, each of which is used to generate a triple as follows:
                Optional<Resource> typeIri = getResourceFromElementAttribute(element, TYPEOF_ATTR, context);
                if (typeIri.isPresent()) {
                    logger.debug("Typeof found: {}", typeIri.get());
                    logger.debug("Type of resource resolved to {} {}", typeIri.get().stringValue(), context);
                    Statement stat = this.getValueFactory().createStatement(newSubject, RDF.type.getIRI(), typeIri.get());
                    logger.debug("Statement added: {} {} {}", stat.getSubject().stringValue(), stat.getPredicate().stringValue(), stat.getObject().stringValue());
                    this.getModel().add(stat);
                } else {
                    throw new ParsingErrorException("Typeof statement uses unknown type " + element.attr(TYPEOF_ATTR));
                }
            }
        }
        // If however [current object resource] was set to null, but there are predicates present, then they must be stored as [incomplete triple]s, pending the discovery of a subject that can be used as the object. Also, [current object resource] should be set to a newly created [bnode];
        if (currentObject == null && (element.attribute(REL_ATTR) != null || element.attribute(REV_ATTR) != null)) {
            currentObject = getValueFactory().createBNode();
            if(element.attribute(REL_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    statement.setSubject(newSubject);
                    incompleteStatementSet.add(statement);
                }
            }
            if(element.attribute(REV_ATTR) != null) {
                Optional<Resource> propertyOpt = getResourceFromElementAttribute(element, REL_ATTR, context);
                if(propertyOpt.isPresent() && propertyOpt.get().isIRI()) {
                    IRI property = (IRI) propertyOpt.get();
                    RDFaIncompleteStatement statement = new RDFaIncompleteStatement(property);
                    statement.setObject(newSubject);
                    incompleteStatementSet.add(statement);
                }
            }
        }

        if (element.attribute(TYPEOF_ATTR) != null) {
            String typeIriString = element.attr(TYPEOF_ATTR);
            logger.debug("Typeof found: {}", typeIriString);
            if(context.parentSubjectResource().equals(context.baseIri())) { // Not current subjet was setup using about or src, so we are implicitly creating a blank node
                context.parentSubjectResource(this.getValueFactory().createBNode());
            }
            Optional<Resource> typeIri = resolveStringResource(typeIriString, context);
            if (typeIri.isPresent()) {
                logger.debug("Type of resource resolved to {} {}", typeIri.get().stringValue(), context);
                Statement stat = this.getValueFactory().createStatement(context.parentSubjectResource(), RDF.type.getIRI(), typeIri.get());
                logger.debug("Statement added: {} {} {}", stat.getSubject().stringValue(), stat.getPredicate().stringValue(), stat.getObject().stringValue());
                this.getModel().add(stat);
            } else {
                throw new ParsingErrorException("Typeof statement uses unknown type " + typeIriString);
            }
        }

        for (Element child : element.children()) {
            processElement(child, context, recursive, skipElement);
        }
    }

    /**
     * Surcharge function that initialize the flags and subject and objet to their initial values for processing
     *
     * @param element
     * @param context
     * @param newSubject
     */
    private void processElement(Element element, RDFaEvaluationContext context, Resource newSubject) {
        processElement(element, context, true, false);
    }

    @Override
    public void parse(Reader reader, String baseURI) {
    }

    private Statement incompleteStatementToStatement(RDFaIncompleteStatement incompleteStatement) {
        Objects.requireNonNull(incompleteStatement.getSubject(), "Null subject, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getPredicate(), "Null predicate, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getObject(), "Null object, IncompleteStatement can only be converted if all its component are non-null.");

        return this.getValueFactory().createStatement(incompleteStatement.getSubject(), incompleteStatement.getPredicate(), incompleteStatement.getObject());
    }

    /**
     * Resolves the string representation of a resource found in attributes of an element, be it an IRI, <ahref="https://www.w3.org/TR/rdfa-syntax/#s_curieprocessing">CURIE</a> or relative URI
     *
     * @param stringResource the resource as stored in the attribute of the HTML element
     * @param context        the context of the element evalation
     * @return the full IRI if it is a relative IRI, full IRI or CURIE, nothing otherwise
     */
    private Optional<Resource> resolveStringResource(String stringResource, RDFaEvaluationContext context) {
        logger.debug("Resolution of resource {}, {}", stringResource, context);
        String resultString = stringResource;
        if (resultString.startsWith("[") && resultString.endsWith("]")) {
            resultString = resultString.replaceFirst("\\[", "");
            resultString = resultString.replaceFirst("]", "");
        }


        if (stringUriIsCURIE(resultString)) { // CURIE
            int colonIndex = resultString.indexOf(":");
            String prefixString = resultString.substring(0, colonIndex);
            String localNameString = resultString.substring(colonIndex + 1);
            logger.debug("CURIE with prefix: {} and local name: {}", prefixString, localNameString);
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
            logger.debug("Standard IRI: {}", resultString);
            return Optional.of(this.getValueFactory().createIRI(resultString));

        } else if (resultString.startsWith("_:")) {  // Blank Node
            int colonIndex = resultString.indexOf(":");
            String localNameString = resultString.substring(colonIndex + 1);
            logger.debug("Blank Node: _:{}", localNameString);
            return Optional.of(this.getValueFactory().createBNode(localNameString));
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
