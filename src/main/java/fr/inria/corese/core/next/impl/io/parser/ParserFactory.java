package fr.inria.corese.core.next.impl.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParserFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.impl.io.parser.turtle.ANTLRTurtleParser;

public class ParserFactory extends AbstractRDFParserFactory {

    public ParserFactory() {
        super();
    }

    @Override
    public RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory, RDFParserOptions config) {
        if(format == RdfFormat.JSONLD) {
            return new JSONLDParser(model, factory, config);
        } else if(format == RdfFormat.TURTLE) {
            return new ANTLRTurtleParser(model, factory);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }
}
