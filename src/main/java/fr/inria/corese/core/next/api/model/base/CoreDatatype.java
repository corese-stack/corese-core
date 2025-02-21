package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.sparql.api.IDatatype;

public interface CoreDatatype {

    IRI getIRI();
}

enum DefaultDatatype implements CoreDatatype {
    NONE();

    private DefaultDatatype() {
    }

    @Override
    public IRI getIRI() {
        return null;
    }
}
