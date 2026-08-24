package fr.inria.corese.core.next.data.impl.io.parser;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.support.io.parser.AbstractRDFParserFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.data.impl.io.parser.nquads.NQuadsParser;
import fr.inria.corese.core.next.data.impl.io.parser.ntriples.NTriplesParser;
import fr.inria.corese.core.next.data.impl.io.parser.rdfa.RDFaParser;
import fr.inria.corese.core.next.data.impl.io.parser.rdfxml.RDFXMLParser;
import fr.inria.corese.core.next.data.impl.io.parser.turtle.TurtleParser;
import fr.inria.corese.core.next.data.impl.io.parser.trig.TriGParser;

/**
 * Factory class for creating RDF parsers. Generates according to the RDFFormat provided.
 */
public class DefaultRDFParserFactory extends AbstractRDFParserFactory {

    /**
     * Default constructor for DefaultRDFParserFactory.
     *
     * The constructor is protected to prevent instantiation from outside the
     * package.
     */
    public DefaultRDFParserFactory() {
        super();
    }

    /**
     * Creates an RDF parser for the given format, model, value factory, and configuration.
     * @param format The RDF format to use for parsing.
     * @param model The model to which the parsed data will be added.
     * @param factory The value factory to use for creating RDF values.
     * @param config The configuration to use for parsing.
     * @return An RDF parser for the given format, model, value factory, and configuration.
     */
    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory, IOOptions config) {
        if (RDFFormat.JSONLD.equals(format)) {
            return new JSONLDParser(model, factory, config);
        } else if (RDFFormat.TURTLE.equals(format)) {
            return new TurtleParser(model, factory, config);
        } else if (RDFFormat.NTRIPLES.equals(format)) {
            return new NTriplesParser(model, factory, config);
        } else if (RDFFormat.NQUADS.equals(format) || RDFFormat.RDFC_1_0.equals(format)) {
            return new NQuadsParser(model, factory, config);
        } else if (RDFFormat.RDFXML.equals(format)) {
            return new RDFXMLParser(model, factory, config);
        } else if (RDFFormat.TRIG.equals(format)) {
            return new TriGParser(model, factory, config);
        } else if (RDFFormat.RDFA.equals(format)) {
            return new RDFaParser(model, factory, config);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    /**
     * Creates an RDF parser for the given format, model, and value factory.
     * @param format The {@link RDFFormat} to use for parsing.
     * @param model  The {@link Model} to which the parsed data will be added.
     * @param factory The {@link ValueFactory} factory to use for creating RDF values.
     * @return An RDF parser for the given format, model, and value factory.
     */
    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        if (RDFFormat.JSONLD.equals(format)) {
            return new JSONLDParser(model, factory);
        } else if (RDFFormat.TURTLE.equals(format)) {
            return new TurtleParser(model, factory);
        } else if (RDFFormat.NTRIPLES.equals(format)) {
            return new NTriplesParser(model, factory);
        } else if (RDFFormat.NQUADS.equals(format) || RDFFormat.RDFC_1_0.equals(format)) {
            return new NQuadsParser(model, factory);
        } else if (RDFFormat.RDFXML.equals(format)) {
            return new RDFXMLParser(model, factory);
        } else if (RDFFormat.TRIG.equals(format)) {
            return new TriGParser(model, factory);
        } else if (RDFFormat.RDFA.equals(format)) {
            return new RDFaParser(model, factory);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

}