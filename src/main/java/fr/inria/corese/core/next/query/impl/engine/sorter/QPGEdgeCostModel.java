package fr.inria.corese.core.next.query.impl.engine.sorter;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;

import java.util.List;
import static fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorterConst.OBJECT;
import static fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorterConst.PREDICATE;
import static fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorterConst.SUBJECT;
import static fr.inria.corese.core.next.query.impl.engine.sorter.IEstimate.MAX_COST;

/**
 * Cost model for QPG edge
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class QPGEdgeCostModel extends AbstractCostModel {

    //weight 6, 5, 4, 3, 2, 1
    private static final int[][] JOINT_PATTERN = new int[][]{
        {PREDICATE, OBJECT}, {SUBJECT, PREDICATE}, {SUBJECT, OBJECT},
        {OBJECT, OBJECT}, {SUBJECT, SUBJECT}, {PREDICATE, PREDICATE}
    };
    private final QPGEdge edge;
    private int nShare = 0;
    private int jType = -1;

    public QPGEdgeCostModel(QPGEdge edge) {
        this.edge = edge;
        if (this.estimatable()) {
            if (this.edge.get(0).getType() == ExpType.Type.EDGE && this.edge.get(1).getType() == ExpType.Type.EDGE) {
                this.setJType();
            }
            this.setNShare();
            this.edge.setType(QPGEdge.BI_DIRECT);
        } else {
            this.edge.setType(QPGEdge.SIMPLE);
        }
    }

    private void setNShare() {
        this.nShare = this.edge.getVariables().size();
    }

    private void setJType() {
        QPGNode node1 = edge.get(0);
        QPGNode node2 = edge.get(1);

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

    @Override
    public void estimate(List<Object> params) {
        if (!(isParametersOK(params) && estimatable())) {
            this.edge.setCost(IEstimate.NA_COST);
            return;
        }

        QPGNode node1 = edge.get(0);
        QPGNode node2 = edge.get(1);

        ExpType.Type tNode1 = node1.getType();
        ExpType.Type tNode2 = node2.getType();
        //1. type of one of them is FILTER or VALUES or BIND, ne assign pas le weight
        if (!QuerySorterConst.evaluable(tNode1) && !QuerySorterConst.evaluable(tNode2)) {
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
        return "QPGEdgeWeightModel{" + "nShare=" + nShare + ", jType=" + jType + '}';
    }

    @Override
    public boolean isParametersOK(List<Object> params) {
        return true;
    }

    @Override
    public final boolean estimatable() {
        return QuerySorterConst.evaluable(this.edge.get(0).getType()) && QuerySorterConst.evaluable(this.edge.get(1).getType());
    }
}
