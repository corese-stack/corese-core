package fr.inria.corese.core.storage.api.dataManager;

import fr.inria.corese.core.producer.MetadataManager;
import fr.inria.corese.core.sparql.triple.parser.HashMapList;

/**
 * Interface to adapt an external storage system to Corese.
 * {@code DataManagerRead} for {@code select where} SPARQL queries.
 * {@code DataManagerUpdate} for {@code update} and {@code construct} queries.
 * 
 * @author Olivier Corby
 * @author Rémi ceres
 */
public interface DataManager extends DataManagerRead, DataManagerUpdate {

    /* ******************
     * MetaDataManager *
     *******************/

    /**
     * Indicates whether or not this DataManage has a MetaDataManager.
     * 
     * @return true if this DataManager has a MetaDataManager, otherwise false.
     */
    default boolean hasMetadataManager() {
        return getMetadataManager() != null;
    }

    /**
     * Getter of the MetaDataManager associated with this DataManager.
     * 
     * @return MetaDataManager associated with this DataManager, {@code null} if
     *         this DataManage has no MetaDataManager.
     */
    default MetadataManager getMetadataManager() {
        return null;
    }

    /**
     * Getter of the MetaDataManager associated with this DataManager. Create
     * a new MetaDataManager if it does not exist.
     * 
     * @return MetaDataManager associated with this DataManager.
     */
    default MetadataManager getCreateMetadataManager() {
        if (!hasMetadataManager()) {
            setMetadataManager(new MetadataManager(this));
        }
        return getMetadataManager();
    }

    /**
     * Setter of the MetaDataManager associated with this DataManager.
     * 
     * @param metaDataManager New MataDataManager.
     */
    default void setMetadataManager(MetadataManager metaDataManager) {
    }

    // for init purpose, called by corese StorageFactory
    default void start(HashMapList<String> map) {
    }

    // service store parameter
    default void init(HashMapList<String> map) {
    }
    
    // manage edge index i as named graph kg:rule_i
    @SuppressWarnings("unused")
    default void setRuleDataManager(boolean b) {
    }

    /* ***************
     * Transactions *
     ****************/


    /**
     * Start a read transaction.
     * A read transaction can be re-entrant, i.e. several read transactions can be
     * nested within each other.
     * Only if the transactions are supported.
     */
    default void startReadTransaction() {
    }

    /**
     * End a read transaction.
     * If several read transactions are nested within each other, this function
     * terminates only the most recent read transaction.
     * Only if the transactions are supported.
     */
    default void endReadTransaction() {
    }

    /**
     * Start a write transaction.
     * A write transaction can't be re-entrant.
     * Only if the transactions are supported.
     */
    default void startWriteTransaction() {
    }

    /**
     * End a write transaction.
     * Only if the transactions are supported.
     */
    default void endWriteTransaction() {
    }


    default void startRuleEngine() {
    }
    
    default void endRuleEngine() {
    }

}
