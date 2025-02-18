package fr.inria.corese.core.next.api.model;
public interface IRI extends Resource {

	@Override
	default boolean isIRI() {
		return true;
	}

	String getNamespace();

	String getLocalName();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

}
