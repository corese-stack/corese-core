package fr.inria.corese.core.next.datamanager.api;

/**
 * Builder interface for creating ModelDataManager instances.
 */
public interface ModelDataManagerBuilder {

    /**
     * Builds and returns a new ModelDataManager instance
     * with the configured settings.
     *
     * @return a new ModelDataManager instance
     * @throws IllegalStateException if required configuration is missing
     */
    ModelDataManager build();
}