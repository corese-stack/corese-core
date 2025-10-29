package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.io.Reader;

public class RDFaParser extends AbstractRDFParser {
    protected RDFaParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    protected RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFa;
    }

    @Override
    public void parse(InputStream in, String baseURI) {
    }

    @Override
    public void parse(Reader reader, String baseURI) {

    }
}
