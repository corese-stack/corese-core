package fr.inria.corese.core.compiler.federate;

import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Service;
import fr.inria.corese.core.sparql.triple.parser.Variable;

import java.util.ArrayList;
import java.util.List;

public class Util {
    List<Atom> intersection(Service s1, Service s2) {
        return intersection(s1.getServiceList(), s2.getServiceList());
    }

    List<Atom> intersection(List<Atom> l1, List<Atom> l2) {
        List<Atom> list = new ArrayList<>();
        for (Atom at : l1) {
            if (l2.contains(at)) {
                list.add(at);
            }
        }
        return list;
    }

    boolean equalURI(List<Atom> list1, List<Atom> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (Atom elem : list1) {
            if (!list2.contains(elem)) {
                return false;
            }
        }
        return true;
    }

    boolean equal(List<Variable> list1, List<Variable> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (Variable elem : list1) {
            if (!list2.contains(elem)) {
                return false;
            }
        }
        return true;
    }

    boolean hasIntersection(List<Variable> list1, List<Variable> list2) {
        return !intersectionVariable(list1, list2).isEmpty();
    }

    List<Variable> intersectionVariable(List<Variable> list1, List<Variable> list2) {
        ArrayList<Variable> list = new ArrayList<>();
        for (Variable elem : list1) {
            if (list2.contains(elem) && !list.contains(elem)) {
                list.add(elem);
            }
        }
        return list;
    }

    boolean includedIn(List<Variable> list1, List<Variable> list2) {
        for (Variable elem : list1) {
            if (!list2.contains(elem)) {
                return false;
            }
        }
        return true;
    }

}
