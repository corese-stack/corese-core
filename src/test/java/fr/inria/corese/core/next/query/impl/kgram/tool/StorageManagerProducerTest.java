package fr.inria.corese.core.next.query.impl.kgram.tool;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType.Type;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.core.Eval;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Mappings;
import fr.inria.corese.core.next.query.impl.kgram.core.Query;
import fr.inria.corese.core.next.query.impl.kgram.execution.RdfTermMatcher;
import fr.inria.corese.core.next.query.impl.kgram.execution.SparqlKgramEvaluator;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageManagerProducerTest {

    private static final String ALICE = "http://example.org/alice";
    private static final String BOB = "http://example.org/bob";
    private static final String CAROL = "http://example.org/carol";
    private static final String KNOWS = "http://example.org/knows";
    private static final String NAME = "http://example.org/name";
    private static final String GRAPH = "http://example.org/graph";

    private ValueFactory valueFactory;
    private MemoryStorageManager storage;
    private StorageManagerProducer producer;

    @BeforeEach
    void setUp() {
        valueFactory = new CoreseValueFactory();
        storage = MemoryStorageManager.builder().build();
        producer = new StorageManagerProducer(storage);

        insert(iri(ALICE), iri(KNOWS), iri(BOB));
        insert(iri(ALICE), iri(KNOWS), iri(CAROL));
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));
        insert(iri(CAROL), iri(NAME), valueFactory.createLiteral("Carol"), iri(GRAPH));
    }

    @Test
    @DisplayName("Unbound ?s ?p ?o enumerates every storage statement as KGRAM edges")
    void unboundSpoEnumeratesEveryStatement() {
        Node s = variable("s");
        Node p = variable("p");
        Node o = variable("o");

        List<Edge> edges = edges(new TestQueryEdge(s, KgramNodes.rootProperty(), p, o));

        assertEquals(4, edges.size());
        assertTrue(edges.stream().anyMatch(edge ->
                ALICE.equals(edge.getNode(0).getLabel())
                        && KNOWS.equals(edge.getEdgeNode().getLabel())
                        && BOB.equals(edge.getNode(1).getLabel())));
    }

    @Test
    @DisplayName("Constant predicate is pushed into the StatementPattern")
    void constantPredicateFiltersStorageStatements() {
        Node s = variable("s");
        Node o = variable("o");

        List<Edge> edges = edges(new TestQueryEdge(s, resource(KNOWS), null, o));

        assertEquals(2, edges.size());
        assertTrue(edges.stream().allMatch(edge -> KNOWS.equals(edge.getEdgeNode().getLabel())));
        assertEquals(
                Set.of(BOB, CAROL),
                edges.stream().map(edge -> edge.getNode(1).getLabel()).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("Bound predicate variable is pushed into the StatementPattern")
    void boundPredicateVariableFiltersStorageStatements() {
        Node s = variable("s");
        Node p = variable("p");
        Node o = variable("o");
        Environment environment = mock(Environment.class);
        when(environment.getNode(p)).thenReturn(resource(NAME));

        List<Edge> edges = edges(new TestQueryEdge(s, KgramNodes.rootProperty(), p, o), null, List.of(), environment);

        assertEquals(2, edges.size());
        assertTrue(edges.stream().allMatch(edge -> NAME.equals(edge.getEdgeNode().getLabel())));
    }

    @Test
    @DisplayName("Graph node restricts the StatementPattern context")
    void graphNodeRestrictsContext() {
        Node s = variable("s");
        Node o = variable("o");

        List<Edge> edges = edges(new TestQueryEdge(s, resource(NAME), null, o), resource(GRAPH), List.of(), null);

        assertEquals(1, edges.size());
        Edge edge = edges.getFirst();
        assertEquals(CAROL, edge.getNode(0).getLabel());
        assertEquals("Carol", edge.getNode(1).getLabel());
        assertEquals(GRAPH, edge.getGraph().getLabel());
    }

    @Test
    @DisplayName("Graph node is accepted when it belongs to the named dataset")
    void graphNodeAllowedByFromRestrictsContext() {
        Node s = variable("s");
        Node o = variable("o");

        List<Edge> edges = edges(
                new TestQueryEdge(s, resource(NAME), null, o),
                resource(GRAPH),
                List.of(resource(GRAPH)),
                null);

        assertEquals(1, edges.size());
        assertEquals(CAROL, edges.getFirst().getNode(0).getLabel());
    }

    @Test
    @DisplayName("Graph node outside the named dataset returns no candidate edges")
    void graphNodeOutsideFromReturnsNoEdges() {
        Node s = variable("s");
        Node o = variable("o");

        List<Edge> edges = edges(
                new TestQueryEdge(s, resource(NAME), null, o),
                resource(GRAPH),
                List.of(resource("http://example.org/other-graph")),
                null);

        assertTrue(edges.isEmpty());
    }

    @Test
    @DisplayName("FROM nodes restrict the StatementPattern contexts")
    void fromNodesRestrictContexts() {
        Node s = variable("s");
        Node o = variable("o");

        List<Edge> edges = edges(new TestQueryEdge(s, resource(NAME), null, o), null, List.of(resource(GRAPH)), null);

        assertEquals(1, edges.size());
        assertEquals(CAROL, edges.getFirst().getNode(0).getLabel());
    }

    @Test
    @DisplayName("Invalid predicate binding returns no candidate edges")
    void invalidPredicateBindingReturnsNoEdges() {
        Node s = variable("s");
        Node p = variable("p");
        Node o = variable("o");
        Environment environment = mock(Environment.class);
        when(environment.getNode(p)).thenReturn(literal("not-a-predicate"));

        List<Edge> edges = edges(new TestQueryEdge(s, KgramNodes.rootProperty(), p, o), null, List.of(), environment);

        assertTrue(edges.isEmpty());
    }

    @Test
    @DisplayName("Invalid subject binding returns no candidate edges")
    void invalidSubjectBindingReturnsNoEdges() {
        Node s = variable("s");
        Node o = variable("o");
        Environment environment = mock(Environment.class);
        when(environment.getNode(s)).thenReturn(literal("not-a-subject"));

        List<Edge> edges = edges(new TestQueryEdge(s, resource(KNOWS), null, o), null, List.of(), environment);

        assertTrue(edges.isEmpty());
    }

    @Test
    @DisplayName("Eval executes SELECT * WHERE { ?s ?p ?o } over StorageManagerProducer")
    void evalExecutesSelectStarSpoOverStorageManagerProducer() throws Exception {
        Node s = variable("s");
        Node p = variable("p");
        Node o = variable("o");
        Query query = selectStarSpoQuery(s, p, o);
        Eval eval = Eval.create(producer, new SparqlKgramEvaluator(), new RdfTermMatcher());

        Mappings mappings = eval.query(query);

        assertEquals(4, mappings.size());
        assertContainsMapping(mappings, ALICE, KNOWS, BOB);
    }

    @Test
    @DisplayName("Parser and bridge execute SELECT * WHERE { ?s ?p ?o } over StorageManagerProducer")
    void parserBridgeExecutesSelectStarSpoOverStorageManagerProducer() throws Exception {
        QueryAst ast = new SparqlParser().parse("SELECT * WHERE { ?s ?p ?o }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        Query query = new CoreseAstQueryBuilder().toNextQuery(select);
        Eval eval = Eval.create(producer, new SparqlKgramEvaluator(), new RdfTermMatcher());

        Mappings mappings = eval.query(query);

        assertEquals(4, mappings.size());
        assertContainsMapping(mappings, ALICE, KNOWS, BOB);
    }

    @Test
    @DisplayName("Producer materializes BGP mappings for SELECT * WHERE { ?s ?p ?o }")
    void producerMaterializesBgpMappings() {
        Node s = variable("s");
        Node p = variable("p");
        Node o = variable("o");
        Query query = selectStarSpoQuery(s, p, o);
        Environment environment = mock(Environment.class);
        when(environment.getQuery()).thenReturn(query);

        assertEquals(1, query.getBody().size());
        Exp bgp = query.getBody().first();
        assertEquals(1, bgp.size());
        assertEquals(4, edges(bgp.first().getEdge()).size());
        Mappings mappings = producer.getMappings(null, List.of(), bgp, environment);

        assertEquals(4, mappings.size());
        assertContainsMapping(mappings, ALICE, KNOWS, BOB);
    }

    @Test
    @DisplayName("Producer joins BGP mappings across repeated variables")
    void producerJoinsBgpMappingsAcrossRepeatedVariables() {
        Node s = variable("s");
        Node o = variable("o");
        Node name = variable("name");
        Query query = selectKnowsNameQuery(s, o, name);
        Environment environment = mock(Environment.class);
        when(environment.getQuery()).thenReturn(query);

        Exp bgp = query.getBody().first();
        Mappings mappings = producer.getMappings(null, List.of(), bgp, environment);

        assertEquals(2, mappings.size());
        assertContainsNameMapping(mappings, ALICE, BOB, "Bob");
        assertContainsNameMapping(mappings, ALICE, CAROL, "Carol");
    }

    private static void assertContainsMapping(Mappings mappings, String subject, String predicate, String object) {
        assertTrue(mappings.getMappingList().stream().anyMatch(mapping ->
                subject.equals(mapping.getValue("s").getLabel())
                        && predicate.equals(mapping.getValue("p").getLabel())
                        && object.equals(mapping.getValue("o").getLabel())));
    }

    private static void assertContainsNameMapping(Mappings mappings, String subject, String object, String name) {
        assertTrue(mappings.getMappingList().stream().anyMatch(mapping ->
                subject.equals(mapping.getValue("s").getLabel())
                        && object.equals(mapping.getValue("o").getLabel())
                        && name.equals(mapping.getValue("name").getLabel())));
    }

    private void insert(Resource subject, IRI predicate, Value object, Resource... contexts) {
        if (contexts == null || contexts.length == 0) {
            storage.getMutationOperations().insertStatement(valueFactory.createStatement(subject, predicate, object));
            return;
        }
        for (Resource context : contexts) {
            storage.getMutationOperations().insertStatement(
                    valueFactory.createStatement(subject, predicate, object, context));
        }
    }

    private IRI iri(String iri) {
        return valueFactory.createIRI(iri);
    }

    private List<Edge> edges(Edge queryEdge) {
        return edges(queryEdge, null, List.of(), null);
    }

    private List<Edge> edges(Edge queryEdge, Node graphNode, List<Node> from, Environment environment) {
        List<Edge> result = new ArrayList<>();
        producer.getEdges(graphNode, from, queryEdge, environment).forEach(result::add);
        return result;
    }

    private static Node variable(String name) {
        return new NodeImpl(Variable.create(name));
    }

    private static Node resource(String iri) {
        return new NodeImpl(Constant.create(DatatypeMap.newResource(iri)));
    }

    private static Node literal(String label) {
        return new NodeImpl(Constant.create(DatatypeMap.newLiteral(label)));
    }

    private static Query selectStarSpoQuery(Node s, Node p, Node o) {
        Exp bgp = Exp.create(Type.BGP);
        bgp.add(Exp.create(Type.EDGE, new TestQueryEdge(s, KgramNodes.rootProperty(), p, o)));
        return selectQuery(bgp);
    }

    private static Query selectKnowsNameQuery(Node s, Node o, Node name) {
        Exp bgp = Exp.create(Type.BGP);
        bgp.add(Exp.create(Type.EDGE, new TestQueryEdge(s, resource(KNOWS), null, o)));
        bgp.add(Exp.create(Type.EDGE, new TestQueryEdge(o, resource(NAME), null, name)));
        return selectQuery(bgp);
    }

    private static Query selectQuery(Exp bgp) {
        Exp body = Exp.create(Type.AND);
        body.add(bgp);
        Query query = Query.create(body);
        query.collect();
        List<Node> select = query.selectNodesFromPattern();
        query.setSelect(select);
        query.setSelectFun(select.stream().map(node -> Exp.create(Type.NODE, node)).toList());
        return query;
    }

    private record TestQueryEdge(Node subject, Node edgeNode, Node edgeVariable, Node object) implements Edge {

        @Override
        public Node getNode(int i) {
            return switch (i) {
                case 0 -> subject;
                case 1 -> object;
                default -> throw new IndexOutOfBoundsException();
            };
        }

        @Override
        public Node getEdgeNode() {
            return edgeNode;
        }

        @Override
        public Node getEdgeVariable() {
            return edgeVariable;
        }

        @Override
        public Node getProperty() {
            return edgeVariable == null ? edgeNode : edgeVariable;
        }

        @Override
        public String getEdgeLabel() {
            return edgeNode.getLabel();
        }

        @Override
        public Node getGraph() {
            return null;
        }

        @Override
        public Edge getEdge() {
            return this;
        }
    }
}
