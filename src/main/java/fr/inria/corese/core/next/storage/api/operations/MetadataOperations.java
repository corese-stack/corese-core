package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;

import java.util.Optional;
import java.util.Set;

/**
 * Structural summaries and cardinality statistics exposed by an RDF storage backend.
 *
 * <h2>Usage &amp; Performance Considerations</h2>
 * <ul>
 *   <li><strong>Materialized Sets:</strong> Methods such as {@link #getPredicates()}, {@link #getSubjects()},
 *       and {@link #getObjects()} return fully materialized, unmodifiable sets. On very large stores,
 *       calling {@code getSubjects()} or {@code getObjects()} can incur significant heap allocation overhead.</li>
 *   <li><strong>Cost-Based Optimization (CBO):</strong> {@link #getStatistics()} and
 *       {@link #findPredicateStatistics(IRI)} provide exact or sampled cardinality metrics
 *       utilized by query planners to reorder triple join patterns efficiently.</li>
 *   <li><strong>Named Graph Registry:</strong> {@link #getContexts()} returns strictly the identifiers
 *       of named graphs; the default graph is not considered a named context and is not part of this set.</li>
 * </ul>
 */
public interface MetadataOperations {

    /**
     * Returns all predicates in the model.
     *
     * @return Set of predicates (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<IRI> getPredicates() throws StorageException;

    /**
     * Returns all subject resources in the model.
     *
     * @return Set of subject resources (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Resource> getSubjects() throws StorageException;

    /**
     * Returns all object values in the model.
     *
     * @return Set of object values (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Value> getObjects() throws StorageException;

    /**
     * Returns all named graph identifiers. The default graph is not a named
     * context and is therefore not included.
     *
     * @return Set of context resources (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Resource> getContexts() throws StorageException;

    /**
     * Returns statistics about the model content and structure.
     *
     * @return Model statistics
     * @throws StorageException if statistics cannot be computed
     */
    StorageStatistics getStatistics() throws StorageException;

    /**
     * Returns statistics about a specific predicate, or {@code null} if unavailable.
     *
     * @param predicate the predicate IRI to inspect
     * @return predicate statistics, or {@code null} if unavailable or not tracked
     * @throws StorageException if statistics cannot be computed
     */
    default PredicateStatistics getPredicateStatistics(IRI predicate) throws StorageException {
        return null;
    }

    /**
     * Returns statistics about a specific predicate wrapped in an {@link Optional}.
     *
     * <p>This method provides a modern, type-safe alternative to {@link #getPredicateStatistics(IRI)}
     * that avoids returning raw {@code null} values.</p>
     *
     * @param predicate the predicate IRI to inspect (must not be {@code null})
     * @return an {@link Optional} containing the predicate statistics if available, or {@link Optional#empty()}
     * @throws StorageException if statistics cannot be computed
     */
    default Optional<PredicateStatistics> findPredicateStatistics(IRI predicate) throws StorageException {
        return Optional.ofNullable(getPredicateStatistics(predicate));
    }
}
