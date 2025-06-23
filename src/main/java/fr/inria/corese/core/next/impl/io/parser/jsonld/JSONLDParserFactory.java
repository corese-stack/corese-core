package fr.inria.corese.core.next.impl.io.parser.jsonld;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserFactory;

public class JSONLDParserFactory implements RDFParserFactory {

    private static final JSONLDParserFactory INSTANCE = new JSONLDParserFactory();

    public static JSONLDParserFactory getInstance() {
        return INSTANCE;
    }

    private JSONLDParserFactory() {

    }

    @Override
    public RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory) {
        if(format == RdfFormat.JSONLD) {
            return new JSONLDParser(model, factory);
        }
        return null;
    }
}
