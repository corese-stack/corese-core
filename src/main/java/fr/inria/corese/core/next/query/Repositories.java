package fr.inria.corese.core.next.query;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.impl.repository.CoreseRepository;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;

import java.util.Objects;

/**
 * Public entry point for creating ready-to-use Corese repositories.
 *
 * <p>The common path requires no implementation imports or explicit
 * initialization:</p>
 *
 * <pre>{@code
 * try (Repository repository = Repositories.create()) {
 *     repository.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }");
 *     boolean present = repository.ask("ASK { <urn:s> <urn:p> <urn:o> }");
 * }
 * }</pre>
 *
 * <p>A repository owns the storage manager passed to it. Closing the repository
 * closes that storage manager. Instances returned by this class are open and
 * immediately usable.</p>
 */
public final class Repositories {

    private Repositories() {
    }

    /**
     * Creates an open repository backed by the default in-memory storage.
     *
     * @return a ready-to-use repository
     */
    public static Repository create() {
        return create(StorageConfig.memory());
    }

    /**
     * Creates an open repository using the storage plugin selected by the
     * supplied configuration.
     *
     * @param config storage plugin configuration
     * @return a ready-to-use repository owning the selected storage
     */
    public static Repository create(StorageConfig config) {
        StorageConfig checkedConfig = Objects.requireNonNull(config, "config");
        try {
            return new CoreseRepository(Storages.create(checkedConfig), checkedConfig);
        } catch (PluginException e) {
            throw new RepositoryException("Could not create repository storage", e);
        }
    }

    /**
     * Creates an open repository that takes ownership of a storage manager.
     * An uninitialized manager is initialized with an empty configuration; an
     * already-running manager is adopted as-is.
     *
     * @param storage storage manager whose ownership is transferred
     * @return a ready-to-use repository
     */
    public static Repository create(StorageManager storage) {
        return create(storage, StorageConfig.builder().build());
    }

    /**
     * Creates an open repository that takes ownership of a storage manager.
     * The configuration is used only when the manager is not initialized yet.
     *
     * @param storage storage manager whose ownership is transferred
     * @param config initialization configuration
     * @return a ready-to-use repository
     */
    public static Repository create(StorageManager storage, StorageConfig config) {
        return new CoreseRepository(
                Objects.requireNonNull(storage, "storage"),
                Objects.requireNonNull(config, "config"));
    }
}
