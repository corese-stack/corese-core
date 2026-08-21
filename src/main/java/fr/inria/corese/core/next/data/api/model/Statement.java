package fr.inria.corese.core.next.data.api.model;

import java.io.Serializable;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

/**
 * Represents a value in RDF. Roughly equivalent to a quad with subject, predicate, object, and context.
 */
public interface Statement extends Serializable {

    /**
     * Gets the subject of this statement.
     */
    Resource getSubject();

    /**
     * Gets the predicate of this statement.
     */
    IRI getPredicate();

    /**
     * Gets the object of this statement.
     */
    Value getObject();

    /**
     * Gets the context of this statement.
     */
    Resource getContext();

    /**
     * Compares this statement to another object.
     */
    @Override
    boolean equals(Object other);

    /**
     * Computes the hash code of this statement.
     */
    @Override
    int hashCode();

}
