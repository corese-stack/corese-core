package fr.inria.corese.core.next.api;

/**
 * Super interface of all resources of an RDF graph (statements, IRI, blank nodes) as defined for RDF 1.2.
 */
public interface Resource extends Value {

	/**
	 * @return true
	 */
	@Override
	default boolean isResource() {
		return true;
	}
	/**
	 * @return true if this resource is a blank node, false otherwise (i.e., if it is an IRI).
	 */
	boolean isBlank();

	/**
	 * Retrieves the unique identifier of this resource.
	 * For IRIs, this is the IRI string. For blank nodes, this is the blank node identifier.
	 */
	String getID();


}
