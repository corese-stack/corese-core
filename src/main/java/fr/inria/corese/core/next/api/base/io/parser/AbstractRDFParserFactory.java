package fr.inria.corese.core.next.api.base.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserFactory;

public abstract class AbstractRDFParserFactory implements RDFParserFactory {

    protected AbstractRDFParserFactory() {
    }

    @Override
    public RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory) {
        return createRDFParser(format, model, factory, null);
    }
}
