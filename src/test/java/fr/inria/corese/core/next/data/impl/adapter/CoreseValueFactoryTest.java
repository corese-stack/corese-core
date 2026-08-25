package fr.inria.corese.core.next.data.impl.adapter;

import java.time.Duration;
import java.math.BigDecimal;
import java.math.BigInteger;

import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseIRI;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseNodeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.factory.ValueFactoryTest;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.impl.adapter.literal.CoreseLanguageTaggedStringLiteral;
import fr.inria.corese.core.next.data.impl.adapter.literal.CoreseTyped;
import fr.inria.corese.core.sparql.datatype.CoreseDate;

import static org.junit.jupiter.api.Assertions.*;

class CoreseValueFactoryTest extends ValueFactoryTest {

    private String stringTestValue;
    private IRI xsdStringIRI;

    private Resource subject;
    private IRI predicate;
    private Resource context;

    @BeforeEach
    @Override
    public void setUp() {
        this.valueFactory = new CoreseValueFactory();
        stringTestValue = "String value";
        xsdStringIRI = XSDDatatype.STRING.getIRI();
        subject = new CoreseIRI("http://corese.com/subject");
        predicate = new CoreseIRI("http://corese.com/predicate");
    }

    @Test
    @Override
    public void testCreateLiteralTemporalAmount() {
        Duration duration = Duration.ofHours(23);
        assertNotNull(this.valueFactory.createLiteral(duration));
    }

    @Test
    void testCreateLiteralWithLabel() {
        // Test createLiteral with label
        Literal literal = valueFactory.createLiteral(stringTestValue);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSDDatatype.STRING, literal.getCoreDatatype());
    }

    @Test
    void testCreateLiteralWithLabelAndLanguage() {
        String testLanguage = "en";

        // Test createLiteral with label and language
        Literal literal = valueFactory.createLiteral(stringTestValue, testLanguage);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseLanguageTaggedStringLiteral);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(testLanguage, literal.getLanguage().orElse(null));
        assertEquals(RDFDatatype.LANGSTRING, literal.getCoreDatatype());
    }

    @Test
    void testCreateLiteralWithDatatypeIRI() {
        // Test createLiteral with IRI datatype (XSD.STRING)
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSDDatatype.STRING, literal.getCoreDatatype());
    }

    @Test
    void testCreateStatementWithoutContext() {
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI);
        CoreseStatement statement = (CoreseStatement) valueFactory.createStatement(subject, predicate, literal);
        assertNotNull(statement);
        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(literal, statement.getObject());
        assertNull(statement.getContext());
    }

    @Test
    void testCreateStatementWithContext() {
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI);

        CoreseStatement statement = (CoreseStatement) valueFactory.createStatement(subject, predicate, literal, context);

        assertNotNull(statement);
        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(literal, statement.getObject());
        assertEquals(context, statement.getContext());
    }

    @Test
    void testCreateFOAFURI() {
        IRI foaf = valueFactory.createIRI("http://xmlns.com/foaf/0.1/");
        assertNotNull(foaf);
        assertEquals("http://xmlns.com/foaf/0.1/", foaf.stringValue());
    }

    @Test
    void testDateCreation() {
        IRI xsdDate = valueFactory.createIRI("http://www.w3.org/2001/XMLSchema#date");
        String literalStringValue = "2025-11-20";
        Literal date = valueFactory.createLiteral(literalStringValue, xsdDate);

        assertNotNull(date);
        assertEquals(XSD.xsdDate.getIRI().stringValue(), date.getDatatype().stringValue());
        assertEquals(literalStringValue, date.getLabel());
        assertInstanceOf(CoreseDate.class, ((CoreseNodeAdapter) date).getCoreseNode());
    }

    @Test
    void typedNumericLiteralPreservesLexicalFormAndDatatype() {
        Literal literal = valueFactory.createLiteral("01", XSDDatatype.INT.getIRI());

        assertEquals("01", literal.getLabel());
        assertEquals(XSDDatatype.INT.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.INT, literal.getCoreDatatype());
        assertEquals(1, literal.intValue());
    }

    @Test
    void booleanLiteralPreservesValidAndIllTypedLexicalForms() {
        Literal numericTrue = valueFactory.createLiteral("1", XSDDatatype.BOOLEAN.getIRI());
        Literal numericFalse = valueFactory.createLiteral("0", XSDDatatype.BOOLEAN.getIRI());

        assertTrue(numericTrue.booleanValue());
        assertFalse(numericFalse.booleanValue());
        assertEquals("1", numericTrue.getLabel());
        assertEquals("0", numericFalse.getLabel());
        assertNotEquals(numericTrue, valueFactory.createLiteral("true", XSDDatatype.BOOLEAN.getIRI()));
        Literal illTyped = valueFactory.createLiteral("TRUE", XSDDatatype.BOOLEAN.getIRI());

        assertEquals("TRUE", illTyped.getLabel());
        assertEquals(XSDDatatype.BOOLEAN.getIRI(), illTyped.getDatatype());
        assertEquals(XSDDatatype.BOOLEAN, illTyped.getCoreDatatype());
        assertThrows(IncorrectOperationException.class, illTyped::booleanValue);
    }

    @Test
    void illTypedNumericLiteralRemainsAnRdfTermWithoutANumericValue() {
        Literal illTyped = valueFactory.createLiteral("not-an-integer", XSDDatatype.INTEGER.getIRI());

        assertEquals("not-an-integer", illTyped.getLabel());
        assertEquals(XSDDatatype.INTEGER.getIRI(), illTyped.getDatatype());
        assertEquals(XSDDatatype.INTEGER, illTyped.getCoreDatatype());
        assertThrows(IncorrectOperationException.class, illTyped::integerValue);
    }

    @Test
    void arbitrarySizeIntegerIsNotTruncated() {
        BigInteger value = new BigInteger("1234567890123456789012345678901234567890");

        Literal literal = valueFactory.createLiteral(value);

        assertEquals(value.toString(), literal.getLabel());
        assertEquals(value, literal.integerValue());
        assertEquals(new BigDecimal(value), literal.decimalValue());
    }

    @Test
    void primitiveOverloadsUseTheirDocumentedXsdDatatypes() {
        assertEquals(XSDDatatype.BYTE, valueFactory.createLiteral((byte) 1).getCoreDatatype());
        assertEquals(XSDDatatype.SHORT, valueFactory.createLiteral((short) 1).getCoreDatatype());
        assertEquals(XSDDatatype.INT, valueFactory.createLiteral(1).getCoreDatatype());
        assertEquals(XSDDatatype.LONG, valueFactory.createLiteral(1L).getCoreDatatype());
        assertEquals(XSDDatatype.FLOAT, valueFactory.createLiteral(1.0f).getCoreDatatype());
        assertEquals(XSDDatatype.DOUBLE, valueFactory.createLiteral(1.0d).getCoreDatatype());
        assertEquals(XSDDatatype.DECIMAL,
                valueFactory.createLiteral(BigDecimal.ONE).getCoreDatatype());
        assertEquals(XSDDatatype.INTEGER,
                valueFactory.createLiteral(BigInteger.ONE).getCoreDatatype());
    }
}
