package fr.inria.corese.core.next.impl.parser.jsonld;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.parser.RDFFormat;
import fr.inria.corese.core.next.api.parser.RDFFormats;
import fr.inria.corese.core.next.api.parser.RDFParser;
import fr.inria.corese.core.next.api.parser.RDFParserFactory;

public class JSONLDParserFactory implements RDFParserFactory {
    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        if(format == RDFFormats.JSON_LD) {
            return new JSONLDParser(model, factory);
        }
        return null;
    }
}
