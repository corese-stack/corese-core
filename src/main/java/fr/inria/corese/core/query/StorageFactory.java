package fr.inria.corese.core.query;

import fr.inria.corese.core.query.DatasetManager.TypeDataBase;
import fr.inria.corese.core.sparql.triple.parser.URLServer;
import fr.inria.corese.core.storage.api.dataManager.DataManager;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StorageFactory {

    private static StorageFactory singleton = null;

    private HashMap<String, DataManager> map;

    public StorageFactory() {
        setMap(new HashMap<>());
    }

    public static StorageFactory getSingleton() {
        if (singleton == null) {
            singleton = new StorageFactory();
        }
        return singleton;
    }

    public static void defineDataManager(TypeDataBase typeDB, String id, String param) {
    }

    public void defineDataManager(String id, DataManager man) {
        defineDataManager(new URLServer(id), man);
    }

    public void defineDataManager(URLServer id, DataManager man) {

        // Check if id already exists
        if (getMap().containsKey(id.getServer())) {
            throw new InvalidParameterException("DataManager already exists for id: " + id.getServer());
        }

        getMap().put(id.getServer(), man);
        man.getCreateMetadataManager();
        man.start(id.getMap());
        if (man.hasMetadataManager()) {
            man.getMetadataManager().startDataManager();
        }
    }

    public DataManager getDataManager(String id) {
        return getMap().get(id);
    }

    public Collection<DataManager> getDataManagerList() {
        return getMap().values();
    }

    public Map<String, DataManager> getMap() {
        return map;
    }

    public void setMap(Map<String, DataManager> map) {
        this.map = (HashMap<String, DataManager>) map;
    }

}
