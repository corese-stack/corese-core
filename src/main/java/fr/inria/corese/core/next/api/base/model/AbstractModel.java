package fr.inria.corese.core.next.api.base.model;

import java.io.Serial;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;

/**
 * Abstract class that implements the Model interface.
 * This class provides default implementations for the methods in the Model
 * interface,
 */
public abstract class AbstractModel extends AbstractSet<Statement> implements Model {

    @Override
    public Model unmodifiable() {
        return new ReadOnlyModel(this);
    }

    @Override
    public Namespace setNamespace(String prefix, String name) {
        Optional<? extends Namespace> existing = getNamespace(prefix);

        if (!existing.isPresent() || !existing.get().getName().equals(name)) {
            Namespace namespace = new ModelNamespace(prefix, name);
            setNamespace(namespace);
            return namespace;
        }

        return existing.get();
    }

    /**
     * Internal implementation of the {@link Namespace} interface used by
     * {@link AbstractModel}.
     * <p>
     * Represents a simple, immutable namespace binding (prefix → URI).
     * Only used when adding a namespace via {@code setNamespace(prefix, uri)}.
     * </p>
     */
    private static class ModelNamespace extends AbstractNamespace {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String prefix;
        private final String namespaceURI;

        ModelNamespace(String prefix, String namespaceURI) {
            this.prefix = prefix;
            this.namespaceURI = namespaceURI;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getName() {
            return namespaceURI;
        }
    }

    @Override
    public boolean clear(Resource... context) {
        return remove(null, null, null, context);
    }

    @Override
    public Set<Resource> subjects() {
        return new ValueSet<Resource>() {

            @Override
            public boolean contains(Object object) {
                if (object instanceof Resource) {
                    return AbstractModel.this.contains((Resource) object, null, null);
                }
                return false;
            }

            @Override
            public boolean remove(Object object) {
                if (object instanceof Resource) {
                    return AbstractModel.this.remove((Resource) object, null, null);
                }
                return false;
            }

            @Override
            public boolean add(Resource subject) {
                return AbstractModel.this.add(subject, null, null);
            }

            @Override
            protected Resource term(Statement statement) {
                return statement.getSubject();
            }

            @Override
            protected void removeIteration(Iterator<Statement> iterator, Resource subject) {
                AbstractModel.this.removeTermIteration(iterator, subject, null, null);
            }
        };
    }

    @Override
    public Set<IRI> predicates() {
        return new ValueSet<IRI>() {

            @Override
            public boolean contains(Object object) {
                if (object instanceof IRI) {
                    return AbstractModel.this.contains(null, (IRI) object, null);
                }
                return false;
            }

            @Override
            public boolean remove(Object object) {
                if (object instanceof IRI) {
                    return AbstractModel.this.remove(null, (IRI) object, null);
                }
                return false;
            }

            @Override
            public boolean add(IRI predicate) {
                return AbstractModel.this.add(null, predicate, null);
            }

            @Override
            protected IRI term(Statement statement) {
                return statement.getPredicate();
            }

            @Override
            protected void removeIteration(Iterator<Statement> iterator, IRI predicate) {
                AbstractModel.this.removeTermIteration(iterator, null, predicate, null);
            }
        };
    }

    @Override
    public Set<Value> objects() {
        return new ValueSet<Value>() {

            @Override
            public boolean contains(Object object) {
                if (object instanceof Value) {
                    return AbstractModel.this.contains(null, null, (Value) object);
                }
                return false;
            }

            @Override
            public boolean remove(Object object) {
                if (object instanceof Value) {
                    return AbstractModel.this.remove(null, null, (Value) object);
                }
                return false;
            }

            @Override
            public boolean add(Value value) {
                return AbstractModel.this.add(null, null, value);
            }

            @Override
            protected Value term(Statement statement) {
                return statement.getObject();
            }

            @Override
            protected void removeIteration(Iterator<Statement> iterator, Value value) {
                AbstractModel.this.removeTermIteration(iterator, null, null, value);
            }
        };
    }

    @Override
    public Set<Resource> contexts() {
        return new ValueSet<Resource>() {

            @Override
            public boolean contains(Object object) {
                if (object instanceof Resource || object == null) {
                    return AbstractModel.this.contains(null, null, null, (Resource) object);
                }
                return false;
            }

            @Override
            public boolean remove(Object object) {
                if (object instanceof Resource || object == null) {
                    return AbstractModel.this.remove(null, null, null, (Resource) object);
                }
                return false;
            }

            @Override
            public boolean add(Resource context) {
                return AbstractModel.this.add(null, null, null, context);
            }

            @Override
            protected Resource term(Statement statement) {
                return statement.getContext();
            }

            @Override
            protected void removeIteration(Iterator<Statement> iterator, Resource context) {
                AbstractModel.this.removeTermIteration(iterator, null, null, null, context);
            }
        };
    }

    private abstract class ValueSet<V extends Value> extends AbstractSet<V> {

        private final class ValueSetIterator implements Iterator<V> {

            private final Iterator<Statement> statementIterator;
            private final Set<V> seen = new LinkedHashSet<>();

            private Statement currentStatement;
            private Statement nextStatement;

            private ValueSetIterator(Iterator<Statement> iterator) {
                this.statementIterator = iterator;
            }

            @Override
            public boolean hasNext() {
                if (nextStatement == null) {
                    nextStatement = findNext();
                }
                return nextStatement != null;
            }

            @Override
            public V next() {
                if (nextStatement == null) {
                    nextStatement = findNext();
                    if (nextStatement == null) {
                        throw new NoSuchElementException();
                    }
                }

                currentStatement = nextStatement;
                nextStatement = null;

                V value = term(currentStatement);
                seen.add(value);
                return value;
            }

            @Override
            public void remove() {
                if (currentStatement == null) {
                    throw new IllegalStateException();
                }

                removeIteration(statementIterator, term(currentStatement));
                currentStatement = null;
            }

            private Statement findNext() {
                while (statementIterator.hasNext()) {
                    Statement statement = statementIterator.next();
                    V value = term(statement);
                    if (!seen.contains(value)) {
                        return statement;
                    }
                }
                return null;
            }
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueSetIterator(AbstractModel.this.iterator());
        }

        @Override
        public void clear() {
            AbstractModel.this.clear();
        }

        @Override
        public boolean isEmpty() {
            return AbstractModel.this.isEmpty();
        }

        @Override
        public int size() {
            Iterator<Statement> iterator = AbstractModel.this.iterator();
            try {
                Set<V> uniqueTerms = new LinkedHashSet<>();
                while (iterator.hasNext()) {
                    uniqueTerms.add(term(iterator.next()));
                }
                return uniqueTerms.size();
            } finally {
                AbstractModel.this.closeIterator(iterator);
            }
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean modified = false;

            Iterator<?> iterator = collection.iterator();
            try {
                while (iterator.hasNext()) {
                    modified |= remove(iterator.next());
                }
                return modified;
            } finally {
                closeIterator(collection, iterator);
            }
        }

        @Override
        public Object[] toArray() {
            Iterator<Statement> iterator = AbstractModel.this.iterator();
            try {
                Set<V> uniqueTerms = new LinkedHashSet<>();
                while (iterator.hasNext()) {
                    uniqueTerms.add(term(iterator.next()));
                }
                return uniqueTerms.toArray();
            } finally {
                AbstractModel.this.closeIterator(iterator);
            }
        }

        @Override
        public <T> T[] toArray(T[] array) {
            Iterator<Statement> iterator = AbstractModel.this.iterator();
            try {
                Set<V> uniqueTerms = new LinkedHashSet<>();
                while (iterator.hasNext()) {
                    uniqueTerms.add(term(iterator.next()));
                }
                return uniqueTerms.toArray(array);
            } finally {
                AbstractModel.this.closeIterator(iterator);
            }
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Iterator<?> iterator = collection.iterator();
            try {
                while (iterator.hasNext()) {
                    if (!contains(iterator.next())) {
                        return false;
                    }
                }
                return true;
            } finally {
                closeIterator(collection, iterator);
            }
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            boolean modified = false;

            Iterator<? extends V> iterator = collection.iterator();
            try {
                while (iterator.hasNext()) {
                    if (add(iterator.next())) {
                        modified = true;
                    }
                }
                return modified;
            } finally {
                closeIterator(collection, iterator);
            }
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            Iterator<V> iterator = iterator();
            try {
                boolean modified = false;
                while (iterator.hasNext()) {
                    if (!collection.contains(iterator.next())) {
                        iterator.remove();
                        modified = true;
                    }
                }
                return modified;
            } finally {
                closeIterator(iterator);
            }
        }

        // Must be implemented by subclasses: how to extract a term from a Statement
        @Override
        public abstract boolean add(V term);

        protected abstract V term(Statement statement);

        protected abstract void removeIteration(Iterator<Statement> iterator, V term);

        protected void closeIterator(Iterator<?> iterator) {
            AbstractModel.this.closeIterator(((ValueSetIterator) iterator).statementIterator);
        }

        private void closeIterator(Collection<?> collection, Iterator<?> iterator) {
            if (collection instanceof AbstractModel) {
                ((AbstractModel) collection).closeIterator(iterator);
            } else if (collection instanceof ValueSet) {
                ((ValueSet<?>) collection).closeIterator(iterator);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return !contains(null, null, null);
    }

    @Override
    public boolean contains(Object object) {
        if (object instanceof Statement) {
            Statement statement = (Statement) object;
            return contains(
                    statement.getSubject(),
                    statement.getPredicate(),
                    statement.getObject(),
                    statement.getContext());
        }

        return false;
    }

    @Override
    public Object[] toArray() {
        Iterator<Statement> iterator = iterator();
        try {
            List<Object> collected = new ArrayList<>(size());

            while (iterator.hasNext()) {
                collected.add(iterator.next());
            }

            return collected.toArray();
        } finally {
            closeIterator(iterator);
        }
    }

    @Override
    public <T> T[] toArray(T[] array) {
        Iterator<Statement> iterator = iterator();
        try {
            List<Object> tempList = new ArrayList<>(size());
            while (iterator.hasNext()) {
                tempList.add(iterator.next());
            }
            return tempList.toArray(array);
        } finally {
            closeIterator(iterator);
        }
    }

    @Override
    public boolean add(Statement statement) {
        return add(
                statement.getSubject(),
                statement.getPredicate(),
                statement.getObject(),
                statement.getContext());
    }

    @Override
    public boolean remove(Object object) {
        if (object instanceof Statement) {
            if (isEmpty()) {
                return false;
            }

            Statement statement = (Statement) object;
            return remove(
                    statement.getSubject(),
                    statement.getPredicate(),
                    statement.getObject(),
                    statement.getContext());
        }

        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Iterator<?> iterator = collection.iterator();
        try {
            while (iterator.hasNext()) {
                if (!contains(iterator.next())) {
                    return false;
                }
            }
            return true;
        } finally {
            closeIterator(collection, iterator);
        }
    }

    @Override
    public boolean addAll(Collection<? extends Statement> collection) {
        boolean modified = false;

        Iterator<? extends Statement> iterator = collection.iterator();
        try {
            while (iterator.hasNext()) {
                if (add(iterator.next())) {
                    modified = true;
                }
            }

            return modified;
        } finally {
            closeIterator(collection, iterator);
        }
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean modified = false;

        Iterator<Statement> iterator = iterator();
        try {
            while (iterator.hasNext()) {
                if (!collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }

            return modified;
        } finally {
            closeIterator(iterator);
        }
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean modified = false;

        // Iterate over the smaller collection for better performance
        if (size() > collection.size()) {
            Iterator<?> iterator = collection.iterator();
            try {
                while (iterator.hasNext()) {
                    // Attempt to remove each element from this collection
                    modified |= remove(iterator.next());
                }
            } finally {
                closeIterator(collection, iterator);
            }
        } else {
            Iterator<?> iterator = iterator();
            try {
                while (iterator.hasNext()) {
                    Object element = iterator.next();
                    // Remove elements present in the input collection
                    if (collection.contains(element)) {
                        iterator.remove();
                        modified = true;
                    }
                }
            } finally {
                closeIterator(iterator);
            }
        }

        return modified;
    }

    @Override
    public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return () -> filter(subject, predicate, object, contexts).iterator();
    }

    @Override
    public void clear() {
        remove(null, null, null);
    }

    /**
     * Releases any resources associated with the given iterator, if applicable.
     * <p>
     * Specifically handles internal iterators used by ValueSet views,
     * delegating the cleanup to the underlying statement iterator.
     * </p>
     *
     * @param iterator the iterator to release
     */
    // todo: Use pattern matching to check if the iterator is of type
    // ValueSet.ValueSetIterator
    // when Java 17+ is used
    protected void closeIterator(Iterator<?> iterator) {
        if (iterator instanceof ValueSet.ValueSetIterator) {
            ValueSet.ValueSetIterator valueSetIterator = (ValueSet.ValueSetIterator) iterator;
            closeIterator(valueSetIterator.statementIterator);
        }
    }

    public abstract void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
            Resource... contexts);

    /**
     * Attempts to delegate iterator cleanup to the appropriate container,
     * if the given collection supports it (e.g., AbstractModel or ValueSet).
     */
    private void closeIterator(Collection<?> collection, Iterator<?> iterator) {
        if (collection instanceof AbstractModel) {
            ((AbstractModel) collection).closeIterator(iterator);
        } else if (collection instanceof ValueSet) {
            ((ValueSet<?>) collection).closeIterator(iterator);
        }
    }

}
