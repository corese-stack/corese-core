package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseModel;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

public abstract class ResultSerializerTest {

    private Model model = null;

    protected Model getModel() {
        if(model == null) {
            Model newModel = new CoreseModel();
            ValueFactory valueFactory = new CoreseAdaptedValueFactory();
            RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.TURTLE, newModel, valueFactory);
            StringReader reader = new StringReader(getDataTurtleString());
            parser.parse(reader);
            this.model = newModel;
        }
        return this.model;
    }

    protected String getEmptyResultQuery() {
        return """
                SELECT ?s {
                ?s a <http://example.com/nothing> .
                }
                """;
    }

    protected String getDataTurtleString() {
        return """
                @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
                
                _:a  foaf:name   "Johnny Lee Outlaw" .
                _:a  foaf:mbox   <mailto:jlow@example.com> .
                _:b  foaf:name   "Peter Goodguy" .
                _:b  foaf:mbox   <mailto:peter@example.org> .
                _:c  foaf:mbox   <mailto:carol@example.org> .
                """;
    }

    protected abstract ResultSerializer getResultSerializer();

    protected abstract String getEmptyResultsString();

    @Test
    @DisplayName("Tests the serialization of an empty result")
    void emptyResult() {
    }

    protected abstract String getResultsWithUris();

    @Test
    @DisplayName("Tests the serialization of results containing only URIs")
     void resultsWithUris();

    protected abstract String getResultsWithLiterals();

    @Test
    @DisplayName("Tests the serialization of results containing only literals")
    void resultsWithLiterals();

    protected abstract String getResultsWithBlankNodes();

    @Test
    @DisplayName("Tests the serialization of results with blank nodes")
    void resultsWithBlankNodes();

    protected abstract String getResultsWithMultipleLines();

    @Test
    @DisplayName("Tests the serialization of results of at least 2 lines")
    void resultsWithMultipleLines();

    protected abstract String getResultsWithMultiLinesLiteral();

    @Test
    @DisplayName("Tests the serialization of a result containing a literal that contains break lines")
    void resultsWithMultiLinesLiteral();
}
