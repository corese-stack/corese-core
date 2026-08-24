package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreDatatypesTest {

    @Test
    void testGetDatatypeFromIRI_ValidXsdDatatype() {
        // Test valid XSD datatype
        IRI xsdStringIri = new SimpleIRI("http://www.w3.org/2001/XMLSchema#string");
        CoreDatatype datatype = CoreDatatypes.from(xsdStringIri);
        assertEquals(XSDDatatype.STRING, datatype);
    }

    @Test
    void testGetDatatypeFromIRI_ValidRdfDatatype() {
        // Test valid RDF datatype (langString)
        IRI langStringIri = new SimpleIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
        CoreDatatype datatype = CoreDatatypes.from(langStringIri);
        assertEquals(RDFDatatype.LANGSTRING, datatype);
    }

    @Test
    void testGetDatatypeFromIRI_InvalidDatatype() {
        // Test unknown datatype
        IRI invalidIri = new SimpleIRI("http://example.com/unknownDatatype");
        CoreDatatype datatype = CoreDatatypes.from(invalidIri);

        assertEquals(CoreDatatype.NONE, datatype);
    }

    @Test
    void testGetDatatypeFromIRI_NullDatatype() {
        // Test passing a null IRI
        CoreDatatype datatype = CoreDatatypes.from(null);

        assertEquals(CoreDatatype.NONE, datatype);
    }
}
