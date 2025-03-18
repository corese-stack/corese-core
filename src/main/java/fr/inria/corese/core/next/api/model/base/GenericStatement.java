package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Resource;
import fr.inria.corese.core.next.api.model.Statement;
import fr.inria.corese.core.next.api.model.Value;

import java.util.Objects;

/**
 * An implementation of the Statement interface with support for Java Generics.
 *
 * @implNote This class is marked as experimental because there may still be changes to the class.
 */
public class GenericStatement<R extends Resource, I extends IRI, V extends Value> implements Statement {

	// Fields for storing the values of the Statement. The fields subject, predicate and object may not be null, context
	// may be null. All are protected, because classes that extend this class should have direct access to the fields.
	protected final R subject;
	protected final I predicate;
	protected final V object;
	protected final R context;

	public GenericStatement(R subject, I predicate, V object, R context) {
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
		this.object = Objects.requireNonNull(object, "object must not be null");
		this.context = context;
	}

	@Override
	public R getSubject() {
		return subject;
	}

	@Override
	public I getPredicate() {
		return predicate;
	}

	@Override
	public V getObject() {
		return object;
	}

	@Override
	public R getContext() {
		return context;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Statement)) {
			return false;
		}

		Statement that = (Statement) o;

		return subject.equals(that.getSubject()) &&
				predicate.equals(that.getPredicate()) &&
				object.equals(that.getObject()) &&
				Objects.equals(context, that.getContext());
	}

	@Override
	public int hashCode() {
		// Inlined Objects.hash(subject, predicate, object,context) to avoid array creation
		int result = 1;
		result = 31 * result + subject.hashCode();
		result = 31 * result + predicate.hashCode();
		result = 31 * result + object.hashCode();
		result = 31 * result + (context == null ? 0 : context.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "("
				+ subject
				+ ", " + predicate
				+ ", " + object
				+ ") [" + context + "]";
	}
}
