package fr.inria.corese.core.next.storage.impl.memory.plugin;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.plugin.StoragePlugin;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;

/** Default native in-memory storage provider with SPO, POS and OSP indexes. */
public final class MemoryStoragePlugin implements StoragePlugin {

    /** Creates the stateless provider required by ServiceLoader. */
    public MemoryStoragePlugin() {
        // The provider holds no state; create(StorageConfig) initializes each database.
    }

    /**
     * {@inheritDoc}
     *
     * @return the plugin identifier memory
     */
    @Override
    public String getName() {
        return "memory";
    }

    /**
     * {@inheritDoc}
     *
     * @return the backend description
     */
    @Override
    public String getDescription() {
        return "Native memory storage with SPO, POS and OSP indexes";
    }

    /**
     * {@inheritDoc}
     *
     * @param config storage configuration selecting this backend
     * @return true if the non-null configuration selects memory
     */
    @Override
    public boolean supports(final StorageConfig config) {
        return config != null && config.getType().map(getName()::equalsIgnoreCase).orElse(false);
    }

    /**
     * {@inheritDoc}
     *
     * @param config storage configuration selecting this backend
     * @return an initialized owning native storage manager
     */
    @Override
    public StorageManager create(final StorageConfig config) {
        return new MemoryStorageManager(config);
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
