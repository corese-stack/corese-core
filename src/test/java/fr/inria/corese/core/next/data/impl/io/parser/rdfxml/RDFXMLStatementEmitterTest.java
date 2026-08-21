package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the RDFXMLStatementEmitter class.
 * This test suite verifies that the emitter correctly adds RDF statements to the provided
 * Model based on various RDF/XML constructs including:
 * - Plain literals
 * - Typed literals
 * - Language-tagged literals
 * - Resource IRIs
 * - Blank nodes
 * - RDF types
 * - Property attributes
 */
public class RDFXMLStatementEmitterTest extends ParserTestBase {

    private Model model;
    private RDFXMLStatementEmitter emitter;

    @BeforeEach
    public void setUp() {
        model = createTestModel();
        emitter = new RDFXMLStatementEmitter(model, valueFactory);
    }

    /**
     * Test emitting a plain literal statement without language or datatype.
     * Asserts that the triple is added to the model correctly.
     */
    @Test
    public void testEmitLiteral_plain() {
        Literal literal = valueFactory.createLiteral("hello");
        Resource subject = valueFactory.createBNode();
        IRI predicate = valueFactory.createIRI("http://example.org/predicate");
        emitter.emitLiteral(subject, predicate, "hello", null, null);
        assertEquals(1, model.size());
        Iterable<Statement> statements = model.getStatements(subject, predicate, literal);
        boolean found = false;
        for (Statement stmt : statements) {
            if (stmt.getSubject().equals(subject) &&
                    stmt.getPredicate().equals(predicate) &&
                    stmt.getObject().stringValue().equals(literal.stringValue())) {
                found = true;
                break;
            }
        }

        assertTrue(found, "Expected statement not found in model");
    }

    /**
     * Test emitting a literal with a language tag.
     * Verifies that the correct literal is added to the model.
     */
    @Test
    public void testEmitLiteral_withLang() {
        Resource subject = valueFactory.createBNode();
        IRI predicate = valueFactory.createIRI("http://example.org/predicate");
        emitter.emitLiteral(subject, predicate, "bonjour", null, "fr");

        Value obj = model.objects().iterator().next();
        assertTrue(obj.isLiteral());
        assertEquals("bonjour", obj.stringValue());
    }

    /**
     * Test emitting a literal with a datatype IRI.
     * Verifies that the correct typed literal is added to the model.
     */
    @Test
    public void testEmitLiteral_withDatatype() {
        Resource subject = valueFactory.createBNode();
        IRI predicate = valueFactory.createIRI("http://example.org/age");
        emitter.emitLiteral(subject, predicate, "42", XSDDatatype.INTEGER.getIRI().stringValue(), null);

        Value obj = model.objects().iterator().next();
        assertTrue(obj.isLiteral());
        assertEquals("42", obj.stringValue());
    }

    /**
     * Test emitting a rdf:type statement for a subject.
     * Verifies that the rdf:type triple is correctly created.
     */
    @Test
    public void testEmitType() {
        Resource subject = valueFactory.createIRI("http://example.org/Alice");
        emitter.emitType(subject, "http://example.org/Person");

        assertTrue(model.contains(subject, RDF.type.getIRI(), valueFactory.createIRI("http://example.org/Person")));
    }

    /**
     * Test emitting a triple where the object is a resource IRI resolved against a base.
     */
    @Test
    public void testEmitResourceTriple() {
        Resource subject = valueFactory.createIRI("http://example.org/Alice");
        IRI predicate = valueFactory.createIRI("http://example.org/knows");
        emitter.emitResourceTriple(subject, predicate, "Bob", "http://example.org/");

        assertTrue(model.contains(subject, predicate, valueFactory.createIRI("http://example.org/Bob")));
    }

    /**
     * Test emitting a triple where the object is a blank node identified by nodeID.
     */
    @Test
    public void testEmitBNodeTriple() {
        Resource subject = valueFactory.createIRI("http://example.org/Alice");
        IRI predicate = valueFactory.createIRI("http://example.org/knows");
        emitter.emitBNodeTriple(subject, predicate, "b123");

        assertEquals(1, model.size());
        Value obj = model.objects().iterator().next();
        assertTrue(obj.stringValue().contains("_:b123"));
    }

    /**
     * Test emitting a generic triple with subject, predicate, and object resources.
     */
    @Test
    public void testEmitTriple() {
        Resource s = valueFactory.createIRI("http://example.org/s");
        IRI p = valueFactory.createIRI("http://example.org/p");
        Resource o = valueFactory.createIRI("http://example.org/o");

        emitter.emitTriple(s, p, o);

        assertTrue(model.contains(s, p, o));
    }

    /**
     * Test emitting triples from XML attributes.
     */
    @Test
    public void testEmitPropertyAttributes() {
        Resource s = valueFactory.createIRI("http://example.org/thing");
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("http://example.org/", "foo", "ex:foo", "CDATA", "val1");
        attrs.addAttribute("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about", "rdf:about", "CDATA", "ignored");

        emitter.emitPropertyAttributes(s, attrs);

        assertEquals(1, model.size());
        Value object = model.objects().iterator().next();
        assertEquals("val1", object.stringValue());
    }
}
