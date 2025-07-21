package fr.inria.corese.core.next.api.base.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

/**
 * Abstract class for RDFParser that set up the inner Model and ValueFactory
 */
public abstract class AbstractRDFParser implements RDFParser {

    private final Model model;
    private final ValueFactory valueFactory;
    private IOOptions config;

    public IOOptions getConfig() {
        return config;
    }

    public void setConfig(IOOptions config) {
        this.config = config;
    }

    protected AbstractRDFParser(Model model, ValueFactory factory) {
            this(model, factory, null);
    }

    protected AbstractRDFParser(Model model, ValueFactory factory, IOOptions config) {
        Objects.requireNonNull(model);
        Objects.requireNonNull(factory);
        this.model = model;
        this.valueFactory = factory;
        this.config = config;
    }

    @Override
    public void parse(InputStream in) {
        parse(in, null);
    }

    @Override
    public void parse(Reader reader) {
        parse(reader, null);
    }

    /**
     * @return the model populated by the parser
     */
    protected Model getModel() {
        return model;
    }

    /**
     * @return the value factory used by the parser
     */
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
}
