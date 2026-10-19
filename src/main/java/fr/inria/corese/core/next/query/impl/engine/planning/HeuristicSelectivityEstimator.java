package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates pattern and join selectivity using bindings, filters, and connectivity.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class HeuristicSelectivityEstimator implements SelectivityEstimator {

    private QueryPlanGraph graph;
    private QueryStatistics producer;

    @Override
    public void estimate(QueryPlanGraph graph, Producer producer) {
        this.graph = graph;
        this.producer = producer instanceof QueryStatistics queryProducer ? queryProducer : null;

        estimateNodes();
        estimateEdges();
    }

    //assign costs for nodes based on ?-tuple pattern
    private void estimateNodes() {
        // 1. get all models of nodes in QPG
        List<NodeCostModel> models = new ArrayList<>();
        for (QueryPlanNode n : this.graph.getAllNodes()) {
            if (n.getType() == ExpType.Type.EDGE || n.getType() == ExpType.Type.GRAPH) {
                NodeCostModel p = n.getCostModel();
                if (n.getType() == ExpType.Type.EDGE) {
                    p.setParameters(graph);
                }
                models.add(p);
            }
        }

        //** 3 sort by priority
        //generate basic patterns using numbers({Ns Np No} | null)
        int[][] basicPatterns = BindingPatternOrder.generateBasicPattern(producer);
        NodeCostModel.sort(models, basicPatterns, producer);

        //--put the same triple patterns in one list and assign the same selectivity to
        //--all the patterns in this list, needs to be improved by distinguwish the same patterns
        //--using stats data if available
        // l1: t11, t12
        // l2: t21
        // l3: t31, t32, t33
        // l4..
        List<List<NodeCostModel>> modelList = groupModels(models, basicPatterns);

        //** 2 assign cost
        for (int i = 0; i < modelList.size(); i++) {
            for (NodeCostModel model : modelList.get(i)) {
                model.estimate(modelList.size(), i);
            }
        }
    }

    private List<List<NodeCostModel>> groupModels(List<NodeCostModel> models, int[][] patterns) {
        List<List<NodeCostModel>> groups = new ArrayList<>();
        int index = 0;
        while (index < models.size()) {
            NodeCostModel first = models.get(index++);
            List<NodeCostModel> group = new ArrayList<>();
            group.add(first);
            while (index < models.size()
                    && NodeCostModel.compareModel(first, models.get(index), patterns, producer) == 0) {
                group.add(models.get(index++));
            }
            groups.add(group);
        }
        return groups;
    }

    //assign weight/sel for edge between triple pattern
    private void estimateEdges() {
        for (QueryPlanEdge edge : graph.getEdges(QueryPlanEdge.BI_DIRECT)) {
            edge.getCostModel().estimate();
        }
    }
}
