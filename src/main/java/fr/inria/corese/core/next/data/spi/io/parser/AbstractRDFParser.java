package fr.inria.corese.core.next.data.spi.io.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;

/**
 * Abstract class for RDFParser that set up the inner Model and ValueFactory
 */
public abstract class AbstractRDFParser implements RDFParser {

    private final Model model;
    private final ValueFactory valueFactory;
    private RDFParsingOptions config;

    /**
     * Gets the configuration options for the parser.
     *
     * @return the configuration options
     */
    public RDFParsingOptions getConfig() {
        return config;
    }

    /**
     * Sets the configuration options for the parser.
     *
     * @param config the configuration options to be set
     */
    public void setConfig(RDFParsingOptions config) {
        this.config = config;
    }

    /**
     * Constructor for AbstractRDFParser that initializes the model and value
     * factory. The created object uses empty RDF parsing options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     */
    protected AbstractRDFParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFParsingOptions() {
        });
    }

    /**
     * Constructor for AbstractRDFParser that initializes the model, value factory,
     * and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param config  optional configuration options for the parser
     */
    protected AbstractRDFParser(Model model, ValueFactory factory, RDFParsingOptions config) {
        Objects.requireNonNull(model);
        Objects.requireNonNull(factory);
        Objects.requireNonNull(config);
        this.model = model;
        this.valueFactory = factory;
        this.config = config;
    }

    @Override
    public void parse(InputStream in) throws ParsingException {
        if (getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            String baseIRI = baseIRIOptions.getBaseIRI();
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseIRI);
        } else {
            parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
        }
    }

    @Override
    public void parse(Reader reader) throws ParsingException {
        if (getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            String baseIRI = baseIRIOptions.getBaseIRI();
            parse(reader, baseIRI);
        } else {
            parse(reader, null);
        }
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
