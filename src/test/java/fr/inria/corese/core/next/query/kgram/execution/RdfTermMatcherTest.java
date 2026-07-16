package fr.inria.corese.core.next.query.kgram.execution;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Matcher;
import fr.inria.corese.core.next.query.kgram.tool.EnvironmentImpl;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RdfTermMatcher: strict RDF-term matching for graph pattern execution")
class RdfTermMatcherTest {

    private final RdfTermMatcher matcher = new RdfTermMatcher();

    private static Node iri(String label) {
        return new NodeImpl(Constant.createResource(label));
    }

    private static Node var(String name) {
        return new NodeImpl(Variable.create(name));
    }

    /**
     * Environment binding a single query variable to a value; every other lookup is unbound.
     */
    private static Environment bind(Node queryVar, Node value) {
        return new EnvironmentImpl() {
            @Override
            public Node getNode(Node q) {
                return q != null && q.same(queryVar) ? value : null;
            }
        };
    }

    @Test
    @DisplayName("A constant matches an equal constant")
    void constantMatchesEqualConstant() {
        assertTrue(matcher.match(iri("http://example.org/a"), iri("http://example.org/a"), null));
    }

    @Test
    @DisplayName("A constant does not match a different constant")
    void constantRejectsDifferentConstant() {
        assertFalse(matcher.match(iri("http://example.org/a"), iri("http://example.org/b"), null));
    }

    @Test
    @DisplayName("An unbound variable matches any term (null environment)")
    void unboundVariableMatchesAnyTerm() {
        assertTrue(matcher.match(var("s"), iri("http://example.org/a"), null));
    }

    @Test
    @DisplayName("An unbound variable matches any term (environment returns null)")
    void unboundVariableInEnvironmentMatchesAnyTerm() {
        Environment env = bind(var("other"), iri("http://example.org/z")); // ?s stays unbound
        assertTrue(matcher.match(var("s"), iri("http://example.org/a"), env));
    }

    @Test
    @DisplayName("An already-bound variable matches only its bound value (BGP join, compatible)")
    void boundVariableMatchesSameValue() {
        Node s = var("s");
        Environment env = bind(s, iri("http://example.org/a"));
        assertTrue(matcher.match(s, iri("http://example.org/a"), env));
    }

    @Test
    @DisplayName("An already-bound variable rejects a different value (BGP join, conflicting)")
    void boundVariableRejectsDifferentValue() {
        Node s = var("s");
        Environment env = bind(s, iri("http://example.org/a"));
        assertFalse(matcher.match(s, iri("http://example.org/b"), env));
    }

    @Test
    @DisplayName("null is an absent node, not a wildcard: only null matches null")
    void nullIsNotAWildcard() {
        Node a = iri("http://example.org/a");
        assertFalse(matcher.match(null, a, null), "null query does not match a term");
        assertFalse(matcher.match(a, null, null), "a term does not match a null target");
        assertTrue(matcher.match((Node) null, null, null), "null matches null");
    }


    @Test
    @DisplayName("Variable-predicate edge { ?s ?p ?o } matches a concrete triple (predicate via edge variable)")
    void edgeWithVariablePredicateMatches() {
        // query: ?s ?p ?o  (predicate is a variable, exposed via getEdgeVariable())
        Edge query = new TestEdge(var("s"), null, var("p"), var("o"));
        // target: <a> <p> <b>
        Edge target = new TestEdge(iri("http://example.org/a"),
                iri("http://example.org/p"), null, iri("http://example.org/b"));

        assertTrue(matcher.match(query, target, null));
    }

    @Test
    @DisplayName("Fixed-predicate edge { ?s <p> ?o } matches only the same predicate (predicate via edge node)")
    void edgeWithFixedPredicateMatchesSamePredicate() {
        // query: ?s <p> ?o  (fixed predicate, edge variable is null → predicate via getEdgeNode())
        Edge query = new TestEdge(var("s"), iri("http://example.org/p"), null, var("o"));

        Edge samePredicate = new TestEdge(iri("http://example.org/a"),
                iri("http://example.org/p"), null, iri("http://example.org/b"));
        Edge otherPredicate = new TestEdge(iri("http://example.org/a"),
                iri("http://example.org/q"), null, iri("http://example.org/b"));

        assertTrue(matcher.match(query, samePredicate, null), "same predicate matches");
        assertFalse(matcher.match(query, otherPredicate, null), "different predicate does not match");
    }

    @Test
    @DisplayName("same() is true for equal nodes, false otherwise, false when left is null")
    void sameSemantics() {
        Node a = iri("http://example.org/a");
        Node b = iri("http://example.org/b");
        assertTrue(matcher.same(null, a, iri("http://example.org/a"), null));
        assertFalse(matcher.same(null, a, b, null));
        assertFalse(matcher.same(null, null, a, null), "null left is not equal to anything");
    }

    @Test
    @DisplayName("Mode defaults to UNDEF and is settable")
    void modeDefaultsToUndefAndIsSettable() {
        assertEquals(Matcher.UNDEF, matcher.getMode());
        matcher.setMode(Matcher.RELAX);
        assertEquals(Matcher.RELAX, matcher.getMode());
    }

    /**
     * Minimal {@link Edge} for matcher tests: carries subject/object plus the predicate as either a
     * fixed edge node or an edge variable, mirroring {@code AstBackedEdge}'s representation.
     */
    private static final class TestEdge implements Edge {
        private final Node subject;
        private final Node edgeNode;
        private final Node edgeVariable;
        private final Node object;

        TestEdge(Node subject, Node edgeNode, Node edgeVariable, Node object) {
            this.subject = subject;
            this.edgeNode = edgeNode;
            this.edgeVariable = edgeVariable;
            this.object = object;
        }

        @Override
        public Node getNode(int i) {
            return i == 0 ? subject : object;
        }

        @Override
        public Node getProperty() {
            return edgeVariable != null ? edgeVariable : edgeNode;
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
            return edgeNode == null ? null : edgeNode.getLabel();
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