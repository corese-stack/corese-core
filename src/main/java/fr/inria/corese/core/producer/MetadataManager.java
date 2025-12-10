package fr.inria.corese.core.producer;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.logic.Distance;
import fr.inria.corese.core.storage.api.dataManager.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Corese object associated to DataManager
 * enables corese core to manage additional data and computation such as:
 * Distance, transitiveRelation
 */
public class MetadataManager {
    private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);

    private DataManager dataManager;
    private Distance distance;

    public MetadataManager() {
    }

    public MetadataManager(DataManager man) {
        setDataManager(man);
    }

    // called by StorageFactory
    public void startDataManager() {
    }

    void start() {
    }

    public void endDataManager() {
    }

    public void startReadTransaction() {
    }

    public void endReadTransaction() {
    }

    public void startWriteTransaction() {
    }

    public void endWriteTransaction() {
        clean();
    }

    void clean() {
        setDistance(null);
    }

    public Distance getCreateDistance() {
        if (getDistance() == null) {
            setDistance(new Distance(getDataManager()));
            getDistance().start();
        }
        return getDistance();
    }

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    // n1 subClassOf* n2
    public boolean transitiveRelation(Node n1, Node predicate, Node n2) {
        if (n1.equals(n2)) {
            return true;
        }
        return isTransitive(n1, predicate, n2, new HashMap<>());
    }

    /**
     * Take loop into account
     */
    boolean isTransitive(Node subject, Node predicate, Node object, HashMap<String, Node> t) {
        Iterable<Edge> it = getDataManager().getEdges(subject, predicate, null, null);

        if (it == null) {
            return false;
        }

        t.put(subject.getLabel(), subject);

        for (Edge ent : it) {
            Node node = ent.getNode(1);
            if (node.equals(object)) {
                return true;
            }
            if (node.equals(subject)) {
                continue;
            }
            if (t.containsKey(node.getLabel())) {
                continue;
            }
            if (isTransitive(node, predicate, object, t)) {
                return true;
            }
        }

        t.remove(subject.getLabel());

        return false;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

}
