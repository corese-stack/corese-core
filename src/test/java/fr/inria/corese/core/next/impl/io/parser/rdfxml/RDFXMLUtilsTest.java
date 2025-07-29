package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RDFXMLUtilsTest {

    private final ValueFactory factory = new CoreseAdaptedValueFactory();

    @Test
    public void testExpandQName() {
        assertEquals("http://example.org/test", RDFXMLUtils.expandQName("http://example.org/", "test", "ex:test"));
        assertEquals("ex:test", RDFXMLUtils.expandQName(null, null, "ex:test"));
    }

    @Test
    public void testResolveDatatype() {
        assertEquals(Optional.of(XSD.STRING), RDFXMLUtils.resolveDatatype(XSD.STRING.getIRI().stringValue()));
        assertTrue(RDFXMLUtils.resolveDatatype("http://nonexistentdatatype").isEmpty());
    }

    @Test
    public void testExtractSubjectWithAbout() {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(RDF.type.getNamespace(), "about", "", "CDATA", "http://example.org/subject");
        Resource subject = RDFXMLUtils.extractSubject(attrs, factory, null);
        assertEquals("http://example.org/subject", subject.stringValue());
    }

    @Test
    public void testExtractSubjectWithNodeID() {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(RDF.type.getNamespace(), "nodeID", "", "CDATA", "b123");
        Resource subject = RDFXMLUtils.extractSubject(attrs, factory, null);
        assertTrue(subject.stringValue().contains("_:b123"));
    }

    @Test
    public void testExtractSubjectWithID() {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(RDF.type.getNamespace(), "ID", "", "CDATA", "id123");
        Resource subject = RDFXMLUtils.extractSubject(attrs, factory, "http://example.org/");
        assertEquals("http://example.org/id123", subject.stringValue());
    }

    @Test
    public void testResolveAgainstBase() {
        assertEquals("http://base.org/path", RDFXMLUtils.resolveAgainstBase("path", "http://base.org/"));
    }

    @Test
    public void testIsSyntaxAttribute() {
        assertTrue(RDFXMLUtils.isSyntaxAttribute(RDF.type.getNamespace(), "about", "rdf:about"));
        assertTrue(RDFXMLUtils.isSyntaxAttribute(null, "lang", "xml:lang"));
        assertFalse(RDFXMLUtils.isSyntaxAttribute("http://example.org/", "type", "ex:type"));
    }

    @Test
    public void testIsContainer() {
        assertTrue(RDFXMLUtils.isContainer("Bag", RDF.type.getNamespace()));
        assertFalse(RDFXMLUtils.isContainer("notAContainer", "http://example.org/"));
    }

    @Test
    public void testCreateRdfCollection() {
        Model model = new CoreseModel();
        Resource r1 = factory.createIRI("http://example.org/A");
        Resource r2 = factory.createIRI("http://example.org/B");
        Resource head = RDFXMLUtils.createRdfCollection(List.of(r1, r2), model, factory);

        assertNotNull(head);
        assertTrue(model.size() > 0);
        assertTrue(model.contains(null, RDF.first.getIRI(), r1));
        assertTrue(model.contains(null, RDF.rest.getIRI(), RDF.nil.getIRI()));
    }



}
