package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.common.literal.RDF;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.Writer;

public class XMLSerializer implements ResultSerializer {

    private final DocumentBuilderFactory xmlDocumentBuilder = DocumentBuilderFactory.newDefaultInstance();
    private final TupleQueryResult results;
    private final IOOptions options;

    public XMLSerializer(TupleQueryResult results) {
        this(results, new XMLSerializerOptions.Builder().build());
    }

    public XMLSerializer(TupleQueryResult results, IOOptions options) {
        this.results = results;
        this.options = options;
        xmlDocumentBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "no");
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            Document resultDocument = this.xmlDocumentBuilder.newDocumentBuilder().newDocument();
            Element root = resultDocument.createElementNS(XMLSerializerConstants.SPARQL_RESULT_NS, XMLSerializerConstants.SPARQL_QNAME);
            Element head = resultDocument.createElement(XMLSerializerConstants.HEAD_QNAME);
            Element results = resultDocument.createElement(XMLSerializerConstants.RESULTS_QNAME);

            // Head
            this.results.getBindingNames().forEach(bindingName -> {
                Element variableElement = resultDocument.createElement(XMLSerializerConstants.VARIABLE_QNAME);
                variableElement.setAttribute(XMLSerializerConstants.NAME_ATTR, bindingName);
                head.appendChild(variableElement);
            });
            if(this.options instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty()) {
                linksOptions.links().forEach(link -> {
                    Element linkElement = resultDocument.createElement(XMLSerializerConstants.LINK_QNAME);
                    linkElement.setAttribute(XMLSerializerConstants.HREF_ATTR, link);
                    head.appendChild(linkElement);
                });
            }
            root.appendChild(head);

            // Results
            this.results.forEach(bindingSet -> {
                results.appendChild(bindingSetToXML(bindingSet, resultDocument));
            });

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
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

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

    private Element bindingSetToXML(BindingSet bindings, Document document) {
        Element bindingSetElement = document.createElement(XMLSerializerConstants.RESULT_QNAME);

        bindings.forEach(binding -> {
            bindingSetElement.appendChild(bindingToXML(binding, document));
        });

        return bindingSetElement;
    }

    private Element bindingToXML(Binding binding, Document document) {
        Element bindingElement = document.createElement(XMLSerializerConstants.BINDING_QNAME);
        bindingElement.setAttribute(XMLSerializerConstants.NAME_ATTR, binding.name());
        bindingElement.appendChild(valueToXML(binding.value(), document));
        return bindingElement;
    }

    private Element valueToXML(Value value, Document document) {
        Element valueElement;
        if(value instanceof IRI iriValue) {
            valueElement = document.createElement(XMLSerializerConstants.URI_QNAME);
            valueElement.setTextContent(iriValue.stringValue());
        } else if (value instanceof BNode bnodeValue) {
            valueElement = document.createElement(XMLSerializerConstants.BNODE_QNAME);
            valueElement.setTextContent(bnodeValue.getID());
        } else if (value instanceof Literal literalValue) {
            valueElement = document.createElement(XMLSerializerConstants.LITERAL_QNAME);
            if (literalValue.getLanguage().isEmpty()
                    && literalValue.getDatatype() != null
                    && literalValue.getDatatype() != XSD.STRING.getIRI()
                    && literalValue.getDatatype() != RDF.LANGSTRING.getIRI()) {
                valueElement.setAttribute(XMLSerializerConstants.DATATYPE_ATTR, literalValue.getDatatype().stringValue());
            }
            if(literalValue.getLanguage().isPresent()) {
                valueElement.setAttribute(XMLSerializerConstants.LANG_ATTR, literalValue.getLanguage().get());
            }
            valueElement.setTextContent(literalValue.stringValue());
        } else {
            throw new SerializationException("Unable to serialize Value " + value.stringValue() +" unknown type", this.getFormat());
        }
        return valueElement;
    }
}
