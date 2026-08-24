package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractResultSerializerTest;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.LinksSerializerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.ResultSerializerTestUtils.MockQueryResults;

class JsonTupleResultSerializerTest extends AbstractResultSerializerTest implements LinksSerializerTest {
    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        return new JsonTupleResultSerializer(results);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new JsonTupleResultSerializer(results, options);
    }

    @Override
    protected String getEmptyResultsString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"a\"," +
                    "\"b\"," +
                    "\"c\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[]" +
            "}" +
        "}";
    }

    @Override
    protected String getResultsWithUrisString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"email\"," +
                    "\"homepage\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"email\":{\"type\":\"uri\",\"value\":\"mailto:jlow@example.com\"}," +
                        "\"homepage\":{\"type\":\"uri\",\"value\":\"https://bsky.app/profile/johnnyleeoutlaw\"}" +
                    "}," +
                    "{" +
                        "\"email\":{\"type\":\"uri\",\"value\":\"mailto:peter@example.org\"}," +
                        "\"homepage\":{\"type\":\"uri\",\"value\":\"https://peter.goodguy.com\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"email\"," +
                    "\"name\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"email\":{\"type\":\"uri\",\"value\":\"mailto:jlow@example.com\"}," +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Johnny Lee Outlaw\"}" +
                    "}," +
                    "{" +
                        "\"email\":{\"type\":\"uri\",\"value\":\"mailto:peter@example.org\"}," +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Peter Goodguy\"}" +
                    "}," +
                    "{" +
                        "\"email\":{\"type\":\"uri\",\"value\":\"mailto:carol@example.org\"}," +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Carol Patoune\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"nb\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"nb\":{\"type\":\"bnode\",\"value\":\"a\"}" +
                    "}," +
                    "{" +
                        "\"nb\":{\"type\":\"bnode\",\"value\":\"b\"}" +
                    "}," +
                    "{" +
                        "\"nb\":{\"type\":\"bnode\",\"value\":\"c\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"mail\"," +
                    "\"depiction\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"mail\":{\"type\":\"uri\",\"value\":\"mailto:carol@example.org\"}," +
                        "\"depiction\":{\"type\":\"literal\",\"value\":\"All this work and no play makes Carols a dull girl,\\nAll this work and no play makes Carols a dull girl,\\nAll this work and no play makes Carols a dull girl,\\nAll this work and no play makes Carols a dull girl.\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"name\"," +
                    "\"desc\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Alice\"}," +
                        "\"desc\":{\"type\":\"literal\",\"value\":\"Literal with a single quote'\"}" +
                    "}," +
                    "{" +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Bernard\"}," +
                        "\"desc\":{\"type\":\"literal\",\"value\":\"Literal with a quote\\\"\"}" +
                    "}," +
                    "{" +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Charles\"}," +
                        "\"desc\":{\"type\":\"literal\",\"value\":\"Literal both quotes single ' and double \\\"\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Override
    protected String getSVStandardResultsString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"x\"," +
                    "\"literal\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"x\":{\"type\":\"uri\",\"value\":\"http://example/x\"}," +
                        "\"literal\":{\"type\":\"literal\",\"value\":\"String\"}" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"uri\",\"value\":\"http://example/x\"}," +
                        "\"literal\":{\"type\":\"literal\",\"value\":\"String-with-dquote\\\"\"}" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"bnode\",\"value\":\"blank0\"}," +
                        "\"literal\":{\"type\":\"literal\",\"value\":\"Blank node\"}" +
                    "}," +
                    "{" +
                        "\"literal\":{\"type\":\"literal\",\"value\":\"Missing 'x'\"}" +
                    "}," +
                    "{" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"uri\",\"value\":\"http://example/x\"}" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"bnode\",\"value\":\"blank1\"}," +
                        "\"literal\":{\"type\":\"literal\",\"value\":\"String-with-lang\",\"xml:lang\":\"en\"}" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"bnode\",\"value\":\"blank1\"}," +
                        "\"literal\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"literal\",\"value\":\"123\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    protected TupleQueryResult getJSONStandardResults() {
        List<String> bindingsNames = List.of("x", "hpage", "name", "mbox", "age", "blurb", "friend");
        Map<String, Value> benchmarkResultsValuesRow1 = new HashMap<>();
        benchmarkResultsValuesRow1.put("x", this.getFactory().createBNode("r1"));
        benchmarkResultsValuesRow1.put("hpage", this.getFactory().createIRI("http://work.example.org/alice/"));
        benchmarkResultsValuesRow1.put("name", this.getFactory().createLiteral("Alice"));
        benchmarkResultsValuesRow1.put("mbox", this.getFactory().createLiteral(""));
        benchmarkResultsValuesRow1.put("blurb", this.getFactory().createLiteral("<p xmlns=\\\"http://www.w3.org/1999/xhtml\\\">My name is <b>alice</b></p>", RDF.XMLLiteral.getIRI()));
        benchmarkResultsValuesRow1.put("friend", this.getFactory().createBNode("r2"));
        Map<String, Value> benchmarkResultsValuesRow2 = new HashMap<>();
        benchmarkResultsValuesRow2.put("x", this.getFactory().createBNode("r2"));
        benchmarkResultsValuesRow2.put("hpage", this.getFactory().createIRI("http://work.example.org/bob/"));
        benchmarkResultsValuesRow2.put("name", this.getFactory().createLiteral("Bob", "en"));
        benchmarkResultsValuesRow2.put("mbox", this.getFactory().createIRI("mailto:bob@work.example.org"));
        benchmarkResultsValuesRow2.put("friend", this.getFactory().createBNode("r1"));
        return new MockQueryResults(bindingsNames, List.of(benchmarkResultsValuesRow1, benchmarkResultsValuesRow2));
    }

    protected String getJSONStandardResultsString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"x\"," +
                    "\"hpage\"," +
                    "\"name\"," +
                    "\"mbox\"," +
                    "\"age\"," +
                    "\"blurb\"," +
                    "\"friend\"" +
                "]," +
                "\"link\":[" +
                    "\"http://www.w3.org/TR/rdf-sparql-XMLres/example.rq\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[" +
                    "{" +
                        "\"x\":{\"type\":\"bnode\",\"value\":\"r1\"}," +
                        "\"hpage\":{\"type\":\"uri\",\"value\":\"http://work.example.org/alice/\"}," +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Alice\"}," +
                        "\"mbox\":{\"type\":\"literal\",\"value\":\"\"}," +
                        "\"blurb\":{\"datatype\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral\",\"type\":\"literal\",\"value\":\"<p xmlns=\\\\\\\"http://www.w3.org/1999/xhtml\\\\\\\">My name is <b>alice</b></p>\"}," +
                        "\"friend\":{\"type\":\"bnode\",\"value\":\"r2\"}" +
                    "}," +
                    "{" +
                        "\"x\":{\"type\":\"bnode\",\"value\":\"r2\"}," +
                        "\"hpage\":{\"type\":\"uri\",\"value\":\"http://work.example.org/bob/\"}," +
                        "\"name\":{\"type\":\"literal\",\"value\":\"Bob\",\"xml:lang\":\"en\"}," +
                        "\"mbox\":{\"type\":\"uri\",\"value\":\"mailto:bob@work.example.org\"}," +
                        "\"friend\":{\"type\":\"bnode\",\"value\":\"r1\"}" +
                    "}" +
                "]" +
            "}" +
        "}";
    }

    @Test
    @DisplayName("Tests the serialization of the results used as example in the SPARQL result JSON format recommendation")
    void JSONStandardResultsTest() {
        JsonResultSerializerOptions options = new JsonResultSerializerOptions.Builder().addLink("http://www.w3.org/TR/rdf-sparql-XMLres/example.rq").build();
        ResultSerializer serializer = getResultSerializer(getJSONStandardResults(), options);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getJSONStandardResultsString(), writer.toString());
    }

    private String getLinksTestResultsString() {
        return "{" +
            "\"head\":{" +
                "\"vars\":[" +
                    "\"a\"," +
                    "\"b\"," +
                    "\"c\"" +
                "]," +
                "\"link\":[" +
                    "\"http://google.com\"," +
                    "\"mailto:bob@corese-test.com\"" +
                "]" +
            "}," +
            "\"results\":{" +
                "\"bindings\":[]" +
            "}" +
        "}";
    }

    private TupleQueryResult getLinksTestResults() {
        return getEmptyResults();
    }

    @Test
    @DisplayName("Tests the serialization of results including several links")
    void linksTest() {
        ResultSerializer serializer = getResultSerializer(getLinksTestResults(), getOptionsWithLinks());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getLinksTestResultsString(), writer.toString());
    }
}
