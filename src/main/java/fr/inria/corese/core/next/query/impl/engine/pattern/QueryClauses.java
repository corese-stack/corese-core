package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates SPARQL query clauses, projection, grouping, ordering, dataset specification,
 * slice limits, and template definitions for {@link QueryPlan}.
 */
public final class QueryClauses {

    private int limit = Integer.MAX_VALUE;
    private int offset = 0;
    private int slice = 20;

    private boolean distinctResults = false;
    private boolean aggregateQuery = false;
    private boolean constructQuery = false;
    private boolean insertQuery = false;
    private boolean deleteQuery = false;
    private boolean updateQuery = false;
    private boolean askQuery = false;
    private boolean listGroup = false;
    private boolean datasetSpecified = false;

    private List<Node> from;
    private List<Node> named;
    private List<Node> selectNodes;
    private List<Exp> selectExp;
    private List<Exp> orderBy;
    private List<Exp> groupBy;
    private Exp having;
    private Exp construct;
    private Exp delete;

    public QueryClauses() {
        from = new ArrayList<>();
        named = new ArrayList<>();
        selectNodes = new ArrayList<>();
        selectExp = new ArrayList<>();
        orderBy = new ArrayList<>();
        groupBy = new ArrayList<>();
    }

    public QueryClauses copyPlan(PlanCopy copies) {
        QueryClauses target = new QueryClauses();
        target.limit = this.limit;
        target.offset = this.offset;
        target.slice = this.slice;
        target.distinctResults = this.distinctResults;
        target.aggregateQuery = this.aggregateQuery;
        target.constructQuery = this.constructQuery;
        target.insertQuery = this.insertQuery;
        target.deleteQuery = this.deleteQuery;
        target.updateQuery = this.updateQuery;
        target.askQuery = this.askQuery;
        target.listGroup = this.listGroup;
        target.datasetSpecified = this.datasetSpecified;
        target.from = copies.copy(this.from);
        target.named = copies.copy(this.named);
        target.selectNodes = copies.copy(this.selectNodes);
        target.selectExp = copies.copy(this.selectExp);
        target.orderBy = copies.copy(this.orderBy);
        target.groupBy = copies.copy(this.groupBy);
        target.having = copies.copy(this.having);
        target.construct = copies.copy(this.construct);
        target.delete = copies.copy(this.delete);
        return target;
    }

    public List<Node> getSelect() {
        return selectNodes;
    }

    public void setSelect(List<Node> list) {
        selectNodes = list;
    }

    public void addSelect(Node node) {
        if (selectExp == null) {
            selectExp = new ArrayList<>();
        }
        selectExp.add(Exp.create(Exp.Type.NODE, node));
    }

    public List<Exp> getSelectFun() {
        return selectExp;
    }

    public void setSelectFun(List<Exp> s) {
        selectExp = s;
    }

    public Exp getSelectExp(String label) {
        for (Exp exp : selectExp) {
            if (exp.getNode() != null && exp.getNode().getLabel().equals(label)) {
                return exp;
            }
        }
        return null;
    }

    public Node getSelectNode(String label) {
        for (Node node : selectNodes) {
            if (node.getLabel().equals(label)) {
                return node;
            }
        }
        return null;
    }

    public List<Exp> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<Exp> s) {
        orderBy = s;
    }

    public void addOrderBy(Node node) {
        orderBy.add(Exp.create(Exp.Type.NODE, node));
    }

    public boolean isOrderBy() {
        return !orderBy.isEmpty();
    }

    public List<Exp> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<Exp> s) {
        groupBy = s;
    }

    public void addGroupBy(Node node) {
        groupBy.add(Exp.create(Exp.Type.NODE, node));
    }

    public boolean isGroupBy() {
        return !groupBy.isEmpty();
    }

    public boolean hasGroupBy() {
        return !groupBy.isEmpty();
    }

    public boolean isListGroup() {
        return listGroup;
    }

    public void setListGroup(boolean listGroup) {
        this.listGroup = listGroup;
    }

    public Exp getHaving() {
        return having;
    }

    public void setHaving(Exp f) {
        having = f;
    }

    public List<Node> getFrom() {
        return from;
    }

    public void setFrom(List<Node> l) {
        from = l;
    }

    public List<Node> getNamed() {
        return named;
    }

    public void setNamed(List<Node> l) {
        named = l;
    }

    public boolean isDatasetSpecified() {
        return datasetSpecified;
    }

    public void setDatasetSpecified(boolean datasetSpecified) {
        this.datasetSpecified = datasetSpecified;
    }

    public List<Node> getFrom(Node gNode) {
        return (gNode == null) ? from : named;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int n) {
        limit = n;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int n) {
        offset = n;
    }

    public int getLimitOffset() {
        if (!isConstruct() && (isOrderBy() || hasGroupBy() || isAggregate())) {
            return Integer.MAX_VALUE;
        }
        if (limit < Integer.MAX_VALUE - offset) {
            return limit + offset;
        }
        return limit;
    }

    public int getSlice() {
        return slice;
    }

    public void setSlice(int n) {
        slice = n;
    }

    public boolean isDistinct() {
        return distinctResults;
    }

    public void setDistinct(boolean b) {
        distinctResults = b;
    }

    public boolean isDistinctRedundantWithGroupBy() {
        if (!isDistinct() || !hasGroupBy() || isListGroup()) {
            return false;
        }
        List<Exp> groupList = getGroupBy();
        List<Node> currentSelect = getSelect();
        if (groupList == null || groupList.isEmpty() || currentSelect == null || currentSelect.isEmpty()) {
            return false;
        }
        Set<String> selectVarNames = extractSelectVariableNames(currentSelect);
        return areAllGroupKeysProjected(groupList, selectVarNames);
    }

    private static Set<String> extractSelectVariableNames(List<Node> selectNodes) {
        return selectNodes.stream()
                .filter(n -> n != null && n.isVariable())
                .map(Node::getLabel)
                .collect(Collectors.toSet());
    }

    private static boolean areAllGroupKeysProjected(List<Exp> groupList, Set<String> selectVarNames) {
        for (Exp groupExp : groupList) {
            if (groupExp == null || groupExp.getNode() == null || groupExp.getFilter() != null) {
                return false;
            }
            Node groupNode = groupExp.getNode();
            if (!groupNode.isVariable() || !selectVarNames.contains(groupNode.getLabel())) {
                return false;
            }
        }
        return true;
    }

    public boolean isAggregate() {
        return aggregateQuery;
    }

    public void setAggregate(boolean b) {
        aggregateQuery = b;
    }

    public boolean isConstruct() {
        return constructQuery;
    }

    public void setConstruct(boolean b) {
        constructQuery = b;
    }

    public Exp getConstruct() {
        return construct;
    }

    public void setConstruct(Exp c) {
        construct = c;
        if (c != null) {
            setConstruct(true);
        }
    }

    public Exp getInsert() {
        return (construct != null) ? construct : Exp.create(Exp.Type.BGP);
    }

    public boolean isInsert() {
        return insertQuery;
    }

    public void setInsert(boolean b) {
        insertQuery = b;
    }

    public boolean isDelete() {
        return deleteQuery;
    }

    public void setDelete(boolean b) {
        deleteQuery = b;
    }

    public Exp getDelete() {
        return delete;
    }

    public void setDelete(Exp c) {
        delete = c;
        if (c != null) {
            setDelete(true);
        }
    }

    public boolean isUpdate() {
        return updateQuery;
    }

    public void setUpdate(boolean b) {
        updateQuery = b;
    }

    public boolean isAsk() {
        return askQuery;
    }

    public void setAsk(boolean b) {
        askQuery = b;
    }
}
