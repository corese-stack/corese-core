package fr.inria.corese.core.next.impl.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.impl.io.parser.nquads.ANTLRNQuadsParser;
import fr.inria.corese.core.next.impl.io.parser.ntriples.ANTLRNTriplesParser;
import fr.inria.corese.core.next.impl.io.parser.turtle.ANTLRTurtleParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ParserFactory class.
 * This class verifies that the factory correctly instantiates the appropriate
 * RDFParser implementation based on the provided RdfFormat.
 */
@ExtendWith(MockitoExtension.class)
class ParserFactoryTest {

    private ParserFactory parserFactory;

    @Mock
    private Model mockModel;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private RDFParserOptions mockParserOptions;


    @BeforeEach
    void setUp() {
        parserFactory = new ParserFactory();
    }

    @Test
    @DisplayName("createRDFParser (with config) should return JSONLDParser for JSONLD format")
    void testCreateRDFParserWithConfig_JSONLD() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.JSONLD, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertTrue(parser instanceof JSONLDParser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRTurtleParser for TURTLE format")
    void testCreateRDFParserWithConfig_TURTLE() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.TURTLE, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRTurtleParser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRNTriplesParser for N-TRIPLES format")
    void testCreateRDFParserWithConfig_NTRIPLES() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NTRIPLES, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRNTriplesParser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRNQuadsParser for N-QUADS format")
    void testCreateRDFParserWithConfig_NQUADS() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NQUADS, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRNQuadsParser);
    }


    @Test
    @DisplayName("createRDFParser (without config) should return JSONLDParser for JSONLD format")
    void testCreateRDFParserWithoutConfig_JSONLD() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.JSONLD, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertTrue(parser instanceof JSONLDParser);
    }

    @Test
    @DisplayName("createRDFParser (without config) should return ANTLRTurtleParser for TURTLE format")
    void testCreateRDFParserWithoutConfig_TURTLE() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.TURTLE, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRTurtleParser);
    }

    @Test
    @DisplayName("createRDFParser (without config) should return ANTLRNTriplesParser for N-TRIPLES format")
    void testCreateRDFParserWithoutConfig_NTRIPLES() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NTRIPLES, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRNTriplesParser);
    }

    @Test
    @DisplayName("createRDFParser (without config) should return ANTLRNQuadsParser for N-QUADS format")
    void testCreateRDFParserWithoutConfig_NQUADS() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NQUADS, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertTrue(parser instanceof ANTLRNQuadsParser);
    }

}
