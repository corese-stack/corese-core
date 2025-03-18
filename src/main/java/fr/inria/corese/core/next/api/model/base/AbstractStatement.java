package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.Statement;

import java.util.Objects;

public abstract class AbstractStatement implements Statement {

	private static final long serialVersionUID = 2151765288493878597L;

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates and contexts in sets of statements are the smallest (and therefore most likely to be identical),
		// so these are checked last.

		return this == o || o instanceof Statement
				&& getObject().equals(((Statement) o).getObject())
				&& getSubject().equals(((Statement) o).getSubject())
				&& getPredicate().equals(((Statement) o).getPredicate())
				&& Objects.equals(getContext(), ((Statement) o).getContext());
	}

	@Override
	public int hashCode() {
		// Inlined Objects.hash(getSubject(), getPredicate(), getObject(), getContext()) to avoid array creation
		int result = 17;
		result = 31 * result + (getSubject() == null ? 0 : getSubject().hashCode());
		result = 31 * result + (getPredicate() == null ? 0 : getPredicate().hashCode());
		result = 31 * result + (getObject() == null ? 0 : getObject().hashCode());
		result = 31 * result + (getContext() == null ? 0 : getContext().hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "("
				+ getSubject()
				+ ", " + getPredicate()
				+ ", " + getObject()
				+ (getContext() == null ? "" : ", " + getContext())
				+ ")";
	}

}
