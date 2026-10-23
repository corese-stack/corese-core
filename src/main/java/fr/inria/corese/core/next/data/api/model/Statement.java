package fr.inria.corese.core.next.data.api.model;

import java.io.Serializable;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

/**
 * Represents an RDF statement or quad in Corese v5, composed of a subject, predicate, object,
 * and optional context (named graph).
 *
 * <h2>Immutability and Value Semantics</h2>
 * <p>Implementations of this interface must be strictly immutable and thread-safe. Once constructed,
 * neither the statement itself nor the references to its component terms may change.</p>
 *
 * <h2>Graph and Context Semantics</h2>
 * <p>When {@link #getContext()} returns {@code null}, the statement belongs to the default graph.
 * When {@link #getContext()} returns a non-null {@link Resource} (an {@link IRI} or {@link fr.inria.corese.core.next.data.api.term.BNode}),
 * the statement is a quad belonging to that designated named graph.</p>
 *
 * <h2>Equality Contract</h2>
 * <p>Two statements {@code s1} and {@code s2} are equal if and only if:
 * <ul>
 *   <li>{@code s1.getSubject().equals(s2.getSubject())}</li>
 *   <li>{@code s1.getPredicate().equals(s2.getPredicate())}</li>
 *   <li>{@code s1.getObject().equals(s2.getObject())}</li>
 *   <li>{@code Objects.equals(s1.getContext(), s2.getContext())}</li>
 * </ul>
 * Equality must be symmetric across different implementations of this interface.</p>
 */
public interface Statement extends Serializable {

    /**
     * Gets the subject resource of this statement (IRI, BNode, or Triple term).
     *
     * @return the non-null subject
     */
    Resource getSubject();

    /**
     * Gets the predicate IRI of this statement.
     *
     * @return the non-null predicate IRI
     */
    IRI getPredicate();

    /**
     * Gets the object value of this statement (Resource or Literal).
     *
     * @return the non-null object value
     */
    Value getObject();

    /**
     * Gets the context of this statement, identifying its containing graph.
     *
     * @return the graph resource identifier, or {@code null} if this statement belongs to the default graph
     */
    Resource getContext();

    /**
     * Compares this statement to another object according to RDF statement equality rules.
     *
     * @param other the object to compare against
     * @return {@code true} if both statements have equal subjects, predicates, objects, and contexts
     */
    @Override
    boolean equals(Object other);

    /**
     * Computes the hash code of this statement based on its subject, predicate, object, and context.
     *
     * @return integer hash code consistent with {@link #equals(Object)}
     */
    @Override
    int hashCode();
}
