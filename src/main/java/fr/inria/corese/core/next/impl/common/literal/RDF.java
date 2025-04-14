package fr.inria.corese.core.next.impl.common.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;

public enum RDF implements CoreDatatype {
    LANGSTRING(fr.inria.corese.core.next.impl.common.vocabulary.RDF.langString.getIRI()),
    HTML(fr.inria.corese.core.next.impl.common.vocabulary.RDF.HTML.getIRI()),
    JSON(fr.inria.corese.core.next.impl.common.vocabulary.RDF.JSON.getIRI()),
    XML_LITERAL(fr.inria.corese.core.next.impl.common.vocabulary.RDF.XMLLiteral.getIRI()),
    ;
    IRI iri;

    RDF(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
