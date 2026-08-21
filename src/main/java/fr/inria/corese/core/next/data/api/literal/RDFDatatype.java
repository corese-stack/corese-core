package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;

/**
 * Enumeration of the RDF datatypes usable as core datatypes of literals.
 */
public enum RDFDatatype implements CoreDatatype {

    /** Language-tagged string literal (rdf:langString). */
    LANGSTRING(fr.inria.corese.core.next.data.api.vocabulary.RDF.langString.getIRI()),

    /** HTML literal content (rdf:HTML). */
    HTML(fr.inria.corese.core.next.data.api.vocabulary.RDF.HTML.getIRI()),

    /** Native JSON literal content (rdf:JSON). */
    JSON(fr.inria.corese.core.next.data.api.vocabulary.RDF.JSON.getIRI()),

    /** XML literal content (rdf:XMLLiteral). */
    XML_LITERAL(fr.inria.corese.core.next.data.api.vocabulary.RDF.XMLLiteral.getIRI()),
    ;

    private final IRI iri;

    /**
     * Constructor for the RDF core datatype enum.
     *
     * @param iri the IRI of the datatype
     */
    RDFDatatype(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
