package fr.inria.corese.core.next.data.api.term;

/**
 * Represents a nested triple term as defined in RDF 1.2 / RDF-star.
 * A triple term can be used as the object of another RDF triple.
 *
 * @see <a href="https://www.w3.org/TR/rdf12-concepts/#section-triple-terms">RDF 1.2 Concepts: Triple Terms</a>
 */
public interface Triple extends Value {

	/**
	 * @return the subject of this triple
	 */
	Resource subject();

	/**
	 * @return the predicate of this triple
	 */
	IRI predicate();

	/**
	 * @return the object of this triple
	 */
	Value object();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();
}
