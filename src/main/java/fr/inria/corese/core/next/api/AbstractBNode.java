package fr.inria.corese.core.next.api;

import java.util.Objects;

/**
 * Abstract base class for blank nodes in an RDF graph.
 * <p>
 * Provides default implementations for {@link BNode#getID()},
 * {@link Object#equals(Object)}, and {@link Object#hashCode()}.
 * </p>
 */
public abstract class AbstractBNode implements BNode {

	private static final String BLANK_NODE_PREFIX = "_:";

	/** Internal identifier for the blank node (unique within a model) */
	private final String id;

	/** Serial version UID for serialization */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a blank node with a specific identifier.
	 * <p>
	 * <b>Warning:</b> This bypasses the automatic ID generation and should only be
	 * used when restoring an existing RDF graph (e.g. during parsing or tests).
	 * </p>
	 */
	protected AbstractBNode(String id) {
		this.id = Objects.requireNonNull(id, "Blank node ID must not be null");
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof BNode))
			return false;
		BNode other = (BNode) o;
		return id.equals(other.getID());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String stringValue() {
		return BLANK_NODE_PREFIX + id;
	}

	@Override
	public String toString() {
		return BLANK_NODE_PREFIX + id;
	}
}
