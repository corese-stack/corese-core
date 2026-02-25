package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.Writer;

/**
 * Serializer class for boolean results (ASK results) for XML
 */
public class XMLBooleanSerializer implements BooleanResultSerializer {

    private final DocumentBuilderFactory xmlDocumentBuilder = DocumentBuilderFactory.newDefaultInstance();
    private final boolean result;
    private final IOOptions options;

    public XMLBooleanSerializer(boolean result) {
        this(result, new XMLSerializerOptions.Builder().build());
    }

    public XMLBooleanSerializer(boolean result, IOOptions options) {
        this.result = result;
        this.options = options;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            Document resultDocument = this.xmlDocumentBuilder.newDocumentBuilder().newDocument();
            Element root = resultDocument.createElementNS(XMLSerializerConstants.SPARQL_RESULT_NS, XMLSerializerConstants.SPARQL_QNAME);
            Element results = resultDocument.createElement(XMLSerializerConstants.BOOLEAN_QNAME);

            // Head
            if(this.options instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty()) {
                Element head = resultDocument.createElement(XMLSerializerConstants.HEAD_QNAME);
                linksOptions.links().forEach(link -> {
                    Element linkElement = resultDocument.createElement(XMLSerializerConstants.LINK_QNAME);
                    linkElement.setAttribute(XMLSerializerConstants.HREF_ATTR, link);
                    head.appendChild(linkElement);
                });
                root.appendChild(head);
            }

            // Results
            results.setTextContent(String.valueOf(this.result));

            root.appendChild(results);
            resultDocument.appendChild(root);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            if(this.options instanceof XMLSerializerOptions xmlSerializerOptions) {
                xmlSerializerOptions.getXmlSettings().forEach((key, value) -> {
                    transformer.setOutputProperty(key, value);
                    if(key.equals(OutputKeys.STANDALONE)) { // Fix for Standalone property being ignored
                        resultDocument.setXmlStandalone(value.equals("yes"));
                    }
                });
            }
            DOMSource source = new DOMSource(resultDocument);
            StreamResult resultStream = new StreamResult(writer);
            transformer.transform(source, resultStream);

        } catch (ParserConfigurationException e) {
            throw new SerializationException("Error during creation of the XML SPARQL result document.", this.getFormat(), e);
        } catch (TransformerConfigurationException e) {
            throw new SerializationException("Error during creation of the XML SPARQL result transformer.", this.getFormat(), e);
        } catch (TransformerException e) {
            throw new SerializationException("Error during writing of the XML SPARQL result document.", this.getFormat(), e);
        }
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.XML;
    }
}
