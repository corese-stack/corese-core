package fr.inria.corese.core.kgram.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.core.Regex;

/**
 * Record intermediate nodes that are visited by a loop path expression such as
 * exp+ exp* exp{n,} Prevent loop among intermediate nodes
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class Visit {
    int count = 0;
    public static boolean speedUp = false;

    //HashMap<State, List<Node>> table;
    //HashMap<Regex, List<Node>> etable;
    //HashMap<Regex, Table> eetable;
    ExpVisitedNode visitedNode;
    HashMap<Regex, Integer> ctable;
    HashMap<Regex, Node> startNode;
    DTable tdistinct;
    LTable ltable;
    ArrayList<Regex> regexList;

    boolean isReverse = false,
            // isCounting = false : new sparql semantics  
            isCounting = false;

    Visit(boolean isRev, boolean isCount) {
        isCounting = isCount;
        isReverse = isRev && isCount;
        //table = new HashMap<State, List<Node>>();
        //etable = new HashMap<Regex, List<Node>>();
        //eetable = new HashMap<Regex, Table>();
        visitedNode = new ExpVisitedNode(isReverse);
        startNode = new HashMap<>();
        ctable = new HashMap<Regex, Integer>();
        regexList = new ArrayList<Regex>();
        tdistinct = new DTable();
        ltable = new LTable();
    }
    
    void setNode(Regex exp, Node n) {
        startNode.put(exp, n);
    }
    
    Node getNode(Regex exp) {
        return startNode.get(exp);
    }

//    class Table1 extends HashMap<Node, Node> {
//    }

    class NodeTable extends TreeMap<Node, Node> {

        NodeTable() {
            super(new Compare());
        }
    }

    class ExpVisitedNode extends HashMap<Regex, VisitedNode> {
        boolean isReverse = false;

        ExpVisitedNode(boolean b) {
            isReverse = b;
        }

        void add(Regex exp, Node n) {
            VisitedNode t = get(exp);
            if (t == null) {
                t = new VisitedNode(isReverse, count++);
                put(exp, t);
            }
            t.add(n);
        }

        void remove(Regex exp, Node n) {
            VisitedNode t = get(exp);
            t.remove(n);
        }

        boolean loop(Regex exp, Node start) {
            VisitedNode table = get(exp);
            if (table == null) {
                return false;
            }
            boolean b = table.loop(exp, start);
            return b;
        }

        boolean exist(Regex exp) {
            return containsKey(exp);
        }

    }

    VisitedNode getTable(Regex exp) {
        return visitedNode.get(exp);
    }

    class VisitedNode {

        NodeTable table;
        List<Node> list;
        boolean isReverse = false;
        int n;
        
        int getIndex() {
            return n;
        }

        VisitedNode(boolean b, int n) {
            this.n = n;
            isReverse = b;
            if (isReverse) {
                list = new ArrayList<Node>();
            } else {
                table = new NodeTable();
            }
        }
        
        int size() {
            return table.size();
        }
        
        void clear() {
            if (table != null) table.clear();
            if (list != null) list.clear();
        }
        
        @Override
        public String toString() {
            if (table == null) {
                return list.toString();
            }
            return table.toString();
        }

        Collection<Node> values() {
            return table.values();
        }

        void add(Node n) {
            if (isReverse) {
                list.add(n);
            } else {
                table.put(n, n);
            }
        }

        void remove(Node n) {
            if (isReverse) {
                list.remove(list.size() - 1);
            } else {
                table.remove(n);
            }
        }

        boolean loop(Regex exp, Node n) {
            if (isReverse) {
                // @deprecated, used for count only
                if (list == null || list.size() < 2) {
                    return false;
                }
                int last = list.size() - 1;

                // use case: ?s {2,} ex:uri
                // Walk through the list backward (from <uri> to ?s), skip 2 first nodes
                // cf W3C pp test case
                Node node = null;
                int count = 0;
                int min = exp.getMin();
                if (min == -1) {
                    min = 0;
                }

                for (int i = last; i >= 0; i--) {
                    if (count < min) {
                        // continue
                    } else if (count == min) {
                        node = list.get(i);
                    } else {
                        if (list.get(i).equals(node)) {
                            return true;
                        }
                    }
                    count++;
                }

                return false;
            } else {
                return table.containsKey(n);
            }
        }

    }

    class DTable extends TreeMap<Node, NodeTable> {

        DTable() {
            super(new Compare());
        }
    }

    class Compare implements Comparator<Node> {

        // TODO: xsd:integer & xsd:decimal may be considered as same node
        // in loop checking
        public int compare(Node o1, Node o2) {
            return o1.compare(o2);
        }
    }

    static Visit create(boolean isReverse, boolean isCount) {
        return new Visit(isReverse, isCount);
    }

    void clear() {
        //table.clear();
    }

    /**
     * ************************************
     *
     * With Regex
     *
     *************************************
     */
    /**
     * Test whether path loops
     */
    boolean nloop(Regex exp, Node start) {
        if (isReverse) {
            ninsert(exp, start);
            if (visitedNode.loop(exp, start)) {
                visitedNode.remove(exp, start);
                return true;
            }
        } else if (visitedNode.loop(exp, start)) {
            return true;
        } else {
            ninsert(exp, start);
        }

        return false;
    }
    
    VisitedNode getVisitedNode(Regex exp) {
        return visitedNode.get(exp);
    }
    
    

    boolean nfirst(Regex exp) {
        return !visitedNode.exist(exp);
    }

    void ninsert(Regex exp, Node start) {
        if (start == null) {
            // subscribe exp to start() above because start node is not bound yet
            declare(exp);
        } else {
            visitedNode.add(exp, start);
        }
    }

    void declare(Regex exp) {
        if (!regexList.contains(exp)) {
            regexList.add(exp);
        }
    }
    
    void undeclare(Regex exp) {
        regexList.remove(exp);
    }

    boolean knows(Regex exp) {
        return regexList.contains(exp);
    }

    void nset(Regex exp, VisitedNode t) {
        if (t != null) {
            visitedNode.put(exp, t);
        }
    }
    
    VisitedNode cleanVisitedNode(Regex exp) {
        return nunset(exp);
    }

    VisitedNode nunset(Regex exp) {
        VisitedNode t = visitedNode.get(exp);
        visitedNode.remove(exp);
        return t;
    }

    /**
     * use case: ?x p+ ?y 
     * ?x is not bound when path starts first occurrence of p
     * binds ?x node is ?x exp is p+ 
     */
    void nstart(Node node) {
        for (Regex exp : regexList) {
            nstart(exp, node);
        }
    }
    
    void nstart(Regex exp, Node node) {
        switch (exp.retype()) {
            case Regex.PLUS:
                break;
            default:
                visitedNode.add(exp, node); // @todo
            }
    }

    /**
     * same use case as nstart() above leave the binding
     */
    void nleave(Node node) {
        for (Regex exp : regexList) {
            nleave(exp, node);
        }
    }
    
    void nleave(Regex exp, Node node) {
        switch (exp.retype()) {
            case Regex.PLUS:
                break;
            default:
                visitedNode.remove(exp, node); // @todo
            }
    }

    /**
     * Remove node from visited list
     */
    void nremove(Regex exp, Node node) {
        if (node == null) {
            undeclare(exp);
            return;
        }
        if (isCounting) {
            visitedNode.remove(exp, node);
        }
    }

    void set(Regex exp, int n) {
        ctable.put(exp, n);
    }

    void count(Regex exp, int n) {
        Integer i = ctable.get(exp);
        if (i == null) {
            i = 0;
        }
        ctable.put(exp, i + n);
    }

    int count(Regex exp) {
        Integer i = ctable.get(exp);
        if (i == null) {
            return 0;
        }
        return i;
    }

    /**
     * **************************************
     *
     * ?x distinct(exp+) ?y
     *
     */
    boolean isDistinct(Node start, Node node) {
        NodeTable t = tdistinct.get(start);
        if (t == null) {
            return true;
        }
        return !t.containsKey(node);
    }

    void addDistinct(Node start, Node node) {
        NodeTable t = tdistinct.get(start);
        if (t == null) {
            t = new NodeTable();
            tdistinct.put(start, t);
        }
        t.put(node, node);
    }

    /**
     * ********************************************
     * ?x short(regex) ?y ?x distinct(regex) ?y
     *
     */
    /**
     * Table for managing path length for each node
     */
    class LTable extends TreeMap<Node, Record> {

        LTable() {
            super(new Compare());
        }

        void setLength(Node n, Regex exp, int l) {
            Record r = get(n);
            if (r == null) {
                r = new Record(exp, l);
                put(n, r);
            } else if (r.exp == exp) {
                r.len = l;
            }
        }

        Integer getLength(Node n, Regex exp) {
            Record r = get(n);
            if (r != null && r.exp == exp) {
                return r.len;
            } else {
                return null;
            }
        }
    }

    class Record {

        Regex exp;
        Integer len;

        Record(Regex e, Integer l) {
            exp = e;
            len = l;
        }

    }

    void initPath() {
        ltable.clear();
    }
    
    /**
     * New start node: clean the table of visited nodes
     */
    void start(Node node) {
        if (!isCounting) {
            //visitedNode.clear(); // @todo
            clearall();
        }
    }
    
    void clearLast() {
        if (! regexList.isEmpty()) {
            VisitedNode t = getVisitedNode(regexList.get(regexList.size()-1));
            if (t != null) {
                t.clear();
            }
        }
    }
    
    /**
     * New start node: clean the table of visited nodes
     */
    void clearall() {
        int i = 0;
        for (Regex exp : regexList) {
            VisitedNode t = getVisitedNode(exp);
            if (t != null) {
                t.clear();
            }
        }
    }

    void setLength(Node n, Regex exp, int l) {
        ltable.setLength(n, exp, l);
    }

    Integer getLength(Node n, Regex exp) {
        return ltable.getLength(n, exp);
    }

}
