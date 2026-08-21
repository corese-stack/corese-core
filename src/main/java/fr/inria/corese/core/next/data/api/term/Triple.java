package fr.inria.corese.core.next.data.api.term;

public interface Triple extends Resource {

	/**
	 * @return the subject of this triple
	 */
	Resource getSubject();

	/**
	 * @return the predicate of this triple
	 */
	IRI getPredicate();

	/**
	 * @return the object of this triple
	 */
	Value getObject();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();
}
