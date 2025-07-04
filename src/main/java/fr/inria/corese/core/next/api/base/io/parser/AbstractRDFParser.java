package fr.inria.corese.core.next.api.base.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.InputStream;
import java.io.Reader;

public abstract class AbstractRDFParser implements RDFParser {

    private final Model model;
    private final ValueFactory valueFactory;
    private RDFParserOptions config;

    public RDFParserOptions getConfig() {
        return config;
    }

    public void setConfig(RDFParserOptions config) {
        this.config = config;
    }

    protected AbstractRDFParser(Model model, ValueFactory factory) {
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
