package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;

import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.OBJECT;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.PREDICATE;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.SUBJECT;
import static fr.inria.corese.core.next.query.impl.engine.planning.SelectivityEstimator.MAX_COST;

/**
 * Estimates join cost from endpoint binding patterns and shared variables.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class JoinCostModel {

    //weight 6, 5, 4, 3, 2, 1
    private static final int[][] JOINT_PATTERN = new int[][]{
        {PREDICATE, OBJECT}, {SUBJECT, PREDICATE}, {SUBJECT, OBJECT},
        {OBJECT, OBJECT}, {SUBJECT, SUBJECT}, {PREDICATE, PREDICATE}
    };
    private final QueryPlanEdge edge;
    private int nShare = 0;
    private int jType = -1;

    public JoinCostModel(QueryPlanEdge edge) {
        this.edge = edge;
        if (this.estimatable()) {
            if (this.edge.get(0).getType() == ExpType.Type.EDGE && this.edge.get(1).getType() == ExpType.Type.EDGE) {
                this.setJType();
            }
            this.setNShare();
            this.edge.setType(QueryPlanEdge.BI_DIRECT);
        } else {
            this.edge.setType(QueryPlanEdge.SIMPLE);
        }
    }

    private void setNShare() {
        this.nShare = this.edge.getVariables().size();
    }

    private void setJType() {
        QueryPlanNode node1 = edge.get(0);
        QueryPlanNode node2 = edge.get(1);

        int[][] jp = JOINT_PATTERN;
        for (int i = 0; i < jp.length; i++) {
            int p1 = jp[i][0];
            int p2 = jp[i][1];

            if (node1.getExpNode(p1).getLabel().equals(node2.getExpNode(p2).getLabel())
                    || node1.getExpNode(p2).getLabel().equals(node2.getExpNode(p1).getLabel())) {
                jType = jp.length - i;
            }
        }
    }

    /** Assigns join selectivity from the shared variables and endpoint patterns. */
    public void estimate() {
        if (!estimatable()) {
            this.edge.setCost(SelectivityEstimator.NA_COST);
            return;
        }

        QueryPlanNode node1 = edge.get(0);
        QueryPlanNode node2 = edge.get(1);

        ExpType.Type tNode1 = node1.getType();
        ExpType.Type tNode2 = node2.getType();
        // Non-evaluable endpoint pairs receive the maximum cost.
        if (!PlanningConstants.evaluable(tNode1) && !PlanningConstants.evaluable(tNode2)) {
            this.edge.setCost(MAX_COST);
            return;
        }

        //3.2. no pattern matched: means no shared variables
        if (nShare == 0) {
            this.edge.setCost(MAX_COST);
            return;
        }

        //2 The EDGE connects at least a GRAPH
        if (tNode1 == ExpType.Type.GRAPH || tNode2 == ExpType.Type.GRAPH) {
            this.edge.setCost(1.0 / 3.0 * nShare);
            return;
        }

        //3. two EDGEs
        //3.2. no pattern matched: means no shared variables
        if (jType == -1) {
            this.edge.setCost(MAX_COST);
            return;
        }

        //3.3 pattern matched, assign weight
        this.edge.setCost(1.0 / jType / nShare);
    }

    @Override
    public String toString() {
        return "JoinCostModel{" + "nShare=" + nShare + ", jType=" + jType + '}';
    }

    public boolean estimatable() {
        return PlanningConstants.evaluable(this.edge.get(0).getType()) && PlanningConstants.evaluable(this.edge.get(1).getType());
    }
}
