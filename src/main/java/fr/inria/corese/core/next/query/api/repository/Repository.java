package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;

/**
 * A Corese repository that contains RDF data that can be queried and updated.
 *
 * @see RepositoryConnection
 */
public interface Repository {


    /**
     * Initializes the repository, making it ready for use.
     *
     * @throws RepositoryException   if initialization fails (e.g., I/O errors, corrupt data)
     * @throws IllegalStateException if already initialized
     */
    void init() throws RepositoryException;

    /**
     * Checks whether the repository has been successfully initialized.
     *
     * @return {@code true} if initialized and ready for use, {@code false} otherwise
     */
    boolean isInitialized();

    /**
     * Shuts down the repository and releases all resources.
     *
     * @throws RepositoryException if shutdown fails (e.g., unable to flush data)
     */
    void shutDown() throws RepositoryException;

    /**
     * Checks whether the repository supports write operations.
     *
     * @return {@code true} if write operations are supported, {@code false} for read-only
     */
    boolean isWritable();

    /**
     * Opens a new connection to this repository.
     *
     * @return a new {@link RepositoryConnection}
     * @throws RepositoryException if the repository is not initialized or closed
     */
    RepositoryConnection getConnection() throws RepositoryException;

    /**
     * Returns the value factory associated with this repository.
     *
     * @return the {@link ValueFactory}
     */
    ValueFactory getValueFactory();
}