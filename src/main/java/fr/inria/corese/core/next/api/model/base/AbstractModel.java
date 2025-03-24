package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractModel implements Model {

    @Override
    public Model unmodifiable() {
        return null;
    }

    @Override
    public Namespace setNamespace(String prefix, String name) {
        return null;
    }

    @Override
    public void setNamespace(Namespace namespace) {

    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return Optional.empty();
    }

    @Override
    public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
        return false;
    }

    @Override
    public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
        return false;
    }

    @Override
    public boolean clear(Resource... context) {
        return false;
    }

    @Override
    public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
        return false;
    }

    @Override
    public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
        return null;
    }

    @Override
    public Set<Resource> subjects() {
        return null;
    }

    @Override
    public Set<IRI> predicates() {
        return null;
    }

    @Override
    public Set<Value> objects() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<Statement> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(Statement statement) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Statement> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
