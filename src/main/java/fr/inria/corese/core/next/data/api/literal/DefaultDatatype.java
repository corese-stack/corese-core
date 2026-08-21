package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;

/**
 * Fallback enum for literals without a specific core datatype or with a custom unmapped IRI.
 */
public enum DefaultDatatype implements CoreDatatype {

    /** Default fallback when no specific CoreDatatype is matched. */
    NONE();

    private DefaultDatatype() {
    }

    @Override
    public IRI getIRI() {
        return null;
    }
}
