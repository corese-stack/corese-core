package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.util.literal.CoreDatatypeHelper;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.common.BasicIRI;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreDatatypeHelperTest {

    @Test
    public void testGetDatatypeFromIRI_ValidXsdDatatype() {
        // Test valid XSD datatype
        IRI xsdStringIri = new BasicIRI("http://www.w3.org/2001/XMLSchema#string");
        CoreDatatype datatype = CoreDatatypeHelper.from(xsdStringIri);
        assertEquals(XSD.STRING, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_ValidRdfDatatype() {
        // Test valid RDF datatype (langString)
        IRI langStringIri = new BasicIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
        CoreDatatype datatype = CoreDatatypeHelper.from(langStringIri);
        assertEquals(RDF.LANGSTRING, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_InvalidDatatype() {
        // Test unknown datatype
        IRI invalidIri = new BasicIRI("http://example.com/unknownDatatype");
        CoreDatatype datatype = CoreDatatypeHelper.from(invalidIri);

        assertEquals(CoreDatatype.NONE, datatype);
    }

    @Test
    public void testGetDatatypeFromIRI_NullDatatype() {
        // Test passing a null IRI
        CoreDatatype datatype = CoreDatatypeHelper.from(null);

        assertEquals(CoreDatatype.NONE, datatype);
    }
}
