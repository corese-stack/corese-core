package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the variable layout, node collection, and binding index allocation for query plans.
 */
public final class VariableLayout {

    private int nodeIndex = 0;
    private int edgeIndex = 0;

    private List<Node> patternNodes = new ArrayList<>();
    private List<Node> patternSelectNodes = new ArrayList<>();
    private List<Node> queryNodes = new ArrayList<>();
    private List<Node> querySelectNodes = new ArrayList<>();
    private List<Node> bindingNodes = new ArrayList<>();
    private List<Node> constructNodes = new ArrayList<>();
    private List<Node> argList = new ArrayList<>();

    public VariableLayout() {
        // Default constructor initializing empty variable collections and layout counters.
    }

    VariableLayout copyPlan(PlanCopy copies) {
        VariableLayout copy = new VariableLayout();
        copy.nodeIndex = this.nodeIndex;
        copy.edgeIndex = this.edgeIndex;
        copy.patternNodes = copies.copy(this.patternNodes);
        copy.patternSelectNodes = copies.copy(this.patternSelectNodes);
        copy.queryNodes = copies.copy(this.queryNodes);
        copy.querySelectNodes = copies.copy(this.querySelectNodes);
        copy.bindingNodes = copies.copy(this.bindingNodes);
        copy.constructNodes = copies.copy(this.constructNodes);
        copy.argList = copies.copy(this.argList);
        return copy;
    }

    public int allocateNodeIndex() {
        return nodeIndex++;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public int allocateEdgeIndex() {
        return edgeIndex++;
    }

    public int getEdgeIndex() {
        return edgeIndex;
    }

    public void setEdgeIndex(int edgeIndex) {
        this.edgeIndex = edgeIndex;
    }

    public void store(Node node, boolean exist, boolean select) {
        if (select) {
            if (exist) {
                add(querySelectNodes, node);
            } else {
                add(patternSelectNodes, node);
            }
        } else {
            if (exist) {
                add(queryNodes, node);
            } else {
                add(patternNodes, node);
            }
        }
    }

    private void add(List<Node> list, Node node) {
        if (node != null && node.isVariable() && !node.isBlank() && !list.contains(node)) {
            list.add(node);
        }
    }

    public Node getNode(String name, Node selectNode) {
        return getExtNode(name, false, selectNode);
    }

    public Node getExtNode(String name, boolean select, Node selectNode) {
        Node node = getPatternNode(name);
        if (node != null) {
            return node;
        }
        if (select && selectNode != null) {
            return selectNode;
        }
        node = getQueryNode(name);
        if (node != null) {
            return node;
        }
        node = getPatternSelectNode(name);
        if (node != null) {
            return node;
        }
        return getQuerySelectNode(name);
    }

    public Node getPatternNode(String name) {
        return get(patternNodes, name);
    }

    public Node getQueryNode(String name) {
        return get(queryNodes, name);
    }

    public Node getPatternSelectNode(String name) {
        return get(patternSelectNodes, name);
    }

    public Node getQuerySelectNode(String name) {
        return get(querySelectNodes, name);
    }

    private Node get(List<Node> list, String name) {
        for (Node node : list) {
            if (name.equals(node.getLabel())) {
                return node;
            }
        }
        return null;
    }

    public List<Node> getPatternNodes() {
        return patternNodes;
    }

    public List<Node> getPatternSelectNodes() {
        return patternSelectNodes;
    }

    public List<Node> getQueryNodes() {
        return queryNodes;
    }

    public List<Node> getQuerySelectNodes() {
        return querySelectNodes;
    }

    public List<Node> getBindingNodes() {
        return bindingNodes;
    }

    public void setBindingNodes(List<Node> bindingNodes) {
        this.bindingNodes = bindingNodes;
    }

    public List<Node> getConstructNodes() {
        return constructNodes;
    }

    public void setConstructNodes(List<Node> constructNodes) {
        this.constructNodes = constructNodes;
    }

    public List<Node> getArgList() {
        return argList;
    }
}
