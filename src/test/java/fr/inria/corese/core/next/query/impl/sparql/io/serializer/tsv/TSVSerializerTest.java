package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.ResultSerializerTest;

public class TSVSerializerTest extends ResultSerializerTest {
    @Override
    protected ResultSerializer getResultSerializer() {
        return null;
    }

    @Override
    protected String getEmptyResultsString() {
        return "";
    }

    @Override
    protected String getResultsWithUris() {
        return "";
    }

    @Override
    protected String getResultsWithLiterals() {
        return "";
    }

    @Override
    protected String getResultsWithBlankNodes() {
        return "";
    }

    @Override
    protected String getResultsWithMultipleLines() {
        return "";
    }

    @Override
    protected String getResultsWithMultiLinesLiteral() {
        return "";
    }
}
