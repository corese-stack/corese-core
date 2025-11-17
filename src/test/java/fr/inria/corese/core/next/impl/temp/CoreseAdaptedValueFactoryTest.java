package fr.inria.corese.core.next.impl.temp;

import java.time.Duration;

import fr.inria.corese.core.next.impl.temp.literal.CoreseDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.model.ValueFactoryTest;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.temp.literal.CoreseLanguageTaggedStringLiteral;
import fr.inria.corese.core.next.impl.temp.literal.CoreseTyped;

import static org.junit.jupiter.api.Assertions.*;

public class CoreseAdaptedValueFactoryTest extends ValueFactoryTest {

    private String stringTestValue;
    private IRI xsdStringIRI;

    private Resource subject;
    private IRI predicate;
    private Resource context;

    @BeforeEach
    @Override
    public void setUp() {
        this.valueFactory = new CoreseAdaptedValueFactory();
        stringTestValue = "String value";
        xsdStringIRI = XSD.STRING.getIRI();
        subject = new CoreseIRI("http://corese.com/subject");
        predicate = new CoreseIRI("http://corese.com/predicate");
    }

    @Test
    @Override
    public void testCreateLiteralTemporalAmount() {
        Duration duration = Duration.ofHours(23);
        this.valueFactory.createLiteral(duration);

        assertNotNull(this.valueFactory.createLiteral(duration));
    }

    @Test
    public void testCreateLiteralWithLabel() {
        // Test createLiteral with label
        Literal literal = valueFactory.createLiteral(stringTestValue);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSD.STRING, literal.getCoreDatatype());
    }

    @Test
    public void testCreateLiteralWithLabelAndLanguage() {
        String testLanguage = "en";

        // Test createLiteral with label and language
        Literal literal = valueFactory.createLiteral(stringTestValue, testLanguage);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseLanguageTaggedStringLiteral);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(testLanguage, literal.getLanguage().orElse(null));
        assertEquals(RDF.LANGSTRING, literal.getCoreDatatype());
    }

    @Test
    public void testCreateLiteralWithDatatypeIRI() {
        // Test createLiteral with IRI datatype (XSD.STRING)
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSD.STRING, literal.getCoreDatatype());
    }

    @Test
    public void testCreateLiteralWithCoreDatatype() {
        // Test createLiteral with CoreDatatype (XSD.STRING)
        Literal literal = valueFactory.createLiteral(stringTestValue, XSD.STRING);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSD.STRING, literal.getCoreDatatype());
    }

    @Test
    public void testCreateLiteralWithDatatypeIRIAndCoreDatatype() {
        // Test createLiteral with IRI datatype and CoreDatatype (XSD.STRING)
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI, XSD.STRING);

        assertNotNull(literal);
        assertTrue(literal instanceof CoreseTyped);
        assertEquals(stringTestValue, literal.getLabel());
        assertEquals(XSD.STRING, literal.getCoreDatatype());
    }

    @Test
    public void testCreateStatementWithoutContext() {
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI, XSD.STRING);
        CoreseStatement statement = (CoreseStatement) valueFactory.createStatement(subject, predicate, literal);
        assertNotNull(statement);
        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(literal, statement.getObject());
        assertNull(statement.getContext());
    }

    @Test
    public void testCreateStatementWithContext() {
        Literal literal = valueFactory.createLiteral(stringTestValue, xsdStringIRI, XSD.STRING);

        CoreseStatement statement = (CoreseStatement) valueFactory.createStatement(subject, predicate, literal, context);

        assertNotNull(statement);
        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(literal, statement.getObject());
        assertEquals(context, statement.getContext());
    }

    @Test
    public void testCreateFOAFURI() {
        IRI foaf = valueFactory.createIRI("http://xmlns.com/foaf/0.1/");
        assertNotNull(foaf);
        assertEquals("http://xmlns.com/foaf/0.1/", foaf.stringValue());
    }

    @Test
    public void testDateCreation() {
        IRI xsdDate = valueFactory.createIRI("http://www.w3.org/2001/XMLSchema#date");
        String literalStringValue = "2025-11-20";
        Literal date = valueFactory.createLiteral(literalStringValue, xsdDate);

        assertNotNull(date);
        assertEquals(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDate.getIRI().stringValue(), date.getDatatype().stringValue());
        assertEquals(literalStringValue, date.getLabel());
        assertInstanceOf(fr.inria.corese.core.sparql.datatype.CoreseDate.class, ((CoreseNodeAdapter) date).getCoreseNode());
    }
}
