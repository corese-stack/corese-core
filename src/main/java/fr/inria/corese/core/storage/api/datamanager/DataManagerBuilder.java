package fr.inria.corese.core.storage.api.datamanager;

/**
 * Builder for DataManager
 */
public interface DataManagerBuilder {

    /**
     * Creates a new DataManager associated to the current configuration.
     * 
     * @return a new DataManager associated to the current configuration.
     */
    DataManager build();
}