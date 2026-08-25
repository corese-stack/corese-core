package fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultIOOptions;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.DefaultResultSerializerFactory;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractResultSerializerTest;

class CsvTupleResultSerializerTest extends AbstractResultSerializerTest {

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        return new DefaultResultSerializerFactory().createTupleSerializer(ResultFormat.CSV, results);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new DefaultResultSerializerFactory().createTupleSerializer(
                ResultFormat.CSV,
                results,
                (ResultIOOptions) options);
    }

    @Override
    protected String getEmptyResultsString() {
        return """
                a,b,c
                """;
    }

    @Override
    protected String getResultsWithUrisString() {
        return """
                email,homepage
                mailto:jlow@example.com,https://bsky.app/profile/johnnyleeoutlaw
                mailto:peter@example.org,https://peter.goodguy.com
                """;
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return """
                email,name
                mailto:jlow@example.com,Johnny Lee Outlaw
                mailto:peter@example.org,Peter Goodguy
                mailto:carol@example.org,Carol Patoune
                """;
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return """
                nb
                _:a
                _:b
                _:c
                """;
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return """
                mail,depiction
                mailto:carol@example.org,"All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl."
                """;
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return """
                name,desc
                Alice,Literal with a single quote'
                Bernard,"Literal with a quote\"\""
                Charles,"Literal both quotes single ' and double \"\""
                """;
    }

    @Override
    protected String getSVStandardResultsString() {
        return """
                x,literal
                http://example/x,String
                http://example/x,"String-with-dquote\"\""
                _:blank0,Blank node
                ,Missing 'x'
                ,
                http://example/x,
                _:blank1,String-with-lang
                _:blank1,123
                """;
    }
}
