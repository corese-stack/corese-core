package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NTriplesSerializerSortTest {

    private MockStatementFactory factory;
    private IRI predicate;

    @BeforeEach
    void setUp() {
        factory = new MockStatementFactory();
        predicate = factory.createIRI("http://example.org/p");
    }

    @Test
    void sortStatementsShouldOutputLinesLexicographically() {
        Resource subjC = factory.createIRI("http://example.org/ccc");
        Resource subjA = factory.createIRI("http://example.org/aaa");
        Resource subjB = factory.createIRI("http://example.org/bbb");
        IRI obj = factory.createIRI("http://example.org/o");

        // Insert in order: C, A, B
        List<Statement> stmts = Arrays.asList(
                factory.createStatement(subjC, predicate, obj),
                factory.createStatement(subjA, predicate, obj),
                factory.createStatement(subjB, predicate, obj)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(true)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        String[] lines = output.strip().split("\n");
        assertTrue(lines[0].contains("aaa"), "first line should contain aaa");
        assertTrue(lines[1].contains("bbb"), "second line should contain bbb");
        assertTrue(lines[2].contains("ccc"), "third line should contain ccc");
    }

    @Test
    void noSortStatementsShouldPreserveInsertionOrder() {
        Resource subjC = factory.createIRI("http://example.org/ccc");
        Resource subjA = factory.createIRI("http://example.org/aaa");
        IRI obj = factory.createIRI("http://example.org/o");

        // Insert C then A
        List<Statement> stmts = Arrays.asList(
                factory.createStatement(subjC, predicate, obj),
                factory.createStatement(subjA, predicate, obj)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(false)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        String[] lines = output.strip().split("\n");
        assertTrue(lines[0].contains("ccc"), "first line should contain ccc (insertion order)");
        assertTrue(lines[1].contains("aaa"), "second line should contain aaa (insertion order)");
    }
}
