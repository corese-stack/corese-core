package fr.inria.corese.core.next.query.impl.kgram.tool;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;

import java.util.*;

/**
 * Data structure: Key -> (node -> Value)
 * Key: (?variable, <uri>) Value: (node, SIMILARITY, algs)
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class ApproximateSearchEnv {

    private final Map<Key, Map<Node, Value>> all;

    public ApproximateSearchEnv() {
        this.all = new HashMap<>();
    }

    public Double getSimilarity(Expr variable, Node node) {
        Key key = new Key(variable);
        Value r = this.get(key, node);
        return (r == null) ? null : r.getSimilarity();
    }

    private Value get(Key key, Node node) {
        Map<Node, Value> candidates = all.get(key);
        return candidates == null ? null : candidates.get(node);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Approximate search\n");
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

    static class Key {

        private final Expr variable;
        private Node uri;

        public Key(Expr variable) {
            this.variable = variable;
        }

        public Expr getVar() {
            return variable;
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
            return Objects.equals(this.variable, other.variable);
        }

        @Override
        public String toString() {
            return "Key{" + "variable=" + variable + ", uri=" + uri + '}';
        }
    }

    static class Value {

        private final Node node;
        private static final double SIMILARITY = -1;
        private final String algorithms;

        public Value(Node node, String algorithms) {
            this.node = node;
            this.algorithms = algorithms;
        }

        public double getSimilarity() {
            return SIMILARITY;
        }

        @Override
        public String toString() {
            return "[" + node + ", " + SIMILARITY + ", " + algorithms + "]";
        }
    }
}
