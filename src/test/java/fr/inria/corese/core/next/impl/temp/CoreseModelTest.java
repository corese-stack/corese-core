package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreseModelTest {

    private CoreseModel coreseModel;

    @Mock
    private Model model;
    @Mock
    private Graph mockCoreseGraph;
    @Mock
    private CoreseValueConverter mockValueConverter;

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
        // Initialise l'instance de CoreseModel pour les tests
        coreseModel = new CoreseModel(mockCoreseGraph, new HashSet<>());

        when(mockValueConverter.toCoreseNode(mockSubjectIRI)).thenReturn(mockSubjectNode);
        when(mockValueConverter.toCoreseNode(mockPredicateIRI)).thenReturn(mockPredicateNode);
        when(mockValueConverter.toCoreseNode(mockObjectValue)).thenReturn(mockObjectNode);
        when(mockValueConverter.toCoreseNode(mockContextResource)).thenReturn(mockContextNode);

        when(mockValueConverter.toCoreseContextArray(any(Resource[].class))).thenAnswer(invocation -> {
            Resource[] contexts = invocation.getArgument(0);
            return Arrays.stream(contexts)
                    .map(mockValueConverter::toCoreseNode)
                    .toArray(Node[]::new);
        });

        when(mockCoreseGraph.addNode(any(Node.class))).thenAnswer(invocation -> {
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(invocation.getArgument(0).toString());
            return node;
        });
        when(mockCoreseGraph.addProperty(anyString())).thenAnswer(invocation -> {
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(invocation.getArgument(0));
            return node;
        });
        when(mockCoreseGraph.addGraph(anyString())).thenAnswer(invocation -> {
            Node node = mock(Node.class);
            when(node.getLabel()).thenReturn(invocation.getArgument(0));
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
    }

    // Tests du Constructeur

    /**
     * Teste que le constructeur par défaut crée un modèle vide,
     * sans aucune déclaration ni espace de noms.
     */
    @Test
    @DisplayName("Devrait créer un modèle vide avec le constructeur par défaut")
    void testDefaultConstructor() {
        CoreseModel newModel = new CoreseModel();

        assertEquals(0, newModel.size(), "Le constructeur par défaut devrait créer un modèle vide.");
        assertTrue(newModel.getNamespaces().isEmpty(), "Le constructeur par défaut devrait créer un modèle sans espace de noms.");
    }

    /**
     * Teste que le constructeur prenant un argument 'Graph' crée un modèle utilisant le graphe fourni
     * et s'initialise sans espace de noms.
     */
    @Test
    @DisplayName("Devrait créer un modèle avec le graphe donné et des espaces de noms vides avec le constructeur Graph")
    void testGraphConstructor() {
        CoreseModel newModel = new CoreseModel(mockCoreseGraph);

        assertEquals(mockCoreseGraph, newModel.getCoreseGraph(), "Le constructeur Graph devrait utiliser le graphe fourni.");
        assertTrue(newModel.getNamespaces().isEmpty(), "Le constructeur Graph devrait créer un modèle sans espace de noms par défaut.");
    }

    /**
     * Teste que le constructeur prenant à la fois un Graph et un Set d'espaces de noms
     * initialise correctement le modèle avec les deux.
     */
    @Test
    @DisplayName("Devrait créer un modèle avec le graphe et les espaces de noms donnés avec le constructeur complet")
    void testFullConstructor() {
        Set<Namespace> testNamespaces = new HashSet<>();
        Namespace ns1 = mock(Namespace.class);
        when(ns1.getPrefix()).thenReturn("ex");
        when(ns1.getName()).thenReturn("http://example.org/");
        testNamespaces.add(ns1);

        CoreseModel newModel = new CoreseModel(mockCoreseGraph, testNamespaces);

        assertEquals(mockCoreseGraph, newModel.getCoreseGraph(), "Le constructeur complet devrait utiliser le graphe fourni.");
        assertEquals(1, newModel.getNamespaces().size(), "Le constructeur complet devrait définir les espaces de noms fournis.");
        assertTrue(newModel.getNamespaces().contains(ns1), "Le constructeur complet devrait inclure l'espace de noms fourni.");
    }

    /**
     * Teste que le constructeur qui ne prend qu'un Set d'espaces de noms
     * initialise correctement le modèle avec ces espaces de noms et un graphe vide (sans déclarations).
     */
    @Test
    @DisplayName("Devrait créer un modèle avec les espaces de noms donnés et un graphe vide avec le constructeur Namespaces")
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

        assertEquals(0, newModel.size(), "Le modèle créé avec les espaces de noms devrait être vide en termes de déclarations.");
        assertEquals(initialNamespaces.size(), newModel.getNamespaces().size(), "Le modèle devrait contenir le nombre d'espaces de noms fourni.");
        assertTrue(newModel.getNamespaces().containsAll(initialNamespaces), "Le modèle devrait contenir tous les espaces de noms fournis.");
        assertTrue(initialNamespaces.containsAll(newModel.getNamespaces()), "Le modèle ne devrait pas contenir d'espaces de noms supplémentaires.");
    }


    /**
     * Teste que le constructeur prenant une collection de Statements copie correctement
     * ces déclarations dans le nouveau modèle.
     */
    @Test
    @DisplayName("Devrait créer un modèle en copiant les déclarations d'une collection")
    void testConstructorFromStatements() {
        // Configure les déclarations mock pour avoir des propriétés de base
        when(statement0.getSubject()).thenReturn(mockSubjectIRI);
        when(statement0.getPredicate()).thenReturn(mockPredicateIRI);
        when(statement0.getObject()).thenReturn(mockObjectValue);

        when(statement1.getSubject()).thenReturn(mockSubjectIRI);
        when(statement1.getPredicate()).thenReturn(mockPredicateIRI);
        when(statement1.getObject()).thenReturn(mockObjectValue);

        List<Statement> statements = List.of(statement0, statement1);

        CoreseModel newModel = new CoreseModel(statements);

        assertEquals(2, newModel.size(), "Le modèle devrait contenir 2 déclarations après la construction à partir de la collection.");
    }

    // Tests d'ajout

    /**
     * Teste que l'ajout d'une déclaration sans contexte appelle avec succès
     * la méthode addEdge du graphe et retourne true.
     */
    @Test
    @DisplayName("Devrait ajouter une déclaration sans contexte avec succès")
    void testAddStatementWithoutContext() {

        IRI realSubjectIRI = new CoreseIRI("http://example.org/realSubject");
        IRI realPredicateIRI = new CoreseIRI("http://example.org/realPredicate");
        IRI realObjectIRI = new CoreseIRI("http://example.org/realObject");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(fr.inria.corese.core.kgram.api.core.Edge.class));

        boolean added = coreseModel.add(realSubjectIRI, realPredicateIRI, realObjectIRI);

        assertTrue(added, "La déclaration devrait être ajoutée avec succès sans contexte.");
        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }

    /**
     * Teste que l'ajout d'une déclaration avec un seul contexte appelle avec succès
     * la méthode addEdge du graphe(avec contexte) et retourne true.
     */
    @Test
    @DisplayName("Devrait ajouter une déclaration avec un seul contexte avec succès")
    void testAddStatementWithSingleContext() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectWithContext");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateWithContext");
        IRI realObject = new CoreseIRI("http://example.org/realObjectWithContext");
        Resource realContext = new CoreseIRI("http://example.org/realGraphContext");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(Edge.class));

        boolean added = coreseModel.add(realSubject, realPredicate, realObject, realContext);

        assertTrue(added, "La déclaration devrait être ajoutée avec succès avec un seul contexte.");
        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class));
    }


    /**
     * Teste que l'ajout d'une déclaration avec plusieurs contextes
     * entraîne plusieurs appels à la méthode addEdge du graphe(un pour chaque contexte)
     * et retourne true.
     */
    @Test
    @DisplayName("Devrait ajouter une déclaration avec plusieurs contextes avec succès")
    void testAddStatementWithMultipleContexts() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectMultiContext");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateMultiContext");
        IRI realObject = new CoreseIRI("http://example.org/realObjectMultiContext");
        Resource realContext1 = new CoreseIRI("http://example.org/realGraphContext1");
        Resource realContext2 = new CoreseIRI("http://example.org/realGraphContext2");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class))).thenReturn(mock(Edge.class));

        boolean added = coreseModel.add(realSubject, realPredicate, realObject, realContext1, realContext2);

        assertTrue(added, "La déclaration devrait être ajoutée avec succès avec plusieurs contextes.");

        verify(mockCoreseGraph, times(2)).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class));
    }

    /**
     * Teste que la méthode add retourne false si aucun changement n'a eu lieu
     * (par exemple, si la déclaration existe déjà dans le graphe).
     */
    @Test
    @DisplayName("Devrait retourner false si aucun changement n'est survenu lors de l'ajout (déclaration existante)")
    void testAddStatementNoChange() {

        IRI realSubject = new CoreseIRI("http://example.org/realSubjectNoChange");
        IRI realPredicate = new CoreseIRI("http://example.org/realPredicateNoChange");
        IRI realObject = new CoreseIRI("http://example.org/realObjectNoChange");

        when(mockCoreseGraph.addEdge(any(Node.class), any(Node.class), any(Node.class))).thenReturn(null);

        boolean added = coreseModel.add(realSubject, realPredicate, realObject);

        assertFalse(added, "La méthode add devrait retourner false si aucun changement n'est survenu.");

        verify(mockCoreseGraph, times(1)).addEdge(any(Node.class), any(Node.class), any(Node.class));
        verify(mockCoreseGraph, never()).addEdge(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }

    //Tests de Contient

    /**
     * Teste que le modèle retourne true si une déclaration existante sans contexte est présente.
     * Vérifie également que la méthode de recherche
     * est appelée correctement.
     */
    @Test
    @DisplayName("Devrait retourner true si le modèle contient une déclaration existante sans contexte")
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

        // Appelle la méthode contains
        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject);

        assertTrue(contains, "Le modèle devrait contenir la déclaration sans contexte.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }

    /**
     * Teste que le modèle retourne false si la déclaration n'est pas présente.
     * Vérifie que la méthode de recherche est bien appelée.
     */
    @Test
    @DisplayName("Devrait retourner false si le modèle ne contient pas la déclaration")
    void testContainsStatementNotPresent() {

        IRI realSubject = new CoreseIRI("http://example.org/subjectNotFound");
        IRI realPredicate = new CoreseIRI("http://example.org/predicateNotFound");
        IRI realObject = new CoreseIRI("http://example.org/objectNotFound");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node[].class)))
                .thenReturn(List.of());
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));


        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject);

        assertFalse(contains, "Le modèle ne devrait pas contenir la déclaration si elle n'est pas présente.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }


    /**
     * Teste que le modèle retourne true si une déclaration existante avec un contexte spécifique est trouvée.
     * Vérifie que la méthode de recherche
     * est appelée avec le contexte.
     */
    @Test
    @DisplayName("Devrait retourner true si le modèle contient une déclaration existante avec un contexte spécifique")
    void testContainsExistingStatementWithContext() {
        fr.inria.corese.core.kgram.api.core.Edge mockReturnedEdge = mock(fr.inria.corese.core.kgram.api.core.Edge.class);
        List<fr.inria.corese.core.kgram.api.core.Edge> edges = List.of(mockReturnedEdge);

        IRI realSubject = new CoreseIRI("http://example.org/subjectWithSpecificContext");
        IRI realPredicate = new CoreseIRI("http://example.org/predicateWithSpecificContext");
        IRI realObject = new CoreseIRI("http://example.org/objectWithSpecificContext");
        Resource realContext = new CoreseIRI("http://example.org/specificGraphContext"); // Supposant que le contexte est un IRI

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node.class)))
                .thenReturn(edges);
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));
        when(mockCoreseGraph.getEdgeFactory().copy(any(fr.inria.corese.core.kgram.api.core.Edge.class))).thenReturn(mockReturnedEdge);

        boolean contains = coreseModel.contains(realSubject, realPredicate, realObject, realContext);

        assertTrue(contains, "Le modèle devrait contenir la déclaration avec le contexte spécifié.");
        verify(mockCoreseGraph, times(1)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node.class));
    }


    /**
     * Teste que le modèle retourne false si un triple non existant est recherché.
     * Vérifie que la méthode de recherche est appelée.
     */
    @Test
    @DisplayName("Devrait retourner false si le modèle ne contient pas un triple non existant")
    void testContainsNonExistingTriple() {
        CoreseAdaptedValueFactory vf = new CoreseAdaptedValueFactory();

        IRI georgeBrassens = vf.createIRI("http://example.org/", "GeorgeBrassens");
        IRI rdfType = RDF.type.getIRI();
        IRI singer = vf.createIRI("http://example.org/", "Singer");

        when(mockCoreseGraph.getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), any(Node[].class)))
                .thenReturn(List.of());
        when(mockCoreseGraph.getEdgeFactory()).thenReturn(mock(fr.inria.corese.core.EdgeFactory.class));

        assertFalse(coreseModel.contains(georgeBrassens, rdfType, singer), "Le modèle ne devrait pas contenir un triple non existant.");
        verify(mockCoreseGraph, times(0)).getEdgesRDF4J(any(Node.class), any(Node.class), any(Node.class), eq(null));
    }
}