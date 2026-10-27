package fr.inria.corese.core.next.query.impl.engine.solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

/**
 * Groups mappings and tracks distinct solutions using immutable tuple keys.
 * Groups are visited in insertion order.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 */
public class Group implements Comparator<Mappings> {

    private static final Mappings DISTINCT_MARKER = new Mappings();

    private final Map<GroupKey, Mappings> table = new LinkedHashMap<>();
    private boolean isDistinct;
    private boolean isExtend;
    private List<Node> nodes;
    private boolean isFake;

    /** Creates an empty group. */
    public Group() {
        this(List.of());
    }

    public Group(List<Node> list) {
        this.nodes = list;
    }

    public static Group create(List<Node> nodeList) {
        return new Group(nodeList);
    }

    @SuppressWarnings("java:S9391") // Avoid allocating a stream pipeline.
    public static Group createFromExp(List<Exp> list) {
        List<Node> nodeList = new ArrayList<>(list.size());
        for (Exp exp : list) {
            nodeList.add(exp.getNode());
        }
        return new Group(nodeList);
    }

    public boolean isFake() {
        return isFake;
    }

    public void setFake(boolean fake) {
        this.isFake = fake;
    }

    public List<Node> getNodeList() {
        return nodes;
    }

    public void setNodeList(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }

    public void setExtend(boolean extend) {
        isExtend = extend;
    }

    @SuppressWarnings("java:S1172") // Retained as an extension point.
    public boolean accept(Node node) {
        return true;
    }

    @Override
    public int compare(Mappings left, Mappings right) {
        return left.compare(left.get(0), right.get(0));
    }

    /** Adds a mapping to its group in expected constant time per tuple lookup. */
    public boolean add(Mapping map) {
        GroupKey key = groupKey(map);
        Mappings mappings = table.computeIfAbsent(key, ignored -> createNewMappings(isFake()));
        mappings.add(map);
        return true;
    }

    private GroupKey groupKey(Mapping map) {
        if (isExtend) {
            map.setGroup(getNodeList());
            return new GroupKey(map.getGroupNodes());
        }
        if (isDistinct) {
            return distinctKey(map);
        }
        Node[] values = new Node[nodes.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = map.getGroupBy(nodes.get(i), i);
        }
        return new GroupKey(values);
    }

    private static Mappings createNewMappings(boolean fake) {
        Mappings mappings = new Mappings();
        mappings.setFake(fake);
        return mappings;
    }

    /** Returns true only for the first occurrence of the distinct tuple. */
    public boolean isDistinct(Mapping map) {
        GroupKey key = distinctKey(map);
        return table.putIfAbsent(key, DISTINCT_MARKER) == null;
    }

    private GroupKey distinctKey(Mapping map) {
        map.computeDistinct(getNodeList());
        return new GroupKey(map.getDistinctNodes());
    }

    public Iterable<Mappings> getValues() {
        return table.values();
    }

    /**
     * Owns a defensive copy of the tuple array, never exposed to callers.
     * As with other hash keys, nodes must retain their term values and indices
     * while stored in the table.
     */
    private static final class GroupKey {

        private final Node[] values;
        private final int hash;

        private GroupKey(Node[] values) {
            this.values = values.clone();
            int[] componentHashes = new int[this.values.length];
            for (int i = 0; i < componentHashes.length; i++) {
                componentHashes[i] = nodeHash(this.values[i]);
            }
            hash = Arrays.hashCode(componentHashes);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GroupKey key) || values.length != key.values.length) {
                return false;
            }
            for (int i = 0; i < values.length; i++) {
                if (!sameNode(values[i], key.values[i])) {
                    return false;
                }
            }
            return true;
        }

        private static boolean sameNode(Node left, Node right) {
            if (left == right) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            // Preserve the original grouping comparator, including datatype ordering.
            return left.compare(right) == 0;
        }

        private static int nodeHash(Node node) {
            if (node == null) {
                return 0;
            }
            return node.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
