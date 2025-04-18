package fr.inria.corese.core.next.api.base.model;

import fr.inria.corese.core.next.api.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract class that implements the Model interface.
 * This class provides default implementations for the methods in the Model interface,
 * throwing UnsupportedOperationException for methods that are not supported.
 */
public abstract class AbstractModel implements Model {

    @Override
    public Model unmodifiable() {
        throw new UnsupportedOperationException("Unmodifiable model not supported");
    }

    @Override
    public Namespace setNamespace(String prefix, String name) {
        throw new UnsupportedOperationException("Setting namespace not supported");
    }

    @Override
    public void setNamespace(Namespace namespace) {
        throw new UnsupportedOperationException("Setting namespace not supported");
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        throw new UnsupportedOperationException("Removing namespace not supported");
    }

    @Override
    public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
        throw new UnsupportedOperationException("Contains operation not supported");
    }

    @Override
    public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
        throw new UnsupportedOperationException("Add operation not supported");
    }

    @Override
    public boolean clear(Resource... context) {
        throw new UnsupportedOperationException("Clear operation not supported");
    }

    @Override
    public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
        throw new UnsupportedOperationException("Remove operation not supported");
    }

    @Override
    public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
        throw new UnsupportedOperationException("Filter operation not supported");
    }

    @Override
    public Set<Resource> subjects() {
        throw new UnsupportedOperationException("Subjects operation not supported");
    }

    @Override
    public Set<IRI> predicates() {
        throw new UnsupportedOperationException("Predicates operation not supported");
    }

    @Override
    public Set<Value> objects() {
        throw new UnsupportedOperationException("Objects operation not supported");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Size operation not supported");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("IsEmpty operation not supported");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Contains operation not supported");
    }

    @Override
    public Iterator<Statement> iterator() {
        throw new UnsupportedOperationException("Iterator operation not supported");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("ToArray operation not supported");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("ToArray operation not supported");
    }

    @Override
    public boolean add(Statement statement) {
        throw new UnsupportedOperationException("Add operation not supported");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Remove operation not supported");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("ContainsAll operation not supported");
    }

    @Override
    public boolean addAll(Collection<? extends Statement> c) {
        throw new UnsupportedOperationException("AddAll operation not supported");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("RetainAll operation not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("RemoveAll operation not supported");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Clear operation not supported");
    }
}
