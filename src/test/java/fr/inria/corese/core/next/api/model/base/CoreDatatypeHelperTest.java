package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreDatatypeHelperTest {

    @Test
    public void testDatatypeMapNotEmpty() {
        // Check if the DATATYPE_MAP is not empty after initialization
        assertFalse( "The datatype map should not be empty.", CoreDatatypeHelper.getDatatypeMap().isEmpty());
    }

    @Test
    public void testGetDatatypeFromIRI_ValidXsdDatatype() {
        // Test valid XSD datatype
        IRI xsdStringIri = new BasicIRI("http://www.w3.org/2001/XMLSchema#string");
        CoreDatatype datatype = CoreDatatype.from(xsdStringIri);
        assertEquals(CoreDatatype.XSD.STRING, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_ValidRdfDatatype() {
        // Test valid RDF datatype (langString)
        IRI langStringIri = new BasicIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
        CoreDatatype datatype = CoreDatatype.from(langStringIri);
        assertEquals(CoreDatatype.RDF.LANGSTRING, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_InvalidDatatype() {
        // Test unknown datatype
        IRI invalidIri = new BasicIRI("http://example.com/unknownDatatype");
        CoreDatatype datatype = CoreDatatype.from(invalidIri);

        assertEquals(CoreDatatype.NONE, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_NullDatatype() {
        // Test passing a null IRI
        CoreDatatype datatype = CoreDatatypeHelper.getDatatypeFromIRI(null);

        assertEquals(CoreDatatype.NONE, datatype);
    }
}
