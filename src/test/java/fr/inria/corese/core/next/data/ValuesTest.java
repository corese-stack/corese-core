package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Values Facade Tests")
class ValuesTest {

    @Test
    @DisplayName("Values.factory() returns a non-null default factory")
    void testFactory() {
        assertNotNull(Values.factory());
    }

    @Test
    @DisplayName("Values.iri creates valid IRIs")
    void testIri() {
        IRI iri1 = Values.iri("http://example.org/alice");
        assertNotNull(iri1);
        assertEquals("http://example.org/alice", iri1.stringValue());

        IRI iri2 = Values.iri("http://example.org/", "bob");
        assertNotNull(iri2);
        assertEquals("http://example.org/bob", iri2.stringValue());
    }

    @Test
    @DisplayName("Values.bnode creates blank nodes with generated and explicit IDs")
    void testBnode() {
        BNode b1 = Values.bnode();
        BNode b2 = Values.bnode();
        assertNotNull(b1);
        assertNotNull(b2);
        assertNotEquals(b1.getID(), b2.getID());

        BNode bExplicit = Values.bnode("b42");
        assertNotNull(bExplicit);
        assertEquals("b42", bExplicit.getID());
    }

    @Test
    @DisplayName("Values.literal creates plain, language, and typed literals")
    void testLiteral() {
        Literal lPlain = Values.literal("hello");
        assertEquals("hello", lPlain.getLabel());

        Literal lLang = Values.literal("bonjour", "fr");
        assertEquals("bonjour", lLang.getLabel());
        assertTrue(lLang.getLanguage().isPresent());
        assertEquals("fr", lLang.getLanguage().get());

        IRI xsdInteger = Values.iri("http://www.w3.org/2001/XMLSchema#integer");
        Literal lTyped = Values.literal("123", xsdInteger);
        assertEquals("123", lTyped.getLabel());
        assertEquals(xsdInteger, lTyped.getDatatype());
    }

    @Test
    @DisplayName("Values.literal creates primitive numeric and boolean literals")
    void testPrimitiveLiterals() {
        Literal lBool = Values.literal(true);
        assertEquals("true", lBool.getLabel());

        Literal lInt = Values.literal(42);
        assertEquals("42", lInt.getLabel());

        Literal lLong = Values.literal(123456789L);
        assertEquals("123456789", lLong.getLabel());

        Literal lDouble = Values.literal(2.5);
        assertEquals("2.5", lDouble.getLabel());

        Literal lDec = Values.literal(new BigDecimal("99.99"));
        assertEquals("99.99", lDec.getLabel());

        Literal lBigInt = Values.literal(BigInteger.valueOf(1000));
        assertEquals("1000", lBigInt.getLabel());
    }

    @Test
    @DisplayName("Values.statement creates default graph and named graph statements")
    void testStatement() {
        IRI s = Values.iri("http://example.org/s");
        IRI p = Values.iri("http://example.org/p");
        Literal o = Values.literal("o");
        IRI g = Values.iri("http://example.org/g");

        Statement defaultGraphStmt = Values.statement(s, p, o);
        assertNotNull(defaultGraphStmt);
        assertEquals(s, defaultGraphStmt.getSubject());
        assertEquals(p, defaultGraphStmt.getPredicate());
        assertEquals(o, defaultGraphStmt.getObject());
        assertNull(defaultGraphStmt.getContext());

        Statement namedGraphStmt = Values.statement(s, p, o, g);
        assertNotNull(namedGraphStmt);
        assertEquals(g, namedGraphStmt.getContext());
    }

    @Test
    @DisplayName("Values.triple creates RDF-star triple terms")
    void testTriple() {
        IRI s = Values.iri("http://example.org/s");
        IRI p = Values.iri("http://example.org/p");
        Literal o = Values.literal("o");

        Triple triple = Values.triple(s, p, o);
        assertNotNull(triple);
        assertEquals(s, triple.subject());
        assertEquals(p, triple.predicate());
        assertEquals(o, triple.object());
    }
}
