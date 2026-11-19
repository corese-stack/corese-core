package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the canonical sort key distinguishes literals that share the
 * same lexical form but have different language tags or datatypes, so the
 * CANONICAL profile always produces the same output regardless of insertion order.
 */
class NTriplesSerializerCanonicalKeyTest {

    private MockStatementFactory factory;
    private Resource subject;
    private IRI predicate;

    @BeforeEach
    void setUp() {
        factory  = new MockStatementFactory();
        subject   = factory.createIRI("http://example.org/s");
        predicate = factory.createIRI("http://example.org/p");
    }

    @Test
    @DisplayName("literals with the same lexical form but different language tags should be sorted stably")
    void sameLexicalFormDifferentLanguageTagsAreSortedStably() {
        // "hello"@fr inserted before "hello"@en — sort should produce @en first
        Literal helloFr = factory.createLiteral("hello", null, "fr");
        Literal helloEn = factory.createLiteral("hello", null, "en");

        List<Statement> stmts = List.of(
                factory.createStatement(subject, predicate, helloFr),
                factory.createStatement(subject, predicate, helloEn)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(true)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.indexOf("@en") < output.indexOf("@fr"),
                "@en should appear before @fr when sorting by canonical key");
    }

    @Test
    @DisplayName("literals with the same lexical form but different datatypes should be sorted stably")
    void sameLexicalFormDifferentDatatypesAreSortedStably() {
        // "42"^^xsd:integer inserted before "42"^^xsd:string — sort by datatype IRI
        Literal integer42 = factory.createLiteral("42", XSDDatatype.INTEGER.getIRI(), null);
        Literal string42  = factory.createLiteral("42", XSDDatatype.STRING.getIRI(),  null);

        // xsd:integer IRI < xsd:string IRI lexicographically
        List<Statement> stmts = List.of(
                factory.createStatement(subject, predicate, string42),
                factory.createStatement(subject, predicate, integer42)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortStatements(true)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.indexOf("integer") < output.indexOf("#string"),
                "xsd:integer should appear before xsd:string when sorting by canonical key");
    }
}
