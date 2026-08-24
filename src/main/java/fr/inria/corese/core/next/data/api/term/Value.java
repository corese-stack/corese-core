package fr.inria.corese.core.next.data.api.term;

import java.io.Serializable;

/**
 * Super interface of all elements of an RDF model (triple, nodes, etc).
 */
public interface Value extends Serializable {

    /**
     * @return whether this value is a blank node
     */
    default boolean isBNode() {
        return this instanceof BNode;
    }

    /**
     * @return whether this value is an IRI
     */
    default boolean isIRI() {
        return this instanceof IRI;
    }

    /**
     * @return whether this value is an RDF resource
     */
    default boolean isResource() {
        return this instanceof Resource;
    }

    /**
     * @return whether this value is a literal
     */
    default boolean isLiteral() {
        return this instanceof Literal;
    }

    /**
     * @return whether this value is an RDF-star triple term
     */
    default boolean isTriple() {
        return this instanceof Triple;
    }

	/**
	 * Returns the value's lexical string. This is not a serialized N-Triples representation:
	 * IRIs are returned without angle brackets, blank nodes without the {@code _:} prefix,
	 * and literals without quotes or datatype suffixes.
	 *
	 * @return the lexical string of this value
	 */
	String stringValue();

}
