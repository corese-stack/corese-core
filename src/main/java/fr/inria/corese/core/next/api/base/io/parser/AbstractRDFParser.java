package fr.inria.corese.core.next.api.base.io.parser;

import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParser;

/**
 * Abstract class for RDFParser that set up the inner Model and ValueFactory
 */
public abstract class AbstractRDFParser implements RDFParser {

    private final Model model;
    private final ValueFactory valueFactory;
    private IOOptions config;

    /**
     * Gets the configuration options for the parser.
     *
     * @return the configuration options
     */
    public IOOptions getConfig() {
        return config;
    }

    /**
     * Sets the configuration options for the parser.
     *
     * @param config the configuration options to be set
     */
    public void setConfig(IOOptions config) {
        this.config = config;
    }

    /**
     * Constructor for AbstractRDFParser that initializes the model and value
     * factory.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     */
    protected AbstractRDFParser(Model model, ValueFactory factory) {
        this(model, factory, null);
    }

    /**
     * Constructor for AbstractRDFParser that initializes the model, value factory,
     * and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param config  optional configuration options for the parser
     */
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
