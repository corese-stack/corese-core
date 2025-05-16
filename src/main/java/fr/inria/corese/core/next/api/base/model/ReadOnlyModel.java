package fr.inria.corese.core.next.api.base.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;

/**
 * A read-only wrapper for a {@link Model}. All modification operations throw
 * {@link UnsupportedOperationException}.
 */
public class ReadOnlyModel extends AbstractModel {

	private static final long serialVersionUID = 8934829374192038471L;

	// The underlying model to delegate read operations to
	private final Model delegate;

	/**
	 * Constructs a ReadOnlyModel that wraps the given backing model.
	 * 
	 * @param backingModel the model to wrap
	 */
	public ReadOnlyModel(Model backingModel) {
		this.delegate = backingModel;
	}

	/**
	 * Returns an unmodifiable view of the namespaces in the model.
	 */
	@Override
	public Set<Namespace> getNamespaces() {
		return Collections.unmodifiableSet(delegate.getNamespaces());
	}

	/**
	 * Returns the namespace for the given prefix, if present.
	 */
	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		return delegate.getNamespace(prefix);
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public Namespace setNamespace(String prefix, String name) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public void setNamespace(Namespace namespace) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}

	/**
	 * Checks if the model contains a statement matching the given pattern.
	 */
	@Override
	public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
		return delegate.contains(subject, predicate, object, contexts);
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}

	/**
	 * Returns an unmodifiable filtered view of the model.
	 */
	@Override
	public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
		return delegate.filter(subject, predicate, object, contexts).unmodifiable();
	}

	/**
	 * Returns an unmodifiable iterator over the statements in the model.
	 */
	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableSet(delegate).iterator();
	}

	/**
	 * Returns the number of statements in the model.
	 */
	@Override
	public int size() {
		return delegate.size();
	}

	/**
	 * Not supported. Throws UnsupportedOperationException.
	 */
	@Override
	public void removeTermIteration(Iterator<Statement> iter, Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		throw new UnsupportedOperationException("Modifications are not supported in ReadOnlyModel");
	}
}
