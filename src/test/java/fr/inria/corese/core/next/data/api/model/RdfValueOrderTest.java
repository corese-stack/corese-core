package fr.inria.corese.core.next.data.api.model;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RdfValueOrderTest {

    private final ValueFactory values = Values.factory();

    @Test
    void followsSparqlTermCategoryOrder() {
        DatatypeValue blank = values.createBNode("b");
        DatatypeValue iri = values.createIRI("http://example.org/resource");
        DatatypeValue literal = values.createLiteral("value");
        DatatypeValue triple = values.createTriple(
                values.createIRI("http://example.org/subject"),
                values.createIRI("http://example.org/predicate"),
                (Value) literal);

        assertTrue(RdfValueOrder.compareValues(null, blank) < 0);
        assertTrue(blank.compare(iri) < 0);
        assertTrue(iri.compare(literal) < 0);
        assertTrue(literal.compare(triple) < 0);
    }

    @Test
    void comparesNumericLiteralsByValueBeforeLexicalForm() {
        DatatypeValue two = values.createLiteral(new BigDecimal("2"));
        DatatypeValue ten = values.createLiteral(new BigDecimal("10"));

        assertTrue(two.compare(ten) < 0);
    }

    @Test
    void usesRdfTermTieBreakersForEqualNumericValues() {
        DatatypeValue integer = values.createLiteral("1", XSD.xsdInteger.getIRI());
        DatatypeValue decimal = values.createLiteral("1.0", XSD.xsdDecimal.getIRI());

        assertTrue(integer.equalsWE(decimal));
        assertNotEquals(0, integer.compare(decimal));
    }

    @Test
    void ordersBooleansAndStringsDeterministically() {
        assertTrue(values.createLiteral(false).compare(values.createLiteral(true)) < 0);
        assertTrue(values.createLiteral("alpha").compare(values.createLiteral("beta")) < 0);
        assertEquals(0, values.createLiteral("same").compare(values.createLiteral("same")));
    }

    @Test
    void numericValueEqualityHandlesNanAndSignedZero() {
        Literal leftNan = floatingPointLiteral(Double.NaN);
        Literal rightNan = floatingPointLiteral(Double.NaN);

        assertFalse(leftNan.equalsWE(rightNan));
        assertTrue(values.createLiteral(-0.0d).equalsWE(values.createLiteral(0.0d)));
    }

    private static Literal floatingPointLiteral(double value) {
        Literal literal = mock(Literal.class, CALLS_REAL_METHODS);
        when(literal.getCoreDatatype()).thenReturn(XSDDatatype.DOUBLE);
        when(literal.doubleValue()).thenReturn(value);
        return literal;
    }
}
