package fr.inria.corese.core.load.rdfa;

import org.semarglproject.rdf.core.ParseException;
import org.semarglproject.sink.TripleSink;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.load.AddTripleHelper;
import fr.inria.corese.core.load.AddTripleHelperDataManager;
import fr.inria.corese.core.load.ILoadSerialization;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.storage.api.dataManager.DataManager;

/**
 * Implements the interface TripleSink (from semargl) in order to add the
 * triples (that are parsed by parser of semargl) to the graph of corese system
 *
 * @author Fuqi Song, wimmics inria i3s
 * @date Jan 2014 new
 */
public class CoreseRDFaTripleSink implements TripleSink {

    private AddTripleHelper helper;
    private Graph graph;
    private DataManager dataManager;
    private Node graphSource;

    /**
     * Constructor
     * 
     * @param graph  Graph
     * @param source Name of source graph
     */
    public CoreseRDFaTripleSink(Graph graph, DataManager man, String source, Load load) {
        this.graph = graph;

        if (source == null) {
            graphSource = this.graph.addDefaultGraphNode();
        } else {
            graphSource = this.graph.addGraph(source);
        }

        setDataManager(man);
        if (man == null) {
            helper = AddTripleHelper.create(graph, load);
        } else {
            helper = new AddTripleHelperDataManager(graph, man, load);
        }
    }

    @Override
    public void addNonLiteral(String subject, String predicate, String object) {
        addTriple(subject, predicate, object, null, null, ILoadSerialization.NON_LITERAL);
    }

    @Override
    public void addPlainLiteral(String subject, String predicate, String content, String lang) {
        addTriple(subject, predicate, content, lang, null, ILoadSerialization.LITERAL);
    }

    @Override
    public void addTypedLiteral(String subject, String predicate, String content, String type) {
        addTriple(subject, predicate, content, null, type, ILoadSerialization.LITERAL);
    }

    private void addTriple(String subj, String pred, String obj, String lang, String type, int literalType) {
        helper.addTriple(subj, pred, obj, lang, type, literalType, graphSource);
    }

    @Override
    public void setBaseUri(String string) {
        // nothing
    }

    @Override
    public void startStream() throws ParseException {
        // nothing
    }

    @Override
    public void endStream() throws ParseException {
        // nothing
    }

    @Override
    public boolean setProperty(String string, Object o) {
        return false;
    }

    /**
     * Set parameters for helper class
     * 
     * @param renameBNode
     * @param limit
     */
    public void setHelper(boolean renameBNode, int limit) {
        helper.setRenameBlankNode(renameBNode);
        helper.setLimit(limit);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }
}
