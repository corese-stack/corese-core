package fr.inria.corese.core.next.api.model;

/**
 * Super interface of all resources of an RDF graph (statements, IRI, blank nodes) as defined for RDF 1.2.
 */
public interface Resource extends Value {

	@Override
	default boolean isResource() {
		return true;
	}

}
