package fr.inria.corese.core.next.query.impl.kgram.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.IEstimate;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.ISort;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.QPGNode;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.QPGraph;
import fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1.DepthFirstBestSearch;
import fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1.HeuristicsBasedEstimation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.QuerySorterConst.plannable;

/**
 * A new sorter for QP
 *
 */
public class SorterNew extends Sorter {

    public void sort(Exp expression, List<Exp> bindings, Producer prod) {
        if (expression.size() < 2) return;

        Map<Integer, List<Exp>> ESGs = tokenize(expression);
        if (ESGs.isEmpty()) return;


        // === Iterate each sub set and create ESG and sorting ===
        for (Entry<Integer, List<Exp>> entrySet : ESGs.entrySet()) {
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

            //** For the following case where a graph has a bound value, put the BIND before
            //** the graph pattern (because normally we put BIND just after where its variable
            //** is used.
            //** bind (<uri> as ?g)
            //** graph ?g {?x ?p ?y}
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
        List<Exp> aESG = new ArrayList<>();
        Map<Integer, List<Exp>> ESGs = new LinkedHashMap<>();
        ESGs.put(0, aESG);

        // == 1. split the expressions
        List<Exp> exps = e.getExpList();
        for (int i = 0; i < exps.size(); i++) {
            Exp ee = exps.get(i);
            if (plannable(ee.type())) {
                if (i > 0 && !plannable(exps.get(i - 1).type())) {
                    aESG = new ArrayList<>();
                    ESGs.put(i, aESG);
                }

                aESG.add(ee);
            }
        }

        // == 2.remove the ones containing less that 2 expressions
        ESGs.entrySet().removeIf(integerListEntry -> integerListEntry.getValue().size() < 2);

        return ESGs;
    }
}
