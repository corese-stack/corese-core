package fr.inria.corese.core.next.impl.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.parser.RDFFormat;
import fr.inria.corese.core.next.api.base.parser.RDFFormats;
import fr.inria.corese.core.next.api.base.parser.RDFParser;

import java.io.InputStream;
import java.io.Reader;

public class TurtleParser implements RDFParser {

    private final Model model;
    private final RDFFormat format = RDFFormats.TURTLE;

    public TurtleParser(Model model) {
        this.model = model;
    }


    @Override
    public RDFFormat getRDFFormat() {
        return format;
    }

    @Override
    public void parse(InputStream in) {

    }

    @Override
    public void parse(InputStream in, String baseURI) {

    }

    @Override
    public void parse(Reader reader) {

    }

    @Override
    public void parse(Reader reader, String baseURI) {

    }
}
