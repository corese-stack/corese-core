package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Group;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;

import java.util.List;

/**
 * Handles DISTINCT filtering, deduplication, and solution acceptance for {@link Mappings}.
 */
public final class MappingsDistinctFilter {

    private final Mappings mappings;

    public MappingsDistinctFilter(Mappings mappings) {
        this.mappings = mappings;
    }

    /**
     * Initializes distinct tracking based on query plan and execution options.
     */
    public void initiate(QueryPlan q, boolean distinct, boolean all) {
        mappings.setQuery(q);
        mappings.isDistinct = distinct;
        if (q != null) {
            mappings.isListGroup = q.isListGroup();
            mappings.setSelect(q.getSelect());
            if (distinct) {
                List<Exp> expList = all ? getAllExpList(q) : q.getSelectFun();
                Group distinctGroup = Group.createFromExp(expList);
                distinctGroup.setDistinct(true);
                distinctGroup.setDuplicate(q.isDistribute());
                mappings.setDistinct(distinctGroup);
            }
        }
    }

    /**
     * Computes expression list representing all variables in pattern body or select functions.
     */
    public List<Exp> getAllExpList(QueryPlan q) {
        List<Node> nodesFromPattern = q.selectNodesFromPattern();
        if (nodesFromPattern.isEmpty()) {
            return q.getSelectFun();
        }
        return q.toExp(nodesFromPattern);
    }

    /**
     * Returns a distinct copy of mappings across all variables in the query.
     */
    public Mappings distinct() {
        Mappings res = new Mappings();
        res.initiate(mappings.getQuery(), true, true);
        for (Mapping m : mappings) {
            res.submit(m);
        }
        return res;
    }

    /**
     * Returns a distinct copy of mappings based on a given node list.
     */
    public Mappings distinct(List<Node> list) {
        QueryPlan query = mappings.getQuery();
        List<Node> selectList = (query != null) ? query.getSelect() : null;
        return distinct(selectList, list);
    }

    /**
     * Returns a distinct copy of mappings projected on selectList and deduplicated on distinctList.
     */
    public Mappings distinct(List<Node> selectList, List<Node> distinctList) {
        Mappings map = new Mappings(mappings.getQuery());
        map.setSelect(selectList);
        Group distinctListGroup = Group.create(distinctList);
        distinctListGroup.setDistinct(true);
        for (Mapping m : mappings) {
            if (distinctListGroup.isDistinct(m)) {
                map.add(m);
            }
        }
        return map;
    }

    /**
     * Submits a mapping to the result set if it passes the distinct acceptance check.
     */
    public void submit(Mapping a) {
        if (a != null && acceptable(a)) {
            mappings.add(a);
        }
    }

    /**
     * Submits a mapping during aggregation.
     */
    public void submit2(Mapping a) {
        QueryPlan query = mappings.getQuery();
        if (query != null && query.isAggregate()) {
            if (accept(a)) {
                mappings.add(a);
            }
        } else {
            mappings.add(a);
        }
    }

    /**
     * Checks if a mapping is acceptable given the query configuration.
     */
    public boolean acceptable(Mapping m) {
        QueryPlan query = mappings.getQuery();
        return (query != null && query.isAggregate()) || accept(m);
    }

    /**
     * Checks if a node is accepted by distinct group.
     */
    public boolean accept(Node node) {
        return mappings.getDistinct() == null || mappings.getDistinct().accept(node);
    }

    /**
     * Checks if a mapping is distinct according to current projection and distinct group.
     */
    public boolean accept(Mapping r) {
        if (mappings.select == null || mappings.select.isEmpty()) {
            return true;
        }
        if (mappings.isDistinct && mappings.getDistinct() != null) {
            return mappings.getDistinct().isDistinct(r);
        }
        return true;
    }
}
