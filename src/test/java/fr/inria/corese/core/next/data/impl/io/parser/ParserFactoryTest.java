package fr.inria.corese.core.next.data.impl.io.parser;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.io.IOOptions;
import fr.inria.corese.core.next.data.io.parser.RDFParser;
import fr.inria.corese.core.next.data.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.data.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.data.impl.io.parser.nquads.NQuadsParser;
import fr.inria.corese.core.next.data.impl.io.parser.ntriples.NTriplesParser;
import fr.inria.corese.core.next.data.impl.io.parser.turtle.TurtleParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    private IOOptions mockParserOptions;


    @BeforeEach
    void setUp() {
        parserFactory = new ParserFactory();
    }

    @Test
    @DisplayName("createRDFParser (with config) should return JSONLDParser for JSONLD format")
    void testCreateRDFParserWithConfig_JSONLD() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.JSONLD, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertInstanceOf(JSONLDParser.class, parser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRTurtleParser for TURTLE format")
    void testCreateRDFParserWithConfig_TURTLE() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.TURTLE, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertInstanceOf(TurtleParser.class, parser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRNTriplesParser for N-TRIPLES format")
    void testCreateRDFParserWithConfig_NTRIPLES() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NTRIPLES, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertInstanceOf(NTriplesParser.class, parser);
    }

    @Test
    @DisplayName("createRDFParser (with config) should return ANTLRNQuadsParser for N-QUADS format")
    void testCreateRDFParserWithConfig_NQUADS() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NQUADS, mockModel, mockValueFactory, mockParserOptions);
        assertNotNull(parser);
        assertInstanceOf(NQuadsParser.class, parser);
    }


    @Test
    @DisplayName("createRDFParser (without config) should return JSONLDParser for JSONLD format")
    void testCreateRDFParserWithoutConfig_JSONLD() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.JSONLD, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertInstanceOf(JSONLDParser.class, parser);
    }

    @Test
    @DisplayName("createRDFParser (without config) should return ANTLRTurtleParser for TURTLE format")
    void testCreateRDFParserWithoutConfig_TURTLE() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.TURTLE, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertInstanceOf(TurtleParser.class, parser);
    }

    @Test
    @DisplayName("createRDFParser (without config) should return ANTLRNTriplesParser for N-TRIPLES format")
    void testCreateRDFParserWithoutConfig_NTRIPLES() {
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.NTRIPLES, mockModel, mockValueFactory);
        assertNotNull(parser);
        assertInstanceOf(NTriplesParser.class, parser);
    }


}
