package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
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

/**
 * Serializer for SPARQL results in XML as described in <a href="https://www.w3.org/TR/2013/REC-rdf-sparql-XMLres-20130321/">the W3C recommendation<a/>
 */
public class XmlTupleResultSerializer implements ResultSerializer {

    private final DocumentBuilderFactory xmlDocumentBuilder = DocumentBuilderFactory.newDefaultInstance();
    private final TupleQueryResult results;
    private final IOOptions options;

    public XmlTupleResultSerializer(TupleQueryResult results) {
        this(results, new XmlResultSerializerOptions.Builder().build());
    }

    public XmlTupleResultSerializer(TupleQueryResult results, IOOptions options) {
        this.results = results;
        this.options = options;
        xmlDocumentBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "no");
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            Document resultDocument = this.xmlDocumentBuilder.newDocumentBuilder().newDocument();
            Element root = resultDocument.createElementNS(XmlResultConstants.SPARQL_RESULT_NS, XmlResultConstants.SPARQL_QNAME);
            Element head = resultDocument.createElement(XmlResultConstants.HEAD_QNAME);
            Element resultsElement = resultDocument.createElement(XmlResultConstants.RESULTS_QNAME);

            // Head
            this.results.getBindingNames().forEach(bindingName -> {
                Element variableElement = resultDocument.createElement(XmlResultConstants.VARIABLE_QNAME);
                variableElement.setAttribute(XmlResultConstants.NAME_ATTR, bindingName);
                head.appendChild(variableElement);
            });
            if(this.options instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty()) {
                linksOptions.links().forEach(link -> {
                    Element linkElement = resultDocument.createElement(XmlResultConstants.LINK_QNAME);
                    linkElement.setAttribute(XmlResultConstants.HREF_ATTR, link);
                    head.appendChild(linkElement);
                });
            }
            root.appendChild(head);

            // Results
            this.results.forEach(bindingSet -> resultsElement.appendChild(bindingSetToXML(bindingSet, resultDocument)));

            root.appendChild(resultsElement);
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
        Element bindingSetElement = document.createElement(XmlResultConstants.RESULT_QNAME);

        bindings.forEach(binding -> bindingSetElement.appendChild(bindingToXML(binding, document)));

        return bindingSetElement;
    }

    private Element bindingToXML(Binding binding, Document document) {
        Element bindingElement = document.createElement(XmlResultConstants.BINDING_QNAME);
        bindingElement.setAttribute(XmlResultConstants.NAME_ATTR, binding.name());
        bindingElement.appendChild(valueToXML(binding.value(), document));
        return bindingElement;
    }

    private Element valueToXML(Value value, Document document) {
        Element valueElement;
        switch (value) {
            case IRI iriValue -> {
                valueElement = document.createElement(XmlResultConstants.URI_QNAME);
                valueElement.setTextContent(iriValue.stringValue());
            }
            case BNode bnodeValue -> {
                valueElement = document.createElement(XmlResultConstants.BNODE_QNAME);
                valueElement.setTextContent(bnodeValue.getID());
            }
            case Literal literalValue -> {
                valueElement = document.createElement(XmlResultConstants.LITERAL_QNAME);
                if (literalValue.getLanguage().isEmpty()
                        && literalValue.getDatatype() != null
                        && literalValue.getDatatype() != XSDDatatype.STRING.getIRI()
                        && literalValue.getDatatype() != RDFDatatype.LANGSTRING.getIRI()) {
                    valueElement.setAttribute(XmlResultConstants.DATATYPE_ATTR, literalValue.getDatatype().stringValue());
                }
                if (literalValue.getLanguage().isPresent()) {
                    valueElement.setAttribute(XmlResultConstants.LANG_ATTR, literalValue.getLanguage().get());
                }
                valueElement.setTextContent(literalValue.stringValue());
            }
            default ->
                    throw new SerializationException("Unable to serialize Value " + value.stringValue() + " unknown type", this.getFormat());
        }
        return valueElement;
    }
}
