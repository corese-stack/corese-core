package fr.inria.corese.core.next.storage.api.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import java.util.Objects;

/**
 * Statistics about a specific predicate within an RDF storage backend.
 *
 * @param predicate the predicate IRI
 * @param count total number of statements with this predicate
 * @param distinctSubjects number of distinct subjects with this predicate
 * @param distinctObjects number of distinct objects with this predicate
 */
public record PredicateStatistics(
        IRI predicate,
        long count,
        long distinctSubjects,
        long distinctObjects
) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws IllegalArgumentException if any count is negative
     */
    public PredicateStatistics {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        if (count < 0 || distinctSubjects < 0 || distinctObjects < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
    }
}
