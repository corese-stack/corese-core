package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.query.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;

import java.util.*;

/**
 * Data structure: Key -> (node -> Value)
 * Key: (?var, <uri>) Value: (node, similarity, algs)
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class ApproximateSearchEnv {

    private static int code = 0;
    private final int id;
    private final Map<Key, Map<Node, Value>> all;
    
    public ApproximateSearchEnv() {
        this.id = code++;
        this.all = new HashMap<>();
    }

    public void add(Expr var, Node uri, Node node, String alg, double sim) {
        Key key = new Key(var, uri);

        if (all.containsKey(key)) {
            Map<Node, Value> value = all.get(key);
            if (!value.containsKey(node)) {
                Value r = new Value(node, alg, sim);
                value.put(node, r);
            }
        } else {
            Map<Node, Value> m = new HashMap<>();
            Value r = new Value(node, alg, sim);
            m.put(node, r);
            all.put(key, m);
        }
    }

    public Double getSimilarity(Expr var, Node node) {
        Key key = new Key(var);
        Value r = this.get(key, node);
        return (r == null) ? null : r.getSimilarity();
    }

    private Value get(Key key, Node node) {
        if (this.all.containsKey(key)) {
            if (this.all.get(key).containsKey(node)) {
                return this.all.get(key).get(node);
            }
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Appx search [" + this.id + "]\n");
        for (Map.Entry<Key, Map<Node, Value>> entrySet : all.entrySet()) {
            Key key = entrySet.getKey();
            Map<Node, Value> value = entrySet.getValue();
            sb.append(key).append("\n");
            for (Value v : value.values()) {
                sb.append("\t").append(v.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Aggregate and get value of similarity using all existing variables
     * 
     */
    public Double aggregate(Environment env) {
        List<Expr> lv = this.getVariables();
        return aggregate(env, lv);
    }

    /**
     * Aggreate with existing variables (excpet for the given variable)
     */
    public Double aggregate(Environment env, Expr var, double sim) {
        List<Expr> lv = this.getVariables();
        lv.remove(var);

        double cb = (lv.isEmpty()) ? 1 : aggregate(env, lv);
        return cb * sim;
    }

    private Double aggregate(Environment env, List<Expr> lv) {
        if (lv.isEmpty()) {
            return 1.0;
        }

        double sim = 1;

        //calculate similarity
        for (Expr var : lv) {
            Node node = env.getNode(var);
            if (node == null) {
                continue;
            }

            Double s = this.getSimilarity(var, (Node) node.getValue());
            if (s != null) {
                sim *= s;
            }
        }
        return sim;
    }

    public List<Expr> getVariables() {
        List<Expr> lv = new ArrayList<>();
        for (Key k : this.all.keySet()) {
            lv.add(k.getVar());
        }

        return lv;
    }

    static class Key {

        private final Expr var;
        private Node uri;

        public Key(Expr var, Node uri) {
            this(var);
            this.uri = uri;
        }

        public Key(Expr var) {
            this.var = var;
        }

        public Expr getVar() {
            return var;
        }

        public Node getUri() {
            return uri;
        }

        @Override
        public int hashCode() {
            return 5;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            return Objects.equals(this.var, other.var);
        }

        @Override
        public String toString() {
            return "Key{" + "var=" + var + ", uri=" + uri + '}';
        }
    }

    static class Value {

        private final Node node;
        private double similarity = -1;
        private final String algorithms;

        public Value(Node node, String algorithms, double sim) {
            this(node, algorithms);
            this.similarity = sim;
        }

        public Value(Node node, String algorithms) {
            this.node = node;
            this.algorithms = algorithms;
        }

        public Node getNode() {
            return node;
        }

        public String getAlgorithms() {
            return algorithms;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return "[" + node + ", " + similarity + ", " + algorithms + "]";
        }
    }
}
