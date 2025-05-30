package fr.inria.corese.core.next.api.model.serialization;

import fr.inria.corese.core.next.api.base.model.serialization.Rdf4jSerializationUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the {@link Rdf4jSerializationUtil} class.
 * This class verifies the correct serialization of RDF4J Models
 * to various RDF formats (Turtle, JSON-LD, RDF/XML, N-Triples) and tests
 * the utility methods for format resolution and error handling.
 */
class Rdf4jSerializationUtilTest {

    private static final Logger logger = LoggerFactory.getLogger(Rdf4jSerializationUtilTest.class);


    /**
     * Tests the serialization of an RDF4J Model to Turtle format.
     * Verifies that the output can be parsed back into an equivalent model.
     *
     * @throws IOException If an I/O error occurs during serialization.
     * @throws RDFParseException If the serialized output cannot be parsed.
     */
    @Test
    void testSerializeToTurtle() throws IOException, RDFParseException {
        Model originalModel = createTestModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rdf4jSerializationUtil.serializeToTurtle(originalModel, out);

        String result = out.toString();
        logger.debug("testSerializeToTurtle Output:\n{}" , result);


        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
        Model reparsedModel = Rio.parse(in, "", RDFFormat.TURTLE);
        assertEquals(originalModel, reparsedModel, "The re-parsed Turtle model should be equal to the original model.");
    }

    /**
     * Tests the serialization of an RDF4J Model to JSON-LD format.
     * Verifies that the output can be parsed back into an equivalent model.
     *
     * @throws IOException If an I/O error occurs during serialization.
     * @throws RDFParseException If the serialized output cannot be parsed.
     */
    @Test
    void testSerializeToJsonLd() throws IOException, RDFParseException {
        Model originalModel = createTestModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rdf4jSerializationUtil.serializeToJsonLd(originalModel, out);

        String result = out.toString();
        logger.debug("testSerializeToJsonLd Output:\n{}" , result);

        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
        Model reparsedModel = Rio.parse(in, "", RDFFormat.JSONLD);
        assertEquals(originalModel, reparsedModel, "The re-parsed JSON-LD model should be equal to the original model.");
    }

    /**
     * Tests the serialization of an RDF4J Model to RDF/XML format.
     * Verifies that the output can be parsed back into an equivalent model.
     *
     * @throws IOException If an I/O error occurs during serialization.
     * @throws RDFParseException If the serialized output cannot be parsed.
     */
    @Test
    void testSerializeToRdfXml() throws IOException, RDFParseException {
        Model originalModel = createTestModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rdf4jSerializationUtil.serializeToRdfXml(originalModel, out);

        String result = out.toString();
        logger.debug("testSerializeToRdfXml Output:\n{}" , result);

        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
        Model reparsedModel = Rio.parse(in, "", RDFFormat.RDFXML);
        assertEquals(originalModel, reparsedModel, "The re-parsed RDF/XML model should be equal to the original model.");
    }



    /**
     * Tests the generic serialization method using a format string, specifically for Turtle.
     * Verifies that the output can be parsed back into an equivalent model.
     *
     * @throws IOException If an I/O error occurs during serialization.
     * @throws RDFParseException If the serialized output cannot be parsed.
     */
    @Test
    void testSerializeWithFormatString() throws IOException, RDFParseException {
        Model originalModel = createTestModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Rdf4jSerializationUtil.serialize(originalModel, out, "turtle");

        String result = out.toString();
        logger.debug("testSerializeWithFormatString (Turtle) Output:\n{}" , result);

        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
        Model reparsedModel = Rio.parse(in, "", RDFFormat.TURTLE);
        assertEquals(originalModel, reparsedModel, "The re-parsed Turtle model (from string method) should be equal to the original model.");
    }

    /**
     * Tests the {@link Rdf4jSerializationUtil#getRdfFormat(String)} method for supported formats.
     * Verifies that the correct {@link RDFFormat} enum is returned for valid input strings.
     */
    @Test
    void testGetRdfFormat() {
        assertEquals(RDFFormat.TURTLE, Rdf4jSerializationUtil.getRdfFormat("turtle"));
        assertEquals(RDFFormat.TURTLE, Rdf4jSerializationUtil.getRdfFormat("TTL"));
        assertEquals(RDFFormat.JSONLD, Rdf4jSerializationUtil.getRdfFormat("jsonld"));
        assertEquals(RDFFormat.RDFXML, Rdf4jSerializationUtil.getRdfFormat("rdfxml"));
    }

    /**
     * Tests the {@link Rdf4jSerializationUtil#getRdfFormat(String)} method's error handling
     * for unsupported format strings.
     * Verifies that an {@link IllegalArgumentException} is thrown as expected.
     */
    @Test
    void testGetRdfFormatThrowsForUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> Rdf4jSerializationUtil.getRdfFormat("unsupported"));
    }

    /**
     * Tests the null checks within the serialization methods.
     * Verifies that {@link NullPointerException}s are thrown when null arguments are provided
     * for model, output stream, or format string.
     */
    @Test
    void testNullChecks() {
        Model model = createTestModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();


        assertThrows(NullPointerException.class,
                () -> Rdf4jSerializationUtil.serialize(null, out, "turtle"));


        assertThrows(NullPointerException.class,
                () -> Rdf4jSerializationUtil.serialize(model, null, "turtle"));

        assertThrows(NullPointerException.class,
                () -> Rdf4jSerializationUtil.serialize(model, out, null));
    }

    /**
     * Tests the {@link Rdf4jSerializationUtil#getSupportedFormats()} method.
     * Verifies that the returned set contains expected format identifiers and has the correct size.
     */
    @Test
    void testGetSupportedFormats() {
        var formats = Rdf4jSerializationUtil.getSupportedFormats();
        assertTrue(formats.contains("turtle"));
        assertTrue(formats.contains("jsonld"));
        assertTrue(formats.contains("rdfxml"));
        assertEquals(6, formats.size());
    }

    /**
     * Creates a simple RDF4J Model for testing purposes.
     * The model contains a single triple: ex:test rdf:type foaf:Person.
     *
     * @return A {@link Model} instance populated with test data.
     */
    private Model createTestModel() {

        ValueFactory vf = SimpleValueFactory.getInstance();
        Model model = new LinkedHashModel();
        model.add(
                vf.createIRI("http://example.org/test"),
                RDF.TYPE,
                FOAF.PERSON
        );
        return model;
    }
}
