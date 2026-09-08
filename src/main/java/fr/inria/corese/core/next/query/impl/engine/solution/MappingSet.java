package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Utilitary class for Join Minus Optional
 * Compute common variables in left and right expression
 * and whether common variables are always bound
 * Select subset of Mappings from left exp relevant for right expression
 * optional(s p o, minus(o q t, t r s))
 * variable o can be used in minus expression, variable s cannot
 * special case for union:
 * join(s p o, union(s q t, o q t))
 * there is no common variable bound in branches of union
 * return complete Mappings, then union() will process it in the branches: left s and right o
 * In addition, in case of successful subset of Mappings, return also the original Mappings in case there is
 * an union in right expression
 * If union is in subquery, the subquery may skip the original Mappings depending on its select
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 */
public class MappingSet {
    Mappings map;
    Exp exp;
    MappingSet set1;
    MappingSet set2;
    HashMap<String, String> union;        // union of variables
    HashMap<String, String> intersection; // intersection of variables
    List<String> varList;
    boolean isBound = false;


    public MappingSet(Query q, Exp exp, MappingSet s1, MappingSet s2) {
        this.exp = exp;
        this.set1 = s1;
        this.set2 = s2;
        setQuery(q);
    }

    public MappingSet(Query q, Mappings mappings) {
        this.map = mappings;
        union = new HashMap<>();
        intersection = new HashMap<>();
        setQuery(q);
        process();
    }

    public Mappings getMappings() {
        return map;
    }

    /**
     * Variables in common in map1 and map2
     */
    List<String> computeVarList() {
        return set1.intersectionOfUnion(set2);
    }

    /**
     * Common variables bound in every Mapping in map1 and map2 ?
     */
    boolean checkBound(List<String> vars) {
        return set1.inIntersection(vars) && set2.inIntersection(vars);
    }

    boolean hasIntersection(List<Node> nodeList) {
        for (Node node : nodeList) {
            if (getIntersection().containsKey(node.getLabel())) {
                return true;
            }
        }
        return false;
    }

    public void start() {
        varList = computeVarList();
        isBound = checkBound(varList);

        if (isBound) {
            set2.getMappings().sort(varList);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("inter: ").append(getIntersection()).append(System.lineSeparator());
        sb.append("union: ").append(getUnion()).append(System.lineSeparator());
        if (set1 != null) {
            sb.append("first var: ").append(set1.getUnion()).append(System.lineSeparator());
        }
        if (set2 != null) {
            sb.append("rest var:  ").append(set2.getUnion()).append(System.lineSeparator());
        }
        if (varList != null) {
            sb.append("common var:  ").append(varList).append(" always bound: ").append(isBound);
        }
        return sb.toString();
    }

    /**
     * return Mapping in map2 potential compatible with m in map1 for optional
     * If common variables are bound in every Mapping,
     * find potential compatible Mapping by dichotomy and iterate
     * else return all Mappings
     * PRAGMA: if isBound, map2 has been sorted by start() above
     */
    public Iterable<Mapping> getCandidateMappings(Mapping m) {
        if (isBound) {
            return () -> new Iterate(m);
        }
        return set2.getMappings();
    }

    /**
     * Is there one Mapping in map2 minus compatible with map in map1
     */
    public boolean minusCompatible(Mapping targetMap) {
        if (varList.isEmpty()) {
            // no common variables
            return false;
        } else {
            if (isBound) {
                // check map compatible by dichotomy in map2
                return set2.getMappings().minusCompatible(targetMap, varList);
            } else {
                for (Mapping minus : set2.getMappings()) {
                    // enumerate map2
                    if (targetMap.minusCompatible(minus, varList)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    List<String> intersectionOfUnion(MappingSet model) {
        ArrayList<String> commonVars = new ArrayList<>();
        for (String v : getUnion().keySet()) {
            if (model.getUnion().containsKey(v)) {
                commonVars.add(v);
            }
        }
        return commonVars;
    }

    boolean inIntersection(List<String> vars) {
        return inSet(vars, getIntersection());
    }

    boolean inSet(List<String> vars, HashMap<String, String> table) {
        for (String v : vars) {
            if (!table.containsKey(v)) {
                return false;
            }
        }
        return true;
    }

    HashMap<String, String> getUnion() {
        return union;
    }

    HashMap<String, String> getIntersection() {
        return intersection;
    }

    /**
     * compute union and intersection of variables in Mappings map
     */
    void process() {
        if (!map.isEmpty()) {
            Mapping m = map.get(0);
            for (String varString : m.getVariableNames()) {
                intersection.put(varString, varString);
            }
        }
        List<String> remove = new ArrayList<>();
        for (Mapping m : map) {
            for (String varString : m.getVariableNames()) {
                union.put(varString, varString);
            }
            remove.clear();
            for (String varString : intersection.keySet()) {
                if (m.getNodeValue(varString) == null) {
                    remove.add(varString);
                }
            }
            for (String varString : remove) {
                intersection.remove(varString);
            }
        }
    }


    /**
     * in-scope variables in exp except bind except those that are only in
     * right arg of an optional in exp and skip statements after first
     * union/minus/optional/graph in exp
     */
    public Mappings prepareMappingsRest(Exp expression) {
        List<Node> nodeListInScope = expression.getRecordInScopeNodesWithoutBind();
        if (!nodeListInScope.isEmpty() && hasIntersection(nodeListInScope)) {
            // generate values when at least one variable in-subscope is always
            // bound in map1, otherwise it would generate duplicates in map2
            // or impose irrelevant bindings
            // map = select distinct map1 wrt exp inscope nodes
            Mappings distinctMap = getMappings().distinct(nodeListInScope);
            distinctMap.setNodeList(nodeListInScope);
            // record original Mappings because union in exp may process it
            // more precisely. see Eval unionData()
            // s p o {s q r} union {o q r}
            // map node list = {r}
            // whereas we can get s for first branch and o for second branch
            // this is why we record original Mappings for union in exp if any
            distinctMap.setJoinMappings(getMappings());
            return distinctMap;
        }
        // there is no in-scope variable.
        // return original Mappings in case of union in exp (see comment above)
        return getMappings();
    }

    public void setQuery(Query query) {
        // query is kept for compatibility
    }

    class Iterate implements Iterator<Mapping> {

        int n;
        Mapping m;
        Mappings targetMap;

        Iterate(Mapping m) {
            targetMap = set2.getMappings();
            this.n = targetMap.find(m, varList);
            this.m = m;
        }

        @Override
        public boolean hasNext() {
            return n >= 0 && n < targetMap.size() && targetMap.get(n).optionalCompatible(m, varList);
        }

        @Override
        public Mapping next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return targetMap.get(n++);
        }
    }

}
