package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import org.xml.sax.Attributes;

import java.util.Optional;

import static fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLUtils.*;

public class RDFXMLStatementEmitter {

    private final Model model;
    private final ValueFactory factory;

    public RDFXMLStatementEmitter(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }

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

    public void emitType(Resource subject, String expandedQName) {
        model.add(factory.createStatement(subject, RDF.type.getIRI(), factory.createIRI(expandedQName)));
    }

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

    public void emitResourceTriple(Resource subject, IRI predicate, String resource, String baseURI) {
        model.add(factory.createStatement(
                subject,
                predicate,
                factory.createIRI(resolveAgainstBase(resource, baseURI))
        ));
    }

    public void emitBNodeTriple(Resource subject, IRI predicate, String nodeID) {
        model.add(factory.createStatement(
                subject,
                predicate,
                factory.createBNode("_:" + nodeID)
        ));
    }



    public void emitTriple(Resource subject, IRI predicate, Resource object) {
        model.add(factory.createStatement(subject, predicate, object));
    }
}