package fr.inria.corese.core.next.storage.impl.model;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.support.model.AbstractModel;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.model.ModelException;
import fr.inria.corese.core.next.data.impl.model.ModelException.ModelOperation;
import fr.inria.corese.core.next.data.impl.model.FilteredModel;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@link Model} interface backed by a {@link StorageManager}.
 *
 * <p>This model is a live view over runtime storage services.</p>
 */
@SuppressWarnings("java:S2160")
public final class StorageModel extends AbstractModel {

    private final StorageManager storage;
    private final ValueFactory valueFactory;
    private final Set<Namespace> namespaces;
    private final boolean unmodifiable;

    /**
     * Constructs a new StorageModel.
     *
     * @param storage      the storage manager (must not be null)
     * @param valueFactory the value factory (must not be null)
     * @param namespaces   the initial namespaces (may be null)
     * @param unmodifiable whether this model is unmodifiable
     */
    private StorageModel(
            StorageManager storage,
            ValueFactory valueFactory,
            Set<Namespace> namespaces,
            boolean unmodifiable) {
        this.storage = Objects.requireNonNull(storage, "StorageManager must not be null");
        this.valueFactory = Objects.requireNonNull(valueFactory, "ValueFactory must not be null");
        this.namespaces = namespaces != null ? new HashSet<>(namespaces) : new HashSet<>();
        this.unmodifiable = unmodifiable;
    }

    /**
     * Returns a new builder for StorageModel.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StorageModel}.
     */
    public static final class Builder {
        private StorageManager storage;
        private ValueFactory valueFactory;
        private Set<Namespace> namespaces;

        /**
         * Sets the storage manager.
         *
         * @param storage the storage manager (must not be null)
         * @return this builder
         */
        public Builder storage(StorageManager storage) {
            this.storage = storage;
            return this;
        }

        /**
         * Sets the value factory.
         *
         * @param valueFactory the value factory (must not be null)
         * @return this builder
         */
        public Builder valueFactory(ValueFactory valueFactory) {
            this.valueFactory = valueFactory;
            return this;
        }

        /**
         * Sets the initial namespaces.
         *
         * @param namespaces the initial namespaces (may be null)
         * @return this builder
         */
        public Builder namespaces(Set<Namespace> namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        /**
         * Builds a new StorageModel instance.
         *
         * @return a new model instance
         * @throws IllegalStateException if storage or valueFactory is null
         */
        public StorageModel build() {
            if (storage == null) {
                throw new IllegalStateException("StorageManager is required");
            }
            if (valueFactory == null) {
                throw new IllegalStateException("ValueFactory is required");
            }
            return new StorageModel(storage, valueFactory, namespaces, false);
        }
    }

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
        checkModifiable();
        Objects.requireNonNull(subject, "Subject must not be null");
        Objects.requireNonNull(predicate, "Predicate must not be null");
        Objects.requireNonNull(object, "Object must not be null");

        try {
            if (contexts == null || contexts.length == 0) {
                Statement stmt = valueFactory.createStatement(subject, predicate, object);
                return storage.getMutationOperations()
                        .insertStatement(stmt)
                        .getAffectedStatement()
                        .isPresent();
            }

            // Add once per context
            boolean changed = false;
            for (Resource context : contexts) {
                Statement stmt = valueFactory.createStatement(subject, predicate, object, context);
                changed |= storage.getMutationOperations()
                        .insertStatement(stmt)
                        .getAffectedStatement()
                        .isPresent();
            }
            return changed;

        } catch (StorageException e) {
            throw new ModelException(ModelOperation.ADD,
                    "Failed to add statement [" + subject + ", " + predicate + ", " + object + "]", e);
        }
    }

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
        try {
            StatementPattern pattern = StatementPattern.of(subject, predicate, object, contexts);
            return storage.getQueryOperations().exists(pattern);
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.CONTAINS,
                    "Failed to check statement existence", e);
        }
    }

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
        checkModifiable();

        try {
            return storage.getMutationOperations()
                    .deleteStatements(subject, predicate, object, contexts)
                    .isSuccess();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.REMOVE,
                    "Failed to remove statements", e);
        }
    }

    @Override
    public boolean clear(Resource... contexts) {
        checkModifiable();

        try {
            return storage.getMutationOperations()
                    .clear(contexts)
                    .isSuccess();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.CLEAR,
                    "Failed to clear model"
                            + (contexts != null && contexts.length > 0
                                    ? " (contexts: " + Arrays.toString(contexts) + ")"
                                    : ""),
                    e);
        }
    }

    @Override
    public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
        try {
            StatementPattern pattern = StatementPattern.of(subject, predicate, object, contexts);
            return storage.getQueryOperations()
                    .query(pattern)
                    .toList();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.QUERY,
                    "Failed to query statements", e);
        }
    }

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return new FilteredModel(this, subject, predicate, object, contexts) {

            @Override
            @SuppressWarnings("NullableProblems")
            public Iterator<Statement> iterator() {
                return StorageModel.this.getFilterIterator(subject, predicate, object, contexts);
            }

            @Override
            protected void removeFilteredTermIteration(
                    Iterator<Statement> iterator,
                    Resource subject,
                    IRI predicate,
                    Value object,
                    Resource... contexts) {
                StorageModel.this.removeTermIteration(iterator, subject, predicate, object, contexts);
            }
        };
    }

    @Override
    public Set<Resource> subjects() {
        try {
            return storage.getMetadataOperations().getSubjects();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.GET_SUBJECTS,
                    "Failed to retrieve subjects", e);
        }
    }

    @Override
    public Set<IRI> predicates() {
        try {
            return storage.getMetadataOperations().getPredicates();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.GET_PREDICATES,
                    "Failed to retrieve predicates", e);
        }
    }

    @Override
    public Set<Value> objects() {
        try {
            return storage.getMetadataOperations().getObjects();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.GET_OBJECTS,
                    "Failed to retrieve objects", e);
        }
    }

    @Override
    public Set<Resource> contexts() {
        try {
            return storage.getMetadataOperations().getContexts();
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.GET_CONTEXTS,
                    "Failed to retrieve contexts", e);
        }
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return Collections.unmodifiableSet(namespaces);
    }

    @Override
    public void setNamespace(Namespace namespace) {
        checkModifiable();
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        removeNamespace(namespace.getPrefix());
        namespaces.add(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        checkModifiable();
        return getNamespace(prefix).filter(namespaces::remove);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Iterator<Statement> iterator() {
        return getFilterIterator(null, null, null);
    }

    @Override
    public void removeTermIteration(
            Iterator<Statement> iterator,
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts) {
        checkModifiable();
        remove(subject, predicate, object, contexts);
    }

    @Override
    public int size() {
        try {
            StatementPattern pattern = StatementPattern.of(null, null, null);
            return Math.toIntExact(storage.getQueryOperations().count(pattern));
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.SIZE,
                    "Failed to count statements", e);
        }
    }

    @Override
    public Model unmodifiable() {
        if (unmodifiable) {
            return this;
        }
        return new StorageModel(storage, valueFactory, namespaces, true);
    }


    /**
     * Returns the value factory.
     *
     * @return the value factory
     */
    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    /**
     * Checks if this model is modifiable and throws an exception if not.
     *
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    private void checkModifiable() {
        if (unmodifiable) {
            throw new UnsupportedOperationException("Model is unmodifiable");
        }
    }

    /**
     * Returns an iterator over statements matching the given pattern.
     *
     * @param subject   the subject filter (null = wildcard)
     * @param predicate the predicate filter (null = wildcard)
     * @param object    the object filter (null = wildcard)
     * @param contexts  the context filters (null/empty = wildcard)
     * @return an iterator over matching statements
     * @throws ModelException if iterator creation fails
     */
    private Iterator<Statement> getFilterIterator(
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts) {

        class ModelIterator implements Iterator<Statement> {
            private final Iterator<Statement> delegate;
            private Statement last;

            ModelIterator(Iterator<Statement> delegate) {
                this.delegate = delegate;
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Statement next() {
                last = delegate.next();
                return last;
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new IllegalStateException("No current element");
                }
                StorageModel.this.remove(last);
                last = null;
            }
        }

        try {
            StatementPattern pattern = StatementPattern.of(subject, predicate, object, contexts);
            List<Statement> statements = storage.getQueryOperations()
                    .query(pattern)
                    .toList();
            return new ModelIterator(statements.iterator());
        } catch (StorageException e) {
            throw new ModelException(ModelOperation.ITERATE,
                    "Failed to create iterator", e);
        }
    }
}
