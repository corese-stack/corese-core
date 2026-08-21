package fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.sorter.core.*;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.QuerySorterConst.*;
import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.IEstimate.MAX_COST;
import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.IEstimate.MIN_COST_0;

/**
 * Class for constructing the pattern of a triple, including many parameters
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class QPGNodeCostModel extends AbstractCostModel {

    //FV: filter, number of variables(in the triple pattern) appeared in all the filters
    //    the more the better (less selectivity)
    //FN: filter, variables appeared in how many filters
    //G: graph
    private final static int S = 0, P = 1, O = 2, G = 3, FF = 4, FV = 5;
    private final static int PARAMETER_LEN = 6;
    //list of vairables appeared in the expression
    List<String> variables = new ArrayList<>();
    private final int[] pattern = new int[PARAMETER_LEN];

    private final QPGNode node;

    public QPGNodeCostModel(QPGNode n, List<Exp> bindings) {
        this.node = n;
        //todo with GNode
        Node gNode;

        if (n.getType() == ExpType.Type.EDGE) {
            Edge e = n.getExp().getEdge();

            //the value of S P O G can be: 0[bound], ?[length of constant list], Integer.MAX_VALUE[unbound]
            this.pattern[S] = getNodeType(e.getNode(0), bindings);
            this.pattern[P] = getNodeType(e.getEdgeVariable() == null ? e.getEdgeNode() : e.getEdgeVariable(), bindings);
            this.pattern[O] = getNodeType(e.getNode(1), bindings);
            this.pattern[G] = BOUND; //triple pattern appeared in default graph is considered as BOUND

            // get the variables in the triple pattern
            n.getExp().getVariables(variables);
        } else if (n.getType() == ExpType.Type.GRAPH) {
            //obtain the graph node
            if (n.getExp().size() > 0 && n.getExp().get(0).type() == ExpType.Type.GRAPHNODE) {
                //GRAPH{GRAPHNODE{NODE{data:aliceFoaf } } AND...}
                gNode = n.getExp().get(0).get(0).getNode();
                this.pattern[G] = getNodeType(gNode, bindings);
            }
        }
    }

    @Override
    public void estimate(List<Object> params) {
        if (!(isParametersOK(params) && estimatable())) {
            this.node.setCost(Integer.MAX_VALUE);
            return;
        }

        int size = (Integer) params.get(0);
        int index = (Integer) params.get(1);

        //size == 1 means there is only one list, in which all nodes should have same costs
        if(size == 1){
            this.node.setCost(MAX_COST);
            return;
        }

        double itl = (MAX_COST - MIN_COST_0) / (size - 1);
        this.node.setCost(itl * index + MIN_COST_0);
    }

    //Get the type of a node (s p o) in a triple pattern
    //Bound (constant) or unbound(varaible) or detected constant(s) (list of constant(s))
    private int getNodeType(Node n, List<Exp> bindings) {
        if (n == null) {
            return NA;
        }
        return n.isVariable() ? (isBound(bindings, n) ? LIST : UNBOUND) : BOUND;
    }

    /**
     * Set the paramters other than basic paremeters, only can be set after
     * construction of the whole basic pattern graph
     *
     */
    public void setParameters(QPGraph graph) {
        this.setFilterNumber(graph);
        //other parameters can be added here if needed
        //...
    }

    // FV: filter variables
    // FF: filter number
    private void setFilterNumber(QPGraph graph) {
        List<QPGNode> nodes = graph.getAllNodes(ExpType.Type.FILTER);

        int noOfFilter = 0, noOfVariable = 0;

        //1 get all variables in all filters
        List<String> variablesInFilters = new ArrayList<>();

        //**2 check triple pattern has how many filters FF
        for (QPGNode n : nodes) {
            List<String> l = n.getExp().getFilter().getVariables();
            variablesInFilters.addAll(l);

            for (String var1 : l) {
                boolean flag = false;
                for (String var2 : variables) {
                    if (var1.equalsIgnoreCase(var2)) {
                        noOfFilter++;
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    break;
                }
            }
        }
        this.pattern[FF] = noOfFilter;

        //**3 calculate no. of variables appeared in all filters
        for (String var1 : variablesInFilters) {
            for (String var2 : variables) {
                if (var1.equalsIgnoreCase(var2)) {
                    noOfVariable++;
                }
            }
        }
        this.pattern[FV] = noOfVariable;

    }

    /**
     * Find match by using triple patterns list
     *
     */
    public final int match(int[][] basicPatterns) {
        for (int i = 0; i < basicPatterns.length; i++) {
            boolean match = true;
            for (int j = 0; j < basicPatterns[i].length; j++) {
                match = match && (basicPatterns[i][j] == this.pattern[j]);
            }

            if (match) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Sort a list of triple patterns according to predefined rules
     *
     * @param bp basic patterns
     * @param ip interface producer
     */
    public static void sort(List<QPGNodeCostModel> patterns, final int[][] bp, final IProducerQP ip) {
        patterns.sort((m1, m2) -> compareModel(m1, m2, bp, ip));
    }

    //order by selectivity asending(0-1) (more selective - less selective)
    public static int compareModel(QPGNodeCostModel m1, QPGNodeCostModel m2, final int[][] bp, final IProducerQP ip) {
        //GRAPH
        //rule 1: GRAPH has more priority than EDGE
        ExpType.Type type1 = m1.node.getExp().type();
        ExpType.Type type2 = m2.node.getExp().type();

        //to/can be extended!!
        //when comparing EDGE/GRAPH, EDGE has priority except if EDGE = (? ? ?)
        //R1
        if (type1 == ExpType.Type.GRAPH && type2 == ExpType.Type.EDGE) {
            return (m2.match(bp) == (bp.length - 1)) ? -1 : 1;
        } else if (type1 == ExpType.Type.EDGE && type2 == ExpType.Type.GRAPH) {
            return (m1.match(bp) == (bp.length - 1)) ? 1 : -1;
            //when comparing two GRAPHs, compare by the type of named graph (bound, unbound, etc...)
        } else if (type1 == ExpType.Type.GRAPH && type2 == ExpType.Type.GRAPH) {
            // when comparing two GRAPHs, compare by the type of named graph (bound, unbound, etc...)
            return Integer.compare(m1.pattern[G], m2.pattern[G]);
        } else { // EDGE - EDGE
            // get the sequence in the simple pattern list
            int i1 = m1.match(bp);
            int i2 = m2.match(bp);

            // R2
            if (i1 != i2) {
                return Integer.compare(i1, i2);
            }

            // R2a - only for {? p ?}, because for other patterns it's not accurate
            boolean isPredicateOnlyPattern = m1.pattern[S] == 1 && m1.pattern[P] == 0 && m1.pattern[O] == 1;
            if (ip != null && isPredicateOnlyPattern) {
                int np1 = ip.getCount(m1.node, PREDICATE);
                int np2 = ip.getCount(m2.node, PREDICATE);
                if (np1 != np2) {
                    return Integer.compare(np1, np2);
                }
            }

            // R4
            if (m1.pattern[FF] != m2.pattern[FF]) {
                return Integer.compare(m2.pattern[FF], m1.pattern[FF]); // Reversed comparison
            }

            // R5
            if (m1.pattern[FV] != m2.pattern[FV]) {
                return Integer.compare(m2.pattern[FV], m1.pattern[FV]); // Reversed comparison
            }

            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("model [");
        for (int j : this.pattern) {
            s.append(j).append(", ");
        }

        s.append(node.getCost()).append("] ");
        return s.toString();
    }

    public boolean isBound(List<Exp> bindings, Node var) {
        for (Exp exp : bindings) {
            if (var.getLabel().equalsIgnoreCase(exp.get(0).getNode().getLabel())) {
                //todo in future (or not)??
                //calculate the number of constants bound to this variable
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isParametersOK(List<Object> params) {
        return params != null && params.size() == 2 && params.get(0) instanceof Integer
                && params.get(1) instanceof Integer;
    }

    @Override
    public boolean estimatable() {
        return QuerySorterConst.evaluable(this.node.getType());
    }

    }
