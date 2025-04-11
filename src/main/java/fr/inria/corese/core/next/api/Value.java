package fr.inria.corese.core.next.api;

import java.io.Serializable;

/**
 * Super interface of all elements of an RDF model (statements, nodes, etc).
 */
public interface Value extends Serializable {

	default boolean isBNode() {
		return false;
	}

	default boolean isIRI() {
		return false;
	}

	default boolean isResource() {
		return false;
	}

	default boolean isLiteral() {
		return false;
	}

	default boolean isTriple() {
		return false;
	}

	String stringValue();

}
