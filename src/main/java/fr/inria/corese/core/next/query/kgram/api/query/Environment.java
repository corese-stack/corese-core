package fr.inria.corese.core.next.query.kgram.api.query;

import fr.inria.corese.core.next.query.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.*;
import fr.inria.corese.core.next.query.kgram.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.kgram.path.Path;
import fr.inria.corese.core.next.query.kgram.tool.ApproximateSearchEnv;
import fr.inria.corese.core.sparql.api.IDatatype;
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
     */
    Query getQuery();

    BindingContext getBind();

    void setBind(BindingContext b);

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
     */
    Node getNode(String label);

    /**
     * Return the target node bound to query node with label
     *
     */
    Node getNode(Node qNode);

    /**
     * Return the query node at index n
     *
     */
    Node getQueryNode(int n);

    /**
     * Return the query node with label
     *
    */
    Node getQueryNode(String label);

    /**
     * Test whether query node is bound
     *
     */
    boolean isBound(Node qNode);


    /**
     * Return the path length corresponding to query node
     *
     */
    int pathLength(Node qNode);

    Path getPath(Node qNode);

    // aggregates

    /**
     * Count the number of non null values of query node
     * count duplicates
     */

    int count();

    KgramEventDispatcher getEventManager();

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
