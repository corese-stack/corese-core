package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Group;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopy;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopyable;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage list of Mapping, result of a query.
 * Delegates sorting, distinct filtering, relational set operations, and aggregation
 * to specialized single-responsibility components.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2009
 */
public final class Mappings
        implements Comparator<Mapping>, Iterable<Mapping>, PlanCopyable<Mappings> {

    private static final String NL = "\n";

    @SuppressWarnings("java:S1845") // select field holds projection nodes
    List<Node> select;
    boolean isDistinct = false;
    // satisfy having(test)
    boolean isValid = true;
    // if true, store all Mapping of the group
    boolean isListGroup = false;
    boolean sortWithDesc = true;
    int count = 0;
    private QueryPlan query;
    private final List<Mapping> list;
    // Original join Mappings
    private Mappings joinMappings;
    // Update result
    private List<Edge> insert;
    private List<Edge> delete;
    // in-scope node list
    private List<Node> nodeList;
    private Group group;
    private Group distinct;
    private Eval eval;
    // fake result in case of aggregate without result
    private boolean isFake = false;
    // parse error in service result
    private boolean error = false;
    // return Binding stack as part of result to share it
    private Object provenance;
    // Linked Result URL List
    private final List<String> link;
    // service result log
    private int length = 0;
    // limit number of results to be displayed
    private int display = Integer.MAX_VALUE;

    private final MappingsSorter sorter;
    private final MappingsDistinctFilter distinctFilter;
    private final MappingsRelationalOperations relationalOps;
    private final MappingsAggregator aggregator;

    public static void setGroupAggregator(GroupAggregator aggregator) {
        MappingsAggregator.setGroupAggregator(aggregator);
    }

    public static GroupAggregator getGroupAggregator() {
        return MappingsAggregator.getGroupAggregator();
    }

    /** Copies a static VALUES table into independent row containers. */
    @Override
    public Mappings copyPlan(PlanCopy copies) {
        Mappings target = new Mappings();
        copies.remember(this, target);
        for (Mapping row : list) {
            target.add(copies.copy(row));
        }
        return target;
    }

    public Mappings() {
        list = new ArrayList<>();
        link = new ArrayList<>();
        sorter = new MappingsSorter(this);
        distinctFilter = new MappingsDistinctFilter(this);
        relationalOps = new MappingsRelationalOperations(this);
        aggregator = new MappingsAggregator(this);
    }

    Mappings(QueryPlan q) {
        this();
        query = q;
    }

    public static Mappings create(QueryPlan q) {
        return create(q, false);
    }

    public static Mappings create(QueryPlan q, boolean subEval) {
        Mappings lMap = new Mappings(q);
        if (q != null) {
            lMap.init(q, subEval);
        }
        return lMap;
    }

    void init(QueryPlan q, boolean subEval) {
        initiate(q, !subEval && q.isDistinct() && !q.isDistinctRedundantWithGroupBy());
    }

    void initiate(QueryPlan q, boolean b) {
        initiate(q, b, false);
    }

    void initiate(QueryPlan q, boolean b, boolean all) {
        distinctFilter.initiate(q, b, all);
    }

    List<Exp> getAllExpList(QueryPlan q) {
        return distinctFilter.getAllExpList(q);
    }

    public Mappings distinct() {
        return distinctFilter.distinct();
    }

    public Mappings distinct(List<Node> list) {
        return distinctFilter.distinct(list);
    }

    public Mappings distinct(List<Node> selectList, List<Node> distinctList) {
        return distinctFilter.distinct(selectList, distinctList);
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    void setCount(int n) {
        count = n;
    }

    public Mappings add(Mapping m) {
        getMappingList().add(m);
        return this;
    }

    public List<Mapping> getList() {
        return list;
    }

    public void add(Mappings lm) {
        for (Mapping m : lm) {
            add(m);
        }
    }

    @Override
    public Iterator<Mapping> iterator() {
        return list.iterator();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Mapping get(int n) {
        return list.get(n);
    }

    public Mapping set(int n, Mapping m) {
        return list.set(n, m);
    }

    public void remove(int n) {
        list.remove(n);
    }

    public void clear() {
        list.clear();
    }

    public QueryPlan getQuery() {
        return query;
    }

    public Mappings setQuery(QueryPlan q) {
        query = q;
        return this;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean all) {
        return toString(all, false, getDisplay());
    }

    public String toString(boolean all, boolean ptr, int max) {
        StringBuilder sb = new StringBuilder();
        if (all && getSelect() != null) {
            for (Node qNode : getSelect()) {
                sb.append(qNode.getLabel()).append(" ");
            }
            sb.append(NL);
        }
        int i = 0;
        for (Mapping map : this) {
            if (i++ >= max) {
                break;
            }
            sb.append(map.toString());
            sb.append(NL);
        }
        return sb.toString();
    }

    public List<Node> getSelect() {
        return select;
    }

    void setSelect(List<Node> lNode) {
        select = lNode;
    }

    public void setSelect(Node node) {
        select = new ArrayList<>();
        select.add(node);
    }

    public Node getNode(String varString) {
        if (isEmpty()) {
            return null;
        }
        return get(0).getNode(varString);
    }

    public DatatypeValue getValue(String varString) {
        Node node = getNode(varString);
        if (node != null) {
            return node.getDatatypeValue();
        }
        return null;
    }

    public void submit(Mapping a) {
        distinctFilter.submit(a);
    }

    public boolean acceptable(Mapping m) {
        return distinctFilter.acceptable(m);
    }

    void submit2(Mapping a) {
        distinctFilter.submit2(a);
    }

    public boolean accept(Node node) {
        return distinctFilter.accept(node);
    }

    public boolean accept(Mapping r) {
        return distinctFilter.accept(r);
    }

    boolean isValid() {
        return isValid;
    }

    void setValid(boolean b) {
        isValid = b;
    }

    public void sort(List<String> varList) {
        sorter.sort(varList);
    }

    int find(Mapping m, List<String> list) {
        return sorter.find(m, list);
    }

    boolean minusCompatible(Mapping m, List<String> list) {
        return sorter.minusCompatible(m, list);
    }

    void sort(Eval eval) {
        sorter.sort(eval);
    }

    void sort() {
        sorter.sort();
    }

    public void sort(Eval eval, Node node) {
        sorter.sort(eval, node);
    }

    public void sort(Node node) {
        sorter.sort(node);
    }

    public int find(Node node, Node qnode) {
        return sorter.find(node, qnode);
    }

    @Override
    public int compare(Mapping m1, Mapping m2) {
        return sorter.compare(m1, m2);
    }

    public void complete(Eval eval) {
        sorter.complete(eval);
    }

    void limitOffset() {
        sorter.limitOffset();
    }

    public void aggregate(Evaluator evaluator, Memory memory, Producer p) {
        aggregator.aggregate(evaluator, memory, p);
    }

    public void aggregate(QueryPlan q, Evaluator evaluator, Memory memory, Producer p) {
        aggregator.aggregate(q, evaluator, memory, p);
    }

    void aggregate(Evaluator eval, Exp exp, Memory memory, Producer p, int n) {
        aggregator.aggregate(eval, exp, memory, p, n);
    }

    public void prepareAggregate(Mapping map, QueryPlan q, Map<String, DatatypeValue> bn, int n) {
        aggregator.prepareAggregate(map, q, bn, n);
    }

    public void groupBy() {
        aggregator.groupBy();
    }

    public void groupBy(Group group) {
        aggregator.groupBy(group);
    }

    public Mappings project() {
        for (Mapping map : this) {
            map.project(getQuery());
        }
        return this;
    }

    public Mappings project(Node q) {
        Mappings map = create(getQuery());
        for (Mapping m : this) {
            Mapping res = m.project(q);
            if (res != null) {
                map.add(res);
            }
        }
        return map;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group g) {
        group = g;
    }

    public Group getCreateGroup() {
        return aggregator.getCreateGroup();
    }

    Group createGroup() {
        return aggregator.createGroup();
    }

    Group createGroup(List<Exp> list) {
        return aggregator.createGroup(list);
    }

    Group createGroup(Exp exp) {
        return aggregator.createGroup(exp);
    }

    Group createGroup(List<Exp> list, boolean extend) {
        return aggregator.createGroup(list, extend);
    }

    Group group(List<Exp> list) {
        return Group.createFromExp(list);
    }

    public Node max(Node qNode) {
        return aggregator.max(qNode);
    }

    public Node min(Node qNode) {
        return aggregator.min(qNode);
    }

    public Node getCommonNode(Mappings map) {
        return relationalOps.getCommonNode(map);
    }

    public Mappings union(Mappings lm) {
        return relationalOps.union(lm);
    }

    public Mappings and(Mappings lm) {
        return relationalOps.join(lm);
    }

    public Mappings join(Mappings lm) {
        return relationalOps.join(lm);
    }

    public Mappings joiner(Mappings lm) {
        return relationalOps.joiner(lm);
    }

    public Mappings joiner(Mappings map2, Node cmn) {
        return relationalOps.joiner(map2, cmn);
    }

    public Mappings minus(Mappings lm) {
        return relationalOps.minus(lm);
    }

    public Mappings optional(Mappings lm) {
        return relationalOps.option(lm);
    }

    public Mappings option(Mappings lm) {
        return relationalOps.option(lm);
    }

    public void join(Node varNode, Node val) {
        relationalOps.join(varNode, val);
    }

    public boolean inScope(Node varNode) {
        if (getNodeList() != null) {
            return getNodeList().contains(varNode);
        }
        if (getSelect() != null) {
            return getSelect().contains(varNode);
        }
        return true;
    }

    public List<Node> aggregate(Node varNode) {
        return aggregator.aggregate(varNode);
    }

    public Mappings limit(int n) {
        return sorter.limit(n);
    }

    public List<Edge> getInsert() {
        return insert;
    }

    public void setInsert(List<Edge> lInsert) {
        insert = lInsert;
    }

    public List<Edge> getDelete() {
        return delete;
    }

    public void setDelete(List<Edge> lDelete) {
        delete = lDelete;
    }

    public Mappings getMappings() {
        return this;
    }

    public Eval getEval() {
        return eval;
    }

    public void setEval(Eval eval) {
        this.eval = eval;
    }

    public boolean isFake() {
        return isFake;
    }

    public Mappings setFake(boolean isFake) {
        this.isFake = isFake;
        return this;
    }

    public boolean isNodeList() {
        return nodeList != null;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public Object getProvenance() {
        return provenance;
    }

    public void setProvenance(Object provenance) {
        this.provenance = provenance;
    }

    public void setNamedGraph(Node node) {
        for (Mapping map : this) {
            map.setNamedGraph(node);
        }
    }

    public String getLink() {
        if (link.isEmpty()) {
            return null;
        }
        return link.get(0);
    }

    public void addLink(String link) {
        this.link.add(link);
    }

    public List<String> getLinkList() {
        return link;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public Mappings getJoinMappings() {
        return joinMappings;
    }

    public void setJoinMappings(Mappings joinMappings) {
        this.joinMappings = joinMappings;
    }

    public Group getDistinct() {
        return distinct;
    }

    public void setDistinct(Group distinct) {
        this.distinct = distinct;
    }

    public List<Mapping> getMappingList() {
        return list;
    }
}
