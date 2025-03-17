package fr.inria.corese.core.load;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.triple.api.Creator;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Exp;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.RDFList;
import fr.inria.corese.core.sparql.triple.parser.Triple;
import fr.inria.corese.core.util.Property;
import fr.inria.corese.core.util.Property.Value;

/**
 * Implementation of RDF triple creation for Turtle and SPARQL-based formats.
 * This class extends TripleCreatorBase and provides methods to process
 * RDF statements dynamically, handling blank nodes, RDF lists, and triple
 * references (for RDF-star support).
 * 
 * It is optimized for streaming RDF parsing and ensures proper namespace
 * resolution while inserting triples into the graph.
 * 
 * @author Olivier Corby, INRIA 2012
 */
public class TurtleSparqlTripleCreator extends TripleCreatorBase implements Creator {
    public static boolean USE_REFERENCE_ID = true;

    HashMap<String, String> blank;
    HashMap<String, Node> reference;
    NSManager nsm;
    Node source;
    Stack<Node> stack;
    String base;
    private boolean renameBlankNode = true;
    Load load;

    TurtleSparqlTripleCreator(Graph g, Load ld) {
        super(g, ld);
        graph = g;
        load = ld;
        blank = new HashMap<>();
        reference = new HashMap<>();
        nsm = NSManager.create();
        stack = new Stack<>();
    }

    public static TurtleSparqlTripleCreator create(Graph g, Load ld) {
        return new TurtleSparqlTripleCreator(g, ld);
    }

    @Override
    public void graph(Atom src) {
        stack.add(source);
        if (src.isBlankOrBlankNode()) {
            source = addGraph(getID(src.getLabel()), true);
        } else {
            source = addGraph(src);
        }
    }

    @Override
    public void endGraph(Atom src) {
        source = stack.pop();
    }

    @Override
    public boolean accept(Atom subject, Atom property, Atom object) {
        return true;
    }

    @Override
    public void triple(Atom graph, Atom subject, Atom property, Atom object) {
        triple(getGraph(graph), subject, property, object);
    }

    Node getGraph(Atom graph) {
        if (graph == null) {
            return addDefaultGraphNode();
        } else if (graph.isBlankOrBlankNode()) {
            return addGraph(getID(graph.getLabel()), true);
        } else {
            return addGraph(graph);
        }
    }

    @Override
    public void triple(Atom subject, Atom property, Atom object) {
        if (source == null) {
            source = addDefaultGraphNode();
        }
        triple(source, subject, property, object);
    }

    @Override
    public void triple(Atom property, List<Atom> termList, boolean nested) {
        if (source == null) {
            source = addDefaultGraphNode();
        }

        Node predicate = getProperty(property);

        ArrayList<Node> nodeList = new ArrayList<>();
        for (Atom at : termList) {
            Node n = getObject(at, predicate, nodeList);
            nodeList.add(n);
        }

        Edge e = create(source, predicate, nodeList, nested);
        add(e);
    }

    @Override
    public void triple(Atom property, List<Atom> termList) {
        triple(property, termList, false);
    }

    /**
     * Creates and inserts an RDF triple into the graph.
     * 
     * This method first checks whether the property is accepted. If so, it resolves
     * the subject, predicate, and object nodes, creates the corresponding edge,
     * and inserts it into the graph.
     * 
     * If the property corresponds to an ontology import (owl:imports), it triggers
     * the import process by calling `handleOntologyImport(property, object)`.
     * 
     * @param source        The graph node where the triple belongs.
     * @param subjectNode   The subject of the triple.
     * @param predicateNode The predicate (property) of the triple.
     * @param objectNode    The object of the triple.
     * @return The created Edge, or null if the property is not accepted.
     */
    Edge triple(Node source, Atom subjectNode, Atom predicateNode, Atom objectNode) {
        if (!accept(predicateNode.getLabel())) {
            return null;
        }

        Node s = getSubject(subjectNode);
        Node p = getProperty(predicateNode);
        Node o = objectNode.isLiteral() ? getLiteral(predicateNode, objectNode.getConstant()) : getNode(objectNode);

        Edge e = create(source, s, p, o);
        add(e);
        handleOntologyImport(predicateNode, objectNode);
        return e;
    }

    /**
     * Checks if the given property corresponds to owl:imports and, if applicable,
     * triggers the import process for the specified object.
     * 
     * This method ensures that ontology imports only occur when the
     * `OWL_AUTO_IMPORT` property is enabled.
     * 
     * @param property The predicate of the triple.
     * @param object   The object of the triple, expected to contain an import URI.
     */
    void handleOntologyImport(Atom property, Atom object) {
        if (Load.IMPORTS.equals(property.getLongName())
                && Property.getBooleanValue(Value.OWL_AUTO_IMPORT)) {
            load.imports(object.getLongName());
        }
    }

    @Override
    public void list(RDFList l) {
        for (Exp exp : l.getBody()) {
            if (exp.isTriple()) {
                Triple t = exp.getTriple();
                triple(t.getSubject(), t.getProperty(), t.getObject());
            }
        }
    }

    Node getLiteral(Atom pred, Constant lit) {
        if (lit.getDatatypeValue().isList()) {
            return addNode(lit);
        }
        String lang = lit.getLang();
        String datatype = nsm.toNamespace(lit.getDatatype());
        if (lang == "") {
            lang = null;
        }
        return addLiteral(pred.getLabel(), lit.getLabel(), datatype, lang);
    }

    Node getLiteral(Constant lit) {
        if (lit.getDatatypeValue().isList()) {
            return addNode(lit);
        }
        return getLiteralBasic(lit);
    }

    Node getLiteralBasic(Constant lit) {
        String lang = lit.getLang();
        String datatype = nsm.toNamespace(lit.getDatatype());
        if (lang == "") {
            lang = null;
        }
        return addLiteral(lit.getLabel(), datatype, lang);
    }

    Node getObject(Atom object) {
        return getObject(object, null, null);
    }

    Node getObject(Atom object, Node predicate, List<Node> nodeList) {
        Node o;
        if (object.isLiteral()) {
            o = getLiteral(object.getConstant());
        } else {
            o = getNode(object, predicate, nodeList);
        }
        return o;
    }

    Node getNode(Atom c) {
        return getNode(c, null, null);
    }

    Node getNode(Atom c, Node predicate, List<Node> nodeList) {
        if (c.isTriple()) {
            return getTripleReference(c, predicate, nodeList);
        }
        if (c.isBlank() || c.isBlankNode()) {
            return getBlank(c);
        } else {
            return addResource(c.getLabel());
        }
    }

    Node getBlank(Atom c) {
        Node n = addBlank(getID(c.getLabel()));
        return n;
    }

    Node getTripleReference(Atom at, Node predicate, List<Node> nodeList) {
        if (nodeList == null || nodeList.size() < 2) {
            return addTripleReference(at);
        } else {
            return addTripleReference(at, nodeList.get(0), predicate, nodeList.get(1));
        }
    }

    Node addTripleReference(Atom at) {
        Node n = reference.get(at.getLabel());
        if (n == null) {
            // should not happen because references are created
            // before they are used
            n = addTripleReference(tripleID(at.getLabel()));
            reference.put(at.getLabel(), n);
        }
        return n;
    }

    Node addTripleReference(Atom at, Node s, Node p, Node o) {
        if (USE_REFERENCE_ID) {
            // gerenare unique ID for every occurrence of same s p o
            return addTripleReferenceNew(at, s, p, o);
        } else {
            return addTripleReference(at);
        }
    }

    Node addTripleReferenceNew(Atom at, Node s, Node p, Node o) {
        Node n = reference.get(at.getLabel());
        if (n == null) {
            n = getGraph().addTripleReference(s, p, o);
            reference.put(at.getLabel(), n);
        }
        return n;

    }

    Node getSubject(Atom c) {
        return getNode(c);
    }

    String getID(String b) {
        if (isRenameBlankNode()) {
            return basicID(b);
        }
        return b;
    }

    String basicID(String b) {
        String id = blank.get(b);
        if (id == null) {
            id = newBlankID();
            blank.put(b, id);
        }
        return id;
    }

    String tripleID(String b) {
        String id = blank.get(b);
        if (id == null) {
            id = getGraph().newTripleReferenceID();
            blank.put(b, id);
        }
        return id;
    }

    @Override
    public boolean isRenameBlankNode() {
        return renameBlankNode;
    }

    @Override
    public void setRenameBlankNode(boolean renameBlankNode) {
        this.renameBlankNode = renameBlankNode;
    }

}
