package fr.inria.corese.core.sparql.storage.api;

/**
 *
 * Interface that needs to be implemented for persistenting literal/string
 * 
 * @author Fuqi Song, WImmics Inria I3S
 * @date 13 janv. 2015
 */
public interface IStorage {

    //public final static int STORAGE_RAM = 10;
    int STORAGE_DB = 20;
    int STORAGE_FILE = 30;

    /**
     * Write the string to persistent storage
     *
     * @param id
     * @param literal
     * @return 
     */
    boolean write(int id, String literal);

    /**
     * Read the string by id
     *
     * @param id
     * @return
     */
    String read(int id);

    /**
     * Delete the string by its id
     *
     * @param id
     */
    void delete(int id);

    /**
     * Check if the current manager can be used
     *
     * @param str
     * @return
     */
    boolean check(String str);
    boolean check(int length);

    /**
     * Get the status of manager
     * 
     * @return 
     */
    boolean enabled();

    /**
     * Enable or distable the manager
     * 
     * @param enabled 
     */
    void enable(boolean enabled);

    /**
     * Return the type of storage that current manager manages
     *
     * @return
     */
    int getStorageType();

    /**
     * Initialization, ex, setup connection, set parameters, etc
     */
    void init();

    /**
     * Clean up, ex, close connections
     */
    void clean();
  
    
}
