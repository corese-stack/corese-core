package fr.inria.corese.core.next.data.impl.io.serializer.nquads;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NQuadsSerializerSortTest {

    private MockStatementFactory factory;
    private Resource subject;
    private IRI predicate;
    private IRI object;

    @BeforeEach
    void setUp() {
        factory = new MockStatementFactory();
        subject   = factory.createIRI("http://example.org/s");
        predicate = factory.createIRI("http://example.org/p");
        object    = factory.createIRI("http://example.org/o");
    }

    @Test
    @DisplayName("sorting should include graph as the last sort key for N-Quads")
    void sortIncludesGraphAsLastKey() {
        // Two quads identical in SPO but different graphs inserted in reverse order.
        // Sorting should place graphA before graphB.
        Resource graphB = factory.createIRI("http://example.org/graphB");
        Resource graphA = factory.createIRI("http://example.org/graphA");

        List<Statement> stmts = List.of(
                factory.createStatement(subject, predicate, object, graphB),
                factory.createStatement(subject, predicate, object, graphA)
        );

        NQuadsSerializerOptions opts = new NQuadsSerializerOptions.Builder()
                .sortStatements(true)
                .build();

        StringWriter writer = new StringWriter();
        new NQuadsSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.indexOf("graphA") < output.indexOf("graphB"),
                "graphA should appear before graphB when sorting is enabled");
    }

    @Test
    @DisplayName("no sorting should preserve insertion order for quads")
    void noSortPreservesInsertionOrderForQuads() {
        Resource graphB = factory.createIRI("http://example.org/graphB");
        Resource graphA = factory.createIRI("http://example.org/graphA");

        List<Statement> stmts = List.of(
                factory.createStatement(subject, predicate, object, graphB),
                factory.createStatement(subject, predicate, object, graphA)
        );

        NQuadsSerializerOptions opts = new NQuadsSerializerOptions.Builder()
                .sortStatements(false)
                .build();

        StringWriter writer = new StringWriter();
        new NQuadsSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.indexOf("graphB") < output.indexOf("graphA"),
                "graphB should appear first (insertion order preserved)");
    }
}
