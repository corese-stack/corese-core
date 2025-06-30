package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.IOConfig;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserFactory;

public class TurtleParserFactory implements RDFParserFactory {

    public TurtleParserFactory() {
        super();
    }

    @Override
    public RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory) {
        if (!format.equals(RdfFormat.TURTLE)) {
            throw new IllegalArgumentException("Unsupported format : " + format);
        }
        return new ANTLRTurtleParser(model, factory);
    }

    @Override
    public RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory, IOConfig config) {
        RDFParser parser = createRDFParser(format, model, factory);
        parser.setConfig(config);
        return parser;
    }
}
