package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.io.format.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.option.LinksOptions;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.io.Writer;
import java.util.Objects;

/** Streaming serializer for the SPARQL 1.1 Query Results XML Format. */
public class XmlTupleResultSerializer implements ResultSerializer {

    private final TupleQueryResult results;
    private final IOOptions options;

    public XmlTupleResultSerializer(TupleQueryResult results) {
        this(results, new XmlResultSerializerOptions.Builder().build());
    }

    public XmlTupleResultSerializer(TupleQueryResult results, IOOptions options) {
        this.results = Objects.requireNonNull(results, "results");
        this.options = Objects.requireNonNull(options, "options");
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        XmlResultWriter xml = new XmlResultWriter(writer, options);
        xml.startDocument();
        writeHead(xml);
        writeResults(xml);
        xml.endDocument();
    }

    private void writeHead(XmlResultWriter xml) {
        xml.start(XmlResultConstants.HEAD_QNAME);
        for (String bindingName : results.getBindingNames()) {
            xml.empty(XmlResultConstants.VARIABLE_QNAME);
            xml.attribute(XmlResultConstants.NAME_ATTR, bindingName);
        }
        if (options instanceof LinksOptions linksOptions) {
            for (String link : linksOptions.links()) {
                xml.empty(XmlResultConstants.LINK_QNAME);
                xml.attribute(XmlResultConstants.HREF_ATTR, link);
            }
        }
        xml.end();
    }

    private void writeResults(XmlResultWriter xml) {
        if (!results.hasNext()) {
            xml.empty(XmlResultConstants.RESULTS_QNAME);
            return;
        }
        xml.start(XmlResultConstants.RESULTS_QNAME);
        do {
            writeBindingSet(xml, results.next());
        } while (results.hasNext());
        xml.end();
    }

    private void writeBindingSet(XmlResultWriter xml, BindingSet bindings) {
        boolean empty = results.getBindingNames().stream().noneMatch(bindings::hasBinding);
        if (empty) {
            xml.empty(XmlResultConstants.RESULT_QNAME);
            return;
        }
        xml.start(XmlResultConstants.RESULT_QNAME);
        for (String bindingName : results.getBindingNames()) {
            if (!bindings.hasBinding(bindingName)) {
                continue;
            }
            xml.start(XmlResultConstants.BINDING_QNAME);
            xml.attribute(XmlResultConstants.NAME_ATTR, bindingName);
            writeValue(xml, bindings.getValue(bindingName));
            xml.end();
        }
        xml.end();
    }

    private void writeValue(XmlResultWriter xml, Value value) {
        switch (value) {
            case IRI iri -> xml.textElement(XmlResultConstants.URI_QNAME, iri.stringValue());
            case BNode bNode -> xml.textElement(XmlResultConstants.BNODE_QNAME, bNode.getID());
            case Literal literal -> writeLiteral(xml, literal);
            default -> throw new SerializationException(
                    "Unable to serialize value " + value.stringValue(), getFormat());
        }
    }

    private static void writeLiteral(XmlResultWriter xml, Literal literal) {
        xml.start(XmlResultConstants.LITERAL_QNAME);
        if (literal.getLanguage().isPresent()) {
            xml.languageAttribute(literal.getLanguage().orElseThrow());
        } else if (literal.getDatatype() != null
                && !literal.getDatatype().equals(XSDDatatype.STRING.getIRI())
                && !literal.getDatatype().equals(RDFDatatype.LANGSTRING.getIRI())) {
            xml.attribute(XmlResultConstants.DATATYPE_ATTR, literal.getDatatype().stringValue());
        }
        xml.textElementContent(literal.stringValue());
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.XML;
    }
}
