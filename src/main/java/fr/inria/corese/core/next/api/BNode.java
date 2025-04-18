package fr.inria.corese.core.next.api;

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
	 * @return The identifier of the blank node. This identifier is unique in the context of the Model that contains it.
	 * @see <a href="https://www.w3.org/TR/rdf11-concepts/#section-blank-nodes">RDF-1.1 Concepts and Abstract Syntax: 3.4 Blank Nodes</a>
	 */
	String getID();

	/**
	 *
	 * @param o
	 * @return true if o is a BNode and that has the same identifier as this BNode.
	 */
	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

}
