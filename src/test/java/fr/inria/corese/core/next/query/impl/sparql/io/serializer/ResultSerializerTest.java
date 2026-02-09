package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseModel;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.result.CoreseTupleQueryResult;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    protected TupleQueryResult executeTupleQuery(String queryString) throws EngineException {
        if(this.getModel() instanceof CoreseModel coreseModel) {
            QueryProcess qProcess = QueryProcess.create(coreseModel.getCoreseGraph());
            Mappings results = qProcess.query(queryString);
            return new CoreseTupleQueryResult(results);
        }
        return null;
    }

    protected String getDataTurtleString() {
        return """
                @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
                
                _:a  foaf:name   "Johnny Lee Outlaw" .
                _:a  foaf:mbox   <mailto:jlow@example.com> .
                _:a  foaf:homepage <https://bsky.app/profile/johnnyleeoutlaw> .
                _:b  foaf:name   "Peter Goodguy" .
                _:b  foaf:mbox   <mailto:peter@example.org> .
                _:c  foaf:mbox   <mailto:carol@example.org> .
                _:c  foaf:depiction   '''All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl.
                ''' .
                """;
    }

    protected abstract ResultSerializer getResultSerializer(TupleQueryResult results);

    protected abstract String getEmptyResultsString();

    @Test
    @DisplayName("Tests the serialization of an empty result")
    void emptyResult() throws EngineException {
        String emptyResultQuery =  """
                SELECT ?s {
                ?s a <http://example.com/nothing> .
                }
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(emptyResultQuery));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getEmptyResultsString(), writer.toString());
    }

    protected abstract String getResultsWithUris();

    @Test
    @DisplayName("Tests the serialization of results containing only URIs")
     void resultsWithUris() throws EngineException {
        String resultWithUrisQuery =  """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?mbox ?homepage {
                    ?s foaf:mbox ?mbox ;
                        foaf:homepage ?homepage .
                }
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(resultWithUrisQuery));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithUris(), writer.toString());
    }

    protected abstract String getResultsWithLiterals();

    @Test
    @DisplayName("Tests the serialization of results containing only literals")
    void resultsWithLiterals() throws EngineException {
        String resultWithLiterals =  """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?name {
                    ?s foaf:name ?name .
                } ORDER BY DESC(?name)
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(resultWithLiterals));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithLiterals(), writer.toString());
    }

    protected abstract String getResultsWithBlankNodes();

    @Test
    @DisplayName("Tests the serialization of results with blank nodes")
    void resultsWithBlankNodes() throws EngineException {
        String resultWithLiterals = """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?nb {
                    ?nb foaf:mbox ?mail .
                } ORDER BY DESC(?nb)
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(resultWithLiterals));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithBlankNodes(), writer.toString());
    }

    protected abstract String getResultsWithMultipleLines();

    @Test
    @DisplayName("Tests the serialization of results of at least 2 lines")
    void resultsWithMultipleLines() throws EngineException {
        String resultWithMultipleLines = """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?name ?mbox {
                    ?s foaf:name ?name ;
                        foaf:mbox ?mbox .
                } ORDER BY DESC(?name)
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(resultWithMultipleLines));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithMultipleLines(), writer.toString());
    }

    protected abstract String getResultsWithMultiLinesLiteral();

    @Test
    @DisplayName("Tests the serialization of a result containing a literal that contains break lines")
    void resultsWithMultiLinesLiteral() throws EngineException {
        String resultWithMultiLinesLiteral = """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?depiction {
                    ?s foaf:mbox <mailto:carol@example.org> ;
                        foaf:depiction ?depiction .
                }
                """;

        ResultSerializer serializer = getResultSerializer(executeTupleQuery(resultWithMultiLinesLiteral));
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithMultiLinesLiteral(), writer.toString());
    }
}
