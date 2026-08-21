package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
public enum DefaultDatatype implements CoreDatatype {
    NONE();

    private DefaultDatatype() {
    }

    @Override
    public IRI getIRI() {
        return null;
    }
}
