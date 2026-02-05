package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.datamanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.support.model.ModelStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * Implementation of metadata operations for CoreseModelDataManager.
 * Provides access to model statistics and structural information.
 */
public class MetadataOperationsImpl implements MetadataOperations {

    private static final Logger logger = LoggerFactory.getLogger(MetadataOperationsImpl.class);

    /**
     * The underlying Corese model instance.
     */
    private final Model model;

    /**
     * Constructs a new metadata operations handler for the specified model.
     *
     * @param model the Corese Model to query; must not be null.
     * @throws IllegalArgumentException if the provided model is null.
     */
    public MetadataOperationsImpl(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
    }

    /**
     * Retrieves all unique predicates used within the model.
     *
     * @return an unmodifiable Set of predicate IRIs.
     * @throws DataManagerException if the metadata retrieval fails.
     */
    @Override
    public Set<IRI> getPredicates() throws DataManagerException {
        try {
            logger.debug("Getting predicates from model");

            Set<IRI> predicates = model.predicates();

            logger.debug("Found {} unique predicates", predicates.size());
            return Collections.unmodifiableSet(predicates);

        } catch (Exception e) {
            logger.error("Failed to get predicates", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get predicates: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Retrieves all unique subject resources within the model.
     *
     * @return an unmodifiable Set of subject resources.
     * @throws DataManagerException if the subject retrieval fails.
     */
    @Override
    public Set<Resource> getSubjects() throws DataManagerException {
        try {
            logger.debug("Getting subjects from model");

            Set<Resource> subjects = model.subjects();

            logger.debug("Found {} subjects", subjects.size());
            return Collections.unmodifiableSet(subjects);

        } catch (Exception e) {
            logger.error("Failed to get subjects", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get subjects: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Retrieves all unique object values within the model.
     *
     * @return an unmodifiable Set of object values.
     * @throws DataManagerException if the object retrieval fails.
     */
    @Override
    public Set<Value> getObjects() throws DataManagerException {
        try {
            logger.debug("Getting objects from model");

            Set<Value> objects = model.objects();

            logger.debug("Found {} objects", objects.size());
            return Collections.unmodifiableSet(objects);

        } catch (Exception e) {
            logger.error("Failed to get objects", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get objects: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Retrieves all named graph identifiers (contexts) currently defined in the model.
     *
     * @return an unmodifiable Set of context resources.
     * @throws DataManagerException if the context retrieval fails.
     */
    @Override
    public Set<Resource> getContexts() throws DataManagerException {
        try {
            logger.debug("Getting all contexts");

            Set<Resource> contexts = model.contexts();

            logger.debug("Found {} contexts", contexts.size());
            return Collections.unmodifiableSet(contexts);

        } catch (Exception e) {
            logger.error("Failed to get contexts", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get contexts: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Collects and returns general statistics about the model size and composition.
     *
     * @return a {@link ModelStatistics} object containing counts for statements, subjects, predicates, objects, and contexts.
     * @throws DataManagerException if statistics collection fails.
     */
    @Override
    public ModelStatistics getStatistics() throws DataManagerException {
        try {
            logger.debug("Collecting model statistics");

            ModelStatistics stats = getModelStatistics();

            logger.debug("Statistics collected: {}", stats);
            return stats;

        } catch (Exception e) {
            logger.error("Failed to collect statistics", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to collect statistics: " + e.getMessage(),
                    e
            );
        }
    }

    private ModelStatistics getModelStatistics() throws DataManagerException {
        long statementCount = model.size();
        long subjectCount = getSubjects().size();
        long predicateCount = getPredicates().size();
        long objectCount = getObjects().size();
        long contextCount = getContexts().size();

        // Create ModelStatistics using Record constructor
        return new ModelStatistics(
                statementCount,
                subjectCount,
                predicateCount,
                objectCount,
                contextCount
        );
    }
}