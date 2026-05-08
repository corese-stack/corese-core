package fr.inria.corese.core.kgram.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.core.PointerType;
import static fr.inria.corese.core.kgram.api.core.PointerType.PATH;
import fr.inria.corese.core.kgram.api.core.Pointerable;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.tool.EdgeInv;
import fr.inria.corese.core.kgram.tool.ProducerDefault;

/**
 *
 * List of relations between two resources found by path Can be used as a
 * Producer to enumerate path edges/nodes
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class Path extends ProducerDefault implements Pointerable {

    boolean loopNode = true,
            isShort = false,
            isReverse = false;
    int max = Integer.MAX_VALUE;
    int weight = 0;
    ArrayList<Edge> path;
    int radius = 0;

    public Path() {
        setMode(Producer.EXTENSION);
        path = new ArrayList<Edge>();
    }

    public Path(boolean b) {
        this();
        isReverse = b;
    }

    Path(int n) {
        setMode(Producer.EXTENSION);
        path = new ArrayList<Edge>(n);
    }

    
    @Override
    public Path getPathObject() {
        return this;
    }
    
    @Override
    public Object getPointerObject() {
        return this;
    }
    
    @Override
    public String getDatatypeLabel() {
        if (path.size() == 1) {
            return String.format("(1)[%s]", path.get(0));
        }
        if (path.size() > 1) {
            return String.format("(%s)[%s ...]", path.size(), path.get(0));
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
        path.add(ent);
    }

    public void add(Edge ent, int w) {
        path.add(ent);
        weight += w;
    }

    public void remove(Edge ent, int w) {
        path.remove(path.size() - 1);
        weight -= w;
    }


    // before reverse path
    // edge may be EdgeInv in case of ^p
    // firstNode is the SPARQL binding of subject node
    public Node firstNode() {
        int fst = 0;
        if (isReverse) {
            fst = size() - 1;
        }
        return get(fst).getNode(0);
    }

    // lastNode is the SPARQL binding of object node
    public Node lastNode() {
        int lst = size() - 1;
        if (isReverse) {
            lst = 0;
        }
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
        String str = "path[" + path.size() + "]{";
        if (path.size() > 1) {
            str += "\n";
        }
        for (Edge edge : path) {
            str += edge + "\n";
        }
        str += "}";
        return str;
    }

    @Override
    public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env) {
        return path;
    }

    @Override
    public Iterable getLoop() {
        return path;
    }
    
    @Override
    public Edge getValue(String var, int n){
        return path.get(n);
    }
    
}
