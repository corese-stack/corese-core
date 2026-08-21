package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
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
public class XmlBooleanResultSerializer implements BooleanResultSerializer {

    private final DocumentBuilderFactory xmlDocumentBuilder = DocumentBuilderFactory.newDefaultInstance();
    private final boolean result;
    private final IOOptions options;

    public XmlBooleanResultSerializer(boolean result) {
        this(result, new XmlResultSerializerOptions.Builder().build());
    }

    public XmlBooleanResultSerializer(boolean result, IOOptions options) {
        this.result = result;
        this.options = options;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            Document resultDocument = this.xmlDocumentBuilder.newDocumentBuilder().newDocument();
            Element root = resultDocument.createElementNS(XmlResultConstants.SPARQL_RESULT_NS, XmlResultConstants.SPARQL_QNAME);
            Element results = resultDocument.createElement(XmlResultConstants.BOOLEAN_QNAME);

            // Head
            if(this.options instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty()) {
                Element head = resultDocument.createElement(XmlResultConstants.HEAD_QNAME);
                linksOptions.links().forEach(link -> {
                    Element linkElement = resultDocument.createElement(XmlResultConstants.LINK_QNAME);
                    linkElement.setAttribute(XmlResultConstants.HREF_ATTR, link);
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
            if(this.options instanceof XmlResultSerializerOptions xmlSerializerOptions) {
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
