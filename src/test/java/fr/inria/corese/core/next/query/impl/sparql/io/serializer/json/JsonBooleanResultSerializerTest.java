package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractBooleanResultSerializerTest;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.LinksSerializerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonBooleanResultSerializerTest extends AbstractBooleanResultSerializerTest implements LinksSerializerTest {
    @Override
    protected BooleanResultSerializer getSerializer(boolean result) {
        return new JsonBooleanResultSerializer(result);
    }

    @Override
    protected BooleanResultSerializer getSerializer(boolean result, IOOptions options) {
        return new JsonBooleanResultSerializer(result, options);
    }

    @Override
    protected String getTrueResultString() {
        return "{\"head\":{},\"boolean\":true}";
    }

    @Override
    protected String getFalseResultString() {
        return "{\"head\":{},\"boolean\":false}";
    }

    private String getLinksTestResultsString() {
        return "{" +
            "\"head\":{" +
                "\"link\":[" +
                    "\"http://google.com\"," +
                    "\"mailto:bob@corese-test.com\"" +
                "]" +
            "}," +
            "\"boolean\":true" +
        "}";
    }

    private boolean getLinksTestResults() {
        return true;
    }

    @Test
    @DisplayName("Tests the serialization of results including several links")
    void linksTest() {
        ResultSerializer serializer = getSerializer(getLinksTestResults(), getOptionsWithLinks());
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getLinksTestResultsString(), writer.toString());
    }
}
