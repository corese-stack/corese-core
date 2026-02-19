package fr.inria.corese.core.next.storagemanager.api;

/**
 * Builder interface for creating StorageManager instances.
 */
public interface StorageManagerBuilder {

    /**
     * Builds and returns a new StorageManager instance
     * with the configured settings.
     *
     * @return a new StorageManager instance
     * @throws IllegalStateException if required configuration is missing
     */
    StorageManager build();
}