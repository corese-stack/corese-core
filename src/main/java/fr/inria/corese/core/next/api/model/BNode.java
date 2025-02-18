package fr.inria.corese.core.next.api.model;

public interface BNode extends Resource {

	@Override
	default boolean isBNode() {
		return true;
	}

	String getID();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

}
