package fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1;

import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimate the selectivity of triple pattern by heuristics
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class HeuristicsBasedEstimation implements IEstimate {

    private QPGraph graph;
    private IProducerQP producer;

    @Override
    public void estimate(QPGraph graph, Producer producer, Object parameters) {
        this.graph = graph;
        this.producer = producer instanceof IProducerQP queryProducer ? queryProducer : null;

        estimateNodes();
        estimateEdges();
    }

    //assign costs for nodes based on ?-tuple pattern
    private void estimateNodes() {
        // 1. get all models of nodes in QPG
        List<QPGNodeCostModel> models = new ArrayList<>();
        for (QPGNode n : this.graph.getAllNodes()) {
            if (n.getType() == ExpType.Type.EDGE || n.getType() == ExpType.Type.GRAPH) {
                QPGNodeCostModel p = n.getCostModel();
                if (n.getType() == ExpType.Type.EDGE) {
                    p.setParameters(graph);
                }
                models.add(p);
            }
        }

        //** 3 sort by priority
        //generate basic patterns using numbers({Ns Np No} | null)
        int[][] basicPatterns = BasicPatternGenerator.generateBasicPattern(producer);
        QPGNodeCostModel.sort(models, basicPatterns, producer);

        //--put the same triple patterns in one list and assign the same selectivity to
        //--all the patterns in this list, needs to be improved by distinguwish the same patterns
        //--using stats data if available
        // l1: t11, t12
        // l2: t21
        // l3: t31, t32, t33
        // l4..
        List<List<QPGNodeCostModel>> modelList = groupModels(models, basicPatterns);

        //** 2 assign cost
        for (int i = 0; i < modelList.size(); i++) {
            for (AbstractCostModel model : modelList.get(i)) {
                model.estimate(List.of(modelList.size(), i));
            }
        }
    }

    private List<List<QPGNodeCostModel>> groupModels(List<QPGNodeCostModel> models, int[][] patterns) {
        List<List<QPGNodeCostModel>> groups = new ArrayList<>();
        int index = 0;
        while (index < models.size()) {
            QPGNodeCostModel first = models.get(index++);
            List<QPGNodeCostModel> group = new ArrayList<>();
            group.add(first);
            while (index < models.size()
                    && QPGNodeCostModel.compareModel(first, models.get(index), patterns, producer) == 0) {
                group.add(models.get(index++));
            }
            groups.add(group);
        }
        return groups;
    }

    //assign weight/sel for edge between triple pattern
    private void estimateEdges() {
        for (QPGEdge edge : graph.getEdges(QPGEdge.BI_DIRECT)) {
            edge.getCostModel().estimate(null);
        }
    }
}
