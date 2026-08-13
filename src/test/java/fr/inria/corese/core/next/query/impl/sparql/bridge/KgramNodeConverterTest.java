package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KgramNodeConverterTest {

    private CoreseValueFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CoreseValueFactory();
    }

    @Test
    @DisplayName("IRI node converts to API IRI with the same string value")
    void iriNodeConvertsToApiIRI() {
        Node node = NodeImpl.forIRI("http://example.org/alice");

        Value value = KgramNodeConverter.nodeToValue(node, factory);

        assertInstanceOf(IRI.class, value);
        assertEquals("http://example.org/alice", value.stringValue());
    }

    @Test
    @DisplayName("Blank node converts to API BNode with the same ID")
    void blankNodeConvertsToApiBNode() {
        Node node = NodeImpl.forBlank("b1");

        Value value = KgramNodeConverter.nodeToValue(node, factory);

        assertInstanceOf(BNode.class, value);
        assertEquals("b1", ((BNode) value).getID());
    }

    @Test
    @DisplayName("Language-tagged literal converts to API Literal preserving label and lang")
    void langLiteralConvertsToApiLiteral() {
        Node node = NodeImpl.forLiteral("hello", null, "en");

        Value value = KgramNodeConverter.nodeToValue(node, factory);

        assertInstanceOf(Literal.class, value);
        Literal lit = (Literal) value;
        assertEquals("hello", lit.getLabel());
        assertEquals("en", lit.getLanguage().orElse(null));
    }

    @Test
    @DisplayName("Typed literal converts to API Literal preserving label and datatype IRI")
    void typedLiteralConvertsToApiLiteral() {
        String xsdInteger = "http://www.w3.org/2001/XMLSchema#integer";
        Node node = NodeImpl.forLiteral("42", xsdInteger, null);

        Value value = KgramNodeConverter.nodeToValue(node, factory);

        assertInstanceOf(Literal.class, value);
        Literal lit = (Literal) value;
        assertEquals("42", lit.getLabel());
        assertNotNull(lit.getDatatype());
        assertEquals(xsdInteger, lit.getDatatype().stringValue());
        assertTrue(lit.getLanguage().isEmpty());
    }

    @Test
    @DisplayName("Plain literal (no lang, no explicit datatype) converts to API Literal with label")
    void plainLiteralConvertsToApiLiteral() {
        Node node = NodeImpl.forLiteral("bare", null, null);

        Value value = KgramNodeConverter.nodeToValue(node, factory);

        assertInstanceOf(Literal.class, value);
        assertEquals("bare", ((Literal) value).getLabel());
    }

}
