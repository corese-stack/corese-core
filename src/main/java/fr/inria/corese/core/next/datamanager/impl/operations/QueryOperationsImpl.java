package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.datamanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of query operations for CoreseModelDataManager.
 * Converts Model's Iterable-based API to modern Stream-based API.
 */
public class QueryOperationsImpl implements QueryOperations {

    private static final Logger logger = LoggerFactory.getLogger(QueryOperationsImpl.class);

    /** The underlying Corese model instance to be queried. */
    private final Model model;

    /**
     * Constructs query operations for a specific model.
     *
     * @param model the Corese Model instance to query; must not be null.
     * @throws IllegalArgumentException if the provided model is null.
     */
    public QueryOperationsImpl(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
    }

    /**
     * Executes a query based on the provided statement pattern and returns a stream of results.
     *
     * @param pattern the pattern defining filters for subject, predicate, object, and contexts.
     * @return a {@link Stream} of {@link Statement} objects matching the pattern.
     * @throws DataManagerException if the query fails at the model level.
     * @throws IllegalArgumentException if the pattern is null.
     */
    @Override
    public Stream<Statement> query(StatementPattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Querying with pattern: {}", pattern);

            // Extract pattern components
            Resource subject = pattern.getSubject();
            IRI predicate = pattern.getPredicate();
            Value object = pattern.getObject();
            Resource[] contexts = pattern.getContexts();

            // Obtain the iterator from the Model
            Iterable<Statement> iterable = model.getStatements(subject, predicate, object, contexts);

            // Convert the Iterable to a Stream for modern API usage
            Stream<Statement> stream = StreamSupport.stream(iterable.spliterator(), false);

            logger.debug("Query executed successfully");
            return stream;

        } catch (Exception e) {
            logger.error("Failed to execute query with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to execute query: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Counts the number of statements matching the provided pattern.
     *
     * @param pattern the pattern defining the statements to count.
     * @return the number of matching statements.
     * @throws DataManagerException if the counting operation fails.
     */
    @Override
    public long count(StatementPattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Counting with pattern: {}", pattern);

            // General case: perform a query and count the stream elements
            try (Stream<Statement> stream = query(pattern)) {
                long count = stream.count();
                logger.debug("Count (general iteration): {}", count);
                return count;
            }

        } catch (DataManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to count statements: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Checks if at least one statement matches the provided pattern.
     *
     * @param pattern the pattern defining the search criteria.
     * @return {@code true} if at least one matching statement exists, {@code false} otherwise.
     * @throws DataManagerException if the existence check fails.
     */
    @Override
    public boolean exists(StatementPattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Checking existence with pattern: {}", pattern);

            Resource subject = pattern.getSubject();
            IRI predicate = pattern.getPredicate();
            Value object = pattern.getObject();
            Resource[] contexts = pattern.getContexts();

            // Use Model's contains method for efficient existence check
            boolean exists = model.contains(subject, predicate, object, contexts);

            logger.debug("Exists: {}", exists);
            return exists;

        } catch (Exception e) {
            logger.error("Failed to check existence with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to check statement existence: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Filters the model according to the given statement pattern.
     *
     * @param pattern the pattern defining the filter criteria.
     * @return a new Model containing all statements matching the pattern.
     * @throws DataManagerException if the filter operation fails.
     */
    @Override
    public Model filter(StatementPattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Filtering model with pattern: {}", pattern);

            Resource subject = pattern.getSubject();
            IRI predicate = pattern.getPredicate();
            Value object = pattern.getObject();
            Resource[] contexts = pattern.getContexts();

            Model filtered = model.filter(subject, predicate, object, contexts);

            logger.debug("Filter completed, result size: {}", filtered.size());
            return filtered;

        } catch (Exception e) {
            logger.error("Failed to filter model with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to filter model: " + e.getMessage(),
                    e
            );
        }
    }
}