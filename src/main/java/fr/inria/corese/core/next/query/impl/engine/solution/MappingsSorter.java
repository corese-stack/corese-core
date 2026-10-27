package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles sorting, binary searching, and limit/offset operations for {@link Mappings}.
 */
public final class MappingsSorter {

    private static final int UNBOUND_ORDER = -1;

    private final Mappings mappings;

    public MappingsSorter(Mappings mappings) {
        this.mappings = mappings;
    }

    /**
     * Sorts mappings by variable names.
     *
     * @param varList list of variable names
     */
    public void sort(List<String> varList) {
        Collections.sort(mappings.getMappingList(), new VariableSorter(varList));
    }

    /**
     * Finds mapping index compatible with m for given variables.
     */
    public int find(Mapping m, List<String> list) {
        return find(m, getVariableSorter(list), 0, mappings.size() - 1);
    }

    VariableSorter getVariableSorter(List<String> varList) {
        return new VariableSorter(varList);
    }

    int find(Mapping m, VariableSorter vs, int first, int last) {
        if (first >= last) {
            return first;
        }
        int mid = (first + last) / 2;
        Mapping mm = mappings.get(mid);
        int res = vs.compare(mm, m);
        if (res >= 0) {
            return find(m, vs, first, mid);
        }
        return find(m, vs, mid + 1, last);
    }

    /**
     * Checks if mapping m is compatible with any mapping for minus operation.
     */
    public boolean minusCompatible(Mapping m, List<String> list) {
        int n = find(m, getVariableSorter(list), 0, mappings.size() - 1);
        if (n >= 0 && n < mappings.size()) {
            Mapping mm = mappings.get(n);
            return m.minusCompatible(mm, list);
        }
        return false;
    }

    /**
     * Sorts with evaluation context.
     */
    public void sort(Eval eval) {
        mappings.setEval(eval);
        sort();
        mappings.setEval(null);
    }

    /**
     * Standard sort according to query ORDER BY clauses.
     */
    public void sort() {
        mappings.getMappingList().sort(this::compare);
    }

    /**
     * Sorts according to a specific node using an evaluator.
     */
    public void sort(Eval eval, Node node) {
        prepare(node);
        sort(eval);
    }

    /**
     * Sorts according to a specific node without evaluator.
     */
    public void sort(Node node) {
        prepare(node);
        sort();
    }

    void prepare(Node node) {
        mappings.sortWithDesc = false;
        for (Mapping m : mappings) {
            m.setOrderBy(m.getNode(node));
        }
    }

    /**
     * Finds index of node where qnode equals node with standard sort.
     */
    public int find(Node node, Node qnode) {
        return find(node, qnode, 0, mappings.size() - 1);
    }

    int find(Node node, Node qnode, int first, int last) {
        if (first >= last) {
            return first;
        }
        int mid = (first + last) / 2;
        Node n1 = mappings.getMappingList().get(mid).getNodeValue(qnode);
        int res = compare(n1, node);
        if (res >= 0) {
            return find(node, qnode, first, mid);
        }
        return find(node, qnode, mid + 1, last);
    }

    /**
     * Node comparator delegating to visitor if evaluator is present.
     */
    int comparator(Node n1, Node n2) {
        if (mappings.getEval() != null) {
            return mappings.getEval().getVisitor().compare(
                    mappings.getEval(),
                    n1.compare(n2),
                    n1.getDatatypeValue(),
                    n2.getDatatypeValue());
        }
        return n1.compare(n2);
    }

    /**
     * Compares two mappings according to ORDER BY expressions.
     */
    public int compare(Mapping m1, Mapping m2) {
        Node[] order1 = m1.getOrderBy();
        Node[] order2 = m2.getOrderBy();
        QueryPlan query = mappings.getQuery();
        List<Exp> orderBy = (query != null) ? query.getOrderBy() : Collections.emptyList();

        for (int i = 0; i < order1.length && i < order2.length; i++) {
            boolean desc = mappings.sortWithDesc && i < orderBy.size() && orderBy.get(i).status();
            int res = compareOrdersAtIndex(order1[i], order2[i], desc);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

    private int compareOrdersAtIndex(Node n1, Node n2, boolean desc) {
        int res;
        if (n1 != null && n2 != null) {
            res = comparator(n1, n2);
        } else if (n1 == null && n2 == null) {
            res = 0;
        } else if (n1 == null) {
            res = UNBOUND_ORDER;
        } else {
            res = -UNBOUND_ORDER;
        }
        return desc ? desc(res) : res;
    }

    int desc(int i) {
        if (i == 0) {
            return 0;
        }
        return (i < 0) ? 1 : -1;
    }

    int compare(Node n1, Node n2) {
        if (n1 != null && n2 != null) {
            return comparator(n1, n2);
        }
        if (n1 == null && n2 == null) {
            return 0;
        }
        return (n1 == null) ? UNBOUND_ORDER : -UNBOUND_ORDER;
    }

    /**
     * Applies ORDER BY sorting and LIMIT/OFFSET slice.
     */
    public void complete(Eval eval) {
        QueryPlan query = mappings.getQuery();
        if (query != null && !query.getOrderBy().isEmpty()) {
            sort(eval);
        }
        limitOffset();
    }

    /**
     * Truncates mappings based on query OFFSET and LIMIT.
     */
    public void limitOffset() {
        QueryPlan query = mappings.getQuery();
        if (query == null) {
            return;
        }
        if (query.getOffset() > 0) {
            for (int i = 0; i < query.getOffset() && mappings.size() > 0; i++) {
                mappings.remove(0);
            }
        }
        while (mappings.size() > query.getLimit()) {
            mappings.remove(mappings.size() - 1);
        }
    }

    /**
     * Limits the number of results to n.
     */
    public Mappings limit(int n) {
        while (mappings.size() > n) {
            mappings.remove(mappings.size() - 1);
        }
        return mappings;
    }

    final class VariableSorter implements Comparator<Mapping> {

        private final List<String> varList;

        VariableSorter(List<String> list) {
            this.varList = list;
        }

        @Override
        public int compare(Mapping m1, Mapping m2) {
            for (String varName : varList) {
                Node n1 = m1.getNodeValue(varName);
                Node n2 = m2.getNodeValue(varName);
                int res = MappingsSorter.this.compare(n1, n2);
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        }
    }
}
