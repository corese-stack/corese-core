package fr.inria.corese.core.next.api.model;

import java.io.Serializable;

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
