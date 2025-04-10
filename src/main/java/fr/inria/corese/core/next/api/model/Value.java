package fr.inria.corese.core.next.api.model;

import java.io.Serializable;

/**
 * Super interface of all elements of an RDF model (triple, nodes, etc).
 */
public interface Value extends Serializable {

	/**
	 * @return true if this value is a blank node (i.e. an instance of BNode)
	 */
	default boolean isBNode() {
		return false;
	}

	/**
	 * @return true if this value is a IRI (i.e. an instance of IRI)
	 */
	default boolean isIRI() {
		return false;
	}

	/**
	 * @return true if this value is a resource (i.e. an instance of Resource)
	 */
	default boolean isResource() {
		return false;
	}

	/**
	 * @return true if this value is a literal (i.e. an instance of Literal)
	 */
	default boolean isLiteral() {
		return false;
	}

	/**
	 * @return true if this value is a named graph (i.e. an instance of NamedGraph)
	 */
	default boolean isTriple() {
		return false;
	}

	/**
	 * @return the string representation of this value (expected to be the same as the NTriples representation)
	 */
	String stringValue();

}
