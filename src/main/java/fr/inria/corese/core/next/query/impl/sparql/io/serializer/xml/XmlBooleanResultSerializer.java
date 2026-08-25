package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;

import java.io.Writer;
import java.util.Objects;

/** Streaming serializer for SPARQL boolean results in XML. */
public class XmlBooleanResultSerializer implements BooleanResultSerializer {

    private final boolean result;
    private final IOOptions options;

    public XmlBooleanResultSerializer(boolean result) {
        this(result, new XmlResultSerializerOptions.Builder().build());
    }

    public XmlBooleanResultSerializer(boolean result, IOOptions options) {
        this.result = result;
        this.options = Objects.requireNonNull(options, "options");
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        XmlResultWriter xml = new XmlResultWriter(writer, options);
        xml.startDocument();
        writeHead(xml);
        xml.textElement(XmlResultConstants.BOOLEAN_QNAME, Boolean.toString(result));
        xml.endDocument();
    }

    private void writeHead(XmlResultWriter xml) {
        if (options instanceof LinksOptions linksOptions && !linksOptions.links().isEmpty()) {
            xml.start(XmlResultConstants.HEAD_QNAME);
            for (String link : linksOptions.links()) {
                xml.empty(XmlResultConstants.LINK_QNAME);
                xml.attribute(XmlResultConstants.HREF_ATTR, link);
            }
            xml.end();
        } else {
            xml.empty(XmlResultConstants.HEAD_QNAME);
        }
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.XML;
    }
}
