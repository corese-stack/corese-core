package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.Iterator;

/**
 * Handles relational set operations (join, union, minus, optional) on {@link Mappings}.
 */
public final class MappingsRelationalOperations {

    private final Mappings mappings;

    public MappingsRelationalOperations(Mappings mappings) {
        this.mappings = mappings;
    }

    /**
     * Returns the common node between the first solutions of two mappings, or null.
     */
    public Node getCommonNode(Mappings other) {
        if (mappings.isEmpty() || other.isEmpty()) {
            return null;
        }
        return mappings.get(0).getCommonNode(other.get(0));
    }

    /**
     * Computes the union of two Mappings sets.
     */
    public Mappings union(Mappings lm) {
        Mappings res = (mappings.getQuery() != null && mappings.getQuery() == lm.getQuery())
                ? Mappings.create(mappings.getQuery())
                : new Mappings();
        for (Mapping m : mappings) {
            res.add(m);
        }
        for (Mapping m : lm) {
            res.add(m);
        }
        return res;
    }

    /**
     * Computes the inner join of two Mappings sets.
     */
    public Mappings join(Mappings lm) {
        Mappings res = Mappings.create(mappings.getQuery());
        for (Mapping m1 : mappings) {
            for (Mapping m2 : lm) {
                Mapping map = m1.join(m2);
                if (map != null) {
                    res.add(map);
                }
            }
        }
        return res;
    }

    /**
     * Computes the merge joiner of two Mappings sets.
     */
    public Mappings joiner(Mappings lm) {
        Mappings res = Mappings.create(mappings.getQuery());
        for (Mapping m1 : mappings) {
            for (Mapping m2 : lm) {
                Mapping map = m1.merge(m2);
                if (map != null) {
                    res.add(map);
                }
            }
        }
        return res;
    }

    /**
     * Optimized joiner when map2 is sorted on common variable cmn.
     */
    public Mappings joiner(Mappings map2, Node cmn) {
        Mappings res = Mappings.create(mappings.getQuery());
        for (Mapping m1 : mappings) {
            Node val = m1.getNodeValue(cmn);
            if (val == null) {
                joinCommonUnbound(res, m1, map2);
            } else {
                joinCommonBound(res, m1, map2, cmn, val);
            }
        }
        return res;
    }

    private void joinCommonUnbound(Mappings res, Mapping m1, Mappings map2) {
        for (Mapping m2 : map2) {
            Mapping m = m1.merge(m2);
            if (m != null) {
                res.add(m);
            }
        }
    }

    private void joinCommonBound(Mappings res, Mapping m1, Mappings map2, Node cmn, Node val) {
        joinPrefixUnbound(res, m1, map2, cmn);
        joinMatchingRange(res, m1, map2, cmn, val);
    }

    private void joinPrefixUnbound(Mappings res, Mapping m1, Mappings map2, Node cmn) {
        for (Mapping m2 : map2) {
            Node val2 = m2.getNodeValue(cmn);
            if (val2 == null) {
                Mapping m = m1.merge(m2);
                if (m != null) {
                    res.add(m);
                }
            } else {
                break;
            }
        }
    }

    private void joinMatchingRange(Mappings res, Mapping m1, Mappings map2, Node cmn, Node val) {
        int index = map2.find(val, cmn);
        if (index >= 0 && index < map2.size()) {
            for (int i = index; i < map2.size(); i++) {
                Mapping m2 = map2.get(i);
                Node n2 = m2.getNodeValue(cmn);
                if (n2 == null || !val.match(n2)) {
                    break;
                }
                Mapping m = m1.merge(m2);
                if (m != null) {
                    res.add(m);
                }
            }
        }
    }

    /**
     * Computes the SPARQL MINUS between this and lm.
     */
    public Mappings minus(Mappings lm) {
        Mappings res = new Mappings();
        for (Mapping m1 : mappings) {
            boolean ok = true;
            for (Mapping m2 : lm) {
                if (m1.compatible(m2)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                res.add(m1);
            }
        }
        return res;
    }

    /**
     * Computes the SPARQL OPTIONAL between this and lm.
     */
    public Mappings option(Mappings lm) {
        Mappings res = new Mappings();
        for (Mapping m1 : mappings) {
            boolean ok = false;
            for (Mapping m2 : lm) {
                Mapping map = m1.join(m2);
                if (map != null) {
                    ok = true;
                    res.add(map);
                }
            }
            if (!ok) {
                res.add(m1);
            }
        }
        return res;
    }

    /**
     * In-place join of a variable with a bound value.
     */
    public void join(Node varNode, Node val) {
        if (mappings.getSelect() != null && !mappings.getSelect().contains(varNode)) {
            mappings.getSelect().add(varNode);
        }
        Iterator<Mapping> it = mappings.getMappingList().iterator();
        while (it.hasNext()) {
            Mapping m = it.next();
            Node node = m.getNodeValue(varNode);
            if (node == null) {
                m.addNode(varNode, val);
            } else if (!node.equals(val)) {
                it.remove();
            }
        }
    }
}
