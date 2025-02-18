package fr.inria.corese.core.next.api.model;

public interface Triple extends Resource {

	@Override
	default boolean isTriple() {
		return true;
	}

	Resource getSubject();

	IRI getPredicate();

	Value getObject();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

}
