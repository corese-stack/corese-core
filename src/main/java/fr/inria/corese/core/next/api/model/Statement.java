package fr.inria.corese.core.next.api.model;

import java.io.Serializable;

public interface Statement extends Serializable {

    /**
     * Gets the subject of this statement.
     *
     * @return The statement's subject.
     */
    Resource getSubject();

    /**
     * Gets the predicate of this statement.
     *
     * @return The statement's predicate.
     */
    IRI getPredicate();

    /**
     * Gets the object of this statement.
     *
     * @return The statement's object.
     */
    Value getObject();

    /**
     * Gets the context of this statement.
     *
     * @return The statement's context, or <var>null</var> in case of the null context or if not applicable.
     */
    Resource getContext();

    /**
     * Compares this statement to another object.
     *
     * @param other the object to compare this statement to
     * @return {@code true} if the other object is an instance of {@code Statement} and if their
     *         {@linkplain #getSubject() subjects}, {@linkplain #getPredicate() predicates}, {@linkplain #getObject()
     *         objects} and {@linkplain #getContext() contexts} are equal; {@code false} otherwise
     */
    @Override
    boolean equals(Object other);

    /**
     * Computes the hash code of this statement.
     *
     * @return a hash code for this statement computed as {@link java.util.Objects#hash Objects.hash}(
     *         {@link #getSubject()}, {@link #getPredicate()}, {@link #getObject()}, {@link #getContext()})
     */
    @Override
    int hashCode();

}
