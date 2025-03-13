package fr.inria.corese.core.next.api.model;

/**
 * Represents a blank node in a RDF graph.
 * @see <a href="https://www.w3.org/TR/rdf11-concepts/#section-blank-nodes">RDF-1.1 Concepts and Abstract Syntax: 3.4 Blank Nodes</a>
 */
public interface BNode extends Resource {

	@Override
	default boolean isBNode() {
		return true;
	}

	/**
	 * @return The identifier of the blank node
	 */
	String getID();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

}
