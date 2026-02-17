package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.transform.OutputKeys;
import java.io.StringWriter;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.ResultSerializerTestUtils.MockQueryResults;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XMLSerializerOptionsTest {

    @Test
    @DisplayName("Tests the application of the indent XML option to the serializer")
    void xmlSerializerIndentPropertyTest() {
        StringWriter outputWriter = new StringWriter();
        IOOptions options = new XMLSerializerOptions.Builder().setXMLSetting(OutputKeys.INDENT, XMLSerializerConstants.YES_PROPERTY_VALUE).build();
        MockQueryResults results = new MockQueryResults(List.of("x"), List.of());
        ResultSerializer serializer = new XMLSerializer(results, options);
        serializer.write(outputWriter);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n" +
                "    <head>\n" +
                "        <variable name=\"x\"/>\n" +
                "    </head>\n" +
                "    <results/>\n" +
                "</sparql>\n", outputWriter.toString());
    }

    @Test
    @DisplayName("Tests the application of the standalone XML options to the serializer")
    void xmlSerializerStandalonePropertyTest() {
        StringWriter outputWriter = new StringWriter();
        IOOptions options = new XMLSerializerOptions.Builder().setXMLSetting(OutputKeys.STANDALONE, XMLSerializerConstants.NO_PROPERTY_VALUE).build();
        MockQueryResults results = new MockQueryResults(List.of("x"), List.of());
        ResultSerializer serializer = new XMLSerializer(results, options);
        serializer.write(outputWriter);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                    "<head>" +
                        "<variable name=\"x\"/>" +
                    "</head>" +
                    "<results/>" +
                "</sparql>", outputWriter.toString());
    }

    @Test
    @DisplayName("Tests the application of the declaration XML options to the serializer")
    void xmlSerializerOmitDeclarationPropertyTest() {
        StringWriter outputWriter = new StringWriter();
        IOOptions options = new XMLSerializerOptions.Builder().setXMLSetting(OutputKeys.OMIT_XML_DECLARATION, XMLSerializerConstants.YES_PROPERTY_VALUE).build();
        MockQueryResults results = new MockQueryResults(List.of("x"), List.of());
        ResultSerializer serializer = new XMLSerializer(results, options);
        serializer.write(outputWriter);

        assertEquals("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                "<head>" +
                "<variable name=\"x\"/>" +
                "</head>" +
                "<results/>" +
                "</sparql>", outputWriter.toString());
    }

    @Test
    @DisplayName("Tests the application of the declaration XML options to the serializer")
    void xmlSerializerMediaTypePropertyTest() {
        StringWriter outputWriter = new StringWriter();
        IOOptions options = new XMLSerializerOptions.Builder().setXMLSetting(OutputKeys.OMIT_XML_DECLARATION, XMLSerializerConstants.YES_PROPERTY_VALUE).build();
        MockQueryResults results = new MockQueryResults(List.of("x"), List.of());
        ResultSerializer serializer = new XMLSerializer(results, options);
        serializer.write(outputWriter);

        assertEquals("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">" +
                "<head>" +
                "<variable name=\"x\"/>" +
                "</head>" +
                "<results/>" +
                "</sparql>", outputWriter.toString());
    }

}
