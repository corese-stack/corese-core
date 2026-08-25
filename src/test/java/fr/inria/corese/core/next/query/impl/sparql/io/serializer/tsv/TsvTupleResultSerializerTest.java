package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.format.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.option.ResultSerializationOptions;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.DefaultResultSerializerFactory;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractResultSerializerTest;

class TsvTupleResultSerializerTest extends AbstractResultSerializerTest {

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        return new DefaultResultSerializerFactory().createTupleSerializer(ResultFormat.TSV, results);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new DefaultResultSerializerFactory().createTupleSerializer(
                ResultFormat.TSV,
                results,
                (ResultSerializationOptions) options);
    }

    @Override
    protected String getEmptyResultsString() {
        return "?a\t?b\t?c\n";
    }

    @Override
    protected String getResultsWithUrisString() {
        return """
                ?email\t?homepage
                <mailto:jlow@example.com>\t<https://bsky.app/profile/johnnyleeoutlaw>
                <mailto:peter@example.org>\t<https://peter.goodguy.com>
                """;
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return """
                ?email\t?name
                <mailto:jlow@example.com>\t"Johnny Lee Outlaw"
                <mailto:peter@example.org>\t"Peter Goodguy"
                <mailto:carol@example.org>\t"Carol Patoune"
                """;
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return """
                ?nb
                _:a
                _:b
                _:c
                """;
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return """
                ?mail\t?depiction
                <mailto:carol@example.org>\t"All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl,
                All this work and no play makes Carols a dull girl."
                """;
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return """
                ?name\t?desc
                "Alice"\t"Literal with a single quote'"
                "Bernard"\t"Literal with a quote\\""
                "Charles"\t"Literal both quotes single ' and double \\""
                """;
    }

    @Override
    protected String getSVStandardResultsString() {
        return """
                ?x\t?literal
                <http://example/x>\t"String"
                <http://example/x>\t"String-with-dquote\\""
                _:blank0\t"Blank node"
                \t"Missing 'x'"
                \t
                <http://example/x>\t
                _:blank1\t"String-with-lang"@en
                _:blank1\t123
                """;
    }
}
