package fr.inria.corese.core.next.storagemanager.api.support.model;

/**
 * Statistics about the content and structure of an RDF storage backend.
 *
 */
public record StorageStatistics(
        long statementCount,
        long subjectCount,
        long predicateCount,
        long objectCount,
        long contextCount
) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any count is negative
     */
    public StorageStatistics {
        if (statementCount < 0 || subjectCount < 0 || predicateCount < 0 ||
                objectCount < 0 || contextCount < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
    }

    /**
     * Checks whether the storage is empty (contains no statements).
     *
     * @return {@code true} if {@code statementCount == 0}
     */
    public boolean isEmpty() {
        return statementCount == 0;
    }

    /**
     * Calculates the average number of statements per unique subject.
     *
     * @return the average statements per subject, or {@code 0.0} if no subjects exist
     */
    public double getAverageStatementsPerSubject() {
        return subjectCount > 0 ? (double) statementCount / subjectCount : 0.0;
    }

    /**
     * Calculates the average number of statements per unique predicate.
     *
     *
     * @return the average statements per predicate, or {@code 0.0} if no predicates exist
     */
    public double getAverageStatementsPerPredicate() {
        return predicateCount > 0 ? (double) statementCount / predicateCount : 0.0;
    }

    /**
     * Calculates the average number of statements per named graph (context).
     *
     *
     * @return the average statements per context, or {@code 0.0} if no contexts exist
     */
    public double getAverageStatementsPerContext() {
        return contextCount > 0 ? (double) statementCount / contextCount : 0.0;
    }
}