package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.model.DatatypeNodeFactory;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Graph;
import fr.inria.corese.core.next.query.impl.engine.model.Node;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;

import java.util.List;

/**
 * Interface for the Connector that produces candidate edges for KGRAM
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Producer {

    int DEFAULT = 0;
    // Producer delivers from Path of from Provenance Graph
    int EXTENSION = 2;

    /**
     * KGRAM calls this method before executing a query. It enables to
     * initialize the Producer
     */
    default void init(Query q) {
        // Optional lifecycle hook for producer implementations.
    }

    default void start(Query q) {
    }

    default void finish(Query q) {
    }

    int getMode();

    /**
     * A hook to tune Producer
     */
    void setMode(int n);

    /**
     * Return all graph nodes that are known by the Producer. Should return
     * graph nodes that are member of from (if any).
     *
     * @param gNode The graph query node
     * @param from  The from named node list (may be empty)
     * @param env   The binding environment
     * @return graph nodes
     */
    Iterable<Node> getGraphNodes(Node gNode, List<Node> from, Environment env);


    /**
     * Return candidate edges that match a query edge. Matcher checks conformity
     * afterwards. If there is a gNode If it is bound in environment, should
     * return edges from the same graph as gNode value. Otherwise if there is a
     * from, should return edges whose graph is in from. If there is no gNode,
     * if there is a from, should return edges whose graph is in from. Should
     * return edges whose nodes conform to current bindings in environment,
     * nevertheless bindings are checked afterwards in KGRAM.
     *
     * @param gNode The query graph node if any
     * @param from  The from named if gNode is not null, otherwise the from (from
     *              may be empty)
     * @param qEdge The query edge
     * @param env   The current mapping : query node -> target node
     * @return Candidate edge iterator
     */
    Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env);

    /** Whether KGRAM should evaluate BGP edges incrementally using its regular stack. */
    default boolean evaluatesBGPWithEdges() {
        return false;
    }

    /** Releases an edge traversal, including when KGRAM leaves it before exhaustion. */
    default void closeEdges(Iterable<Edge> edges) {
        // Most producers return in-memory collections without owned resources.
    }

    Mappings getMappings(Node gNode, List<Node> from, Exp exp, Environment env);

    /*
     * ************** value to node *************
     */

    /**
     * Given a value from the filter language, return a Node that contains this
     * value use case: select fun(?x) as ?y
     *
     * @param value a target value from the filter language
     */
    Node getNode(Object value);

    // Cast a Java or RDF value into the native datatype contract.
    DatatypeValue getValue(Object value);

    // Native datatype value from an RDF or Java value.
    DatatypeValue getDatatypeValue(Object value);

    /**
     * use case: filter (?x = ?y) filter(?x = 'cst') is it possible to bind ?x
     * to the argument to optimize query processing ? node is the argument
     */
    boolean isBindable(Node node);


    DatatypeNodeFactory getDatatypeNodeFactory();

    /**
     * Given an object resulting from the evaluation of an extension function,
     * return Mappings that represents the values use case: select (sql('select
     * fom where') as (?x, ?y)) in case of sql, the object is a
     * java.sql.ResultSet
     *
     * @param qNodes the query nodes to bind with values of object
     * @return Mappings
     */
    Mappings map(List<Node> qNodes, DatatypeValue value);

    Mappings map(List<Node> qNodes, DatatypeValue value, int n);

    /**
     * graph node { } Node node represents (contains) a graph
     */
    boolean isProducer(Node node);

    Producer getProducer(Node node, Environment env);

    // use case: Producer created for a specific query
    Query getQuery();

    // May return an object that implement the RDF graph
    Graph getGraph();

    Node getGraphNode();

    void setGraphNode(Node n);

    Edge copy(Edge ent);

    void close();

    // generate fresh new blank node ID
    String blankNode();
}
