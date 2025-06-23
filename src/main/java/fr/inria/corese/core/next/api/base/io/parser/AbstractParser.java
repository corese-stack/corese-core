package fr.inria.corese.core.next.api.base.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.InputStream;
import java.io.Reader;

public abstract class AbstractParser implements RDFParser {

    private final Model model;
    private final ValueFactory valueFactory;

    protected AbstractParser(Model model, ValueFactory factory) {
            this.model = model;
            this.valueFactory = factory;

    }

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        parse(in, null);
    }

    @Override
    public void parse(Reader reader) throws ParsingErrorException {
        parse(reader, null);
    }

    protected Model getModel() {
        return model;
    }

    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
}
