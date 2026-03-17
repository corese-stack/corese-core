package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Sort direction used in a SPARQL {@code ORDER BY} clause.
 */
public enum OrderDirection {

    /**
     * Ascending order: smallest values first.
     */
    ASC,

    /** Descending order: largest values first. */
    DESC
}