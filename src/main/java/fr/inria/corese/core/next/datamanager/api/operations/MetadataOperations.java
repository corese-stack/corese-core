package fr.inria.corese.core.next.datamanager.api.operations;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.ModelStatistics;

import java.util.Set;

/**
 * Metadata operations for the Model.
 * Provides access to model structure information (subjects, predicates, objects, contexts, statistics).
 */
public interface MetadataOperations {

    /**
     * Returns all predicates in the model.
     *
     * @return Set of predicates (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<IRI> getPredicates() throws DataManagerException;

    /**
     * Returns all subject resources in the model.
     *
     * @return Set of subject resources (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Resource> getSubjects() throws DataManagerException;

    /**
     * Returns all object values in the model.
     *
     * @return Set of object values (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Value> getObjects() throws DataManagerException;

    /**
     * Returns all contexts (named graphs) in the model.
     *
     * @return Set of context resources (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Resource> getContexts() throws DataManagerException;

    /**
     * Returns statistics about the model content and structure.
     *
     * @return Model statistics
     * @throws DataManagerException if statistics cannot be computed
     */
    ModelStatistics getStatistics() throws DataManagerException;
}