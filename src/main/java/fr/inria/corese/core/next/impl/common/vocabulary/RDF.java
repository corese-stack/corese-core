package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.impl.common.BasicIRI;

/**
 * Defines the RDF vocabulary.
 *
 * Because of the declaration of several Literal datatype, it is made to implement the CoreDatatype interface.
 */
public enum RDF implements Vocabulary {
    HTML("HTML"),
    langString("langString"),
    PlainLiteral("PlainLiteral"),
    XMLLiteral("XMLLiteral"),
    CompoundLiteral("CompoundLiteral"),
    JSON("JSON"),
    List("List"),
    Seq("Seq"),
    Bag("Bag"),
    Alt("Alt"),
    Statement("Statement"),
    Property("Property"),
    first("first"),
    rest("rest"),
    nil("nil"),
    subject("subject"),
    predicate("predicate"),
    object("object"),
    type("type"),
    value("value"),
    direction("direction"),
    language("language");


    private final IRI iri;

    RDF(String localName) {
        this.iri = new BasicIRI(getNamespace(), localName);
    }
    @Override
    public IRI getIRI() {
        return this.iri;
    }

    @Override
    public String getNamespace() {
        return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    }

    @Override
    public String getPreferredPrefix() {
        return "rdf";
    }
}
