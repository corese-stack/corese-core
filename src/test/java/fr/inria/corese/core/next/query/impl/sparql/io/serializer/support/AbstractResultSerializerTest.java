package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.ResultSerializerTestUtils.MockQueryResults;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractResultSerializerTest {

    private final ValueFactory factory = new CoreseValueFactory();

    protected abstract ResultSerializer getResultSerializer(TupleQueryResult results);
    protected abstract ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options);

    protected ValueFactory getFactory() {
        return this.factory;
    }

    protected abstract String getEmptyResultsString();

    protected TupleQueryResult getEmptyResults() {
        return new TupleQueryResult() {
            @Override
            public List<String> getBindingNames() {
                return List.of("a", "b", "c");
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public BindingSet next() {
                return null;
            }

            @Override
            public void close() {

            }
        };
    }

    @Test
    @DisplayName("Tests the serialization of an empty result")
    void emptyResultTest() {
        ResultSerializer serializer = getResultSerializer(getEmptyResults());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getEmptyResultsString(), writer.toString());
    }

    protected abstract String getResultsWithUrisString();

    protected TupleQueryResult getResultsWithUris() {
        List<String> bindingsNames = List.of("email", "homepage");
        Map<String, Value> resultWithUrisValuesRow1 = new HashMap<>();
        resultWithUrisValuesRow1.put("email", factory.createIRI("mailto:jlow@example.com"));
        resultWithUrisValuesRow1.put("homepage", factory.createIRI("https://bsky.app/profile/johnnyleeoutlaw"));
        Map<String, Value> resultWithUrisValuesRow2 = new HashMap<>();
        resultWithUrisValuesRow2.put("email", factory.createIRI("mailto:peter@example.org"));
        resultWithUrisValuesRow2.put("homepage", factory.createIRI("https://peter.goodguy.com"));
        return new MockQueryResults(bindingsNames, List.of(resultWithUrisValuesRow1, resultWithUrisValuesRow2));
    }

    @Test
    @DisplayName("Tests the serialization of results containing only URIs")
     void resultsWithUrisTest() {
        ResultSerializer serializer = getResultSerializer(getResultsWithUris());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithUrisString(), writer.toString());
    }

    protected abstract String getResultsWithLiteralsString();

    protected TupleQueryResult getResultsWithLiterals() {
        List<String> bindingsNames = List.of("email", "name");
        Map<String, Value> resultWithUrisValuesRow1 = new HashMap<>();
        resultWithUrisValuesRow1.put("name", factory.createLiteral("Johnny Lee Outlaw"));
        resultWithUrisValuesRow1.put("email", factory.createIRI("mailto:jlow@example.com"));
        Map<String, Value> resultWithUrisValuesRow2 = new HashMap<>();
        resultWithUrisValuesRow2.put("name", factory.createLiteral("Peter Goodguy"));
        resultWithUrisValuesRow2.put("email", factory.createIRI("mailto:peter@example.org"));
        Map<String, Value> resultWithUrisValuesRow3 = new HashMap<>();
        resultWithUrisValuesRow3.put("name", factory.createLiteral("Carol Patoune"));
        resultWithUrisValuesRow3.put("email", factory.createIRI("mailto:carol@example.org"));
        return new MockQueryResults(bindingsNames, List.of(resultWithUrisValuesRow1, resultWithUrisValuesRow2, resultWithUrisValuesRow3));
    }

    @Test
    @DisplayName("Tests the serialization of results containing only literals")
    void resultsWithLiteralsTest() {

        ResultSerializer serializer = getResultSerializer(getResultsWithLiterals());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithLiteralsString(), writer.toString());
    }

    protected abstract String getResultsWithBlankNodesString();

    protected TupleQueryResult getResultsWithBlankNodes() {
        List<String> bindingsNames = List.of("nb");
        Map<String, Value> resultWithUrisValuesRow1 = new HashMap<>();
        resultWithUrisValuesRow1.put("nb", factory.createBNode("a"));
        Map<String, Value> resultWithUrisValuesRow2 = new HashMap<>();
        resultWithUrisValuesRow2.put("nb", factory.createBNode("b"));
        Map<String, Value> resultWithUrisValuesRow3 = new HashMap<>();
        resultWithUrisValuesRow3.put("nb", factory.createBNode("c"));
        return new MockQueryResults(bindingsNames, List.of(resultWithUrisValuesRow1, resultWithUrisValuesRow2, resultWithUrisValuesRow3));
    }

    @Test
    @DisplayName("Tests the serialization of results with blank nodes")
    void resultsWithBlankNodesTest() {
        ResultSerializer serializer = getResultSerializer(getResultsWithBlankNodes());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultsWithBlankNodesString(), writer.toString());
    }

    protected abstract String getResultsWithMultiLinesLiteralString();

    protected TupleQueryResult getResultsWithMultiLinesLiteral() {
        List<String> bindingsNames = List.of("mail", "depiction");
        Map<String, Value> resultsWithMultiLinesLiteralValuesRow1 = new HashMap<>();
        resultsWithMultiLinesLiteralValuesRow1.put("mail", factory.createIRI("mailto:carol@example.org"));
        resultsWithMultiLinesLiteralValuesRow1.put("depiction", factory.createLiteral("""
All this work and no play makes Carols a dull girl,
All this work and no play makes Carols a dull girl,
All this work and no play makes Carols a dull girl,
All this work and no play makes Carols a dull girl."""));
        return new MockQueryResults(bindingsNames, List.of(resultsWithMultiLinesLiteralValuesRow1));
    }

    @Test
    @DisplayName("Tests the serialization of a result containing a literal that contains break lines")
    void resultsWithMultiLinesLiteralTest() {
        ResultSerializer serializer = getResultSerializer(getResultsWithMultiLinesLiteral());
        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expected = getResultsWithMultiLinesLiteralString().replace("\r\n", "\n");

        String actual = writer.toString().replace("\r\n", "\n");

        assertEquals(expected.trim(), actual.trim()
        );
    }

    protected abstract String getResultWithLiteralContainingQuotesString();

    protected TupleQueryResult getResultWithLiteralContainingQuotes() {
        List<String> bindingsNames = List.of("name", "desc");
        Map<String, Value> resultWithLiteralContainingQuotesValuesRow1 = new HashMap<>();
        resultWithLiteralContainingQuotesValuesRow1.put("name", factory.createLiteral("Alice"));
        resultWithLiteralContainingQuotesValuesRow1.put("desc", factory.createLiteral("Literal with a single quote'"));
        Map<String, Value> resultWithLiteralContainingQuotesValuesRow2 = new HashMap<>();
        resultWithLiteralContainingQuotesValuesRow2.put("name", factory.createLiteral("Bernard"));
        resultWithLiteralContainingQuotesValuesRow2.put("desc", factory.createLiteral("Literal with a quote\""));
        Map<String, Value> resultWithLiteralContainingQuotesValuesRow3 = new HashMap<>();
        resultWithLiteralContainingQuotesValuesRow3.put("name", factory.createLiteral("Charles"));
        resultWithLiteralContainingQuotesValuesRow3.put("desc", factory.createLiteral("Literal both quotes single ' and double \""));
        return new MockQueryResults(bindingsNames, List.of(resultWithLiteralContainingQuotesValuesRow1, resultWithLiteralContainingQuotesValuesRow2, resultWithLiteralContainingQuotesValuesRow3));

    }

    @Test
    @DisplayName("Tests the serialization of the result literals containing either of both types of quotes")
    void resultWithLiteralContainingQuotesTest() {
        ResultSerializer serializer = getResultSerializer(getResultWithLiteralContainingQuotes());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getResultWithLiteralContainingQuotesString(), writer.toString());
    }

    protected abstract String getSVStandardResultsString();

    protected TupleQueryResult getSVStandardResults() {
        List<String> bindingsNames = List.of("x", "literal");
        Map<String, Value> benchmarkResultsValuesRow1 = new HashMap<>();
        benchmarkResultsValuesRow1.put("x", factory.createIRI("http://example/x"));
        benchmarkResultsValuesRow1.put("literal", factory.createLiteral("String"));
        Map<String, Value> benchmarkResultsValuesRow2 = new HashMap<>();
        benchmarkResultsValuesRow2.put("x", factory.createIRI("http://example/x"));
        benchmarkResultsValuesRow2.put("literal", factory.createLiteral("String-with-dquote\""));
        Map<String, Value> benchmarkResultsValuesRow3 = new HashMap<>();
        benchmarkResultsValuesRow3.put("x", factory.createBNode("blank0"));
        benchmarkResultsValuesRow3.put("literal", factory.createLiteral("Blank node"));
        Map<String, Value> benchmarkResultsValuesRow4 = new HashMap<>();
        benchmarkResultsValuesRow4.put("literal", factory.createLiteral("Missing 'x'"));
        Map<String, Value> benchmarkResultsValuesRow5 = new HashMap<>();
        Map<String, Value> benchmarkResultsValuesRow6 = new HashMap<>();
        benchmarkResultsValuesRow6.put("x", factory.createIRI("http://example/x"));
        Map<String, Value> benchmarkResultsValuesRow7 = new HashMap<>();
        benchmarkResultsValuesRow7.put("x", factory.createBNode("blank1"));
        benchmarkResultsValuesRow7.put("literal", factory.createLiteral("String-with-lang", "en"));
        Map<String, Value> benchmarkResultsValuesRow8 = new HashMap<>();
        benchmarkResultsValuesRow8.put("x", factory.createBNode("blank1"));
        benchmarkResultsValuesRow8.put("literal", factory.createLiteral(123));
        return new MockQueryResults(bindingsNames, List.of(benchmarkResultsValuesRow1, benchmarkResultsValuesRow2, benchmarkResultsValuesRow3, benchmarkResultsValuesRow4, benchmarkResultsValuesRow5, benchmarkResultsValuesRow6, benchmarkResultsValuesRow7, benchmarkResultsValuesRow8));
    }

    @Test
    @DisplayName("Tests the serialization of the result given as example in the character separated values standard")
    void svStandardResults() {
        ResultSerializer serializer = getResultSerializer(getSVStandardResults());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getSVStandardResultsString(), writer.toString());
    }
}
