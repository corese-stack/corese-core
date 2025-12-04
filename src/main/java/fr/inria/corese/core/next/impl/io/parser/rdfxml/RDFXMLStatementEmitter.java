package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

import java.util.Optional;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLUtils.*;

/**
 * Emits RDF statements from parsed RDF/XML constructs using a given RDF Model
 * and ValueFactory.
 */
public class RDFXMLStatementEmitter {

    private static final Logger logger = LoggerFactory.getLogger(RDFXMLStatementEmitter.class);

    private final Model model;
    private final ValueFactory factory;

    /**
     * Constructs a new emitter for the given RDF model and value factory.
     *
     * @param model   the RDF model where statements will be added
     * @param factory the RDF value factory used to create RDF terms
     */
    public RDFXMLStatementEmitter(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }

    /**
     * Emits a literal statement with optional datatype or language.
     *
     * @param subject      the subject of the statement
     * @param predicate    the predicate of the statement
     * @param text         the literal value
     * @param datatypeUri  the datatype URI (optional, may be null)
     * @param lang         the language tag (optional, may be null)
     */
    public void emitLiteral(Resource subject, IRI predicate, String text, String datatypeUri, String lang) {
        if (subject == null) {
            throw new ParsingErrorException(
                    "Cannot emit literal statement: subject is null. " +
                            "This may indicate malformed RDF/XML structure.");
        }

        if (predicate == null) {
            throw new ParsingErrorException(
                    "Cannot emit literal statement: predicate is null.");
        }

        Value literal;
        if (datatypeUri != null && !datatypeUri.isEmpty()) {
            Optional<XSD> known = RDFXMLUtils.resolveDatatype(datatypeUri);
            IRI dtype = known.map(XSD::getIRI).orElseGet(() -> {
                logger.error("[Warning] Unknown datatype: %s%n {} ", datatypeUri);
                return factory.createIRI(datatypeUri);
            });

            try {
                literal = factory.createLiteral(text, dtype);
            } catch (IllegalArgumentException e) {
                literal = factory.createLiteral(text);
            }
        } else if (lang != null && !lang.equals("__NO_LANG__") && !lang.isEmpty()) {
            literal = factory.createLiteral(text, lang);
        } else {
            literal = factory.createLiteral(text);
        }

        model.add(factory.createStatement(subject, predicate, literal));
    }


    /**
     * Emits a rdf:type statement for the given subject and type URI.
     *
     * @param subject        the subject resource
     * @param expandedQName  the fully expanded IRI for the type
     */
    public void emitType(Resource subject, String expandedQName) {
        if (subject == null) {
            throw new ParsingErrorException(
                    "Cannot emit type statement: subject is null.");
        }

        model.add(factory.createStatement(
                subject,
                RDF.type.getIRI(),
                factory.createIRI(expandedQName)
        ));
    }

    /**
     * Emits RDF statements for non-syntax XML attributes as predicate-object pairs.
     *
     * @param subject the subject resource
     * @param attrs   the XML attributes associated with the element
     */
    public void emitPropertyAttributes(Resource subject, Attributes attrs) {
        emitPropertyAttribute(subject, attrs);
    }

    /**
     * Emits RDF statements for non-syntax XML attributes as predicate-object pairs.
     *
     * @param subject the subject resource
     * @param attrs   the XML attributes associated with the element
     */
    public void emitPropertyAttribute(Resource subject, Attributes attrs) {
        if (subject == null) {
            throw new ParsingErrorException(
                    "Cannot emit property attributes: subject is null.");
        }

        for (int i = 0; i < attrs.getLength(); i++) {
            String attrURI = attrs.getURI(i);
            String attrLocal = attrs.getLocalName(i);
            String attrQName = attrs.getQName(i);
            String value = attrs.getValue(i);

            if (isSyntaxAttribute(attrURI, attrLocal, attrQName)) continue;

            if (attrURI == null || attrURI.isEmpty()) {
                continue;
            }

            // VALIDATION: rdf:li and rdf:_n CANNOT be used as property attributes
            if (RDF.type.getNamespace().equals(attrURI)) {
                if ("li".equals(attrLocal)) {
                    throw new ParsingErrorException(
                            "rdf:li cannot be used as property attribute. " +
                                    "It can only be used as property element inside containers.");
                }
                if (attrLocal.matches("^_\\d+$")) {
                    throw new ParsingErrorException(
                            "rdf:" + attrLocal + " cannot be used as property attribute. " +
                                    "Container membership properties can only be used as property elements.");
                }
            }

            IRI pred = factory.createIRI(expandQName(attrURI, attrLocal, attrQName));
            model.add(factory.createStatement(subject, pred, factory.createLiteral(value)));
        }
    }

    /**
     * Emits a triple where the object is an IRI resolved against the base URI.
     *
     * @param subject   the subject of the triple
     * @param predicate the predicate of the triple
     * @param resource  the relative or absolute IRI string
     * @param baseURI   the base URI used to resolve the resource
     */
    public void emitResourceTriple(Resource subject, IRI predicate, String resource, String baseURI) {
        model.add(factory.createStatement(
                subject,
                predicate,
                factory.createIRI(resolveAgainstBase(resource, baseURI))
        ));
    }

    /**
     * Emits a triple where the object is a blank node identified by node ID.
     *
     * @param subject   the subject of the triple
     * @param predicate the predicate of the triple
     * @param nodeID    the blank node identifier
     */
    public void emitBNodeTriple(Resource subject, IRI predicate, String nodeID) {
        model.add(factory.createStatement(
                subject,
                predicate,
                factory.createBNode("_:" + nodeID)
        ));
    }

    /**
     * Emits a triple with a resource as object.
     *
     * @param subject   the subject of the triple
     * @param predicate the predicate of the triple
     * @param object    the object resource of the triple
     */
    public void emitTriple(Resource subject, IRI predicate, Resource object) {
        model.add(factory.createStatement(subject, predicate, object));
    }
}