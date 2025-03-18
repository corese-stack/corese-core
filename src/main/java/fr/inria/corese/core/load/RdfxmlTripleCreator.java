package fr.inria.corese.core.load;

import java.util.Hashtable;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fr.com.hp.hpl.jena.rdf.arp.ALiteral;
import fr.com.hp.hpl.jena.rdf.arp.AResource;
import fr.com.hp.hpl.jena.rdf.arp.StatementHandler;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.util.Property;
import fr.inria.corese.core.util.Property.Value;

/**
 * Implementation of RDF graph construction from RDF/XML sources.
 * This class extends TripleCreatorBase and handles the parsing of RDF/XML
 * using the RDF ARP parser. It transforms RDF statements into graph nodes
 * and edges and processes them accordingly.
 * 
 * Additionally, it manages blank nodes, named graph URIs, and supports
 * automatic OWL ontology imports when enabled.
 */
public class RdfxmlTripleCreator extends TripleCreatorBase
        implements Build, StatementHandler, org.xml.sax.ErrorHandler {

    private Node graphNode;
    Hashtable<String, String> blank;

    private String resource;
    private String namedGraphURI;
    private Node node;

    public RdfxmlTripleCreator() {
    }

    RdfxmlTripleCreator(Graph g, Load ld) {
        super(g, ld);
        graph = g;
        blank = new Hashtable<>();
    }

    public static RdfxmlTripleCreator create(Graph g, Load ld) {
        return new RdfxmlTripleCreator(g, ld);
    }

    @Override
    public void statement(AResource subj, AResource pred, ALiteral lit) {
        if (accept(pred.getURI())) {
            Node subject = getSubject(subj);
            Node predicate = getProperty(pred);
            Node value = getLiteral(pred, lit);
            if (value == null) {
                return;
            }
            Edge edge = getEdge(getGraphNode(), subject, predicate, value);
            process(getGraphNode(), edge);
        }
    }

    /**
     * Processes a statement with the given subject, predicate, and object
     * resources.
     * If the predicate URI is accepted, it creates nodes for the subject,
     * predicate,
     * and object, constructs an edge, and processes it. Additionally, if the
     * predicate
     * URI equals the IMPORTS constant and the OWL_AUTO_IMPORT property is enabled,
     * it imports the object URI.
     *
     * @param subj the subject resource of the statement
     * @param pred the predicate resource of the statement
     * @param obj  the object resource of the statement
     */
    @Override
    public void statement(AResource subj, AResource pred, AResource obj) {
        if (accept(pred.getURI())) {
            // Create nodes for subject, predicate, and object
            Node subject = getSubject(subj);
            Node predicate = getProperty(pred);
            Node value = getNode(obj);

            // Construct an edge and process it
            Edge edge = getEdge(getGraphNode(), subject, predicate, value);
            process(getGraphNode(), edge);

            // If the predicate URI equals IMPORTS and OWL_AUTO_IMPORT is enabled, import
            // the object URI
            if (pred.getURI().equals(Load.IMPORTS)
                    && Property.getBooleanValue(Value.OWL_AUTO_IMPORT)) {
                getLoad().imports(obj.getURI());
            }
        }
    }

    @Override
    public void setSource(String src) {
        basicSetSource(src);
    }

    public String getSource() {
        return getNamedGraphURI();
    }

    void basicSetSource(String src) {
        if (getNamedGraphURI() == null || !src.equals(getNamedGraphURI())) {
            setNamedGraphURI(src);
            setGraphNode(addGraph(src));
        }
    }

    @Override
    public void start() {
        super.start();
        blank.clear();
    }

    public void process(Node gNode, Edge edge) {
        add(edge);
    }

    public Edge getEdge(Node source, Node subject, Node predicate, Node value) {
        if (source == null) {
            source = addDefaultGraphNode();
        }
        return create(source, subject, predicate, value);
    }

    public Node getLiteral(AResource pred, ALiteral lit) {
        String lang = lit.getLang();
        String datatype = lit.getDatatypeURI();
        if (lang == "") {
            lang = null;
        }
        return addLiteral(pred.getURI(), lit.toString(), datatype, lang);
    }

    public Node getProperty(AResource res) {
        return addProperty(res.getURI());
    }

    Node getSubject(AResource res) {
        if (res.isAnonymous()) {
            return addBlank(getID(res.getAnonymousID()));
        } else {
            return getResource(res.getURI());
        }
    }

    public Node getNode(AResource res) {
        if (res.isAnonymous()) {
            return addBlank(getID(res.getAnonymousID()));
        } else {
            return addResource(res.getURI());
        }
    }

    Node getResource(String uri) {
        if (resource == null || !resource.equals(uri)) {
            resource = uri;
            node = addResource(uri);
        }
        return node;
    }

    public String getID(String b) {
        String id = blank.get(b);
        if (id == null) {
            id = newBlankID();
            blank.put(b, id);
        }
        return id;
    }

    public int nbBlank() {
        return blank.size();
    }

    public Node getGraphNode() {
        return graphNode;
    }

    public void setGraphNode(Node graphNode) {
        this.graphNode = graphNode;
    }

    public String getNamedGraphURI() {
        return namedGraphURI;
    }

    public void setNamedGraphURI(String namedGraphURI) {
        this.namedGraphURI = namedGraphURI;
    }

    public Load getLoad() {
        return load;
    }

    public void setLoad(Load load) {
        this.load = load;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
    }

}
