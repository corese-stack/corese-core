package fr.inria.corese.core.next.api;

/**
 * Represents an IRI
 * @see <a href="https://www.w3.org/TR/rdf11-concepts/#section-IRIs">RDF 1.1 Concepts and Abstract Syntax: Internationalized Resource Identifiers (IRIs)</a>
 */
public interface IRI extends Resource {

	/**
	 * @return true if this is an IRI, false otherwise
	 */
	@Override
	default boolean isIRI() {
		return true;
	}

	/**
	 *
	 * @return the namespace of the IRI
	 */
	String getNamespace();

	/**
	 *
	 * @return the local name of the IRI
	 */
	String getLocalName();

	/**
	 *
	 * @param o the object to compare
	 * @return true if the object is equal to this IRI, false otherwise
	 */
	@Override
	boolean equals(Object o);

	/**
	 *
	 * @return the hash code of this IRI
	 */
	@Override
	int hashCode();

}
