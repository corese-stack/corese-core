package fr.inria.corese.core.next.data.impl.io.serializer.rdfxml;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.data.impl.io.parser.rdfxml.RDFXMLParser;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.StringReader;
import java.io.StringWriter;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Ensures namespace options preserve both XML validity and RDF meaning. */
class RDFXMLNamespaceTest extends ParserTestBase {

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void namespaceOptionsPreserveGraph(boolean usePrefixes, boolean autoDeclare) throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        model.setNamespace("ex", "http://example.org/");
        var subject = valueFactory.createIRI("http://example.org/subject");
        var node = valueFactory.createBNode();
        model.add(subject, valueFactory.createIRI("http://example.org/name"), valueFactory.createLiteral("Alice", "fr"));
        model.add(subject, valueFactory.createIRI("http://unknown.org/count"), valueFactory.createLiteral("42", XSD.xsdInteger.getIRI()));
        model.add(subject, valueFactory.createIRI("http://unknown.org/link"), node);
        model.add(node, RDF.type.getIRI(), valueFactory.createIRI("http://example.org/Person"));
        var options = new RDFXMLSerializerOptions.Builder()
                .prefixHandler(new PrefixHandler(false))
                .usePrefixes(usePrefixes).autoDeclarePrefixes(autoDeclare).build();

        assertRoundTrip(model, options);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "http://wrong.example/rdf#"})
    void rdfBindingIsPresentAndCannotBeOverridden(String replacement) throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        var predicate = valueFactory.createIRI("http://example.org/property");
        model.add(valueFactory.createIRI("http://example.org/subject"), predicate, valueFactory.createLiteral("value"));
        model.setNamespace("rdf", "http://model.example/incorrect#");
        var mapping = new PrefixHandler(false);
        if (!replacement.isEmpty()) {
            mapping.setPrefix("rdf", replacement);
        }
        var options = new RDFXMLSerializerOptions.Builder().prefixHandler(mapping).build();

        String xml = assertRoundTrip(model, options);
        assertFalse(xml.contains("xmlns:null="));
        assertEquals(RDF.getVocabularyNamespace(), parseXml(xml).getDocumentElement().lookupNamespaceURI("rdf"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.org/123name", "http://example.org/has%20name", "urn:example:property",
            "http://example.org/cafe\u0301", "http://example.org/\u200Cname", "http://example.org/\uD800\uDC00name",
            "http://www.w3.org/2000/xmlns/property"
    })
    void predicateNamesPreserveTheirExactIri(String predicate) throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        model.add(valueFactory.createIRI("http://example.org/subject"),
                valueFactory.createIRI(predicate), valueFactory.createLiteral("value"));

        assertRoundTrip(model, RDFXMLSerializerOptions.defaultConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.org/", "http://example.org/123",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#li",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Description",
            "http://www.w3.org/2000/xmlns/p"
    })
    void unrepresentablePredicatesFailBeforeWriting(String predicate) {
        Model model = createTestModel();
        model.add(valueFactory.createIRI("http://example.org/subject"),
                valueFactory.createIRI(predicate), valueFactory.createLiteral("value"));
        var serializer = new RDFXMLSerializer(model);
        var output = new StringWriter();

        assertThrows(SerializationException.class, () -> serializer.write(output));
        assertEquals("", output.toString());
    }

    @Test
    void datatypeAttributesContainFullIris() throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        var predicate = valueFactory.createIRI("http://example.org/count");
        model.add(valueFactory.createIRI("http://example.org/subject"), predicate,
                valueFactory.createLiteral("42", XSD.xsdInteger.getIRI()));

        String xml = assertRoundTrip(model, RDFXMLSerializerOptions.defaultConfig());
        var element = parseXml(xml).getElementsByTagNameNS("http://example.org/", "count").item(0);
        assertEquals(XSD.xsdInteger.getIRI().stringValue(),
                element.getAttributes().getNamedItemNS(RDF.getVocabularyNamespace(), "datatype").getNodeValue());
    }

    @Test
    void reservedNamespaceIrisInAttributesDoNotCreateNamespaceDeclarations() throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        String namespace = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        var subject = valueFactory.createIRI(namespace + "subject");
        model.add(subject, valueFactory.createIRI("http://example.org/link"), valueFactory.createIRI(namespace + "object"));
        model.add(subject, valueFactory.createIRI("http://example.org/value"),
                valueFactory.createLiteral("text", valueFactory.createIRI(namespace + "datatype")));

        assertRoundTrip(model, RDFXMLSerializerOptions.defaultConfig());
    }

    @Test
    void invalidBlankNodeLabelsAndGeneratedIdsDoNotCollide() throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        var first = valueFactory.createBNode("123bad");
        var second = valueFactory.createBNode("b0");
        var predicate = valueFactory.createIRI("http://example.org/link");
        model.add(first, predicate, second);
        model.add(second, predicate, first);

        assertRoundTrip(model, new RDFXMLSerializerOptions.Builder().stableBlankNodeIds(false).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a\rb", "a\r\nb", "a\nb", "a\tb", "text \uD83D\uDE00"})
    void literalWhitespaceAndUnicodeSurviveRoundTrip(String value) throws ParserConfigurationException, SAXException, IOException {
        Model model = createTestModel();
        model.add(valueFactory.createIRI("http://example.org/subject"),
                valueFactory.createIRI("http://example.org/value"), valueFactory.createLiteral(value));

        assertRoundTrip(model, RDFXMLSerializerOptions.defaultConfig());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 0xD800, 0xFFFF})
    void charactersNotRepresentableInXmlFailExplicitly(int codePoint) {
        Model model = createTestModel();
        model.add(valueFactory.createIRI("http://example.org/subject"),
                valueFactory.createIRI("http://example.org/value"),
                valueFactory.createLiteral("invalid" + Character.toString(codePoint)));
        var serializer = new RDFXMLSerializer(model);
        var output = new StringWriter();

        assertThrows(SerializationException.class, () -> serializer.write(output));
    }

    private String assertRoundTrip(Model original, RDFXMLSerializerOptions options) throws ParserConfigurationException, SAXException, IOException {
        var output = new StringWriter();
        new RDFXMLSerializer(original, options).write(output);
        String xml = output.toString();
        parseXml(xml);
        Model parsed = createTestModel();
        new RDFXMLParser(parsed, valueFactory).parse(new StringReader(xml));
        assertModelsIsomorphic(original, parsed);
        return xml;
    }

    private static org.w3c.dom.Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException e) {
            // Ignore if parser implementation does not recognize JAXP 1.5 properties
        }
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
