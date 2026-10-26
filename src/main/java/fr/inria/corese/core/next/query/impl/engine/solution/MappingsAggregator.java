package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Group;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles SPARQL GROUP BY, aggregates (COUNT, MIN, MAX, SUM, AVG, SAMPLE, GROUP_CONCAT),
 * and HAVING filtering for {@link Mappings}.
 */
public final class MappingsAggregator {

    private static final int SELECT = -1;
    private static final int HAVING = -2;

    private static volatile GroupAggregator groupAggregator;

    private final Mappings mappings;

    public MappingsAggregator(Mappings mappings) {
        this.mappings = mappings;
    }

    public static void setGroupAggregator(GroupAggregator aggregator) {
        groupAggregator = aggregator;
    }

    public static GroupAggregator getGroupAggregator() {
        return groupAggregator;
    }

    public void aggregate(Evaluator evaluator, Memory memory, Producer p) {
        aggregate(mappings.getQuery(), evaluator, memory, p);
    }

    public void aggregate(QueryPlan q, Evaluator evaluator, Memory memory, Producer p) {
        if (mappings.size() == 0) {
            if (q != null && q.isAggregate() && !q.hasGroupBy()) {
                mappings.add(Mapping.fake(q));
                mappings.setFake(true);
            } else {
                return;
            }
        }

        GroupAggregator aggregator = groupAggregator;
        if (aggregator != null && aggregator.tryAggregate(q, mappings)) {
            finish(q);
            return;
        }

        if (q != null) {
            aggregateExpList(q, evaluator, memory, p, q.getSelectFun(), true);
            aggregateExpList(q, evaluator, memory, p, q.getOrderBy(), false);
            if (q.getHaving() != null) {
                aggregateSwitch(q, evaluator, q.getHaving(), memory, p, HAVING);
            }
            finish(q);
        }
    }

    void aggregateExpList(QueryPlan q, Evaluator evaluator, Memory memory, Producer p, List<Exp> list, boolean isSelect) {
        int n = 0;
        for (Exp exp : list) {
            if (exp.isAggregate()) {
                aggregateSwitch(q, evaluator, exp, memory, p, isSelect ? SELECT : n);
            }
            if (!isSelect) {
                n++;
            }
        }
    }

    private void aggregateSwitch(QueryPlan q, Evaluator eval, Exp exp, Memory mem, Producer p, int n) {
        if (exp.isExpGroupBy()) {
            evalGroupByExp(eval, exp, mem, p, n);
        } else if (q.hasGroupBy()) {
            aggregateGroupMembers(getCreateGroup(), eval, exp, mem, p, n);
        } else {
            aggregate(eval, exp, mem, p, n);
        }
    }

    void aggregate(Evaluator eval, Exp exp, Memory memory, Producer p, int n) {
        Mapping firstMap = mappings.get(0);
        memory.aggregate(firstMap);

        if (n == HAVING) {
            aggregateHaving(eval, exp, memory, p);
        } else {
            aggregateSelectOrOrder(eval, exp, memory, p, n);
        }

        memory.pop(firstMap);
    }

    private void aggregateHaving(Evaluator eval, Exp exp, Memory memory, Producer p) {
        boolean res = false;
        try {
            res = exp.getFilter().getExp().test(eval, memory.getBind(), memory, p);
        } catch (QueryTypeErrorException ignored) {
            res = false;
        }
        Eval ev = memory.getEval();
        if (ev != null) {
            ev.getVisitor().having(ev, exp.getFilter().getExp(), res);
        }
        mappings.setValid(res);
    }

    private void aggregateSelectOrOrder(Evaluator eval, Exp exp, Memory memory, Producer p, int n) {
        Eval ev = memory.getEval();
        Node aggregateValue;
        if (exp.getFilter() == null) {
            aggregateValue = memory.getNode(exp.getNode());
        } else {
            aggregateValue = eval(exp.getFilter(), eval, memory, p);
            if (ev != null) {
                ev.getVisitor().aggregate(ev, exp.getFilter().getExp(),
                        (aggregateValue == null) ? null : aggregateValue.getDatatypeValue());
            }
        }

        for (Mapping map : mappings) {
            if (n == SELECT) {
                map.setNode(exp.getNode(), aggregateValue);
            } else {
                map.setOrderBy(n, aggregateValue);
            }
        }
    }

    Node eval(Filter f, Evaluator eval, Environment env, Producer p) {
        try {
            return p.getNode(f.getExp().evalWE(eval, env.getBind(), env, p));
        } catch (QueryTypeErrorException e) {
            return null;
        }
    }

    private void aggregateGroupMembers(Group group, Evaluator eval, Exp exp, Memory mem, Producer p, int n) {
        int mappingCount = 0;
        for (Mappings map : group.getValues()) {
            map.setCount(mappingCount++);
            mem.setGroup(map);
            map.aggregate(eval, exp, mem, p, n);
            mem.setGroup(null);
        }
    }

    public void finish(QueryPlan qq) {
        if (qq == null) {
            return;
        }
        if (qq.hasGroupBy() && !qq.isConstruct()) {
            groupBy();
        } else if (qq.getHaving() != null) {
            having();
        } else if (qq.isAggregate() && !qq.isConstruct()) {
            clean();
        }
    }

    public void having() {
        if (mappings.isValid()) {
            clean();
        } else {
            mappings.clear();
        }
    }

    public void clean() {
        if (mappings.size() > 1) {
            Mapping map = mappings.get(0);
            mappings.clear();
            mappings.add(map);
        }
    }

    public void prepareAggregate(Mapping map, QueryPlan q, Map<String, DatatypeValue> bn, int n) {
        mappings.setCount(n);
        map.setMappings(mappings);
        map.setQuery(q);
        map.setMap(bn);
    }

    void evalGroupByExp(Evaluator eval, Exp exp, Memory mem, Producer p, int n) {
        Group g = createGroup(exp);
        aggregateGroupMembers(g, eval, exp, mem, p, n);
        if (exp.isHaving()) {
            having(eval, exp, mem, p, g);
            mappings.setGroup(null);
        }
    }

    void having(Evaluator eval, Exp exp, Memory mem, Producer p, Group g) {
        Filter f = exp.getHavingFilter();
        mappings.clear();
        for (Mappings lm : g.getValues()) {
            for (Mapping map : lm) {
                mem.push(map, -1);
                if (f.getExp().test(eval, mem.getBind(), mem, p)) {
                    mappings.add(map);
                }
                mem.pop(map);
            }
        }
    }

    public void groupBy() {
        groupBy(getCreateGroup());
    }

    public void groupBy(Group group) {
        mappings.clear();
        for (Mappings lMap : group.getValues()) {
            if (lMap.isValid()) {
                Mapping map = lMap.get(0);
                if (map != null) {
                    map.setMappings(mappings.isListGroup ? lMap : null);
                }
                mappings.submit2(map);
            }
        }
    }

    public Group getCreateGroup() {
        if (mappings.getGroup() == null) {
            mappings.setGroup(createGroup());
        }
        return mappings.getGroup();
    }

    public Group createGroup() {
        QueryPlan query = mappings.getQuery();
        return createGroup((query != null) ? query.getGroupBy() : Collections.emptyList());
    }

    public Group createGroup(List<Exp> list) {
        return createGroup(list, false);
    }

    public Group createGroup(Exp exp) {
        return createGroup(exp.getExpGroupBy(), true);
    }

    public Group createGroup(List<Exp> list, boolean extend) {
        Group gp = Group.createFromExp(list);
        QueryPlan query = mappings.getQuery();
        if (query != null) {
            gp.setDuplicate(query.isDistribute());
        }
        gp.setExtend(extend);
        gp.setFake(mappings.isFake());

        for (Mapping map : mappings) {
            gp.add(map);
        }
        return gp;
    }

    public Node max(Node qNode) {
        return minmax(qNode, true);
    }

    public Node min(Node qNode) {
        return minmax(qNode, false);
    }

    private Node minmax(Node qNode, boolean isMax) {
        Node res = null;
        for (Mapping map : mappings) {
            Node node = map.getNode(qNode);
            if (node != null && (res == null || (isMax && node.compare(res) > 0) || (!isMax && node.compare(res) < 0))) {
                res = node;
            }
        }
        return res;
    }

    public List<Node> aggregate(Node varNode) {
        List<Node> aggregateNodeList = new ArrayList<>();
        for (Mapping m : mappings) {
            Node val = m.getNodeValue(varNode);
            if (val != null && !aggregateNodeList.contains(val)) {
                aggregateNodeList.add(val);
            }
        }
        return aggregateNodeList;
    }
}
