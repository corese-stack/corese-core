package fr.inria.corese.core.next.impl.io.parser.rdfxml.context;

import fr.inria.corese.core.next.api.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Holds shared parsing state during RDF/XML parsing.
 *
 * <p>This class acts as a context holder for the SAX-based RDF/XML parser,
 * allowing multiple elements and handlers to share and manipulate parsing state.
 * It stores stacks for subjects, predicates, datatypes, and languages,
 * as well as temporary collections used during the construction of RDF lists and containers.</p>
 *
 * <p>This context is typically instantiated once per parsing session and passed
 * throughout the parsing logic.</p>
 */
public class RDFXMLContext {

    /** The RDF model to which parsed triples will be added. */
    public Model model;

    /** The factory used to create IRIs, literals, blank nodes, and statements. */
    public ValueFactory factory;

    /** The base URI against which relative IRIs are resolved. */
    public String baseURI;

    /** A single statement buffer (optional use). */
    public Statement statement;

    /** Builder list for rdf:parseType="Collection" elements. */
    public List<Resource> collectionBuilder = new ArrayList<>();

    /** The subject associated with the current RDF collection. */
    public Resource collectionSubject;

    /** The predicate that connects the collection subject to the list head. */
    public IRI collectionPredicate;

    /** Stack of subject resources to manage nesting of elements. */
    public final Deque<Resource> subjectStack = new ArrayDeque<>();

    /** Stack of predicates for tracking current RDF properties. */
    public final Deque<IRI> predicateStack = new ArrayDeque<>();

    /** Stack for xml:lang values scoped by element depth. */
    public final Deque<String> langStack = new ArrayDeque<>();

    /** Stack for rdf:datatype URIs associated with literals. */
    public final Deque<String> datatypeStack = new ArrayDeque<>();

    /** Temporary holder for RDF collection items (unused or optional). */
    public final Deque<Resource> collectionItems = new ArrayDeque<>();

    /** Whether the parser is currently inside an RDF container (rdf:Seq, rdf:Bag, rdf:Alt). */
    public boolean inContainer = false;

    /** Whether the parser is currently inside an RDF collection (rdf:parseType="Collection"). */
    public boolean inCollection = false;

    /** If true, skips pushing a subject onto the stack (used for collection items). */
    public boolean suppressSubject = false;

    /** Counter for rdf:li to rdf:_n expansion. */
    public int liIndex = 1;

    /**
     * Constructs a new context for RDF/XML parsing.
     *
     * @param model   the RDF model to populate with triples
     * @param factory the value factory used to create RDF terms
     */
    public RDFXMLContext(Model model, ValueFactory factory) {
        this.model = model;
        this.factory = factory;
    }
}
