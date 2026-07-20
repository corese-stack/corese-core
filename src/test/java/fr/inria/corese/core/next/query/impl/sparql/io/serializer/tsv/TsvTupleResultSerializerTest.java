package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.DefaultResultSerializerFactory;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.AbstractResultSerializerTest;

public class TsvTupleResultSerializerTest extends AbstractResultSerializerTest {

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        return new DefaultResultSerializerFactory().createTupleSerializer(ResultFormat.TSV, results);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new DefaultResultSerializerFactory().createTupleSerializer(ResultFormat.TSV, results, options);
    }

    @Override
    protected String getEmptyResultsString() {
        return "?a\t?b\t?c\n";
    }

    @Override
    protected String getResultsWithUrisString() {
        return "?email\t?homepage\n" +
            "<mailto:jlow@example.com>\t<https://bsky.app/profile/johnnyleeoutlaw>\n" +
            "<mailto:peter@example.org>\t<https://peter.goodguy.com>\n";
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return "?email\t?name\n" +
                "<mailto:jlow@example.com>\t\"Johnny Lee Outlaw\"\n" +
                "<mailto:peter@example.org>\t\"Peter Goodguy\"\n" +
                "<mailto:carol@example.org>\t\"Carol Patoune\"\n";
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return "?nb\n" +
                "_:a\n" +
                "_:b\n" +
                "_:c\n";
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return "?mail\t?depiction\n" +
                "<mailto:carol@example.org>\t\"All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl.\"\n";
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return "?name\t?desc\n" +
                "\"Alice\"\t\"Literal with a single quote'\"\n" +
                "\"Bernard\"\t\"Literal with a quote\\\"\"\n" +
                "\"Charles\"\t\"Literal both quotes single ' and double \\\"\"\n";
    }

    @Override
    protected String getSVStandardResultsString() {
        return "?x\t?literal\n" +
                "<http://example/x>\t\"String\"\n" +
                "<http://example/x>\t\"String-with-dquote\\\"\"\n" +
                "_:blank0\t\"Blank node\"\n" +
                "\t\"Missing 'x'\"\n" +
                "\t\n" +
                "<http://example/x>\t\n" +
                "_:blank1\t\"String-with-lang\"@en\n" +
                "_:blank1\t123\n";
    }
}
