package fr.inria.corese.core.rule;

import fr.inria.corese.core.kgram.api.core.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Record property cardinality of rule where clause
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 */
public class Record {

    private final int loop;
    Map<Node, Integer> map;
    private int timestamp;
    private Node predicate;
    private Rule rule;
    private int count = 0;

    Record(Rule r, int timestamp, int loop) {
        rule = r;
        this.timestamp = timestamp;
        this.loop = loop;
        map = new HashMap();
    }

    public String toRDF() {
        StringBuilder sb = new StringBuilder();
        sb.append("[a kg:Index");
        sb.append(" ; kg:rule ").append(rule.getIndex());
        sb.append(" ; kg:loop ").append(loop);
        sb.append(" ; kg:time ").append(timestamp);
        for (Node p : map.keySet()) {
            Integer n = map.get(p);
            sb.append("kg:item [ rdf:predicate ").append(p).append(" ; rdf:value ").append(n).append(" ] ;\n");
        }
        sb.append("] .\n");
        return sb.toString();
    }

    /**
     * Accept a rule if there are new triples
     */
    boolean accept(Record oldRecord) {
        int n = 0;
        for (Node pred : rule.getPredicates()) {
            if (get(pred) > oldRecord.get(pred)) {
                n++;
                setPredicate(pred);
            }
        }
        setCount(n);
        return n > 0;
    }

    Integer get(Node n) {
        return map.get(n);
    }

    void put(Node n, Integer i) {
        map.put(n, i);
    }


    public int getTimestamp() {
        return timestamp;
    }


    public void setTimestamp(int index) {
        this.timestamp = index;
    }


    public int nbNewPredicate() {
        return count;
    }


    public void setCount(int count) {
        this.count = count;
    }


    public Node getPredicate() {
        return predicate;
    }


    public void setPredicate(Node predicate) {
        this.predicate = predicate;
    }


    public Rule getRule() {
        return rule;
    }


    public void setRule(Rule rule) {
        this.rule = rule;
    }
}
