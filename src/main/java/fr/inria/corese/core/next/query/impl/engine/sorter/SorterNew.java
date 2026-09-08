package fr.inria.corese.core.next.query.impl.engine.sorter;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorterConst.plannable;

/**
 * A new sorter for QP
 *
 */
public class SorterNew extends Sorter {

    @SuppressWarnings("java:S3776")
    public void sort(Exp expression, List<Exp> bindings, Producer prod) {
        if (expression.size() < 2) return;

        Map<Integer, List<Exp>> esgMap = tokenize(expression);
        if (esgMap.isEmpty()) return;


        // === Iterate each sub set and create ESG and sorting ===
        for (Entry<Integer, List<Exp>> entrySet : esgMap.entrySet()) {
            Integer startIndex = entrySet.getKey();
            List<Exp> exps = entrySet.getValue();

            // ** 1 create graph (list of nodes and their edges) **
            QPGraph bpg = new QPGraph(exps, bindings);

            // ** 2 estimate cost/selectivity **
            // ** 2.1 find the corresponding algorithm **
            IEstimate ies = new HeuristicsBasedEstimation();

            // ** 2.2 estimate **
            ies.estimate(bpg, prod, bindings);

            // ** 3 sort and find the order **
            ISort is;
            is = new DepthFirstBestSearch();

            List<QPGNode> l = is.sort(bpg);

            // For the case where a graph has a bound value, put the BIND before
            // the graph pattern (because normally we put BIND just after where its variable is used).
            // Example: when binding a graph variable before a graph pattern
            List<QPGNode> graphs = bpg.getAllNodes(ExpType.Type.GRAPH);
            for (QPGNode graph : graphs) {
                List<QPGNode> linkedNodes = bpg.getLinkedNodes(graph);
                for (QPGNode bind : linkedNodes) {
                    if (bind.getType() == ExpType.Type.BIND) {
                        int iBind = l.indexOf(bind);
                        int iGraph = l.indexOf(graph);
                        if (iBind > iGraph) {
                            l.remove(bind);
                            l.add(iGraph, bind);
                        }
                    }
                }
            }

            // ** 4 rewrite **
            is.rewrite(expression, l, startIndex);
        }

    }

    // === Split one expression into several subsets that can be sorted
    //ex. T1, T2, F, OPT, T3, T4, VA, UNION, T5, T6 will be splited
    // (0, <T1, T2, F>)
    // (4, <T3, T4, VA>)
    // (8, <T5, T6>)
    private Map<Integer, List<Exp>> tokenize(Exp e) {
        List<Exp> group = new ArrayList<>();
        Map<Integer, List<Exp>> esgMap = new LinkedHashMap<>();
        esgMap.put(0, group);

        // == 1. split the expressions
        List<Exp> exps = e.getExpList();
        for (int i = 0; i < exps.size(); i++) {
            Exp ee = exps.get(i);
            if (plannable(ee.type())) {
                if (i > 0 && !plannable(exps.get(i - 1).type())) {
                    group = new ArrayList<>();
                    esgMap.put(i, group);
                }

                group.add(ee);
            }
        }

        // == 2.remove the ones containing less that 2 expressions
        esgMap.entrySet().removeIf(integerListEntry -> integerListEntry.getValue().size() < 2);

        return esgMap;
    }
}
