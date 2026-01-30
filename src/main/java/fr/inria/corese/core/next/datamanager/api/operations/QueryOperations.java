package fr.inria.corese.core.next.datamanager.api.operations;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;

import java.util.stream.Stream;

/**
 * Query operations for reading statements.
 * Provides modern Stream-based API for querying the model.
 */
public interface QueryOperations {

    /**
     * Queries statements matching the given pattern.
     *
     * @param pattern Statement pattern to match
     * @return Stream of matching statements (must be closed)
     * @throws DataManagerException     if query fails
     * @throws IllegalArgumentException if pattern is null
     */
    Stream<Statement> query(StatementPattern pattern) throws DataManagerException;

    /**
     * Counts statements matching the given pattern.
     * More efficient than query().count() as it doesn't need to load statements.
     *
     * @param pattern Statement pattern to match
     * @return Number of matching statements
     * @throws DataManagerException     if count fails
     * @throws IllegalArgumentException if pattern is null
     */
    long count(StatementPattern pattern) throws DataManagerException;

    /**
     * Checks if at least one statement matches the given pattern.
     * More efficient than query().findAny() as it can stop after first match.
     *
     * @param pattern Statement pattern to match
     * @return true if at least one statement matches
     * @throws DataManagerException     if check fails
     * @throws IllegalArgumentException if pattern is null
     */
    boolean exists(StatementPattern pattern) throws DataManagerException;

    /**
     * Filters the model according to the given statement pattern.
     * Returns a new Model containing all statements matching the pattern.
     *
     * @param pattern Statement pattern to match
     * @return A new Model containing matching statements
     * @throws DataManagerException     if filter fails
     * @throws IllegalArgumentException if pattern is null
     */
    Model filter(StatementPattern pattern) throws DataManagerException;
}