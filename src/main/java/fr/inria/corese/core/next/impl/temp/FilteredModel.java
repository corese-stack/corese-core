package fr.inria.corese.core.next.impl.temp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.base.model.AbstractModel;

public abstract class FilteredModel extends AbstractModel {

    // --- Fields ---

    // The wrapped RDF model to filter.
    private final Model model;

    // Serialization identifier.
    private static final long serialVersionUID = 1L;

    // RDF filter pattern: subject, predicate, object, and contexts.
    protected Resource subjectFilter;
    protected IRI predicateFilter;
    protected Value objectFilter;
    protected Resource[] contextFilters;

    // --- Constructors ---

    /**
     * Constructs a filtered view over the given model using subject, predicate,
     * object, and context filters.
     *
     * @param model           the underlying RDF model to wrap (must not be null)
     * @param subjectFilter   the subject to match, or null to match any subject
     * @param predicateFilter the predicate to match, or null to match any predicate
     * @param objectFilter    the object to match, or null to match any object
     * @param contextFilters  the contexts to match; must not be null (can be empty)
     * @throws NullPointerException if model or contextFilters is null
     */
    protected FilteredModel(AbstractModel model, Resource subjectFilter, IRI predicateFilter, Value objectFilter,
            Resource... contextFilters) {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(contextFilters, "Context filters cannot be null");

        this.model = model;
        this.subjectFilter = subjectFilter;
        this.predicateFilter = predicateFilter;
        this.objectFilter = objectFilter;
        this.contextFilters = contextFilters;
    }

    // --- Public Methods ---

    // --- Add functions ---

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
        if (subject == null) {
            subject = subjectFilter;
        }
        if (predicate == null) {
            predicate = predicateFilter;
        }
        if (object == null) {
            object = objectFilter;
        }
        if (contexts == null || contexts.length == 0) {
            contexts = contextFilters;
        }

        if (!matchesStatement(subject, predicate, object, contexts)) {
            throw new IllegalArgumentException(
                    String.format("Cannot add statement (%s %s %s %s): it does not match the current view filters",
                            subject, predicate, object, Arrays.toString(contexts)));
        }

        return model.add(subject, predicate, object, contexts);
    }

    // --- Contains functions ---

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
        if (subject == null) {
            subject = subjectFilter;
        }
        if (predicate == null) {
            predicate = predicateFilter;
        }
        if (object == null) {
            object = objectFilter;
        }
        if (contexts != null && contexts.length == 0) {
            contexts = contextFilters;
        }

        if (!matchesStatement(subject, predicate, object, contexts)) {
            return false;
        }

        return model.contains(subject, predicate, object, contexts);
    }

    // --- Remove functions ---

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
        if (subject == null) {
            subject = subjectFilter;
        }
        if (predicate == null) {
            predicate = predicateFilter;
        }
        if (object == null) {
            object = objectFilter;
        }
        if (contexts != null && contexts.length == 0) {
            contexts = contextFilters;
        }

        if (!matchesStatement(subject, predicate, object, contexts)) {
            return false;
        }

        return model.remove(subject, predicate, object, contexts);
    }

    // --- Namespace functions ---

    @Override
    public Optional<Namespace> getNamespace(String prefix) {
        return model.getNamespace(prefix);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return model.getNamespaces();
    }

    @Override
    public Namespace setNamespace(String prefix, String name) {
        return model.setNamespace(prefix, name);
    }

    @Override
    public void setNamespace(Namespace namespace) {
        model.setNamespace(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return model.removeNamespace(prefix);
    }

    // --- Filter functions ---

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
        if (subject == null) {
            subject = subjectFilter;
        }
        if (predicate == null) {
            predicate = predicateFilter;
        }
        if (object == null) {
            object = objectFilter;
        }
        if (contexts != null && contexts.length == 0) {
            contexts = contextFilters;
        }

        if (!matchesStatement(subject, predicate, object, contexts)) {
            return new EmptyModel(model);
        }

        return model.filter(subject, predicate, object, contexts);
    }

    // --- Other functions ---

    @Override
    public final void removeTermIteration(Iterator<Statement> iterator, Resource subject, IRI predicate, Value object,
            Resource... contexts) {
        if (subject == null) {
            subject = subjectFilter;
        }
        if (predicate == null) {
            predicate = predicateFilter;
        }
        if (object == null) {
            object = objectFilter;
        }
        if (contexts != null && contexts.length == 0) {
            contexts = contextFilters;
        }

        if (!matchesStatement(subject, predicate, object, contexts)) {
            throw new IllegalStateException(
                    String.format("Cannot remove statement (%s %s %s %s): it does not match the current view filters",
                            subject, predicate, object, Arrays.toString(contexts)));
        }

        removeFilteredTermIteration(iterator, subject, predicate, object, contexts);
    }

    /**
     * Called when a term is removed from an iterator view that respects statement
     * filters.
     *
     * @param iterator  the live iterator (never null)
     * @param subject   the subject, or null
     * @param predicate the predicate, or null
     * @param object    the object, or null
     * @param contexts  the contexts, possibly empty
     */
    protected abstract void removeFilteredTermIteration(
            Iterator<Statement> iterator,
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts);

    @Override
    public int size() {
        Iterator<Statement> iterator = iterator();
        try {
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            return count;
        } finally {
            closeIterator(iterator);
        }
    }

    // --- Private Methods ---

    /**
     * Determines whether a statement matches the given subject, predicate, object,
     * and context filters.
     *
     * @param subject        The statement subject.
     * @param predicate      The statement predicate.
     * @param object         The statement object.
     * @param actualContexts The statement contexts.
     * @return true if all filters match, false otherwise.
     */
    private boolean matchesStatement(Resource subject, IRI predicate, Value object, Resource... actualContexts) {
        if (subjectFilter != null && !subjectFilter.equals(subject)) {
            return false;
        }
        if (predicateFilter != null && !predicateFilter.equals(predicate)) {
            return false;
        }
        if (objectFilter != null && !objectFilter.equals(object)) {
            return false;
        }
        if (!matchContexts(actualContexts, contextFilters)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if all actual contexts match the expected context filters.
     *
     * @param actualContexts   The contexts from the statement being tested.
     * @param expectedContexts The filters to match against.
     * @return true if all actual contexts match the expected filters.
     */
    private boolean matchContexts(Resource[] actualContexts, Resource... expectedContexts) {
        Objects.requireNonNull(actualContexts, "actualContexts must not be null");

        if (actualContexts.length > 0) {
            for (Resource ctx : actualContexts) {
                if (!matchSingleContext(ctx, expectedContexts)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether a single context matches one of the expected contexts.
     *
     * @param actualContext    A single context from the statement.
     * @param expectedContexts The list of allowed contexts.
     * @return true if the context matches, false otherwise.
     */
    private boolean matchSingleContext(Resource actualContext, Resource... expectedContexts) {
        Objects.requireNonNull(expectedContexts, "expectedContexts must not be null");

        if (expectedContexts.length == 0) {
            // No context filter specified, match any context
            return true;
        }

        for (Resource expected : expectedContexts) {
            if (expected == null && actualContext == null) {
                return true;
            }
            if (expected != null && expected.equals(actualContext)) {
                return true;
            }
        }

        return false;
    }

}
