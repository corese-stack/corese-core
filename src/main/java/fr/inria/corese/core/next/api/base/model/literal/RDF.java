package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;

/**
 * Enumeration of the RDF datatypes usable as core datatypes of literals.
 */
public enum RDF implements CoreDatatype {
    LANGSTRING(fr.inria.corese.core.next.api.base.vocabulary.RDF.langString.getIRI()),
    HTML(fr.inria.corese.core.next.api.base.vocabulary.RDF.HTML.getIRI()),
    JSON(fr.inria.corese.core.next.api.base.vocabulary.RDF.JSON.getIRI()),
    XML_LITERAL(fr.inria.corese.core.next.api.base.vocabulary.RDF.XMLLiteral.getIRI()),
    ;
    private final IRI iri;

    /**
     * Constructor for the RDF core datatype enum.
     *
     * @param iri the IRI of the datatype
     */
    RDF(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
