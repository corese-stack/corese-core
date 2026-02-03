package fr.inria.corese.core.next.kgram.path;

import fr.inria.corese.core.next.kgram.api.core.Edge;
import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.api.core.PointerType;
import fr.inria.corese.core.next.kgram.api.core.Pointerable;
import fr.inria.corese.core.next.kgram.api.query.Environment;
import fr.inria.corese.core.next.kgram.api.query.Producer;
import fr.inria.corese.core.next.kgram.tool.EdgeInv;
import fr.inria.corese.core.next.kgram.tool.ProducerDefault;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.kgram.api.core.PointerType.PATH;

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
    ArrayList<Edge> path;

    public Path() {
        setMode(Producer.EXTENSION);
        path = new ArrayList<>();
    }

    public Path(boolean b) {
        this();
        isReverse = b;
    }

    Path(int n) {
        setMode(Producer.EXTENSION);
        path = new ArrayList<>(n);
    }

    public ArrayList<Edge> getEdges() {
        return path;
    }

    @Override
    public String getDatatypeLabel() {
        if (path.size() == 1) {
            return String.format("(1)[%s]", path.getFirst());
        }
        if (path.size() > 1) {
            return String.format("(%s)[%s ...]", path.size(), path.getFirst());
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

    public void clear() {
        path.clear();
    }

    public void add(Edge ent) {
        path.add(ent);
    }

    public void add(Edge ent, int w) {
        path.add(ent);
        weight += w;
    }

    public void remove(int w) {
        path.removeLast();
        weight -= w;
    }

    public void remove() {
        path.removeLast();
    }

    // after reverse path
    public Node getSource() {
        return getEdge(0).getNode(0);
    }

    public Node getTarget() {
        return getEdge(size() - 1).getNode(1);
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
        return path.get(n);
    }

    // Edge or EdgeInv
    public Edge getEdge(int n) {
        Edge ent = path.get(n);
        if (ent instanceof EdgeInv) {
            return ent;
        }
        return ent.getEdge();
    }

    public Edge last() {
        if (size() > 0) {
            return get(size() - 1);
        } else {
            return null;
        }
    }

    public Path copy() {
        Path path = new Path(size());
        for (Edge ent : this.path) {
            // when r is reverse, add real target relation
            if (ent instanceof EdgeInv ee) {
                path.add(ee.getEdgeEntity());
            } else {
                path.add(ent);
            }
        }
        path.setWeight(weight);
        return path;
    }

    public Path copy(Producer p) {
        Path path = new Path(size());
        for (Edge ent : this.path) {
            // when r is reverse, add real target relation
            if (ent instanceof EdgeInv) {
                ent = ((EdgeInv) ent).getEdgeEntity();
            }
            path.add(p.copy(ent));
        }
        path.setWeight(weight);
        return path;
    }

    public int length() {
        return path.size();
    }

    @Override
    public int size() {
        return path.size();
    }

    public int weight() {
        return weight;
    }

    void setWeight(int w) {
        weight = w;
    }

    public Path reverse() {
        for (int i = 0; i < length() / 2; i++) {
            Edge tmp = path.get(i);
            path.set(i, path.get(length() - i - 1));
            path.set(length() - i - 1, tmp);
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("path[" + path.size() + "]{");
        if (path.size() > 1) {
            str.append("\n");
        }
        for (Edge edge : path) {
            str.append(edge).append("\n");
        }
        str.append("}");
        return str.toString();
    }

    @Override
    public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env) {
        return path;
    }

    @Override
    public Iterable<Edge> getLoop() {
        return path;
    }

    @Override
    public Edge getValue(String var, int n) {
        return path.get(n);
    }
}