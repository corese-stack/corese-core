package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.query.kgram.api.core.*;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author corby
 *
 */
public class ProducerDefault implements Producer {

    int mode = Producer.DEFAULT;
    Node graphNode;

    public void setMode(int n) {
        mode = n;
    }

    @Override
    public Iterable<Edge> getEdges(Node node, List<Node> from, Edge edge,
                                   Environment env) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge edge, Environment env, Regex exp,
                                   Node src, Node start,
                                   int index) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<Node> getGraphNodes(Node node, List<Node> from,
                                        Environment env) {
        return new ArrayList<>();
    }

    @Override
    public void initPath(Edge edge, int index) {
    }

    @Override
    public Node getNode(Object value) {
        return null;
    }


    @Override
    public Mappings map(List<Node> nodes, IDatatype object) {
        return null;
    }

    @Override
    public boolean isBindable(Node node) {
        return false;
    }

    @Override
    public Iterable<Node> getNodes(Node gNode, List<Node> from, Edge edge, Environment env, List<Regex> exp, int index) {
        return new ArrayList<>();
    }

    @Override
    public boolean isProducer(Node node) {
        return false;
    }

    @Override
    public Producer getProducer(Node node, Environment env) {
        return null;
    }

    @Override
    public Query getQuery() {
        return null;
    }

    @Override
    public Graph getGraph() {
        return null;
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setGraphNode(Node n) {
        graphNode = n;
    }

    @Override
    public Node getGraphNode() {
        return graphNode;
    }

    @Override
    public Mappings getMappings(Node gNode, List<Node> from, Exp exp, Environment env) {
        //create a new Mappings: empty
        return new Mappings();
    }

    @Override
    public IDatatype getValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IDatatype getDatatypeValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Edge copy(Edge ent) {
        return ent;
    }

    @Override
    public void close() {
    }

    @Override
    public void start(Query q) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void finish(Query q) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DatatypeValueFactory getDatatypeValueFactory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mappings map(List<Node> qNodes, IDatatype object, int n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String blankNode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
