package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractBooleanResultSerializerTest;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.LinksSerializerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlBooleanResultSerializerTest extends AbstractBooleanResultSerializerTest implements LinksSerializerTest {
    @Override
    protected BooleanResultSerializer getSerializer(boolean result) {
        return new XmlBooleanResultSerializer(result);
    }

    @Override
    protected BooleanResultSerializer getSerializer(boolean result, IOOptions options) {
        return new XmlBooleanResultSerializer(result, options);
    }

    @Override
    protected String getTrueResultString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
            "<boolean>true</boolean>" +
        "</sparql>";
    }

    @Override
    protected String getFalseResultString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
            "<boolean>false</boolean>" +
        "</sparql>";
    }

    private String getLinksTestResultsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<link href=\"http://google.com\"/>" +
                        "<link href=\"mailto:bob@corese-test.com\"/>" +
                    "</head>" +
                    "<boolean>true</boolean>" +
                "</sparql>";
    }

    @Test
    @DisplayName("Tests the serialization of results including several links")
    public void linksTest() {
        IOOptions options = getOptionsWithLinks();
        ResultSerializer serializer = new XmlBooleanResultSerializer(true, options);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getLinksTestResultsString(), writer.toString());
    }
}
