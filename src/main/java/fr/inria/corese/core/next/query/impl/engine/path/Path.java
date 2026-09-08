package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.PointerType;
import fr.inria.corese.core.next.query.impl.engine.model.Pointerable;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.model.EdgeInv;
import fr.inria.corese.core.next.query.impl.engine.storage.ProducerDefault;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.engine.model.PointerType.PATH;

/**
 * List of relations between two resources found by path Can be used as a
 * Producer to enumerate path edges/nodes
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class Path extends ProducerDefault implements Pointerable<Edge> {

    boolean isShort = false;
    boolean isReverse = false;
    int max = Integer.MAX_VALUE;
    int weight = 0;
    ArrayList<Edge> edges;

    public Path() {
        setMode(Producer.EXTENSION);
        edges = new ArrayList<>();
    }

    public Path(boolean b) {
        this();
        isReverse = b;
    }

    Path(int n) {
        setMode(Producer.EXTENSION);
        edges = new ArrayList<>(n);
    }

    @Override
    public String getDatatypeLabel() {
        if (edges.size() == 1) {
            return String.format("(1)[%s]", edges.getFirst());
        }
        if (edges.size() > 1) {
            return String.format("(%s)[%s ...]", edges.size(), edges.getFirst());
        }
        return "(0)[]";
    }

    @Override
    public PointerType pointerType() {
        return PATH;
    }

    void setIsShort(boolean b) {
        isShort = b;
    }

    void setMax(int m) {
        max = m;
    }

    int getMax() {
        return max;
    }

    public void add(Edge ent) {
        edges.add(ent);
    }

    public void add(Edge ent, int w) {
        edges.add(ent);
        weight += w;
    }

    public void remove(int w) {
        edges.removeLast();
        weight -= w;
    }

    // before reverse path
    // edge may be EdgeInv in case of ^p
    // firstNode is the SPARQL binding of subject node
    public Node firstNode() {
        int fst = isReverse ? size() - 1 : 0;
        return get(fst).getNode(0);
    }

    // lastNode is the SPARQL binding of object node
    public Node lastNode() {
        int lst = isReverse ? 0 : size() - 1;
        return get(lst).getNode(1);
    }

    public Edge get(int n) {
        return edges.get(n);
    }

    // Edge or EdgeInv
    public Edge getEdge(int n) {
        Edge ent = edges.get(n);
        if (ent instanceof EdgeInv) {
            return ent;
        }
        return ent.getEdge();
    }

    public Path copy(Producer p) {
        Path copy = new Path(size());
        for (Edge ent : this.edges) {
            // when r is reverse, add real target relation
            if (ent instanceof EdgeInv inverseEdge) {
                ent = inverseEdge.getEdgeEntity();
            }
            copy.add(p.copy(ent));
        }
        copy.setWeight(weight);
        return copy;
    }

    public int length() {
        return edges.size();
    }

    @Override
    public int size() {
        return edges.size();
    }

    public int weight() {
        return weight;
    }

    void setWeight(int w) {
        weight = w;
    }

    public Path reverse() {
        for (int i = 0; i < length() / 2; i++) {
            Edge tmp = edges.get(i);
            edges.set(i, edges.get(length() - i - 1));
            edges.set(length() - i - 1, tmp);
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("path[" + edges.size() + "]{");
        if (edges.size() > 1) {
            str.append("\n");
        }
        for (Edge edge : edges) {
            str.append(edge).append("\n");
        }
        str.append("}");
        return str.toString();
    }

    @Override
    public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env) {
        return edges;
    }

    @Override
    public Iterable<Edge> getLoop() {
        return edges;
    }

    @Override
    public Edge getValue(String variable, int n) {
        return edges.get(n);
    }
}