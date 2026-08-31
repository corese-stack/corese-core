package fr.inria.corese.core.next.query.impl.kgram.adapter;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.core.Regex;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.core.SparqlException;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.kgram.api.core.DatatypeValueFactory;

import java.util.List;

/**
 * Minimal stub: satisfies the {@link Producer} type requirement of
 * {@code Expression.evalWE} without providing any real functionality.
 *
 * <p>Producer methods are not invoked during basic SPARQL 1.0 FILTER expression
 * evaluation (equality, comparison, etc.); all methods throw
 * {@link UnsupportedOperationException}.</p>
 */
final class LegacyProducerBridge implements Producer {

    static final LegacyProducerBridge INSTANCE = new LegacyProducerBridge();

    private LegacyProducerBridge() {}

    @Override public int     getMode()                  { throw new UnsupportedOperationException(); }
    @Override public void    setMode(int n)             { throw new UnsupportedOperationException(); }
    @Override public Iterable<Node> getGraphNodes(Node gNode, List<Node> from, Environment env)
                                                        { throw new UnsupportedOperationException(); }
    @Override public boolean isGraphNode(Node gNode, List<Node> from, Environment env)
                                                        { throw new UnsupportedOperationException(); }
    @Override public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env)
                                                        { throw new UnsupportedOperationException(); }
    @Override public Mappings getMappings(Node gNode, List<Node> from, Exp exp, Environment env)
            throws SparqlException                      { throw new UnsupportedOperationException(); }
    @Override public void    initPath(Edge qEdge, int index) { throw new UnsupportedOperationException(); }
    @Override public Iterable<Node> getNodes(Node gNode, List<Node> from, Edge edge, Environment env,
            List<Regex> exp, int index)                 { throw new UnsupportedOperationException(); }
    @Override public Iterable<Edge> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env,
            Regex exp, Node src, Node start, int index) { throw new UnsupportedOperationException(); }
    @Override public Node    getNode(Object value)      { throw new UnsupportedOperationException(); }
    @Override public IDatatype getValue(Object value)   { throw new UnsupportedOperationException(); }
    @Override public IDatatype getDatatypeValue(Object value) { throw new UnsupportedOperationException(); }
    @Override public boolean isBindable(Node node)      { throw new UnsupportedOperationException(); }
    @Override public List<Node> toNodeList(IDatatype value) { throw new UnsupportedOperationException(); }
    @Override public DatatypeValueFactory getDatatypeValueFactory() { throw new UnsupportedOperationException(); }
    @Override public Mappings map(List<Node> qNodes, IDatatype value) { throw new UnsupportedOperationException(); }
    @Override public Mappings map(List<Node> qNodes, IDatatype value, int n) { throw new UnsupportedOperationException(); }
    @Override public boolean isProducer(Node node)      { throw new UnsupportedOperationException(); }
    @Override public Producer getProducer(Node node, Environment env) { throw new UnsupportedOperationException(); }
    @Override public Query   getQuery()                 { throw new UnsupportedOperationException(); }
    @Override public fr.inria.corese.core.kgram.api.core.Graph getGraph() { throw new UnsupportedOperationException(); }
    @Override public Node    getGraphNode()             { throw new UnsupportedOperationException(); }
    @Override public void    setGraphNode(Node n)       { throw new UnsupportedOperationException(); }
    @Override public Edge    copy(Edge ent)             { throw new UnsupportedOperationException(); }
    @Override public void    close()                    { throw new UnsupportedOperationException(); }
    @Override public String  blankNode()                { throw new UnsupportedOperationException(); }
}
