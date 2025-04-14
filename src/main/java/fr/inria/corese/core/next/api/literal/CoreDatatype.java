package fr.inria.corese.core.next.api.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.DefaultDatatype;

/**
 * Represents a literal datatype.
 * It is necessary to declare a datatype as implementing CoreDatatype to implement operations specific to it in the Corese engine.
 */
public interface CoreDatatype {

    CoreDatatype NONE = DefaultDatatype.NONE;

    IRI getIRI();

}

