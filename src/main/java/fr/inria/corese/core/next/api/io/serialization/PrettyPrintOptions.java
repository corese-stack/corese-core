package fr.inria.corese.core.next.api.io.serialization;

public interface PrettyPrintOptions {

    /**
     * Returns the string used for indentation when pretty-printing.
     *
     * @return The indentation string.
     */
    String getIndent();

    /**
     * Checks if human-readable formatting (pretty-printing) is enabled.
     *
     * @return {@code true} if pretty-printing is enabled, {@code false} otherwise.
     */
    boolean prettyPrint();

    /**
     * Returns the maximum desired line length before the serializer attempts to break lines.
     *
     * @return The maximum line length.
     */
    int getMaxLineLength();

    /**
     * Checks if subjects should be sorted alphabetically in the output.
     *
     * @return {@code true} if subject sorting is enabled, {@code false} otherwise.
     */
    boolean sortSubjects();

    /**
     * Checks if predicates should be sorted alphabetically within a subject group.
     *
     * @return {@code true} if predicate sorting is enabled, {@code false} otherwise.
     */
    boolean sortPredicates();
}
