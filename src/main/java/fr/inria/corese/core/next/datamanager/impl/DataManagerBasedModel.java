package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.datamanager.api.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.next.datamanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.datamanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.datamanager.api.support.config.DataManagerConfig;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionManager;
import fr.inria.corese.core.next.datamanager.impl.lifecycle.LifecycleManagerImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.MetadataOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.MutationOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.QueryOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.transaction.TransactionManagerImpl;

import java.io.Serial;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of {@link Model} based directly on DataManager operations.
 */
public class DataManagerBasedModel implements Model {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Internal storage — single source of truth.
     */
    private final InMemoryModelStore store;

    /**
     * Handles query operations (query, exists, filter, count).
     */
    private final QueryOperations queryOps;

    /**
     * Handles mutation operations (insert, delete, clear).
     */
    private final MutationOperations mutationOps;

    /**
     * Handles metadata operations (subjects, predicates, objects, contexts).
     */
    private final MetadataOperations metadataOps;

    /**
     * Manages transaction lifecycle.
     */
    private final TransactionManager transactionManager;

    /**
     * Manages the DataManager lifecycle (initialize, shutdown).
     */
    private final DataManagerLifecycle lifecycle;

    /**
     * Namespace registry, preserving insertion order.
     */
    private final Map<String, Namespace> namespaces = new LinkedHashMap<>();

    /**
     * Protected constructor — use {@link Builder} to create instances.
     *
     * @param builder the builder containing configuration
     */
    protected DataManagerBasedModel(Builder builder) {
        this.store = new InMemoryModelStore();
        StoreBackedModel storeModel = new StoreBackedModel(this.store);

        this.queryOps = new QueryOperationsImpl(storeModel);
        this.mutationOps = new MutationOperationsImpl(storeModel);
        this.metadataOps = new MetadataOperationsImpl(storeModel);
        this.transactionManager = new TransactionManagerImpl(storeModel, builder.transactionSupport, builder.isolationLevel);
        this.lifecycle = new LifecycleManagerImpl(storeModel);
    }

    /**
     * Returns a read-only view of this model.
     * Any attempt to modify the returned model will throw {@link IncorrectOperationException}.
     *
     * @return an unmodifiable view of this model
     */
    @Override
    public Model unmodifiable() {
        return new UnmodifiableModel(this);
    }

    /**
     * Checks whether the model contains at least one statement matching
     * the given subject, predicate, object and optional contexts.
     * Any component can be {@code null} to act as a wildcard.
     *
     * @param subj     the subject to match, or {@code null} for any
     * @param pred     the predicate to match, or {@code null} for any
     * @param obj      the object to match, or {@code null} for any
     * @param contexts optional contexts to match; empty means any context
     * @return {@code true} if a matching statement exists
     */
    @Override
    public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
        try {
            return queryOps.exists(StatementPattern.of(subj, pred, obj, contexts));
        } catch (DataManagerException e) {
            throw new RuntimeException("contains failed", e);
        }
    }

    /**
     * Adds a statement defined by its components to the model.
     * If multiple contexts are provided, the statement is added once per context.
     *
     * @param subj     the subject (must not be {@code null})
     * @param pred     the predicate (must not be {@code null})
     * @param obj      the object (must not be {@code null})
     * @param contexts optional contexts; empty means default graph
     * @return {@code true} if the model was modified
     */
    @Override
    public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
        try {
            return mutationOps.insertStatement(subj, pred, obj, contexts).isSuccess();
        } catch (DataManagerException e) {
            throw new RuntimeException("add(s,p,o) failed", e);
        }
    }

    /**
     * Removes all statements from the specified contexts.
     * If no context is given, all statements are removed regardless of context.
     *
     * @param contexts the contexts to clear; empty means clear all
     * @return {@code true} if any statement was removed
     */
    @Override
    public boolean clear(Resource... contexts) {
        try {
            return mutationOps.clear(contexts).isSuccess();
        } catch (DataManagerException e) {
            throw new RuntimeException("clear failed", e);
        }
    }

    /**
     * Removes all statements matching the given pattern from the model.
     * Any component can be {@code null} to act as a wildcard.
     *
     * @param subj     the subject to match, or {@code null} for any
     * @param pred     the predicate to match, or {@code null} for any
     * @param obj      the object to match, or {@code null} for any
     * @param contexts optional contexts; empty means any context
     * @return {@code true} if any statement was removed
     */
    @Override
    public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
        try {
            return mutationOps.deleteStatements(subj, pred, obj, contexts).isSuccess();
        } catch (DataManagerException e) {
            throw new RuntimeException("remove(s,p,o) failed", e);
        }
    }

    /**
     * Returns an iterable over all statements matching the given pattern.
     * Any component can be {@code null} to act as a wildcard.
     *
     * @param subj     the subject to match, or {@code null} for any
     * @param pred     the predicate to match, or {@code null} for any
     * @param obj      the object to match, or {@code null} for any
     * @param contexts optional contexts; empty means any context
     * @return an {@link Iterable} of matching statements
     */
    @Override
    public Iterable<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts) {
        try {
            Stream<Statement> stream = queryOps.query(StatementPattern.of(subj, pred, obj, contexts));
            return stream::iterator;
        } catch (DataManagerException e) {
            throw new RuntimeException("getStatements failed", e);
        }
    }

    /**
     * Returns a new model containing all statements matching the given pattern.
     * Any component can be {@code null} to act as a wildcard.
     *
     * @param subj     the subject to match, or {@code null} for any
     * @param pred     the predicate to match, or {@code null} for any
     * @param obj      the object to match, or {@code null} for any
     * @param contexts optional contexts; empty means any context
     * @return a new {@link Model} with matching statements
     */
    @Override
    public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
        try {
            return queryOps.filter(StatementPattern.of(subj, pred, obj, contexts));
        } catch (DataManagerException e) {
            throw new RuntimeException("filter failed", e);
        }
    }

    /**
     * Returns the set of all unique subject resources in this model.
     *
     * @return an unmodifiable set of subjects
     */
    @Override
    public Set<Resource> subjects() {
        try {
            return metadataOps.getSubjects();
        } catch (DataManagerException e) {
            throw new RuntimeException("subjects failed", e);
        }
    }

    /**
     * Returns the set of all unique predicate IRIs in this model.
     *
     * @return an unmodifiable set of predicates
     */
    @Override
    public Set<IRI> predicates() {
        try {
            return metadataOps.getPredicates();
        } catch (DataManagerException e) {
            throw new RuntimeException("predicates failed", e);
        }
    }

    /**
     * Returns the set of all unique object values in this model.
     *
     * @return an unmodifiable set of objects
     */
    @Override
    public Set<Value> objects() {
        try {
            return metadataOps.getObjects();
        } catch (DataManagerException e) {
            throw new RuntimeException("objects failed", e);
        }
    }

    /**
     * Returns the set of all named graph identifiers (contexts) in this model.
     *
     * @return an unmodifiable set of context resources
     */
    @Override
    public Set<Resource> contexts() {
        try {
            return metadataOps.getContexts();
        } catch (DataManagerException e) {
            throw new RuntimeException("contexts failed", e);
        }
    }

    /**
     * Returns the namespace associated with the given prefix, if any.
     *
     * @param prefix the namespace prefix to look up
     * @return an {@link Optional} containing the namespace, or empty if not found
     */
    @Override
    public Optional<Namespace> getNamespace(String prefix) {
        return Optional.ofNullable(namespaces.get(prefix));
    }

    /**
     * Returns all namespaces registered in this model.
     *
     * @return an unmodifiable set of namespaces
     */
    @Override
    public Set<Namespace> getNamespaces() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(namespaces.values()));
    }

    /**
     * Registers a namespace with the given prefix and IRI.
     *
     * @param prefix the namespace prefix (must be unique in the model)
     * @param name   the namespace IRI
     * @return the created {@link Namespace}
     */
    @Override
    public Namespace setNamespace(String prefix, String name) {
        Namespace ns = new SimpleNamespace(prefix, name);
        namespaces.put(prefix, ns);
        return ns;
    }

    /**
     * Registers the given namespace object in this model.
     *
     * @param namespace the namespace to register
     */
    @Override
    public void setNamespace(Namespace namespace) {
        namespaces.put(namespace.getPrefix(), namespace);
    }

    /**
     * Removes the namespace associated with the given prefix.
     *
     * @param prefix the prefix of the namespace to remove
     * @return an {@link Optional} containing the removed namespace, or empty if not found
     */
    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return Optional.ofNullable(namespaces.remove(prefix));
    }

    /**
     * Returns the total number of statements in this model.
     *
     * @return the statement count
     */
    @Override
    public int size() {
        return store.size();
    }

    /**
     * Returns {@code true} if this model contains no statements.
     *
     * @return {@code true} if empty
     */
    @Override
    public boolean isEmpty() {
        return store.size() == 0;
    }

    /**
     * Returns {@code true} if this model contains the given object.
     * Only {@link Statement} instances can be contained.
     *
     * @param o the object to check
     * @return {@code true} if the statement is present
     */
    @Override
    public boolean contains(Object o) {
        return o instanceof Statement && store.contains((Statement) o);
    }

    /**
     * Returns an iterator over all statements in this model.
     * The iteration order is not guaranteed.
     *
     * @return a statement iterator
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public Iterator<Statement> iterator() {
        return store.getAllStatements().iterator();
    }

    /**
     * Returns an array containing all statements in this model.
     *
     * @return an array of all statements
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public Object[] toArray() {
        return store.getAllStatements().toArray();
    }

    /**
     * Returns an array containing all statements in this model,
     * using the provided array if it is large enough.
     *
     * @param a   the array to fill
     * @param <T> the component type
     * @return an array of all statements
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public <T> T[] toArray(T[] a) {
        return store.getAllStatements().toArray(a);
    }

    /**
     * Adds a single statement to this model.
     *
     * @param statement the statement to add (must not be {@code null})
     * @return {@code true} if the model was modified
     */
    @Override
    public boolean add(Statement statement) {
        try {
            return mutationOps.insertStatement(statement).isSuccess();
        } catch (DataManagerException e) {
            throw new RuntimeException("add(Statement) failed", e);
        }
    }

    /**
     * Removes the given object from this model if it is a {@link Statement}.
     *
     * @param o the object to remove
     * @return {@code true} if the model was modified
     */
    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Statement)) return false;
        try {
            return mutationOps.deleteStatement((Statement) o).isSuccess();
        } catch (DataManagerException e) {
            throw new RuntimeException("remove(Statement) failed", e);
        }
    }

    /**
     * Returns {@code true} if this model contains all elements in the given collection.
     *
     * @param c the collection to check
     * @return {@code true} if all elements are present
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    /**
     * Adds all statements in the given collection to this model.
     *
     * @param c the collection of statements to add
     * @return {@code true} if the model was modified
     */
    @Override
    public boolean addAll(Collection<? extends Statement> c) {
        boolean modified = false;
        for (Statement s : c) {
            if (add(s)) modified = true;
        }
        return modified;
    }

    /**
     * Retains only the statements present in the given collection.
     * Uses a snapshot to avoid {@link java.util.ConcurrentModificationException}.
     *
     * @param c the collection of statements to retain
     * @return {@code true} if the model was modified
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean retainAll(Collection<?> c) {
        Set<Statement> toRemove = new HashSet<>();
        for (Statement s : store.getAllStatements()) {
            if (!c.contains(s)) toRemove.add(s);
        }
        if (toRemove.isEmpty()) return false;
        toRemove.forEach(this::remove);
        return true;
    }

    /**
     * Removes all statements in the given collection from this model.
     *
     * @param c the collection of statements to remove
     * @return {@code true} if the model was modified
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) modified = true;
        }
        return modified;
    }

    /**
     * Removes all statements from this model.
     */
    @Override
    public void clear() {
        clear(new Resource[0]);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public DataManagerLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Read-only view of a {@link Model}.
     * All write operations throw {@link IncorrectOperationException}.
     */
    private record UnmodifiableModel(Model delegate) implements Model {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Model unmodifiable() {
            return this;
        }

        @Override
        public boolean contains(Resource s, IRI p, Value o, Resource... ctx) {
            return delegate.contains(s, p, o, ctx);
        }

        @Override
        public Iterable<Statement> getStatements(Resource s, IRI p, Value o, Resource... ctx) {
            return delegate.getStatements(s, p, o, ctx);
        }

        @Override
        public Model filter(Resource s, IRI p, Value o, Resource... ctx) {
            return delegate.filter(s, p, o, ctx).unmodifiable();
        }

        @Override
        public Set<Resource> subjects() {
            return delegate.subjects();
        }

        @Override
        public Set<IRI> predicates() {
            return delegate.predicates();
        }

        @Override
        public Set<Value> objects() {
            return delegate.objects();
        }

        @Override
        public Set<Resource> contexts() {
            return delegate.contexts();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Iterator<Statement> iterator() {
            return Set.copyOf(delegate).iterator();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public Optional<Namespace> getNamespace(String prefix) {
            return delegate.getNamespace(prefix);
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return delegate.getNamespaces();
        }

        private static <T> T ro() {
            throw new IncorrectOperationException("Model is unmodifiable");
        }

        @Override
        public boolean add(Statement s) {
            return ro();
        }

        @Override
        public boolean add(Resource s, IRI p, Value o, Resource... ctx) {
            return ro();
        }

        @Override
        public boolean remove(Object o) {
            return ro();
        }

        @Override
        public boolean remove(Resource s, IRI p, Value o, Resource... ctx) {
            return ro();
        }

        @Override
        public boolean clear(Resource... ctx) {
            return ro();
        }

        @Override
        public void clear() {
            ro();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean addAll(Collection<? extends Statement> c) {
            return ro();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean removeAll(Collection<?> c) {
            return ro();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean retainAll(Collection<?> c) {
            return ro();
        }

        @Override
        public Namespace setNamespace(String p, String n) {
            return ro();
        }

        @Override
        public void setNamespace(Namespace n) {
            ro();
        }

        @Override
        public Optional<Namespace> removeNamespace(String p) {
            return ro();
        }
    }

    /**
     * Internal {@link Model} adapter that delegates directly to {@link InMemoryModelStore}.
     */
    private record StoreBackedModel(InMemoryModelStore store) implements Model {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Model unmodifiable() {
            return this;
        }

        @Override
        public boolean contains(Resource s, IRI p, Value o, Resource... ctx) {
            return !store.find(s, p, o, ctx).isEmpty();
        }

        @Override
        public Iterable<Statement> getStatements(Resource s, IRI p, Value o, Resource... ctx) {
            return store.find(s, p, o, ctx);
        }

        @Override
        public boolean add(Statement stmt) {
            return store.add(stmt);
        }


        @Override
        public boolean add(Resource s, IRI p, Value o, Resource... ctx) {
            throw new UnsupportedOperationException("Use add(Statement)");
        }

        @Override
        public boolean remove(Object o) {
            return o instanceof Statement && store.remove((Statement) o);
        }

        @Override
        public boolean remove(Resource s, IRI p, Value o, Resource... ctx) {
            Set<Statement> toRemove = store.find(s, p, o, ctx);
            toRemove.forEach(store::remove);
            return !toRemove.isEmpty();
        }

        @Override
        public boolean clear(Resource... ctx) {
            if (ctx == null || ctx.length == 0) {
                int b = store.size();
                store.clear();
                return store.size() < b;
            }
            for (Resource c : ctx) store.clearContext(c);
            return true;
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public Model filter(Resource s, IRI p, Value o, Resource... ctx) {
            Set<Statement> filtered = store.find(s, p, o, ctx);
            InMemoryModelStore fs = new InMemoryModelStore();
            filtered.forEach(fs::add);
            return new StoreBackedModel(fs);
        }

        @Override
        public Set<Resource> subjects() {
            return store.getSubjects();
        }

        @Override
        public Set<IRI> predicates() {
            return store.getPredicates();
        }

        @Override
        public Set<Value> objects() {
            return store.getObjects();
        }

        @Override
        public Set<Resource> contexts() {
            return store.getContexts();
        }

        @Override
        public int size() {
            return store.size();
        }

        @Override
        public boolean isEmpty() {
            return store.size() == 0;
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof Statement && store.contains((Statement) o);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Iterator<Statement> iterator() {
            return store.getAllStatements().iterator();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Object[] toArray() {
            return store.getAllStatements().toArray();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public <T> T[] toArray(T[] a) {
            return store.getAllStatements().toArray(a);
        }

        @Override
        public boolean addAll(Collection<? extends Statement> c) {
            boolean m = false;
            for (Statement s : c) if (store.add(s)) m = true;
            return m;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean m = false;
            for (Object o : c) if (o instanceof Statement && store.remove((Statement) o)) m = true;
            return m;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean retainAll(Collection<?> c) {
            boolean m = false;
            for (Statement s : store.getAllStatements())
                if (!c.contains(s)) {
                    store.remove(s);
                    m = true;
                }
            return m;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) if (!contains(o)) return false;
            return true;
        }

        @Override
        public Optional<Namespace> getNamespace(String p) {
            return Optional.empty();
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return Collections.emptySet();
        }

        @Override
        public Namespace setNamespace(String p, String n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNamespace(Namespace n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Namespace> removeNamespace(String p) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Indexed in-memory RDF statement store.
     */
    public static final class InMemoryModelStore {

        private final Set<Statement> statements = new HashSet<>();
        private final Map<Resource, Set<Statement>> subjectIndex = new HashMap<>();
        private final Map<IRI, Set<Statement>> predicateIndex = new HashMap<>();
        private final Map<Value, Set<Statement>> objectIndex = new HashMap<>();
        private final Map<Resource, Set<Statement>> contextIndex = new HashMap<>();

        /**
         * Adds a statement to the store and updates all indexes.
         *
         * @param stmt the statement to add; ignored if {@code null}
         * @return {@code true} if the store was modified
         */
        public boolean add(Statement stmt) {
            if (stmt == null) return false;
            boolean added = statements.add(stmt);
            if (added) index(stmt);
            return added;
        }

        /**
         * Removes a statement from the store and updates all indexes.
         *
         * @param stmt the statement to remove; ignored if {@code null}
         * @return {@code true} if the store was modified
         */
        public boolean remove(Statement stmt) {
            if (stmt == null) return false;
            boolean removed = statements.remove(stmt);
            if (removed) unindex(stmt);
            return removed;
        }

        /**
         * Checks whether the store contains the given statement.
         *
         * @param stmt the statement to check
         * @return {@code true} if present
         */
        public boolean contains(Statement stmt) {
            return statements.contains(stmt);
        }

        /**
         * Returns all statements matching the given pattern.
         * Each component can be {@code null} to act as a wildcard.
         * Uses the smallest available index as a starting candidate set.
         *
         * @param s        subject filter, or {@code null} for any
         * @param p        predicate filter, or {@code null} for any
         * @param o        object filter, or {@code null} for any
         * @param contexts context filters; empty or {@code null} means any context
         * @return a set of matching statements (never {@code null})
         */
        public Set<Statement> find(Resource s, IRI p, Value o, Resource[] contexts) {
            Set<Statement> candidates = selectSmallest(s, p, o, contexts);
            if (candidates == null) return Collections.emptySet();

            Set<Statement> results = new HashSet<>(candidates);

            if (s != null) {
                Set<Statement> idx = subjectIndex.get(s);
                if (idx == null) return Collections.emptySet();
                results.retainAll(idx);
            }
            if (p != null) {
                Set<Statement> idx = predicateIndex.get(p);
                if (idx == null) return Collections.emptySet();
                results.retainAll(idx);
            }
            if (o != null) {
                Set<Statement> idx = objectIndex.get(o);
                if (idx == null) return Collections.emptySet();
                results.retainAll(idx);
            }
            if (contexts != null && contexts.length > 0) {
                Set<Statement> byCtx = new HashSet<>();
                for (Resource ctx : contexts) {
                    Set<Statement> idx = contextIndex.get(ctx);
                    if (idx != null) byCtx.addAll(idx);
                }
                results.retainAll(byCtx);
            }
            return results;
        }

        /**
         * Selects the smallest index set available for the given filters.
         * Returns {@code null} if a non-null filter has no matching index entry
         * (meaning the result is empty). Returns all statements if no filter is set.
         *
         * @param s        subject filter
         * @param p        predicate filter
         * @param o        object filter
         * @param contexts context filters
         * @return the smallest candidate set, or {@code null} if result is guaranteed empty
         */
        private Set<Statement> selectSmallest(Resource s, IRI p, Value o, Resource[] contexts) {
            Set<Statement> best = null;
            int bestSize = Integer.MAX_VALUE;
            if (s != null) {
                Set<Statement> idx = subjectIndex.get(s);
                if (idx == null) return null;
                if (idx.size() < bestSize) {
                    best = idx;
                    bestSize = idx.size();
                }
            }
            if (p != null) {
                Set<Statement> idx = predicateIndex.get(p);
                if (idx == null) return null;
                if (idx.size() < bestSize) {
                    best = idx;
                    bestSize = idx.size();
                }
            }
            if (o != null) {
                Set<Statement> idx = objectIndex.get(o);
                if (idx == null) return null;
                if (idx.size() < bestSize) {
                    best = idx;
                    bestSize = idx.size();
                }
            }
            if (contexts != null) for (Resource ctx : contexts) {
                Set<Statement> idx = contextIndex.get(ctx);
                if (idx != null && idx.size() < bestSize) {
                    best = idx;
                    bestSize = idx.size();
                }
            }
            return best != null ? best : statements;
        }

        /**
         * Returns the total number of statements in this store.
         *
         * @return the statement count
         */
        public int size() {
            return statements.size();
        }

        /**
         * Removes all statements and clears all indexes.
         */
        public void clear() {
            statements.clear();
            subjectIndex.clear();
            predicateIndex.clear();
            objectIndex.clear();
            contextIndex.clear();
        }

        /**
         * Removes all statements belonging to the given context.
         *
         * @param ctx the context to clear
         */
        public void clearContext(Resource ctx) {
            Set<Statement> ctxStmts = contextIndex.get(ctx);
            if (ctxStmts != null) new HashSet<>(ctxStmts).forEach(this::remove);
        }

        /**
         * Returns an immutable snapshot of all statements in this store.
         *
         * @return an unmodifiable copy of all statements
         */
        public Set<Statement> getAllStatements() {
            return Set.copyOf(statements);
        }

        /**
         * Returns all unique subjects currently in this store.
         *
         * @return a copy of the subject index key set
         */
        public Set<Resource> getSubjects() {
            return new HashSet<>(subjectIndex.keySet());
        }

        /**
         * Returns all unique predicates currently in this store.
         *
         * @return a copy of the predicate index key set
         */
        public Set<IRI> getPredicates() {
            return new HashSet<>(predicateIndex.keySet());
        }

        /**
         * Returns all unique objects currently in this store.
         *
         * @return a copy of the object index key set
         */
        public Set<Value> getObjects() {
            return new HashSet<>(objectIndex.keySet());
        }

        /**
         * Returns all unique contexts currently in this store.
         *
         * @return a copy of the context index key set
         */
        public Set<Resource> getContexts() {
            return new HashSet<>(contextIndex.keySet());
        }

        /**
         * Adds the given statement to all four indexes.
         *
         * @param stmt the statement to index
         */
        private void index(Statement stmt) {
            subjectIndex.computeIfAbsent(stmt.getSubject(), k -> new HashSet<>()).add(stmt);
            predicateIndex.computeIfAbsent(stmt.getPredicate(), k -> new HashSet<>()).add(stmt);
            objectIndex.computeIfAbsent(stmt.getObject(), k -> new HashSet<>()).add(stmt);
            Resource ctx = stmt.getContext();
            if (ctx != null) contextIndex.computeIfAbsent(ctx, k -> new HashSet<>()).add(stmt);
        }

        /**
         * Removes the given statement from all four indexes,
         * cleaning up empty index entries.
         *
         * @param stmt the statement to unindex
         */
        private void unindex(Statement stmt) {
            removeFromIndex(subjectIndex, stmt.getSubject(), stmt);
            removeFromIndex(predicateIndex, stmt.getPredicate(), stmt);
            removeFromIndex(objectIndex, stmt.getObject(), stmt);
            Resource ctx = stmt.getContext();
            if (ctx != null) removeFromIndex(contextIndex, ctx, stmt);
        }

        /**
         * Removes a statement from a single index entry,
         * and removes the entry itself if it becomes empty.
         *
         * @param index the index map to update
         * @param key   the index key
         * @param stmt  the statement to remove
         * @param <K>   the key type
         */
        private <K> void removeFromIndex(Map<K, Set<Statement>> index, K key, Statement stmt) {
            Set<Statement> set = index.get(key);
            if (set != null) {
                set.remove(stmt);
                if (set.isEmpty()) index.remove(key);
            }
        }
    }


    /**
     * Minimal immutable implementation of {@link Namespace}.
     *
     * @param prefix the namespace prefix
     * @param name   the namespace IRI
     */
    private record SimpleNamespace(String prefix, String name) implements Namespace {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getNamespace() {
            return name;
        }
    }


    /**
     * Creates a new {@link Builder} for {@code DataManagerBasedModel}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean transactionSupport = false;
        private IsolationLevel isolationLevel = IsolationLevel.READ_COMMITTED;

        /**
         * Enables or disables transaction support.
         *
         * @param enable {@code true} to enable transactions
         * @return this builder
         */
        public Builder withTransactions(boolean enable) {
            this.transactionSupport = enable;
            return this;
        }

        /**
         * Sets the default isolation level for transactions.
         *
         * @param level the isolation level to use
         * @return this builder
         */
        public Builder withIsolationLevel(IsolationLevel level) {
            this.isolationLevel = level;
            return this;
        }

        /**
         * Builds a new {@link DataManagerBasedModel} without initializing the lifecycle.
         *
         * @return a new model instance
         */
        public DataManagerBasedModel build() {
            return new DataManagerBasedModel(this);
        }

        /**
         * Builds a new {@link DataManagerBasedModel} and initializes its lifecycle
         * with the default configuration.
         *
         * @return a new initialized model instance
         * @throws DataManagerException if lifecycle initialization fails
         */
        public DataManagerBasedModel buildAndInit() throws DataManagerException {
            DataManagerBasedModel model = build();
            model.getLifecycle().initialize(DataManagerConfig.builder().build());
            return model;
        }
    }

    /**
     * Returns a string representation of this model including its size,
     * number of contexts, and transaction support status.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "DataManagerBasedModel{size=" + store.size() + ", contexts=" + store.getContexts().size() + ", transactions=" + transactionManager.supportsTransactions() + '}';
    }
}