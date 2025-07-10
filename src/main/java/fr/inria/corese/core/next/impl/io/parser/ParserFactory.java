package fr.inria.corese.core.next.impl.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
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
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory, RDFParserOptions config) {
        if(format == RDFFormat.JSONLD) {
            return new JSONLDParser(model, factory, config);
        } else if(format == RDFFormat.TURTLE) {
            return new ANTLRTurtleParser(model, factory, config);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        if(format == RDFFormat.JSONLD) {
            return new JSONLDParser(model, factory);
        } else if(format == RDFFormat.TURTLE) {
            return new ANTLRTurtleParser(model, factory);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

}
