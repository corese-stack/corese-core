package fr.inria.corese.core.api;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.kgram.api.core.Edge;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.exceptions.EngineException;
import fr.inria.corese.sparql.triple.parser.Access;
import fr.inria.corese.sparql.triple.parser.AccessRight;
import fr.inria.corese.sparql.triple.parser.Constant;
import fr.inria.corese.sparql.triple.update.Basic;

/**
 * Broker between GraphManager and graph DataManager for sparql query construct
 * and sparql update Refined by core.producer.DataBrokerConstructLocal for
 * corese graph Refined by core.producer.DataBrokerConstructExtern for external
 * DataManager
 */
public interface DataBrokerConstruct extends DataBroker {

    default Node getNode(Node gNode, IDatatype dt) {
        return dt;
    }

    default void add(Node node) {
    }

    default void add(Node node, int n) {
        add(node);
    }

    default void addPropertyNode(Node node) {
    }

    default void addGraphNode(Node node) {
    }

    default boolean exist(Node property, Node subject, Node object) {
        return false;
    }
    
    default Edge find(Edge edge) {
        return edge;
    }
   

    /**
     * Return null if edge already exists
     */
    default Edge insert(Edge edge) {
        return getDataManager().insert(edge);
    }

    // corese optimization, not for extern
    default void insert(Node predicate, List<Edge> list) {
        for (Edge edge : list) {
            insert(edge);
        }
    }

    /**********************
     * Update
     */

    default List<Edge> delete(Edge edge) {
        List<Edge> result = new ArrayList<>();
        getDataManager().delete(edge).forEach(result::add);
        return result;
    }

    /**
     * Delete occurrences of edge in named graphs of from list keep other
     * occurrences edge has no named graph Return the list of deleted edges
     */
    default List<Edge> delete(Edge edge, List<Constant> from) {
        return new ArrayList<>(0);
    }

    default boolean load(Query q, Basic ope, Access.Level level, AccessRight access) throws EngineException {
        return true;
    }

    default void clear(String name, boolean silent) {
        getDataManager().clear(List.of(DatatypeMap.createResource(name)), silent);
    }

    default void deleteGraph(String name) {
        getDataManager().unDeclareContext(DatatypeMap.createResource(name));
    }

    default void clearNamed() {
        getDataManager().clear();
    }

    default void dropGraphNames() {
        getDataManager().unDeclareAllContexts();
    }

    default void clearDefault() {
        getDataManager().clear();
    }

    default boolean add(String source, String target, boolean silent) {
        return getDataManager().add(DatatypeMap.createResource(source), DatatypeMap.createResource(target), silent);
    }

    default boolean move(String source, String target, boolean silent) {
        return getDataManager().move(DatatypeMap.createResource(source), DatatypeMap.createResource(target), silent);
    }

    default boolean copy(String source, String target, boolean silent) {
        return getDataManager().copy(DatatypeMap.createResource(source), DatatypeMap.createResource(target), silent);
    }

    default void addGraph(String uri) {
        getDataManager().declareContext(DatatypeMap.createResource(uri));
    }
}
