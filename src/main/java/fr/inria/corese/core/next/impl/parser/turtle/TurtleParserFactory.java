package fr.inria.corese.core.next.impl.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;
import fr.inria.corese.core.next.api.base.parser.RDFParserFactory;

public class TurtleParserFactory implements RDFParserFactory {

    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model) {
        if (!format.equals(RDFFormats.TURTLE)) {
            throw new IllegalArgumentException("Unsupported format : " + format);
        }
        return new ANTLRTurtleParser(model);
    }
}
