package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.ExpType.Type;
import fr.inria.corese.core.kgram.api.core.Filter;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.tool.NodeImpl;
import fr.inria.corese.core.next.query.impl.sparql.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that {@code SELECT} queries parsed by the Corese-next SPARQL parser
 * and compiled by {@link CoreseAstQueryBuilder} can be executed by the
 * historical {@link QueryProcess} over a historical {@link Graph}.
 *
 * <p>The legacy KGRAM adapter in this test is deliberately local to the test
 * source set. It proves the interoperability point required by issue #471 without
 * adding a legacy execution path to the Corese-next production bridge. When the
 * historical query engine is removed, this compatibility test and its local
 * adapter can be removed with it.</p>
 */
class CoreseAstQueryBuilderLegacyExecutionTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Parser -> next Query -> legacy Query -> QueryProcess executes SELECT * WHERE { ?s ?p ?o }")
    void executesSelectStarSpoOnLegacyGraph() throws EngineException {
        Mappings mappings = execute("SELECT * WHERE { ?s ?p ?o }", graphWithFamilyData());

        assertEquals(4, mappings.size());
        assertEquals("http://example.org/alice", mappings.getValue("s").getLabel());
        assertEquals("http://example.org/knows", mappings.getValue("p").getLabel());
        assertEquals("http://example.org/bob", mappings.getValue("o").getLabel());
    }

    @Test
    @DisplayName("Explicit projection and constant predicate execute through legacy QueryProcess")
    void executesExplicitProjectionWithConstantPredicate() throws EngineException {
        Mappings mappings = execute(
                "SELECT ?s ?o WHERE { ?s <http://example.org/knows> ?o }",
                graphWithFamilyData());

        assertEquals(2, mappings.size());
        assertEquals("http://example.org/alice", mappings.get(0).getValue("s").getLabel());
        assertEquals("http://example.org/bob", mappings.get(0).getValue("o").getLabel());
        assertEquals("http://example.org/alice", mappings.get(1).getValue("s").getLabel());
        assertEquals("http://example.org/carol", mappings.get(1).getValue("o").getLabel());
    }

    @Test
    @DisplayName("Multiple triple patterns join after parser -> next Query -> legacy Query")
    void executesJoinedBgp() throws EngineException {
        Mappings mappings = execute("""
                SELECT ?friend ?name WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?friend .
                  ?friend <http://example.org/name> ?name
                }
                """, graphWithFamilyData());

        assertEquals(2, mappings.size());
        assertEquals("Bob", mappings.get(0).getValue("name").getLabel());
        assertEquals("Carol", mappings.get(1).getValue("name").getLabel());
    }

    @Test
    @DisplayName("FILTER expressions carried by the next bridge execute in the legacy engine")
    void executesFilter() throws EngineException {
        Mappings mappings = execute("""
                SELECT ?friend WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?friend .
                  FILTER(?friend = <http://example.org/bob>)
                }
                """, graphWithFamilyData());

        assertEquals(1, mappings.size());
        assertEquals("http://example.org/bob", mappings.getValue("friend").getLabel());
    }

    @Test
    @DisplayName("OPTIONAL patterns compiled by WhereCompiler execute in the legacy engine")
    void executesOptional() throws EngineException {
        Mappings mappings = execute("""
                SELECT ?friend ?name WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?friend .
                  OPTIONAL { ?friend <http://example.org/name> ?name }
                }
                """, graphWithFamilyData());

        assertEquals(2, mappings.size());
        assertEquals("Bob", mappings.get(0).getValue("name").getLabel());
        assertEquals("Carol", mappings.get(1).getValue("name").getLabel());
    }

    @Test
    @DisplayName("UNION patterns compiled by WhereCompiler execute in the legacy engine")
    void executesUnion() throws EngineException {
        Mappings mappings = execute("""
                SELECT ?person WHERE {
                  { ?person <http://example.org/name> "Bob" }
                  UNION
                  { ?person <http://example.org/name> "Carol" }
                }
                """, graphWithFamilyData());

        assertEquals(2, mappings.size());
        assertEquals("http://example.org/bob", mappings.get(0).getValue("person").getLabel());
        assertEquals("http://example.org/carol", mappings.get(1).getValue("person").getLabel());
    }

    @Test
    @DisplayName("ORDER BY copied by CoreseAstQueryBuilder executes in legacy QueryProcess")
    void executesOrderBy() throws EngineException {
        Mappings mappings = execute("""
                SELECT ?friend WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?friend
                }
                ORDER BY DESC(?friend)
                """, graphWithFamilyData());

        assertEquals(2, mappings.size());
        assertEquals("http://example.org/carol", mappings.get(0).getValue("friend").getLabel());
        assertEquals("http://example.org/bob", mappings.get(1).getValue("friend").getLabel());
    }

    @Test
    @DisplayName("DISTINCT, LIMIT and OFFSET copied by CoreseAstQueryBuilder execute in legacy QueryProcess")
    void executesDistinctLimitOffset() throws EngineException {
        Mappings mappings = execute("""
                SELECT DISTINCT ?friend WHERE {
                  <http://example.org/alice> <http://example.org/knows> ?friend
                }
                OFFSET 1
                LIMIT 1
                """, graphWithFamilyData());

        assertEquals(1, mappings.size());
        assertEquals("http://example.org/carol", mappings.getValue("friend").getLabel());
    }

    private Mappings execute(String sparql, Graph graph) throws EngineException {
        QueryAst ast = newParserDefault().parse(sparql);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery =
                new CoreseAstQueryBuilder().toNextQuery(select);
        Query query = TestLegacyQueryAdapter.convert(nextQuery);
        return QueryProcess.create(graph).query(query);
    }

    private static Graph graphWithFamilyData() {
        Graph graph = Graph.create();
        insert(graph, "http://example.org/alice", "http://example.org/knows", "http://example.org/bob");
        insert(graph, "http://example.org/alice", "http://example.org/knows", "http://example.org/carol");
        insertLiteral(graph, "http://example.org/bob", "http://example.org/name", "Bob");
        insertLiteral(graph, "http://example.org/carol", "http://example.org/name", "Carol");
        return graph;
    }

    private static void insert(Graph graph, String subject, String predicate, String object) {
        graph.insert(
                DatatypeMap.newResource(subject),
                DatatypeMap.newResource(predicate),
                DatatypeMap.newResource(object));
    }

    private static void insertLiteral(Graph graph, String subject, String predicate, String object) {
        graph.insert(
                DatatypeMap.newResource(subject),
                DatatypeMap.newResource(predicate),
                DatatypeMap.newLiteral(object));
    }

    /**
     * Test harness that adapts the Corese-next KGRAM query built by
     * {@link CoreseAstQueryBuilder} to the legacy KGRAM query classes expected by
     * {@link QueryProcess}.
     *
     * <p>The production bridge remains responsible for turning the parsed AST into
     * the Corese-next runtime shape. This harness keeps only the legacy execution
     * adaptation local to this test: it copies the runtime forms already produced
     * by {@link WhereCompiler} and {@link CoreseAstQueryBuilder} when they have a
     * direct counterpart in the historical KGRAM API, and rejects the rest
     * explicitly. It does not repair query semantics that the Corese-next bridge
     * failed to expose.</p>
     */
    private static final class TestLegacyQueryAdapter {

        private final Map<String, Node> variables = new LinkedHashMap<>();

        private TestLegacyQueryAdapter() {
        }

        private static Query convert(fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            return new TestLegacyQueryAdapter().convertQuery(nextQuery);
        }

        private Query convertQuery(fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            rejectUnsupportedQueryShape(nextQuery);

            Query query = Query.create(convertBody(nextQuery.getBody()));
            query.setAST(selectAst());
            query.collect();
            applySelect(query, nextQuery);
            applyDataset(query, nextQuery);
            applySolutionModifiers(query, nextQuery);
            return query;
        }

        private static void rejectUnsupportedQueryShape(fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            if (nextQuery.getSelect().isEmpty()) {
                throw new IllegalArgumentException("Test adapter expects SELECT variables from the next bridge");
            }
        }

        private static ASTQuery selectAst() {
            ASTQuery ast = ASTQuery.create();
            ast.setResultForm(ASTQuery.QT_SELECT);
            ast.setSelectAll(true);
            return ast;
        }

        private Exp convertBody(fr.inria.corese.core.next.query.impl.kgram.core.Exp nextBody) {
            if (!nextBody.isAnd()) {
                throw new IllegalArgumentException(
                        "Test adapter expects an AND query body, got: " + nextBody.type());
            }
            Exp body = Exp.create(Type.AND);
            for (fr.inria.corese.core.next.query.impl.kgram.core.Exp expression : nextBody) {
                body.add(convertExpression(expression));
            }
            return body;
        }

        private Exp convertExpression(fr.inria.corese.core.next.query.impl.kgram.core.Exp expression) {
            if (expression.isAnd()) {
                return convertBody(expression);
            }
            if (expression.isBGP()) {
                return convertBgp(expression);
            }
            if (expression.isFilter()) {
                return Exp.create(Type.FILTER, filter(expression.getFilter()));
            }
            if (expression.isOptional()) {
                return Exp.create(
                        Type.OPTIONAL,
                        convertExpression(expression.first()),
                        convertExpression(expression.rest()));
            }
            if (expression.isUnion()) {
                return Exp.create(
                        Type.UNION,
                        convertExpression(expression.first()),
                        convertExpression(expression.rest()));
            }
            if (expression.isMinus()) {
                return Exp.create(
                        Type.MINUS,
                        convertExpression(expression.first()),
                        convertExpression(expression.rest()));
            }
            if (expression.isBind()) {
                Exp bind = Exp.create(Type.BIND);
                bind.setFilter(filter(expression.getFilter()));
                bind.setFunctional(expression.isFunctional());
                bind.setNode(node(expression.getNode()));
                return bind;
            }
            // TODO(next-query): SERVICE requires a network-aware execution policy before this adapter can copy it.
            // TODO(next-query): VALUES requires converting next mappings to legacy mappings.
            throw new IllegalArgumentException(
                    "Test adapter does not support expression type: " + expression.type());
        }

        private Exp convertBgp(fr.inria.corese.core.next.query.impl.kgram.core.Exp nextBgp) {
            Exp exp = Exp.create(Type.BGP);
            for (fr.inria.corese.core.next.query.impl.kgram.core.Exp expression : nextBgp) {
                if (!expression.isEdge()) {
                    throw new IllegalArgumentException(
                            "Test adapter supports only EDGE expressions inside BGP, got: " + expression.type());
                }
                exp.add(Exp.create(Type.EDGE, edge(expression.getEdge())));
            }
            return exp;
        }

        private Edge edge(fr.inria.corese.core.next.query.impl.kgram.api.core.Edge nextEdge) {
            return new TestLegacyEdge(
                    node(nextEdge.getNode(0)),
                    node(nextEdge.getEdgeNode()),
                    nextEdge.getEdgeVariable() == null ? null : node(nextEdge.getEdgeVariable()),
                    node(nextEdge.getNode(1)));
        }

        private Node node(fr.inria.corese.core.next.query.impl.kgram.api.core.Node nextNode) {
            if (nextNode.isVariable()) {
                return variables.computeIfAbsent(nextNode.getLabel(), label -> new NodeImpl(Variable.create(label)));
            }
            return new NodeImpl(Constant.create(nextNode.getDatatypeValue()));
        }

        private static Filter filter(fr.inria.corese.core.next.query.impl.kgram.api.core.Filter nextFilter) {
            return nextFilter.getFilterExpression();
        }

        private void applySelect(
                Query legacyQuery,
                fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            List<Exp> selectExpressions = new ArrayList<>();
            for (fr.inria.corese.core.next.query.impl.kgram.api.core.Node nextNode : nextQuery.getSelect()) {
                selectExpressions.add(Exp.create(Type.NODE, node(nextNode)));
            }

            legacyQuery.setSelectFun(selectExpressions);
            legacyQuery.setSelect(selectNodeList(selectExpressions));
        }

        private void applyDataset(
                Query legacyQuery,
                fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            legacyQuery.setFrom(nodeList(nextQuery.getFrom()));
            legacyQuery.setNamed(nodeList(nextQuery.getNamed()));
        }

        private void applySolutionModifiers(
                Query legacyQuery,
                fr.inria.corese.core.next.query.impl.kgram.core.Query nextQuery) {
            legacyQuery.setDistinct(nextQuery.isDistinct());
            legacyQuery.setLimit(nextQuery.getLimit());
            legacyQuery.setOffset(nextQuery.getOffset());
            legacyQuery.setOrderBy(orderByList(nextQuery.getOrderBy()));
        }

        private List<Exp> orderByList(List<fr.inria.corese.core.next.query.impl.kgram.core.Exp> nextOrderBy) {
            List<Exp> legacyOrderBy = new ArrayList<>();
            for (fr.inria.corese.core.next.query.impl.kgram.core.Exp nextExpression : nextOrderBy) {
                Exp expression = Exp.create(Type.NODE, node(nextExpression.getNode()));
                if (nextExpression.getFilter() != null) {
                    expression.setFilter(filter(nextExpression.getFilter()));
                }
                expression.status(nextExpression.status());
                legacyOrderBy.add(expression);
            }
            return legacyOrderBy;
        }

        private static List<Node> selectNodeList(List<Exp> selectExpressions) {
            LinkedHashSet<Node> nodes = new LinkedHashSet<>();
            for (Exp expression : selectExpressions) {
                nodes.add(expression.getNode());
            }
            return new ArrayList<>(nodes);
        }

        private List<Node> nodeList(List<fr.inria.corese.core.next.query.impl.kgram.api.core.Node> nextNodes) {
            List<Node> nodes = new ArrayList<>();
            for (fr.inria.corese.core.next.query.impl.kgram.api.core.Node nextNode : nextNodes) {
                nodes.add(node(nextNode));
            }
            return nodes;
        }
    }

    /**
     * Legacy query edge converted from a Corese-next query edge.
     *
     * <p>The adapter copies the edge node and edge variable already exposed by
     * the Corese-next bridge. It does not repair predicate-variable semantics;
     * the bridge is responsible for exposing the KGRAM root property as edge node
     * and the predicate variable through {@link #getEdgeVariable()}.</p>
     */
    private static final class TestLegacyEdge implements Edge {

        private final Node subject;
        private final Node edgeNode;
        private final Node edgeVariable;
        private final Node object;

        private TestLegacyEdge(Node subject, Node edgeNode, Node edgeVariable, Node object) {
            this.subject = Objects.requireNonNull(subject, "subject");
            this.edgeNode = Objects.requireNonNull(edgeNode, "edgeNode");
            this.edgeVariable = edgeVariable;
            this.object = Objects.requireNonNull(object, "object");
        }

        @Override
        public Node getNode(int i) {
            return switch (i) {
                case 0 -> subject;
                case 1 -> object;
                default -> throw new IndexOutOfBoundsException(
                        "Triple pattern edge has nodes 0 (subject) and 1 (object), got: " + i);
            };
        }

        @Override
        public Node getProperty() {
            return edgeVariable == null ? edgeNode : edgeVariable;
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
        public String getEdgeLabel() {
            return edgeNode.getLabel();
        }

        @Override
        public Node getGraph() {
            return null;
        }

        @Override
        public Node getNode() {
            return null;
        }

        @Override
        public boolean contains(Node node) {
            if (node == null) {
                return false;
            }
            for (int i = 0; i < nbNode(); i++) {
                Node current = getNode(i);
                if (current != null && (current == node || current.same(node))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Edge getEdge() {
            return this;
        }
    }
}
