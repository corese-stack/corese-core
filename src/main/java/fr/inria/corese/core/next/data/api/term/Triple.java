package fr.inria.corese.core.next.data.api.term;

/**
 * Represents a nested triple term as defined in RDF 1.2 / RDF-star.
 * A Triple term can be used as a subject or an object in an RDF graph.
 *
 * @see <a href="https://www.w3.org/TR/rdf12-concepts/#section-triple-terms">RDF 1.2 Concepts: Triple Terms</a>
 */
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
