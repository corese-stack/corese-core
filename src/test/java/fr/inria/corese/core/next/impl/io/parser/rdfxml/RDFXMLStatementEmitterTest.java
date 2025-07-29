package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RDFXMLStatementEmitterTest {

    private Model model;
    private ValueFactory factory;
    private RDFXMLStatementEmitter emitter;

    @BeforeEach
    public void setUp() {
        model = new CoreseModel();
        factory = new CoreseAdaptedValueFactory();
        emitter = new RDFXMLStatementEmitter(model, factory);
    }

    @Test
    public void testEmitLiteral_plain() {
        Literal literal = factory.createLiteral("hello");
        Resource subject = factory.createBNode();
        IRI predicate = factory.createIRI("http://example.org/predicate");
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

    @Test
    public void testEmitLiteral_withLang() {
        Resource subject = factory.createBNode();
        IRI predicate = factory.createIRI("http://example.org/predicate");
        emitter.emitLiteral(subject, predicate, "bonjour", null, "fr");

        Value obj = model.objects().iterator().next();
        assertTrue(obj.isLiteral());
        assertEquals("bonjour", obj.stringValue());
    }

    @Test
    public void testEmitLiteral_withDatatype() {
        Resource subject = factory.createBNode();
        IRI predicate = factory.createIRI("http://example.org/age");
        emitter.emitLiteral(subject, predicate, "42", XSD.INTEGER.getIRI().stringValue(), null);

        Value obj = model.objects().iterator().next();
        assertTrue(obj.isLiteral());
        assertEquals("42", obj.stringValue());
    }

    @Test
    public void testEmitType() {
        Resource subject = factory.createIRI("http://example.org/Alice");
        emitter.emitType(subject, "http://example.org/Person");

        assertTrue(model.contains(subject, RDF.type.getIRI(), factory.createIRI("http://example.org/Person")));
    }

    @Test
    public void testEmitResourceTriple() {
        Resource subject = factory.createIRI("http://example.org/Alice");
        IRI predicate = factory.createIRI("http://example.org/knows");
        emitter.emitResourceTriple(subject, predicate, "Bob", "http://example.org/");

        assertTrue(model.contains(subject, predicate, factory.createIRI("http://example.org/Bob")));
    }

    @Test
    public void testEmitBNodeTriple() {
        Resource subject = factory.createIRI("http://example.org/Alice");
        IRI predicate = factory.createIRI("http://example.org/knows");
        emitter.emitBNodeTriple(subject, predicate, "b123");

        assertTrue(model.size() == 1);
        Value obj = model.objects().iterator().next();
        assertTrue(obj.stringValue().contains("_:b123"));
    }

    @Test
    public void testEmitTriple() {
        Resource s = factory.createIRI("http://example.org/s");
        IRI p = factory.createIRI("http://example.org/p");
        Resource o = factory.createIRI("http://example.org/o");

        emitter.emitTriple(s, p, o);

        assertTrue(model.contains(s, p, o));
    }

    @Test
    public void testEmitPropertyAttributes() {
        Resource s = factory.createIRI("http://example.org/thing");
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("http://example.org/", "foo", "ex:foo", "CDATA", "val1");
        attrs.addAttribute("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about", "rdf:about", "CDATA", "ignored");

        emitter.emitPropertyAttributes(s, attrs);

        assertEquals(1, model.size());
        Value object = model.objects().iterator().next();
        assertEquals("val1", object.stringValue());
    }
}