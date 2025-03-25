package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AbstractStringTest {

    public static class LiteralString extends AbstractString {

        public LiteralString(IRI datatype, String value) {
            super(datatype, value);
        }

        @Override
        public void setCoreDatatype(CoreDatatype coreDatatype) {}
    }

    private AbstractString literal;

    @Before
    public void setUp() {

        IRI iri = new BasicIRI("http://www.w3.org/2001/XMLSchema#string");
        literal = new LiteralString(iri, "Corese");
    }

    @Test
    public void testGetLabel() {
        assertEquals("Corese", literal.getLabel());
    }

    @Test
    public void testGetDatatype() {
        assertNotNull(literal.getDatatype());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", literal.getDatatype().stringValue());
    }

    @Test
    public void testGetCoreDatatype() {
        assertEquals(CoreDatatype.XSD.STRING, literal.getCoreDatatype());
    }

    @Test
    public void testStringValue() {
        assertEquals("Corese", literal.stringValue());
    }

    @Test
    public void testToString() {
        assertEquals("Corese^^<http://www.w3.org/2001/XMLSchema#string>", literal.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        AbstractString anotherLiteral = new LiteralString(XSD.xsdString.getIRI(), "Corese");
        assertEquals(literal, anotherLiteral);
        assertEquals(literal.hashCode(), anotherLiteral.hashCode());
    }
}