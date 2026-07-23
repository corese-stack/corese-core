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

}