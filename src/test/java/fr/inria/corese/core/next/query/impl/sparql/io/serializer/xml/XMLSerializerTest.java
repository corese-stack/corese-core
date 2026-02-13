package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.AbstractResultSerializerTest;

import javax.xml.transform.OutputKeys;

import static fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XMLSerializerConstants.YES_PROPERTY_VALUE;

public class XMLSerializerTest extends AbstractResultSerializerTest {
    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results) {
        XMLSerializerOptions options = new XMLSerializerOptions.Builder().setXMLSetting(OutputKeys.STANDALONE, YES_PROPERTY_VALUE).build();
        return new XMLSerializer(results, options);
    }

    @Override
    protected ResultSerializer getResultSerializer(TupleQueryResult results, IOOptions options) {
        return new XMLSerializer(results, options);
    }

    @Override
    protected String getEmptyResultsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
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
        return "";
    }

    @Override
    protected String getResultsWithLiteralsString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"name\"/>" +
                        "<variable name=\"email\"/>" +
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
        return "";
    }

    @Override
    protected String getResultsWithMultiLinesLiteralString() {
        return "";
    }

    @Override
    protected String getResultWithLiteralContainingQuotesString() {
        return "";
    }

    @Override
    protected String getSVStandardResultsString() {
        return "";
    }
}
