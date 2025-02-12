/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.corese.core.query;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Given two bnodes representing OWL expressions
 * test if they represent the same expression
 * <p>
 * TODO:
 * two RDF List with same elements but in different order do not match yet
 *
 * @author Olivier Corby, Wimmics Inria I3S, 2014
 */
public class MatchBNode {

    TreeNode ttrue;
    TreeNode tfalse;
    Graph graph;
    int count = 0;

    MatchBNode(Graph g) {
        ttrue = new TreeNode();
        tfalse = new TreeNode();
        graph = g;
    }

    /**
     * Store the result in a table
     */
    boolean same(Node n1, Node n2, Environment env, int n) {
        IDatatype dt1 = getValue(n1);
        IDatatype dt2 = getValue(n2);

        IDatatype dt = ttrue.get(dt1);
        if (dt != null && dt.same(dt2)) {
            return true;
        }

        dt = tfalse.get(dt1);
        if (dt != null && dt.same(dt2)) {
            return false;
        }

        boolean b = match(n1, n2, new TreeNode(), n);
        count++;

        if (b) {
            ttrue.put(dt1, dt2);
            ttrue.put(dt2, dt1);
        } else {
            tfalse.put(dt1, dt2);
            tfalse.put(dt2, dt1);
        }
        return b;
    }

    public int getCount() {
        return count;
    }

    public TreeNode getTree(boolean b) {
        if (b) {
            return ttrue;
        } else {
            return tfalse;
        }
    }

    /**
     * Two different blank nodes match if they have the same edges and their
     * target nodes recursively match (same term or blank match)
     * TreeNode tree ensure that same bnode is compared with same bnode during recursion
     * in case of loop
     * PRAGMA: n1 n2 are bnodes
     */
    boolean match(Node n1, Node n2, TreeNode tree, int n) {
        if (n1.same(n2)) {
            return true;
        }

        IDatatype dt1 = getValue(n1);
        IDatatype dt2 = getValue(n2);
        IDatatype dt = tree.get(dt1);
        if (dt != null) {
            // we forbid to match another blank node
            // TODO:  check this
            return dt.same(dt2);
        } else {
            tree.put(dt1, dt2);
        }

        boolean suc = false;

        List<Node> ln1 = graph.getList(n1);

        if (ln1.isEmpty()) {
            List<Edge> l1 = graph.getEdgeListSimple(n1);
            List<Edge> l2 = graph.getEdgeListSimple(n2);

            if (size(l1, l2, n)
                    && match(l1, l2)
                    && match(l1, l2, tree, n + 1)) {
                suc = true;
            }
        } else {
            List<Node> ln2 = graph.getList(n2);
            if (ln1.size() == ln2.size()
                    && matchList(ln1, ln2, tree, n)) {
                suc = true;
            }
        }

        tree.remove(dt1);
        return suc;
    }

    boolean match(List<Edge> l1, List<Edge> l2, TreeNode tree, int n) {
        for (int i = 0; i < l1.size(); i++) {

            Edge e1 = l1.get(i);
            Edge e2 = l2.get(i);
            boolean b = match(e1, e2, tree, n);
            if (!b) {
                return false;
            }
        }
        return true;
    }

    /**
     * Match l1 elements to l2 elements, in any order
     */

    boolean matchList(List<Node> l1, List<Node> l2, TreeNode tree, int n) {
        for (Node n1 : l1) {

            int i = 0;
            boolean suc = false;

            for (Node n2 : l2) {
                if (compare(n1, n2, tree, n)) {
                    suc = true;
                    break;
                } else {
                    i++;
                }
            }

            if (suc) {
                l2.remove(i);
            } else {
                return false;
            }
        }

        return true;
    }


    boolean size(List<Edge> l1, List<Edge> l2, int n) {
        return l1.size() == l2.size();
    }

    boolean match(List<Edge> l1, List<Edge> l2) {

        for (int i = 0; i < l1.size(); i++) {
            Edge e1 = l1.get(i);
            Edge e2 = l2.get(i);
            if (!e1.getEdgeNode().equals(e2.getEdgeNode())) {
                return false;
            }
        }

        return true;
    }

    IDatatype getValue(Node n) {
        return n.getValue();
    }

    boolean match(Edge e1, Edge e2, TreeNode t, int n) {

        return compare(e1.getNode(1), e2.getNode(1), t, n);
    }

    boolean compare(Node x, Node y, TreeNode t, int n) {
        if (x.same(y)) {
            return true;
        } else if (x.isBlank() && y.isBlank()) {
            return match(x, y, t, n);
        }
        return false;
    }

    /**
     * This Comparator enables to retrieve an occurrence of a given Literal
     * already existing in graph in such a way that two occurrences of same
     * Literal be represented by same Node in graph It (may) represent (1
     * integer) and (1.0 float) as two different Nodes Current implementation of
     * EdgeIndex sorted by values ensure join (by dichotomy ...)
     */
    static class Compare implements Comparator<IDatatype> {

        public int compare(IDatatype dt1, IDatatype dt2) {

            // xsd:integer differ from xsd:decimal
            // same node for same datatype
            if (dt1.getDatatypeURI() != null && dt2.getDatatypeURI() != null) {
                int cmp = dt1.getDatatypeURI().compareTo(dt2.getDatatypeURI());
                if (cmp != 0) {
                    return cmp;
                }
            }

            return dt1.compareTo(dt2);
        }
    }

    public class TreeNode extends TreeMap<IDatatype, IDatatype> {

        TreeNode() {
            super(new Compare());
        }

    }


}
