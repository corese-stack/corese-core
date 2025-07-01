package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.api.base.parser.RDFParserFactory;

public class RdfXmlParserFactory implements RDFParserFactory {

    public RdfXmlParserFactory() {
        super();
    }

    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        if (!format.equals(RDFFormats.RDF_XML)) {
            throw new IllegalArgumentException("Unsupported format : " + format);
        }
        return new RdfXmlParser(model, factory);
    }
}