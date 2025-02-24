package fr.inria.corese.core.kgram.sorter.impl.qpv1;

import fr.inria.corese.core.kgram.api.core.ExpType;
import fr.inria.corese.core.kgram.sorter.core.*;

import java.util.List;
import static fr.inria.corese.core.kgram.sorter.core.Const.*;
import static fr.inria.corese.core.kgram.sorter.core.IEstimate.MAX_COST;

/**
 * Cost model for QPG edge 
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 20 oct. 2014
 */
public class QPGEdgeCostModel extends AbstractCostModel {

    //weight 6, 5, 4, 3, 2, 1
    private static final int[][] JOINT_PATTERN = new int[][]{
        {PREDICATE, OBJECT}, {SUBJECT, PREDICATE}, {SUBJECT, OBJECT},
        {OBJECT, OBJECT}, {SUBJECT, SUBJECT}, {PREDICATE, PREDICATE}
    };
    private final QPGEdge edge;
    private int Nshare = 0;
    private int Jtype = -1;

    public QPGEdgeCostModel(QPGEdge edge) {
        this.edge = edge;
        if (this.estimatable()) {
            if(this.edge.get(0).getType()== ExpType.Type.EDGE &&this.edge.get(1).getType()== ExpType.Type.EDGE){
                this.setJtype();
            }
            this.setNshare();
            this.edge.setType(QPGEdge.BI_DIRECT);
        }else{
            this.edge.setType(QPGEdge.SIMPLE);
        }
    }

    private void setNshare() {
        this.Nshare = this.edge.getVariables().size();
    }

    private void setJtype() {
        QPGNode node1 = edge.get(0), node2 = edge.get(1);

        int[][] jp = JOINT_PATTERN;
        for (int i = 0; i < jp.length; i++) {
            int p1 = jp[i][0], p2 = jp[i][1];
            
            if (node1.getExpNode(p1).getLabel().equals(node2.getExpNode(p2).getLabel())
                    || node1.getExpNode(p2).getLabel().equals(node2.getExpNode(p1).getLabel())) {
                Jtype = jp.length - i;
            }
        }
    }

    @Override
    public void estimate(List<Object> params) {
        if(!(isParametersOK(params) && estimatable())){
            this.edge.setCost(IEstimate.NA_COST);
            return;
        }
        
        QPGNode node1 = edge.get(0), node2 = edge.get(1);

        ExpType.Type tNode1 = node1.getType(), tNode2 = node2.getType();
        //1. type of one of them is FILTER or VALUES or BIND, ne assign pas le weight
        if (!Const.evaluable(tNode1) && !Const.evaluable(tNode2)) {
            this.edge.setCost(MAX_COST);
            return;
        }

        //3.2. no pattern matched: means no shared variables
        if (Nshare == 0) {
            this.edge.setCost(MAX_COST);
            return;
        }
        
        //2 The EDGE connects at least a GRAPH
        if (tNode1 == ExpType.Type.GRAPH || tNode2 == ExpType.Type.GRAPH) {
            this.edge.setCost(1.0 / 3.0 * Nshare);
            return;
        }

        //3. two EDGEs
        //3.2. no pattern matched: means no shared variables
        if (Jtype == -1) {
            this.edge.setCost(MAX_COST);
            return;
        }

        //3.3 pattern matched, assign weight
        this.edge.setCost(1.0 / Jtype * 1.0 / Nshare);
    }

    @Override
    public String toString() {
        return "QPGEdgeWeightModel{" + "Nshare=" + Nshare + ", Jtype=" + Jtype + '}';
    }

    @Override
    public boolean isParametersOK(List<Object> params) {
        return true;
    }

    @Override
    final public boolean estimatable() {
        return Const.evaluable(this.edge.get(0).getType()) && Const.evaluable(this.edge.get(1).getType());
    }
}
