package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks variable-to-value bindings during BGP join evaluation.
 */
final class StorageBindingSet {

    private final List<Node> queryNodes = new ArrayList<>();
    private final List<Node> targetNodes = new ArrayList<>();

    StorageBindingSet copy() {
        StorageBindingSet copy = new StorageBindingSet();
        copy.queryNodes.addAll(queryNodes);
        copy.targetNodes.addAll(targetNodes);
        return copy;
    }

    /**
     * Adds a query-variable binding, or validates it against an existing binding.
     *
     * @param queryNode query-side node, usually a variable
     * @param targetNode storage match node
     * @return {@code true} when the binding is compatible with previous bindings
     */
    boolean bind(Node queryNode, Node targetNode) {
        if (queryNode == null || queryNode.isConstant()) {
            return true;
        }
        Node current = get(queryNode);
        if (current == null) {
            queryNodes.add(queryNode);
            targetNodes.add(targetNode);
            return true;
        }
        return current.match(targetNode);
    }

    /**
     * Looks up the target node already bound to a query node.
     *
     * @param queryNode query node to find
     * @return bound target node, or {@code null} when the query node is unbound
     */
    Node get(Node queryNode) {
        if (queryNode == null) {
            return null;
        }
        for (int i = 0; i < queryNodes.size(); i++) {
            if (queryNodes.get(i) == queryNode || queryNodes.get(i).same(queryNode)) {
                return targetNodes.get(i);
            }
        }
        return null;
    }

    /**
     * Converts this local binding set into a KGRAM mapping.
     *
     * @return mapping containing all query-to-target bindings
     */
    Mapping toMapping() {
        return Mapping.create(queryNodes, targetNodes);
    }
}
