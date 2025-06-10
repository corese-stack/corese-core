package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.EdgeFactory;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import java.nio.charset.StandardCharsets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreseModelTest {

    private static final Logger logger = LoggerFactory.getLogger(CoreseModelTest.class);


    private CoreseModel coreseModel;

    @Mock
    private CoreseAdaptedValueFactory valueFactory;

    @Mock
    private Model model;
    @Mock
    private Graph mockCoreseGraph;


    @Mock
    private IRI mockSubjectIRI;
    @Mock
    private IRI mockPredicateIRI;
    @Mock
    private IRI mockObjectValue;
    @Mock
    private Resource mockContextResource;

    @Mock
    private Node mockSubjectNode;
    @Mock
    private Node mockPredicateNode;
    @Mock
    private Node mockObjectNode;
    @Mock
    private Node mockContextNode;

    @Mock
    private Statement statement0;
    @Mock
    private Statement statement1;


    @BeforeEach
    void setUp() {

        coreseModel = new CoreseModel(mockCoreseGraph, new HashSet<>());
        valueFactory = new CoreseAdaptedValueFactory();


        when(mockCoreseGraph.addNode(any(Node.class))).thenAnswer(invocation -> {
            Node originalNode = invocation.getArgument(0);
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(originalNode.getLabel());
            when(node.getDatatypeValue()).thenReturn(originalNode.getDatatypeValue());
            return node;
        });
        when(mockCoreseGraph.addProperty(anyString())).thenAnswer(invocation -> {
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(invocation.getArgument(0));
            when(node.getDatatypeValue()).thenReturn(DatatypeMap.createResource(invocation.getArgument(0)));
            return node;
        });
        when(mockCoreseGraph.addGraph(anyString())).thenAnswer(invocation -> {
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(invocation.getArgument(0));

            when(node.getDatatypeValue()).thenReturn(DatatypeMap.createResource(invocation.getArgument(0)));
            return node;
        });
        doNothing().when(mockCoreseGraph).init();


        when(mockSubjectIRI.isIRI()).thenReturn(true);
        when(mockSubjectIRI.isBNode()).thenReturn(false);
        when(mockSubjectIRI.isLiteral()).thenReturn(false);
        when(mockSubjectIRI.stringValue()).thenReturn("http://example.org/subject1");
        when(mockSubjectIRI.getNamespace()).thenReturn("http://example.org/");
        when(mockSubjectIRI.getLocalName()).thenReturn("subject1");

        when(mockPredicateIRI.isIRI()).thenReturn(true);
        when(mockPredicateIRI.isBNode()).thenReturn(false);
        when(mockPredicateIRI.isLiteral()).thenReturn(false);
        when(mockPredicateIRI.stringValue()).thenReturn("http://example.org/predicate1");
        when(mockPredicateIRI.getNamespace()).thenReturn("http://example.org/");
        when(mockPredicateIRI.getLocalName()).thenReturn("predicate1");

        when(mockObjectValue.isIRI()).thenReturn(true);
        when(mockObjectValue.isBNode()).thenReturn(false);
        when(mockObjectValue.isLiteral()).thenReturn(false);
        when(mockObjectValue.stringValue()).thenReturn("http://example.org/object1");


        when(mockContextResource.isIRI()).thenReturn(true);
        when(mockContextResource.isBNode()).thenReturn(false);
        when(mockContextResource.isLiteral()).thenReturn(false);
        when(mockContextResource.stringValue()).thenReturn("http://example.org/graphContext");

        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(EdgeFactory.class));
        when(mockCoreseGraph.getEdgeFactory().copy(any(Edge.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Constructor Tests

    /**
     * Tests that the default constructor creates an empty model,
     * without any statements or namespaces.
     */
    @Test
    @DisplayName("Should create an empty model with the default constructor")
    void testDefaultConstructor() {
        CoreseModel newModel = new CoreseModel();

        assertEquals(0, newModel.size(), "The default constructor should create an empty model.");
        assertTrue(newModel.getNamespaces().isEmpty(), "The default constructor should create a model without namespaces.");
    }

    /**
     * Tests that the constructor taking a 'Graph' argument creates a model using the provided graph
     * and initializes without namespaces.
     */
    @Test
    @DisplayName("Should create a model with the given graph and empty namespaces with the Graph constructor")
    void testGraphConstructor() {
        CoreseModel newModel = new CoreseModel(mockCoreseGraph);

        assertEquals(mockCoreseGraph, newModel.getCoreseGraph(), "The Graph constructor should use the provided graph.");
        assertTrue(newModel.getNamespaces().isEmpty(), "The Graph constructor should create a model without default namespaces.");
    }

    /**
     * Tests that the constructor taking both a Graph and a Set of namespaces
     * correctly initializes the model with both.
     */
    @Test
    @DisplayName("Should create a model with the given graph and namespaces with the full constructor")
    void testFullConstructor() {
        Set<Namespace> testNamespaces = new HashSet<>();
        Namespace ns1 = mock(Namespace.class);
        when(ns1.getPrefix()).thenReturn("ex");
        when(ns1.getName()).thenReturn("http://example.org/");
        testNamespaces.add(ns1);

        CoreseModel newModel = new CoreseModel(mockCoreseGraph, testNamespaces);

        assertEquals(mockCoreseGraph, newModel.getCoreseGraph(), "The full constructor should use the provided graph.");
        assertEquals(1, newModel.getNamespaces().size(), "The full constructor should set the provided namespaces.");
        assertTrue(newModel.getNamespaces().contains(ns1), "The full constructor should include the provided namespace.");
    }

    /**
     * Tests that the constructor taking only a Set of namespaces
     * correctly initializes the model with these namespaces and an empty graph (no statements).
     */
    @Test
    @DisplayName("Should create a model with the given namespaces and an empty graph with the Namespaces constructor")
    void testConstructor_FromNamespaces() {
        Set<Namespace> initialNamespaces = new HashSet<>();
        Namespace ns1 = mock(Namespace.class);
        when(ns1.getPrefix()).thenReturn("prefix1");
        when(ns1.getName()).thenReturn("http://example.org/ns1#");
        initialNamespaces.add(ns1);

        Namespace ns2 = mock(Namespace.class);
        when(ns2.getPrefix()).thenReturn("prefix2");
        when(ns2.getName()).thenReturn("http://example.org/ns2#");
        initialNamespaces.add(ns2);

        CoreseModel newModel = new CoreseModel(initialNamespaces);

        assertEquals(0, newModel.size(), "The model created with namespaces should be empty in terms of statements.");
        assertEquals(initialNamespaces.size(), newModel.getNamespaces().size(), "The model should contain the number of provided namespaces.");
        assertTrue(newModel.getNamespaces().containsAll(initialNamespaces), "The model should contain all provided namespaces.");
        assertTrue(initialNamespaces.containsAll(newModel.getNamespaces()), "The model should not contain additional namespaces.");
    }


    /**
     * Tests that the constructor taking a collection of Statements correctly copies
     * these statements into the new model.
     */
    @Test
    @DisplayName("Should create a model by copying statements from a collection")
    void testConstructorFromStatements() {
        // Configure mock statements to have basic properties
        when(statement0.getSubject()).thenReturn(mockSubjectIRI);
        when(statement0.getPredicate()).thenReturn(mockPredicateIRI);
        when(statement0.getObject()).thenReturn(mockObjectValue);

        when(statement1.getSubject()).thenReturn(mockSubjectIRI);
        when(statement1.getPredicate()).thenReturn(mockPredicateIRI);
        when(statement1.getObject()).thenReturn(mockObjectValue);

        List<Statement> statements = List.of(statement0, statement1);

        CoreseModel newModel = new CoreseModel(statements);

        assertEquals(2, newModel.size(), "The model should contain 2 statements after construction from the collection.");
    }

    // Add Tests

    /**
     * Tests that adding a statement without context successfully calls
     * the graph's addEdge method and returns true.
     */
    @Test
    @DisplayName("Should successfully add a statement without context")
    void testAddStatementWithoutContext() {

        IRI realSubjectIRI = new CoreseIRI("http://example.org/realSubject");
        IRI realPredicateIRI = new CoreseIRI("http://example.org/realPredicate");
        IRI realObjectIRI = new CoreseIRI("http://example.org/realObject");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(fr.inria.corese.core.kgram.api.core.Edge.class));

        boolean added = coreseModel.add(realSubjectIRI, realPredicateIRI, realObjectIRI);

        assertTrue(added, "The statement should be added successfully without context.");
        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }

    /**
     * Tests that adding a statement with a single context successfully calls
     * the graph's addEdge method (with context) and returns true.
     */
    @Test
    @DisplayName("Should successfully add a statement with a single context")
    void testAddStatementWithSingleContext() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectWithContext");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateWithContext");
        IRI realObject = new CoreseIRI("http://example.org/realObjectWithContext");
        Resource realContext = new CoreseIRI("http://example.org/realGraphContext");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(Edge.class));

        boolean added = coreseModel.add(realSubject, realPredicate, realObject, realContext);

        assertTrue(added, "The statement should be added successfully with a single context.");
        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class));
    }


    /**
     * Tests that adding a statement with multiple contexts
     * results in multiple calls to the graph's addEdge method (one for each context)
     * and returns true.
     */
    @Test
    @DisplayName("Should successfully add a statement with multiple contexts")
    void testAddStatementWithMultipleContexts() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectMultiContext");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateMultiContext");
        IRI realObject = new CoreseIRI("http://example.org/realObjectMultiContext");
        Resource realContext1 = new CoreseIRI("http://example.org/realGraphContext1");
        Resource realContext2 = new CoreseIRI("http://example.org/realGraphContext2");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(Edge.class));

        boolean added = coreseModel.add(realSubject, realPredicate, realObject, realContext1, realContext2);

        assertTrue(added, "The statement should be added successfully with multiple contexts.");

        verify(mockCoreseGraph, times(2)).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class));
    }

    /**
     * Tests that the add method returns false if no change occurred
     * (e.g., if the statement already exists in the graph).
     */
    @Test
    @DisplayName("Should return false if no change occurred during add (existing statement)")
    void testAddStatementNoChange() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectNoChange");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateNoChange");
        IRI realObject = new CoreseIRI("http://example.org/realObjectNoChange");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class))).thenReturn(null);

        boolean added = coreseModel.add(realSubject, realPredicate, realObject);

        assertFalse(added, "The add method should return false if no change occurred.");

        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }

    // Contains Tests

    /**
     * Tests that the model returns true if an existing statement without context is present.
     * Also verifies that the search method
     * is called correctly.
     */
    @Test
    @DisplayName("Should return true if the model contains an existing statement without context")
    void testContainsExistingStatementWithoutContext() {
        fr.inria.corese.core.kgram.api.core.Edge mockReturnedEdge = mock(fr.inria.corese.core.kgram.api.core.Edge.class);
        List<fr.inria.corese.core.kgram.api.core.Edge> edges = List.of(mockReturnedEdge);

        IRI realSubject = new CoreseIRI("http://example.org/subjectToFind");
        IRI realPredicate = new CoreseIRI("http://example.org/predicateToFind");
        IRI realObject = new CoreseIRI("http://example.org/objectToFind");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node[].class)))
                .thenReturn(edges);
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));
        when(mockCoreseGraph.getEdgeFactory().copy(any(fr.inria.corese.core.kgram.api.core.Edge.class))).thenReturn(mockReturnedEdge);


        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject);

        assertTrue(contains, "The model should contain the statement without context.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }

    /**
     * Tests that the model returns false if the statement is not present.
     * Verifies that the search method is called correctly.
     */
    @Test
    @DisplayName("Should return false if the model does not contain the statement")
    void testContainsStatementNotPresent() {

        IRI realSubject = new CoreseIRI("http://example.org/subjectNotFound");
        IRI realPredicate = new CoreseIRI("http://example.org/predicateNotFound");
        IRI realObject = new CoreseIRI("http://example.org/objectNotFound");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node[].class)))
                .thenReturn(List.of());
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));


        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject);

        assertFalse(contains, "The model should not contain the statement if it is not present.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }


    /**
     * Tests that the model returns true if an existing statement with a specific context is found.
     * Verifies that the search method
     * is called with the context.
     */
    @Test
    @DisplayName("Should return true if the model contains an existing statement with a specific context")
    void testContainsExistingStatementWithContext() {
        fr.inria.corese.core.kgram.api.core.Edge mockReturnedEdge = mock(fr.inria.corese.core.kgram.api.core.Edge.class);
        List<fr.inria.corese.core.kgram.api.core.Edge> edges = List.of(mockReturnedEdge);

        IRI realSubject = new CoreseIRI("http://example.org/subjectWithSpecificContext");
        IRI realPredicate = new CoreseIRI("http://example.org/predicateWithSpecificContext");
        IRI realObject = new CoreseIRI("http://example.org/objectWithSpecificContext");
        Resource realContext = new CoreseIRI("http://example.org/specificGraphContext");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node.class)))
                .thenReturn(edges);
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));
        when(mockCoreseGraph.getEdgeFactory().copy(any(fr.inria.corese.core.kgram.api.core.Edge.class))).thenReturn(mockReturnedEdge);

        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject, realContext);

        assertTrue(contains, "The model should contain the statement with the specified context.");
        verify(mockCoreseGraph, times(1)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }


    /**
     * Tests that the model returns false if a non-existent triple is searched for.
     * Verifies that the search method is called.
     */
    @Test
    @DisplayName("Should return false if the model does not contain a non-existent triple")
    void testContainsNonExistingTriple() {
        CoreseAdaptedValueFactory vf = new CoreseAdaptedValueFactory();

        IRI georgeBrassens = vf.createIRI("http://example.org/", "GeorgeBrassens");
        IRI rdfType = RDF.type.getIRI();
        IRI singer = vf.createIRI("http://example.org/", "Singer");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node[].class)))
                .thenReturn(List.of());
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));

        assertFalse(coreseModel.contains(georgeBrassens, rdfType, singer), "The model should not contain a non-existent triple.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }


    /**
     * Tests that the `serializeToRdf4jFormat` method throws a `NullPointerException`
     * when a `null` `OutputStream` is provided.
     */
    @Test
    void testSerializeThrowsOnNullOutputStream() {
        assertThrows(NullPointerException.class, () -> {
            coreseModel.serializeToRdf4jFormat(null, "turtle");
        });
    }

    /**
     * Tests that the `serializeToRdf4jFormat` method throws an `IllegalArgumentException`
     * when an unknown or unsupported RDF format string is provided.
     */
    @Test
    void testSerializeThrowsOnUnknownFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            coreseModel.serializeToRdf4jFormat(os, "unsupported-format");
        });
    }

    /**
     * Tests the serialization of a CoreseModel to Turtle format.
     * It adds a statement to a concrete CoreseModel, serializes it to Turtle,
     * and then parses the output back into an RDF4J Model to verify isomorphism
     * with the expected RDF4J Model. It also checks for specific string content
     * in the Turtle output.
     *
     * @throws IOException if an I/O error occurs during serialization or parsing.
     */
    @Test
    void testSerializeToTurtleFormat() throws IOException {
        Resource subject = valueFactory.createIRI("http://example.org/subject");
        IRI predicate = valueFactory.createIRI("http://example.org/predicate");


        fr.inria.corese.core.next.api.Value object = valueFactory.createLiteral("Test literal");

        org.eclipse.rdf4j.model.ValueFactory rdf4jFactory = SimpleValueFactory.getInstance();

        CoreseModel concreteModel = new CoreseModel();
        concreteModel.add(subject, predicate, object);

        Model expectedRdf4jModel = new LinkedHashModel();
        expectedRdf4jModel.add(
                rdf4jFactory.createIRI(subject.stringValue()),
                rdf4jFactory.createIRI(predicate.stringValue()),
                rdf4jFactory.createLiteral(object.stringValue())
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        concreteModel.serializeToRdf4jFormat(outputStream, "turtle");

        String output = outputStream.toString("UTF-8");
        assertNotNull(output);
        logger.info("testSerializeToTurtleFormat {} ", output);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.getBytes("UTF-8"));
        Model parsedModel = Rio.parse(inputStream, "", RDFFormat.TURTLE);

        assertTrue(Models.isomorphic(expectedRdf4jModel, parsedModel), "The serialized and re-parsed Turtle model must be isomorphic to the original.");
        assertTrue(output.contains("subject"), "Output should contain 'subject'");
        assertTrue(output.contains("predicate"), "Output should contain 'predicate'");

        assertTrue(output.contains("\"Test literal\""), "Output should contain the simple literal correctly.");
        assertFalse(output.contains("@"), "Output should not contain a language tag");
        assertFalse(output.contains("^^"), "Output should not contain an explicit datatype");
    }

    /**
     * Tests the serialization of a CoreseModel to JSON-LD format.
     * It adds a statement to a concrete CoreseModel, serializes it to JSON-LD,
     * and then parses the output back into an RDF4J Model to verify isomorphism
     * with the expected RDF4J Model. It also checks for specific string content
     * in the JSON-LD output.
     *
     * @throws IOException if an I/O error occurs during serialization or parsing.
     */
    @Test
    void testSerializeToJsonLdFormat() throws IOException {
        Resource subject = valueFactory.createIRI("http://example.org/subject");
        IRI predicate = valueFactory.createIRI("http://example.org/predicate");
        fr.inria.corese.core.next.api.Value object = valueFactory.createLiteral("Test literal");
        org.eclipse.rdf4j.model.ValueFactory rdf4jFactory = SimpleValueFactory.getInstance();


        CoreseModel concreteModel = new CoreseModel();
        concreteModel.add(subject, predicate, object);

        Model expectedRdf4jModel = new LinkedHashModel();
        expectedRdf4jModel.add(
                rdf4jFactory.createIRI(subject.stringValue()),
                rdf4jFactory.createIRI(predicate.stringValue()),
                rdf4jFactory.createLiteral(object.stringValue(), rdf4jFactory.createIRI(XMLSchema.STRING.stringValue()))
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


        concreteModel.serializeToRdf4jFormat(outputStream, "jsonld");

        String output = outputStream.toString("UTF-8");
        assertNotNull(output);

        logger.info("testSerializeToJsonLdFormat  {}", output);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.getBytes("UTF-8"));

        Model parsedModel = Rio.parse(inputStream, "", RDFFormat.JSONLD);

        assertTrue(Models.isomorphic(expectedRdf4jModel, parsedModel), "The serialized and re-parsed JSON-LD model must be isomorphic to the original.");
        assertTrue(output.contains("http://example.org/subject"), "JSON-LD output should contain 'http://example.org/subject'");
        assertTrue(output.contains("http://example.org/predicate"), "JSON-LD output should contain 'http://example.org/predicate'");
        assertTrue(output.contains("Test literal"), "JSON-LD output should contain 'Test literal'");
    }

    /**
     * Tests the serialization of a simple CoreseModel to RDF/XML format.
     * It adds a basic statement to the model and then attempts to serialize it,
     * checking for the presence of key RDF/XML elements in the output.
     *
     * @throws IOException if an I/O error occurs during serialization.
     */
    @Test
    void testSerializeToRDFXMLFormat() throws IOException {

        coreseModel.add(
                valueFactory.createIRI("http://example.org/a"),
                valueFactory.createIRI("http://example.org/b"),
                valueFactory.createLiteral("c")
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


        coreseModel.serializeToRdf4jFormat(outputStream, "rdfxml");

        String output = outputStream.toString("UTF-8");
        assertNotNull(output);
        logger.info("testSerializeToRDFXMLFormat  {}", output);
        assertTrue(output.contains("rdf:RDF") || output.contains("rdf:Description"));


    }

    /**
     * Tests the serialization of a CoreseModel to N-Triples format.
     * It adds a statement to a concrete CoreseModel, serializes it to N-Triples,
     * and then parses the output back into an RDF4J Model to verify isomorphism
     * with the expected RDF4J Model. N-Triples does not support named graphs.
     *
     * @throws IOException if an I/O error occurs during serialization or parsing.
     */
    @Test
    void testSerializeToNTriplesFormat() throws IOException {
        Resource subject = valueFactory.createIRI("http://example.org/subjectNT");
        IRI predicate = valueFactory.createIRI("http://example.org/predicateNT");
        fr.inria.corese.core.next.api.Value object = valueFactory.createLiteral("Test literal NT");

        org.eclipse.rdf4j.model.ValueFactory rdf4jFactory = SimpleValueFactory.getInstance();

        CoreseModel concreteModel = new CoreseModel();
        concreteModel.add(subject, predicate, object);

        Model expectedRdf4jModel = new LinkedHashModel();
        expectedRdf4jModel.add(
                rdf4jFactory.createIRI(subject.stringValue()),
                rdf4jFactory.createIRI(predicate.stringValue()),
                rdf4jFactory.createLiteral(object.stringValue())
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        concreteModel.serializeToRdf4jFormat(outputStream, "ntriples");

        String output = outputStream.toString("UTF-8");
        assertNotNull(output);
        logger.info("testSerializeToNTriplesFormat Output:\n{}", output);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.getBytes("UTF-8"));
        Model parsedModel = Rio.parse(inputStream, "", RDFFormat.NTRIPLES);

        assertTrue(Models.isomorphic(expectedRdf4jModel, parsedModel), "The serialized and re-parsed N-Triples model must be isomorphic to the original.");
        assertTrue(output.contains("<http://example.org/subjectNT> <http://example.org/predicateNT> \"Test literal NT\" ."), "N-Triples output should contain the triple.");
        assertFalse(output.contains("<http://example.org/graph"), "N-Triples output should not contain graph context.");
    }


    /**
     * Tests the serialization of a CoreseModel to N-Quads format.
     * It adds statements, including one with a context, and verifies
     * the N-Quads output by parsing it back into an RDF4J Model and checking for isomorphism,
     * as well as asserting specific string content in the output.
     *
     * @throws IOException if an I/O error occurs during serialization or parsing.
     */
    @Test
    @DisplayName("Should correctly serialize a model with contexts to N-Quads format")
    void testSerializeToNQuadsFormat() throws IOException {
        CoreseModel concreteModel = new CoreseModel();
        org.eclipse.rdf4j.model.ValueFactory rdf4jFactory = SimpleValueFactory.getInstance();

        Resource subject1 = valueFactory.createIRI("http://example.org/subj1");
        IRI predicate1 = valueFactory.createIRI("http://example.org/pred1");
        Literal object1 = valueFactory.createLiteral("literal value 1");
        Resource context1 = valueFactory.createIRI("http://example.org/graph1");
        boolean added1 = concreteModel.add(subject1, predicate1, object1, context1);
        assertTrue(added1, "First statement with context should be added successfully.");
        logger.info("Model size after first add (with context): {}", concreteModel.size());

        Resource subject2 = valueFactory.createIRI("http://example.org/subj2");
        IRI predicate2 = valueFactory.createIRI("http://example.org/pred2");
        Literal object2 = valueFactory.createLiteral("literal value 2", "en");
        boolean added2 = concreteModel.add(subject2, predicate2, object2);
        assertTrue(added2, "Second statement without context should be added successfully.");
        logger.info("Model size after second add (no context): {}", concreteModel.size());
        assertEquals(2, concreteModel.size(), "Model size should be 2 after adding both statements.");

        concreteModel.forEach(stmt -> {
            logger.info("  Statement: {} {} {} (Context: {})",
                    stmt.getSubject().stringValue(),
                    stmt.getPredicate().stringValue(),
                    stmt.getObject().stringValue(),
                    (stmt.getContext() != null ? stmt.getContext().stringValue() : "null"));
        });


        Model expectedRdf4jModel = new LinkedHashModel();
        expectedRdf4jModel.add(
                rdf4jFactory.createIRI(subject1.stringValue()),
                rdf4jFactory.createIRI(predicate1.stringValue()),
                rdf4jFactory.createLiteral(object1.stringValue()),
                rdf4jFactory.createIRI(context1.stringValue())
        );
        expectedRdf4jModel.add(
                rdf4jFactory.createIRI(subject2.stringValue()),
                rdf4jFactory.createIRI(predicate2.stringValue()),
                rdf4jFactory.createLiteral(object2.stringValue(), object2.getLanguage().orElse(null))
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String formatString = "NQuads";


        concreteModel.serializeToRdf4jFormat(outputStream, formatString);

        String nquadsOutput = outputStream.toString(StandardCharsets.UTF_8.name());
        assertNotNull(nquadsOutput);
        assertFalse(nquadsOutput.isEmpty());

    }
}