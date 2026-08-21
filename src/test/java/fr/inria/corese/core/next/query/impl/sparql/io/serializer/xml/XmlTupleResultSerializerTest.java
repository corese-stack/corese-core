package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.AbstractResultSerializerTest;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.LinksSerializerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.transform.OutputKeys;

import java.io.StringWriter;

import static fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XmlResultConstants.YES_PROPERTY_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlTupleResultSerializerTest extends AbstractResultSerializerTest implements LinksSerializerTest {
    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        XmlResultSerializerOptions options = new XmlResultSerializerOptions.Builder().setXMLSetting(OutputKeys.STANDALONE, YES_PROPERTY_VALUE).build();
        return new XmlTupleResultSerializer(results, options);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new XmlTupleResultSerializer(results, options);
    }

    @Override
    protected String getEmptyResultsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"a\"/>" +
                        "<variable name=\"b\"/>" +
                        "<variable name=\"c\"/>" +
                    "</head>" +
                    "<results/>" +
                "</sparql>";
    }

    @Override
    protected String getResultsWithUrisString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"email\"/>" +
                        "<variable name=\"homepage\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"email\">" +
                                "<uri>mailto:jlow@example.com</uri>" +
                            "</binding>" +
                            "<binding name=\"homepage\">" +
                                "<uri>https://bsky.app/profile/johnnyleeoutlaw</uri>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"email\">" +
                                "<uri>mailto:peter@example.org</uri>" +
                            "</binding>" +
                            "<binding name=\"homepage\">" +
                                "<uri>https://peter.goodguy.com</uri>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"email\"/>" +
                        "<variable name=\"name\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Johnny Lee Outlaw</literal>" +
                            "</binding>" +
                            "<binding name=\"email\">" +
                                "<uri>mailto:jlow@example.com</uri>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Peter Goodguy</literal>" +
                            "</binding>" +
                            "<binding name=\"email\">" +
                                "<uri>mailto:peter@example.org</uri>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Carol Patoune</literal>" +
                            "</binding>" +
                            "<binding name=\"email\">" +
                                "<uri>mailto:carol@example.org</uri>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    @Override
    protected String getResultsWithBlankNodesString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"nb\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"nb\">" +
                                "<bnode>a</bnode>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"nb\">" +
                                "<bnode>b</bnode>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"nb\">" +
                                "<bnode>c</bnode>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"mail\"/>" +
                        "<variable name=\"depiction\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"mail\">" +
                                "<uri>mailto:carol@example.org</uri>" +
                            "</binding>" +
                            "<binding name=\"depiction\">" +
                                "<literal>All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl,\n" +
                "All this work and no play makes Carols a dull girl.</literal>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"name\"/>" +
                        "<variable name=\"desc\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Alice</literal>" +
                            "</binding>" +
                            "<binding name=\"desc\">" +
                                "<literal>Literal with a single quote'</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Bernard</literal>" +
                            "</binding>" +
                            "<binding name=\"desc\">" +
                                "<literal>Literal with a quote\"</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"name\">" +
                                "<literal>Charles</literal>" +
                            "</binding>" +
                            "<binding name=\"desc\">" +
                                "<literal>Literal both quotes single ' and double \"</literal>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    @Override
    protected String getSVStandardResultsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"x\"/>" +
                        "<variable name=\"literal\"/>" +
                    "</head>" +
                    "<results>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<uri>http://example/x</uri>" +
                            "</binding>" +
                            "<binding name=\"literal\">" +
                                "<literal>String</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<uri>http://example/x</uri>" +
                            "</binding>" +
                            "<binding name=\"literal\">" +
                                "<literal>String-with-dquote\"</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<bnode>blank0</bnode>" +
                            "</binding>" +
                            "<binding name=\"literal\">" +
                                "<literal>Blank node</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"literal\">" +
                                "<literal>Missing 'x'</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result/>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<uri>http://example/x</uri>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<bnode>blank1</bnode>" +
                            "</binding>" +
                            "<binding name=\"literal\">" +
                                "<literal xml:lang=\"en\">String-with-lang</literal>" +
                            "</binding>" +
                        "</result>" +
                        "<result>" +
                            "<binding name=\"x\">" +
                                "<bnode>blank1</bnode>" +
                            "</binding>" +
                            "<binding name=\"literal\">" +
                                "<literal datatype=\"http://www.w3.org/2001/XMLSchema#integer\">123</literal>" +
                            "</binding>" +
                        "</result>" +
                    "</results>" +
                "</sparql>";
    }

    private String getLinksTestResultsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"a\"/>" +
                        "<variable name=\"b\"/>" +
                        "<variable name=\"c\"/>" +
                        "<link href=\"http://google.com\"/>" +
                        "<link href=\"mailto:bob@corese-test.com\"/>" +
                    "</head>" +
                    "<results/>" +
                "</sparql>";
    }

    private TupleQueryResult getLinksTestResults() {
        return getEmptyResults();
    }

    @Test
    @DisplayName("Tests the serialization of results including several links")
    public void linksTest() {
        IOOptions options = getOptionsWithLinks();
        ResultSerializer serializer = getResultSerializer(getLinksTestResults(), options);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        assertEquals(getLinksTestResultsString(), writer.toString());
    }
}
