package fr.inria.corese.core.next.query.kgram.core;

import fr.inria.corese.core.next.query.kgram.api.core.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Sort KGRAM edges in connected order before query process
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 */
public class Sorter {


    public void sort(Query q, Exp exp, List<String> lVar, List<Exp> lBind) {

        List<Node> lNode = new ArrayList<>();

        for (int i = 0; i < exp.size(); i++) {
            Exp e1 = exp.get(i);

            if (e1.isSortable()) {
                if (!lNode.isEmpty() || !lVar.isEmpty() || !leaveFirst()) {
                    // Process expressions unless we're leaving first edge at its place
                    for (int j = i + 1; j < exp.size(); j++) {
                        Exp e2 = exp.get(j);
                        if (e2.isOption()) {
                            // cannot move option because it may bind free variables
                            // that may influence next exp
                            break;
                        } else if (e2.isSortable()) {

                            if (shift(q, e1, e2, lNode, lVar, lBind)) {
                                // ej<ei : put ej at i and shift
                                for (int k = j; k > i; k--) {
                                    // shift to the right
                                    exp.set(k, exp.get(k - 1));
                                }
                                exp.set(i, e2);
                                e1 = e2;
                            }
                        }
                    }
                }

                e1.bind(lNode);
            }
        }
    }


    public boolean leaveFirst() {
        return true;
    }

    /**
     * variable node bound count 2 constant node count 1 variable node not bound
     * count 0
     */
    boolean shift(Query q, Exp e1, Exp e2, List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        if (e1.isGraphPath(e2, e1)) {
            return true;
        }
        if (e1.isGraphPath(e1, e2)) {
            return false;
        }

        return before(q, e1, e2, lNode, lVar, lBind);
    }

    protected boolean before(Query q, Exp e1, Exp e2, List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        int n1 = e1.nBind(lNode, lVar, lBind);
        int n2 = e2.nBind(lNode, lVar, lBind);
        
        if (beforeBind(q, e2, e1)){
            n2 += 1;
        }


        return n2 > n1;
    }

    /**
     * Edge with node in bindings has advantage
     */
    protected boolean beforeBind(Query q, Exp e2, Exp e1) {
        Mappings list = q.getActualMappings();
        if (list != null && !list.isEmpty()) {
            Mapping map = list.get(0);
            if (e1.bind(map)) {
                return false;
            }
            return e2.bind(map);
        }
        return false;
    }

  
}
