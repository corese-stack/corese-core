package fr.inria.corese.core.next.query.impl.engine.planning;

import java.util.List;

/**
 * Connects two planning nodes and records their shared variables and join cost.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class QueryPlanEdge {

    static final int BI_DIRECT = 10;
    static final int SIMPLE = 20;

    private final QueryPlanNode n1;
    private final QueryPlanNode n2;
    private final JoinCostModel costModel;
    private double cost = -1;
    private final List<String> variables;
    private int type;

    public QueryPlanEdge(QueryPlanNode n1, QueryPlanNode n2) {
        this.n1 = n1;
        this.n2 = n2;

        // Record the variables shared by both endpoints.
        this.variables = n1.shared(n2);
        this.costModel = new JoinCostModel(this);
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public JoinCostModel getCostModel() {
        return costModel;
    }

    public List<String> getVariables() {
        return variables;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * Get one of the nodes in the edge
     *
     */
    public QueryPlanNode get(int in) {
        if (in == 0) {
            return n1;
        } else if (in == 1) {
            return n2;
        }
        return null;
    }

    /**
     * Get the other node in the edge
     *
     */
    public QueryPlanNode get(QueryPlanNode n) {
        return n1.equals(n) ? n2 : n1;
    }

    @Override
    public String toString() {
        return "{ " + n1 + ", " + n2 + ", " + cost + ", " + type + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.n1 != null ? this.n1.hashCode() : 0);
        hash = 59 * hash + (this.n2 != null ? this.n2.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        if (this.n1 == null || this.n2 == null) {
            return false;
        }

        final QueryPlanEdge other = (QueryPlanEdge) obj;
        if (other.n1 == null || other.n2 == null) {
            return false;
        }

        return (this.n1.equals(other.n1) && this.n2.equals(other.n2))
                || (this.n1.equals(other.n2) && this.n2.equals(other.n1));
    }
}
