package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ANTLRTrigParserSpec {

    private Model parseFromString(String trigData, String baseURI) throws Exception {
        Model model = new CoreseModel();
        ValueFactory factory = new CoreseAdaptedValueFactory();
        RDFParser parser = new ANTLRTrigParser(model, factory);
        parser.parse(new StringReader(trigData), baseURI);
        return model;
    }

    @Test
    public void testNamedGraphParsing() throws Exception {
        String trig = "@prefix ex: <http://example.org/> .\n" +
                "ex:Graph1 {\n" +
                "  ex:Alice ex:knows ex:Bob .\n" +
                "}";

        Model model = parseFromString(trig, null);

        assertEquals(1, model.size());

        assertEquals(1, model.getNamespaces().size());

        assertEquals(1, model.contexts().size());
    }

    @Test
    public void testDocumentThatContainsOneGraphExample1() throws Exception {
        String trig = """
                # This document encodes one graph.
                @prefix ex: <http://www.example.org/vocabulary#> .
                @prefix : <http://www.example.org/exampleDocument#> .
                
                :G1 { :Monica a ex:Person ;
                              ex:name "Monica Murphy" ;
                              ex:homepage <http://www.monicamurphy.org> ;
                              ex:email <mailto:monica@monicamurphy.org> ;
                              ex:hasSkill ex:Management ,
                                          ex:Programming . }
                """.trim();

        Model model = parseFromString(trig, null);

        assertEquals(6, model.size());

        assertEquals(2, model.getNamespaces().size());

        assertEquals(1, model.contexts().size());
    }

}
