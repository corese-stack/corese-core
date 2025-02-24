package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.kgram.event.EventManager;
import fr.inria.corese.core.kgram.path.Path;
import fr.inria.corese.core.kgram.tool.ApproximateSearchEnv;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;

import java.util.Map;

/**
 * Interface of the binding environment provided by KGRAM
 * e.g. for filter Evaluator
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Environment {


    /**
     * Return current query
     *
     * @return
     */
    Query getQuery();

    Binding getBind();

    void setBind(Binding b);

    boolean hasBind();

    /**
     * @return current graph node (only for filter interpreter)
     */
    Node getGraphNode();

    default void setGraphNode(Node n) {
    }

    /**
     * Return the target node of variable var
     */
    Node getNode(Expr varExpr);

    /**
     * Return the target node bound to query node with label
     *
     * @param label
     * @return
     */
    Node getNode(String label);

    /**
     * Return the target node bound to query node with label
     *
     * @param qNode
     * @return
     */
    Node getNode(Node qNode);

    /**
     * Return the query node at index n
     *
     * @param n
     * @return
     */
    Node getQueryNode(int n);

    /**
     * Return the query node with label
     *
     * @param label
     * @return
     */
    Node getQueryNode(String label);

    /**
     * Test whether query node is bound
     *
     * @param qNode
     * @return
     */
    boolean isBound(Node qNode);


    /**
     * Return the path length corresponding to query node
     *
     * @param qNode
     * @return
     */
    int pathLength(Node qNode);

    Path getPath(Node qNode);

    int pathWeight(Node qNode);


    // aggregates

    /**
     * Count the number of non null values of query node
     * count duplicates
     */

    int count();

    EventManager getEventManager();

    boolean hasEventManager();

    Object getObject();

    void setObject(Object o);

    Exp getExp();

    void setExp(Exp exp);

    // id -> bnode
    Map<String, IDatatype> getMap();

    Edge[] getEdges();

    Node[] getNodes();

    Node[] getQueryNodes();

    Mappings getMappings();

    Mapping getMapping();

    Iterable<Mapping> getAggregate();

    void aggregate(Mapping m, int n);

    Node get(Expr varExpr);

    ASTExtension getExtension();

    ApproximateSearchEnv getAppxSearchEnv();

    Eval getEval();

    void setEval(Eval e);

    ProcessVisitor getVisitor();

    IDatatype getReport();

    void setReport(IDatatype dt);

    int size();

}
