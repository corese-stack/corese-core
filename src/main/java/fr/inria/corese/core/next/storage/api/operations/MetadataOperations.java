package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;

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
     * @throws StorageException if query fails
     */
    Set<IRI> getPredicates() throws StorageException;

    /**
     * Returns all subject resources in the model.
     *
     * @return Set of subject resources (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Resource> getSubjects() throws StorageException;

    /**
     * Returns all object values in the model.
     *
     * @return Set of object values (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Value> getObjects() throws StorageException;

    /**
     * Returns all contexts (named graphs) in the model.
     *
     * @return Set of context resources (unmodifiable)
     * @throws StorageException if query fails
     */
    Set<Resource> getContexts() throws StorageException;

    /**
     * Returns statistics about the model content and structure.
     *
     * @return Model statistics
     * @throws StorageException if statistics cannot be computed
     */
    StorageStatistics getStatistics() throws StorageException;
}