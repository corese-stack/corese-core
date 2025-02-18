package fr.inria.corese.core.next.api.model;

public interface Resource extends Value {

	@Override
	default boolean isResource() {
		return true;
	}

}
