package fr.inria.corese.core.next.api.model.base;


import fr.inria.corese.core.next.api.model.BNode;

public abstract class AbstractBNode implements BNode {

	private static final long serialVersionUID = -4300187708248029921L;

	@Override
	public String stringValue() {
		return getID();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof BNode
				&& getID().equals(((BNode) o).getID());
	}

	@Override
	public int hashCode() {
		return getID().hashCode();
	}

	@Override
	public String toString() {
		return "_:" + getID();
	}

	static class GenericBNode extends AbstractBNode {

		private static final long serialVersionUID = 4964302614523115978L;

		private final String id;

		GenericBNode(String id) {
			this.id = id;
		}

		@Override
		public String getID() {
			return id;
		}

	}

}
