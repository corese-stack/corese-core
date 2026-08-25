package fr.inria.corese.core.next.data.api.model;

import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

/**
 * A mutable, set-based collection of RDF statements.
 *
 * <p>Each statement belongs either to the default graph (a {@code null}
 * context) or to one named graph. Operations accepting several contexts apply
 * independently to each selected graph. Prefix declarations are metadata used
 * by parsers and serializers; they are not RDF statements.</p>
 *
 * <p>Models are not required to support Java object serialization. Use an RDF
 * serializer when data must be persisted or exchanged.</p>
 */
public interface Model extends Set<Statement> {

    /**
     * Returns the namespaces defined in this model.
     */
    Set<Namespace> getNamespaces();

    /**
     * Returns the namespace registered for {@code prefix}, if any.
     */
    default Optional<Namespace> getNamespace(String prefix) {
        if (prefix == null) {
            return Optional.empty();
        }
        return getNamespaces().stream()
                .filter(namespace -> prefix.equals(namespace.getPrefix()))
                .findFirst();
    }

    /**
     * @return a live, read-only view of this model; query operations remain
     *         available and mutation attempts fail
     */
    Model unmodifiable();

    /**
     *
     * @param prefix a prefix for the namespace. It should be unique in the model.
     * @param name   the IRI of the namespace. It should be a valid IRI.
     * @return the new Namespace created with the given prefix and name.
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    Namespace setNamespace(String prefix, String name);

    /**
     * Set the namespace of this model. The prefix should be unique in the model.
     *
     * @param namespace the namespace object to be added.
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    void setNamespace(Namespace namespace);

    /**
     * @param prefix the prefix of the namespace to be removed.
     * @return the removed namespace, or an empty Optional if no namespace with the
     *         given prefix was found.
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    Optional<Namespace> removeNamespace(String prefix);

    /**
     * Check if a statement is present in the model. Can be used to query for
     * statement pattern using {@code null} values.
     *
     * @param subj     a Resource, subject of the statement. Can be {@code null} to
     *                 match any subject.
     * @param pred     an IRI, predicate of the statement. Can be {@code null} to
     *                 match any predicate.
     * @param obj      a Value, object of the statement. Can be {@code null} to
     *                 match any object.
     * @param contexts contexts to match; no contexts or a {@code null} array means
     *                 every graph, while an explicit {@code null} element selects
     *                 the default graph
     * @return true if a statement with the associated context is in the model,
     *         false otherwise.
     */
    boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * Adds a statement to the model with optional context(s).
     * If multiple contexts are provided, the statement is added once per context.
     *
     * @param subj     the subject of the statement (must not be {@code null})
     * @param pred     the predicate of the statement (must not be {@code null})
     * @param obj      the object of the statement (must not be {@code null})
     * @param contexts optional contexts in which to add the statement;
     *                 may be {@code null} or empty to add to the default graph
     * @return {@code true} if the model was modified, {@code false} otherwise
     * @throws UnsupportedOperationException if the model is unmodifiable
     * @throws IllegalArgumentException    if {@code subj}, {@code pred}, or
     *                                     {@code obj} is {@code null}
     */
    boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * Remove statements from the model according to their context. If no context is
     * given, all statements are removed, regardless of context.
     *
     * @param context contexts to clear; no contexts or a {@code null} array means
     *                every graph, while an explicit {@code null} element selects
     *                the default graph
     * @return true if any statement was removed, false if none were present.
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    boolean clear(Resource... context);

    /**
     * Remove statements from the model. If no context is given, all corresponding
     * statements are removed, regardless of context.
     *
     * @param subj     a Resource, subject of the statement. Can be {@code null}.
     * @param pred     an IRI, predicate of the statement. Can be {@code null}.
     * @param obj      a Value, object of the statement. Can be {@code null}.
     * @param contexts contexts to match; no contexts or a {@code null} array means
     *                 every graph, while an explicit {@code null} element selects
     *                 the default graph
     * @return true if any statement was removed, false if none were present.
     * @throws UnsupportedOperationException if the model is unmodifiable
     */
    boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * The returned iterable is read-only when this model is unmodifiable.
     *
     * @param subj     a Resource, subject of the statement. Can be {@code null} to
     *                 match any subject.
     * @param pred     an IRI, predicate of the statement. Can be {@code null} to
     *                 match any predicate.
     * @param obj      a Value, object of the statement. Can be {@code null} to
     *                 match any object.
     * @param contexts contexts to match; no contexts or a {@code null} array means
     *                 every graph, while an explicit {@code null} element selects
     *                 the default graph
     * @return an iterator on a selection of statements.
     */
    Iterable<Statement> getStatements(Resource subj, IRI pred, Value obj,
            Resource... contexts);

    /**
     * Filter the model according to the given statement pattern. The filter is
     * inclusive, meaning that if a statement matches the pattern, it will be
     * included in the result.
     *
     * @param subj     a Resource, subject of the statement. Can be {@code null} to
     *                 match any subject.
     * @param pred     an IRI, predicate of the statement. Can be {@code null} to
     *                 match any predicate.
     * @param obj      a Value, object of the statement. Can be {@code null} to
     *                 match any object.
     * @param contexts contexts to match; no contexts or a {@code null} array means
     *                 every graph, while an explicit {@code null} element selects
     *                 the default graph
     * @return a new Model containing all statements matching the given pattern.
     */
    Model filter(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * Returns a live set view of resources used as subjects. Removing a term
     * removes every statement using it; adding a term is unsupported.
     *
     * @return the subjects in this model
     */
    Set<Resource> subjects();

    /**
     * Returns a live set view of IRIs used as predicates. Removing a term
     * removes every statement using it; adding a term is unsupported.
     *
     * @return the predicates in this model
     */
    Set<IRI> predicates();

    /**
     * Returns a live set view of values used as objects. Removing a term removes
     * every statement using it; adding a term is unsupported.
     *
     * @return the objects in this model
     */
    Set<Value> objects();

    /**
     * Returns a live set view of contexts. Removing a context clears every
     * statement in that graph; adding a context is unsupported.
     *
     * @return contexts used by the model, including {@code null} when the
     *         default graph contains statements
     */
    Set<Resource> contexts();
}
