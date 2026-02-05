package fr.inria.corese.core.next.datamanager.api.support.model;

/**
 * Statistics about a Model's content and structure.
 *
 * @param statementCount Total number of statements
 * @param subjectCount   Number of unique subjects
 * @param predicateCount Number of unique predicates
 * @param objectCount    Number of unique objects
 * @param contextCount   Number of contexts (named graphs)
 */
public record ModelStatistics(
        long statementCount,
        long subjectCount,
        long predicateCount,
        long objectCount,
        long contextCount
) {

    /**
     * Compact constructor with validation.
     */
    public ModelStatistics {
        if (statementCount < 0) {
            throw new IllegalArgumentException("Statement count cannot be negative");
        }
        if (subjectCount < 0) {
            throw new IllegalArgumentException("Subject count cannot be negative");
        }
        if (predicateCount < 0) {
            throw new IllegalArgumentException("Predicate count cannot be negative");
        }
        if (objectCount < 0) {
            throw new IllegalArgumentException("Object count cannot be negative");
        }
        if (contextCount < 0) {
            throw new IllegalArgumentException("Context count cannot be negative");
        }
    }

    /**
     * Calculates the average statements per subject.
     *
     * @return Average or 0.0 if no subjects
     */
    public double getAverageStatementsPerSubject() {
        return subjectCount > 0 ? (double) statementCount / subjectCount : 0.0;
    }

    /**
     * Calculates the average statements per predicate.
     *
     * @return Average or 0.0 if no predicates
     */
    public double getAverageStatementsPerPredicate() {
        return predicateCount > 0 ? (double) statementCount / predicateCount : 0.0;
    }

    /**
     * Calculates the average statements per context.
     *
     * @return Average or 0.0 if no contexts
     */
    public double getAverageStatementsPerContext() {
        return contextCount > 0 ? (double) statementCount / contextCount : 0.0;
    }

    /**
     * Checks if the model is empty.
     *
     * @return true if no statements
     */
    public boolean isEmpty() {
        return statementCount == 0;
    }
}