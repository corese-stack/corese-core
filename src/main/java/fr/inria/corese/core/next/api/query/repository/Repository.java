package fr.inria.corese.core.next.api.query.repository;

import fr.inria.corese.core.next.api.ValueFactory;

import java.io.File;

/**
 *  A Corese repository that contains RDF data that can be queried and update.
 *  It should hold `Graph` or `GraphStore`
 */

public interface Repository {
    void setDataDir(File dataDir);

    File getDataDir();

    void init();

    boolean isInitialized();

    void shutDown();

    boolean isWritable();

    /**
     *
     * @return RepositoryConnection
     */
    RepositoryConnection getConnection();

    ValueFactory getValueFactory();
}
