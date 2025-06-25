package fr.inria.corese.core.next.impl.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ANTLRTurtleParserSpec {
    private Model parseFromString(String turtleData, String baseURI) throws Exception {
        Model model = new CoreseModel();
        RDFParser parser = new ANTLRTurtleParser(model);
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
