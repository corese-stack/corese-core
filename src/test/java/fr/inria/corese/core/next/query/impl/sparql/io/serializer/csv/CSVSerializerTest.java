package fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv;

import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.ResultSerializerFactory;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.AbstractResultSerializerTest;

public class CSVSerializerTest extends AbstractResultSerializerTest {

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        return new ResultSerializerFactory().createSerializer(ResultFormat.CSV, results);
    }

    @Override
    protected String getEmptyResultsString() {
        return """
                a,b,c
                """;
    }

    @Override
    protected String getResultsWithUrisString() {
        return "email,homepage\n" +
                "mailto:jlow@example.com,https://bsky.app/profile/johnnyleeoutlaw\n" +
                "mailto:peter@example.org,https://peter.goodguy.com\n";
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return "email,name\n" +
                "mailto:jlow@example.com,Johnny Lee Outlaw\n" +
                "mailto:peter@example.org,Peter Goodguy\n" +
                "mailto:carol@example.org,Carol Patoune\n";
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return "nb\n" +
                "_:a\n" +
                "_:b\n" +
                "_:c\n";
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return "mail,depiction\n" +
                "mailto:carol@example.org,\"All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl.\"\n";
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return "name,desc\n" +
                "Alice,Literal with a single quote'" +
                "Bernard,\"Literal with a quote\"\"\"" +
                "Charles,\"Literal both quotes single ' and double \"\"\"";
    }

    @Override
    protected String getStandardResultsString() {
        return "x,literal\n" +
                "http://example/x,String\n" +
                "http://example/x,\"String-with-dquote\"\"\"\n" +
                "_:blank0,Blank node\n" +
                ",Missing 'x'\n" +
                ",\n" +
                "http://example/x,\n" +
                "_:blank1,String-with-lang\n" +
                "_:blank1,123\n";
    }
}
