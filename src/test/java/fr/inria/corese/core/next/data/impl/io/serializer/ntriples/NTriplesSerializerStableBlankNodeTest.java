package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that stableBlankNodeIds produces identical output regardless of
 * the original blank node identifiers, so CANONICAL mode is truly deterministic.
 */
class NTriplesSerializerStableBlankNodeTest {

    private MockStatementFactory factory;
    private IRI predicate;
    private IRI object;

    @BeforeEach
    void setUp() {
        factory   = new MockStatementFactory();
        predicate = factory.createIRI("http://example.org/p");
        object    = factory.createIRI("http://example.org/o");
    }

    @Test
    @DisplayName("stableBlankNodeIds should rename blank nodes to sequential b0, b1, … identifiers")
    void stableBlankNodeIdsRenamesBlankNodesSequentially() {
        Resource bnode = factory.createBlankNode("some-random-uuid-1234");

        List<Statement> stmts = List.of(
                factory.createStatement(bnode, predicate, object)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(true)
                .stableBlankNodeIds(true)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.contains("_:b0"), "blank node should be renamed to b0");
        assertFalse(output.contains("some-random-uuid-1234"), "original ID should not appear");
    }

    @Test
    @DisplayName("two graphs with same structure but different blank node IDs should produce identical canonical output")
    void twoGraphsWithDifferentBlankNodeIdsProduceSameCanonicalOutput() {
        // Graph 1: blank node with ID "x1"
        Resource bnode1 = factory.createBlankNode("x1");
        List<Statement> graph1 = List.of(factory.createStatement(bnode1, predicate, object));

        // Graph 2: same structure, blank node with ID "y9999"
        Resource bnode2 = factory.createBlankNode("y9999");
        List<Statement> graph2 = List.of(factory.createStatement(bnode2, predicate, object));

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(true)
                .stableBlankNodeIds(true)
                .build();

        StringWriter w1 = new StringWriter();
        new NTriplesSerializer(graph1, opts).write(w1);

        StringWriter w2 = new StringWriter();
        new NTriplesSerializer(graph2, opts).write(w2);

        assertEquals(w1.toString(), w2.toString(),
                "two graphs with the same structure but different blank node IDs should produce identical canonical output");
    }
}
