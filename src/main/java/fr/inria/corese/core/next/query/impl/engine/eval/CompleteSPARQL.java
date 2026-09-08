/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Compute select expression,
 * Compute value of group by, order by
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class CompleteSPARQL {
    private static final Logger logger = LoggerFactory.getLogger(CompleteSPARQL.class);

    Eval eval;
    Query query;

    CompleteSPARQL(Query q, Eval e) {
        this.eval = e;
        this.query = q;
    }

    /**
     * Complete:
     * select (exp as var)
     * group by, order by
     *
     */
    void complete(Producer p, Mappings map) {
        selectExpression(query, p, map);
        distinct(query, map);
        orderGroup(query, p, map);
    }

    void distinct(Query q, Mappings map) {
        if (q.isAggregate() || !map.isDistinct()) {
            return;
        }

        new ArrayList<>(map.getList()).forEach(m -> {
            map.getList().clear();
            map.submit(m);
        });
    }
    void selectExpression(Query q, Producer p, Mappings map) {
        if (query.isSelectExpression()) {
            HashMap<String, DatatypeValue> bnode = new HashMap<>();
            for (Mapping m : map) {
                bnode.clear();
                m.setMap(bnode);
                m.setQuery(q);
                Mapping res = selectExpression(q, p, m);
                if (res == null) {
                    logger.warn("Select: exp != var value: {}", m);
                }
            }
        }
    }

    Mapping selectExpression(Query q, Producer p, Mapping m) {
        ArrayList<Node> ql = new ArrayList<>();
        ArrayList<Node> tl = new ArrayList<>();

        for (Exp e : q.getSelectFun()) {
            if (!completeProjection(e, p, m, ql, tl)) {
                return null;
            }
        }

        if (!ql.isEmpty()) {
            m.complete(ql, tl);
        }

        return m;
    }

    private boolean completeProjection(Exp expression, Producer producer, Mapping mapping,
            List<Node> queryNodes, List<Node> targetNodes) {
        Filter filter = expression.getFilter();
        if (filter == null) {
            return true;
        }
        Node queryNode = expression.getNode();
        Node current = mapping.getNodeValue(queryNode);
        if (expression.isAggregate()) {
            // Aggregates are evaluated later; reserve their output slot now.
            if (current == null) {
                queryNodes.add(queryNode);
                targetNodes.add(null);
            }
            return true;
        }
        Node result = eval.eval(null, filter, mapping, producer);
        if (result == null) {
            return true;
        }
        if (current != null) {
            return current.equals(result);
        }
        queryNodes.add(queryNode);
        targetNodes.add(result);
        mapping.setNodeValue(queryNode, result);
        return true;
    }

    void orderGroup(Query q, Producer p, Mappings map) {
        for (Mapping m : map) {
            Node[] snode = new Node[q.getOrderBy().size()];
            Node[] gnode = new Node[q.getGroupBy().size()];
            orderGroup(q.getOrderBy(), snode, p, m);
            orderGroup(q.getGroupBy(), gnode, p, m);
            m.setOrderBy(snode);
            m.setGroupBy(gnode);
        }
    }

    void orderGroup(List<Exp> lExp, Node[] nodes, Producer p, Mapping m) {
        int n = 0;
        for (Exp e : lExp) {
            Node qNode = e.getNode();
            if (qNode != null) {
                nodes[n] = m.getNodeValue(qNode);
            }
            if (nodes[n] == null) {
                Filter f = e.getFilter();
                if (f != null && !e.isAggregate()) {
                    nodes[n] = eval.eval(null, f, m, p);
                }

            }
            n++;
        }
    }

}
