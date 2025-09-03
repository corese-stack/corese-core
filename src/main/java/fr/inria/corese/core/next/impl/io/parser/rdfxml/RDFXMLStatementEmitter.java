package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import org.xml.sax.Attributes;

import java.util.Optional;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLUtils.*;

/**
 * Emits RDF statements from parsed RDF/XML constructs using a given RDF Model
 * and ValueFactory.
 */
public class RDFXMLStatementEmitter {

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
        Value literal;
        if (datatypeUri != null && !datatypeUri.isEmpty()) {
            Optional<XSD> known = RDFXMLUtils.resolveDatatype(datatypeUri);
            IRI dtype = known.map(XSD::getIRI).orElseGet(() -> {
                System.err.printf("[Warning] Unknown datatype: %s%n", datatypeUri);
                return factory.createIRI(datatypeUri);
            });
            literal = factory.createLiteral(text, dtype);
        } else if (lang != null && !lang.equals("__NO_LANG__")) {
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
        model.add(factory.createStatement(subject, RDF.type.getIRI(), factory.createIRI(expandedQName)));
    }

    /**
     * Emits RDF statements for non-syntax XML attributes as predicate-object pairs.
     *
     * @param subject the subject resource
     * @param attrs   the XML attributes associated with the element
     */
    public void emitPropertyAttributes(Resource subject, Attributes attrs) {
        for (int i = 0; i < attrs.getLength(); i++) {
            String attrURI = attrs.getURI(i);
            String attrLocal = attrs.getLocalName(i);
            String attrQName = attrs.getQName(i);
            String value = attrs.getValue(i);

            if (isSyntaxAttribute(attrURI, attrLocal, attrQName)) continue;

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