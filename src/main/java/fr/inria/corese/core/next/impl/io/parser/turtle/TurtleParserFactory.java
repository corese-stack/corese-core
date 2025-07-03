package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.api.base.parser.RDFParserFactory;

public class TurtleParserFactory implements RDFParserFactory {

    public TurtleParserFactory() {
        super();
    }

    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        if (!format.equals(RDFFormats.TURTLE)) {
            throw new IllegalArgumentException("Unsupported format : " + format);
        }
        return new ANTLRTurtleParser(model, factory);
    }
}
