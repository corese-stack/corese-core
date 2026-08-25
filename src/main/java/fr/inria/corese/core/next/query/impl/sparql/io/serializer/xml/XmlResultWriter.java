package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.XmlOutputOptions;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Small streaming XML writer shared by tuple and boolean result serializers. */
final class XmlResultWriter {

    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(
            OutputKeys.ENCODING,
            OutputKeys.INDENT,
            OutputKeys.OMIT_XML_DECLARATION,
            OutputKeys.STANDALONE,
            OutputKeys.VERSION);

    private static final String YES = "yes";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String DEFAULT_VERSION = "1.0";
    private static final String INDENT = "    ";

    private final Writer destination;
    private final XMLStreamWriter xml;
    private final boolean indent;
    private int depth;

    XmlResultWriter(Writer destination, IOOptions options) {
        this.destination = Objects.requireNonNull(destination, "writer");
        Map<String, String> properties = options instanceof XmlOutputOptions xmlOptions
                ? xmlOptions.xmlOutputProperties()
                : Map.of();
        properties.keySet().stream()
                .filter(property -> !SUPPORTED_PROPERTIES.contains(property))
                .findFirst()
                .ifPresent(property -> {
                    throw new IllegalArgumentException("Unsupported XML output property: " + property);
                });

        this.indent = YES.equalsIgnoreCase(properties.getOrDefault(OutputKeys.INDENT, "no"));
        writeDeclaration(properties);
        try {
            this.xml = XMLOutputFactory.newFactory().createXMLStreamWriter(destination);
        } catch (XMLStreamException e) {
            throw failure("Could not create XML result writer", e);
        }
    }

    void startDocument() {
        start(XmlResultConstants.SPARQL_QNAME);
        try {
            xml.writeDefaultNamespace(XmlResultConstants.SPARQL_RESULT_NS);
        } catch (XMLStreamException e) {
            throw failure("Could not write the SPARQL results namespace", e);
        }
    }

    void endDocument() {
        end();
        try {
            if (indent) {
                xml.writeCharacters("\n");
            }
            xml.flush();
            destination.flush();
        } catch (XMLStreamException | IOException e) {
            throw failure("Could not finish the XML result document", e);
        }
    }

    void start(String name) {
        beforeElement();
        try {
            xml.writeStartElement(name);
            depth++;
        } catch (XMLStreamException e) {
            throw failure("Could not write XML element " + name, e);
        }
    }

    void empty(String name) {
        beforeElement();
        try {
            xml.writeEmptyElement(name);
        } catch (XMLStreamException e) {
            throw failure("Could not write XML element " + name, e);
        }
    }

    void end() {
        depth--;
        try {
            if (indent) {
                xml.writeCharacters("\n" + INDENT.repeat(depth));
            }
            xml.writeEndElement();
        } catch (XMLStreamException e) {
            throw failure("Could not close XML element", e);
        }
    }

    void textElement(String name, String text) {
        start(name);
        textElementContent(text);
    }

    void textElementContent(String text) {
        try {
            xml.writeCharacters(text);
        } catch (XMLStreamException e) {
            throw failure("Could not write XML text", e);
        }
        endInline();
    }

    void attribute(String name, String value) {
        try {
            xml.writeAttribute(name, value);
        } catch (XMLStreamException e) {
            throw failure("Could not write XML attribute " + name, e);
        }
    }

    void languageAttribute(String language) {
        try {
            xml.writeAttribute(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI, "lang", language);
        } catch (XMLStreamException e) {
            throw failure("Could not write xml:lang", e);
        }
    }

    private void endInline() {
        depth--;
        try {
            xml.writeEndElement();
        } catch (XMLStreamException e) {
            throw failure("Could not close XML element", e);
        }
    }

    private void beforeElement() {
        if (!indent || depth == 0) {
            return;
        }
        try {
            xml.writeCharacters("\n" + INDENT.repeat(depth));
        } catch (XMLStreamException e) {
            throw failure("Could not indent XML result", e);
        }
    }

    private void writeDeclaration(Map<String, String> properties) {
        if (YES.equalsIgnoreCase(properties.getOrDefault(OutputKeys.OMIT_XML_DECLARATION, "no"))) {
            return;
        }
        String version = properties.getOrDefault(OutputKeys.VERSION, DEFAULT_VERSION);
        String encoding = properties.getOrDefault(OutputKeys.ENCODING, DEFAULT_ENCODING);
        String standalone = properties.getOrDefault(OutputKeys.STANDALONE, YES);
        try {
            destination.write("<?xml version=\"");
            destination.write(version);
            destination.write("\" encoding=\"");
            destination.write(encoding);
            destination.write("\" standalone=\"");
            destination.write(standalone);
            destination.write("\"?>");
            if (indent) {
                destination.write('\n');
            }
        } catch (IOException e) {
            throw failure("Could not write XML declaration", e);
        }
    }

    private static SerializationException failure(String message, Throwable cause) {
        return new SerializationException(message, ResultFormat.XML, cause);
    }
}
