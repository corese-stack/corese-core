package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.engine.path.Path;
import fr.inria.corese.core.next.query.impl.engine.eval.ApproximateSearchEnv;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

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
    Map<String, DatatypeValue> getMap();

    Edge[] getEdges();

    Node[] getNodes();

    Node[] getQueryNodes();

    Mappings getMappings();

    Mapping getMapping();

    Iterable<Mapping> getAggregate();

    void aggregate(Mapping m, int n);

    Node get(Expr varExpr);

    ApproximateSearchEnv getAppxSearchEnv();

    Eval getEval();

    void setEval(Eval e);

    ProcessVisitor getVisitor();

    DatatypeValue getReport();

    void setReport(DatatypeValue dt);

    int size();

}
