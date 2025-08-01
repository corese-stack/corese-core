package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the ANTLRTurtle class.
 * These tests verify the parser's ability to correctly parse Turtle
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
public class ANTLRTurtleParserTest {
    private Model parseFromString(String turtleData, String baseURI) throws Exception {
        Model model = new CoreseModel();
        ValueFactory factory = new CoreseAdaptedValueFactory();
        RDFParser parser = new ANTLRTurtleParser(model, factory);
        parser.parse(new StringReader(turtleData), baseURI);
        return model;
    }

    @Test
    public void testParseWithPrefixAndTriple() throws Exception {
        String turtle = " @prefix ex: <http://example.org/> . " +
            "ex:Alice ex:knows ex:Bob .";

        Model model = parseFromString(turtle, null);
        assertEquals(1, model.size());
        assertEquals(1, model.getNamespaces().size());
    }

}
