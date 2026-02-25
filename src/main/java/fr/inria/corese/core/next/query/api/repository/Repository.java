package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;

import java.io.File;

/**
 * A Corese repository that contains RDF data that can be queried and updated.
 *
 * @see RepositoryConnection
 */
public interface Repository {

    /**
     * Sets the data directory where repository files will be stored.
     *
     * @param dataDir the directory for repository data
     * @throws IllegalStateException if the repository is already initialized
     */
    void setDataDir(File dataDir);

    /**
     * Returns the data directory where repository files are stored.
     *
     * @return the data directory, or {@code null} if not set
     */
    File getDataDir();

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