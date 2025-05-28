package fr.inria.corese.core.next.api;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;

/**
 * This class represents an RDF model, a set of statements.
 * This is the central class to handle RDF data.
 * Statements can have zero or several resources as contexts to represent the
 * different graphs in the model.
 * A model also handles a set of namespaces. There can be different prefixes for
 * the same namespace, although it is ill-advised, how it affects serialization
 * is implementation-dependant. There cannot be two namespaces with the same
 * prefix.
 * Statements can have zero or several resources as contexts, representing
 * different named graphs within the modela
 */
public interface Model extends Set<Statement>, Serializable, NamespaceAware {

    /**
     * @return a "read-only" view of this model. "query" operations are possible,
     *         such as {@code filter()} on {@code contains()} but modifications will
     *         throw {@link IncorrectOperationException}.
     */
    Model unmodifiable();

    /**
     *
     * @param prefix a prefix for the namespace. It should be unique in the model.
     * @param name   the IRI of the namespace. It should be a valid IRI.
     * @return the new Namespace created with the given prefix and name.
     * @throws IncorrectOperationException if the Model is unmodifiable.
     */
    Namespace setNamespace(String prefix, String name);

    /**
     * Set the namespace of this model. The prefix should be unique in the model.
     * 
     * @param namespace the namespace object to be added.
     * @throws IncorrectOperationException if the Model is unmodifiable.
     */
    void setNamespace(Namespace namespace);

    /**
     * @param prefix the prefix of the namespace to be removed.
     * @return the removed namespace, or an empty Optional if no namespace with the
     *         given prefix was found.
     * @throws IncorrectOperationException if the Model is unmodifiable.
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
     * @param contexts any Resource, context of the statement. Optional parameter.
     *                 Can be {@code null} to match any context.
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
     * @throws IncorrectOperationException if the model is unmodifiable
     * @throws IllegalArgumentException    if {@code subj}, {@code pred}, or
     *                                     {@code obj} is {@code null}
     */
    boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * Remove statements from the model according to their context. If no context is
     * given, all statements are removed, regardless of context.
     * 
     * @param context a Resource, context of the statement. Optional parameter. Can
     *                be {@code null} to match any context.
     * @return true if any statement was removed, false if none were present.
     * @throws IncorrectOperationException if the Model is unmodifiable.
     */
    boolean clear(Resource... context);

    /**
     * Remove statements from the model. If no context is given, all corresponding
     * statements are removed, regardless of context.
     * 
     * @param subj     a Resource, subject of the statement. Can be {@code null}.
     * @param pred     an IRI, predicate of the statement. Can be {@code null}.
     * @param obj      a Value, object of the statement. Can be {@code null}.
     * @param contexts any Resource, context of the statement. Optional parameter.
     *                 Can be {@code null} to match any context.
     * @return true if any statement was removed, false if none were present.
     * @throws IncorrectOperationException if the Model is unmodifiable.
     */
    boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * The returned iterator must throw {@link IncorrectOperationException} if the
     * model is unmodifiable and a modification is attempted.
     * 
     * @param subj     a Resource, subject of the statement. Can be {@code null} to
     *                 match any subject.
     * @param pred     a an IRI, predicate of the statement. Can be {@code null} to
     *                 match any predicate.
     * @param obj      a Value, object of the statement. Can be {@code null} to
     *                 match any object.
     * @param contexts any Resource, context of the statement. Optional parameter.
     *                 Can be {@code null} to match any context.
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
     * @param contexts any Resource, context of the statement. Optional parameter.
     *                 Can be {@code null} to match any context.
     * @return a new Model containing all statements matching the given pattern.
     */
    Model filter(Resource subj, IRI pred, Value obj, Resource... contexts);

    /**
     * @return the set of resources used as subjects in the statements of the model.
     */
    Set<Resource> subjects();

    /**
     * @return the set of IRIs used as predicates in the statements of the model.
     */
    Set<IRI> predicates();

    /**
     * @return the set of values used as objects in the statements of the model.
     */
    Set<Value> objects();

    /**
     * @return the set of resources used as contexts in the statements of the model.
     */
    Set<Resource> contexts();
}
