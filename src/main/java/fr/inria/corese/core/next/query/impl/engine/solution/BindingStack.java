package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;

import java.util.Arrays;
import java.util.List;

/**
 * Manages arrays of bound query nodes and edges during query evaluation.
 */
public final class BindingStack {

    private int[] nbNodes = new int[0];
    private int[] nbEdges = new int[0];
    private int[] stackIndex = new int[0];
    private Edge[] qEdges = new Edge[0];
    private Edge[] result = new Edge[0];
    private Node[] qNodes = new Node[0];
    private Node[] nodes = new Node[0];
    private int nbEdge = 0;
    private int nbNode = 0;
    private boolean recordEdges = false;

    public void init(Query query, boolean recordEdges) {
        this.recordEdges = recordEdges;
        int nmax = query.nbNodes();
        int emax = recordEdges ? query.nbEdges() : 0;

        nbNodes = new int[nmax];
        stackIndex = new int[nmax];
        nbEdges = new int[emax];
        result = new Edge[emax];
        qEdges = new Edge[emax];

        nodes = new Node[nmax];
        qNodes = new Node[nmax];

        reset();
    }

    public void reset() {
        nbEdge = 0;
        nbNode = 0;
        Arrays.fill(qNodes, null);
        Arrays.fill(nodes, null);
        Arrays.fill(nbNodes, 0);
        Arrays.fill(stackIndex, -1);
        if (recordEdges) {
            Arrays.fill(nbEdges, 0);
            Arrays.fill(result, null);
            Arrays.fill(qEdges, null);
        }
    }

    public boolean push(Node node, Node target, int n, Matcher match, Environment env) {
        if (node.isConstant()) {
            return true;
        }
        int index = node.getIndex();
        if (nodes[index] == null) {
            nodes[index] = target;
            qNodes[index] = node;
            nbNode++;
            nbNodes[index]++;
            stackIndex[index] = n;
            return true;
        } else if (target == null) {
            return false;
        } else if (match.same(node, nodes[index], target, env)) {
            nbNodes[index]++;
            return true;
        }
        return false;
    }

    public void pop(Node node) {
        if (node.isVariable()) {
            int index = node.getIndex();
            if (nbNodes[index] > 0) {
                nbNodes[index]--;
                if (nbNodes[index] == 0) {
                    nbNode--;
                    nodes[index] = null;
                    qNodes[index] = null;
                    stackIndex[index] = -1;
                }
            }
        }
    }

    public void recordEdge(Edge q, Edge ent) {
        int index = q.getEdgeIndex();
        if (nbEdges[index] == 0) {
            nbEdge++;
        }
        nbEdges[index]++;
        qEdges[index] = q;
        result[index] = ent;
    }

    public void popEdge(Edge q) {
        int index = q.getEdgeIndex();
        if (nbEdges[index] > 0) {
            nbEdges[index]--;
            if (nbEdges[index] == 0) {
                nbEdge--;
                qEdges[index] = null;
                result[index] = null;
            }
        }
    }

    public boolean isBound(Node node) {
        return node.isConstant() || (node.getIndex() < nodes.length && nodes[node.getIndex()] != null);
    }

    public Node getNode(Node node) {
        if (node.isConstant()) {
            return node;
        }
        int index = node.getIndex();
        return index < nodes.length ? nodes[index] : null;
    }

    public Node getNode(int index) {
        return index < nodes.length ? nodes[index] : null;
    }

    public Node getNode(String label) {
        for (int i = 0; i < qNodes.length; i++) {
            if (qNodes[i] != null && qNodes[i].getLabel().equals(label)) {
                return nodes[i];
            }
        }
        return null;
    }

    public Node getQueryNode(int index) {
        return index < qNodes.length ? qNodes[index] : null;
    }

    public Node getQueryNode(String label) {
        for (Node qNode : qNodes) {
            if (qNode != null && qNode.getLabel().equals(label)) {
                return qNode;
            }
        }
        return null;
    }

    public int getStackIndex(int index) {
        return index < stackIndex.length ? stackIndex[index] : -1;
    }

    public int getIndex(Node node) {
        return getStackIndex(node.getIndex());
    }

    public int getIndex(Node gNode, Edge edge) {
        int max = -1;
        int length = edge.nbNode();
        for (int i = 0; i < length; i++) {
            Node qNode = edge.getNode(i);
            if (qNode != null) {
                max = Math.max(max, getStackIndex(qNode.getIndex()));
            }
        }
        Node pNode = edge.getEdgeVariable();
        if (pNode != null) {
            max = Math.max(max, getStackIndex(pNode.getIndex()));
        }
        if (gNode != null && gNode.isVariable()) {
            max = Math.max(max, getStackIndex(gNode.getIndex()));
        }
        return max;
    }

    public int getIndex(List<Node> lNodes) {
        int max = -1;
        for (Node node : lNodes) {
            int index = getIndex(node);
            if (index == -1) {
                return -1;
            }
            max = Math.max(max, index);
        }
        return max;
    }

    public Edge getEdge(Edge edge) {
        int index = edge.getEdgeIndex();
        return index < result.length ? result[index] : null;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public Node[] getQueryNodes() {
        return qNodes;
    }

    public Edge[] getEdges() {
        return result;
    }

    public Edge[] getQueryEdges() {
        return qEdges;
    }

    public int getBoundNodeCount() {
        return nbNode;
    }

    public int getBoundEdgeCount() {
        return nbEdge;
    }

    public boolean isRecordEdges() {
        return recordEdges;
    }

    public void setRecordEdges(boolean recordEdges) {
        this.recordEdges = recordEdges;
    }
}
