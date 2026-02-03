package fr.inria.corese.core.next.data.api.base.model.literal;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;

public enum DefaultDatatype implements CoreDatatype {
    NONE();

    private DefaultDatatype() {
    }

    @Override
    public IRI getIRI() {
        return null;
    }
}
