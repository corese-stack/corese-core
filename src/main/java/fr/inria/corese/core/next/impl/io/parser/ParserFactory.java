package fr.inria.corese.core.next.impl.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParserFactory;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.impl.io.parser.nquads.NQuadsParser;
import fr.inria.corese.core.next.impl.io.parser.ntriples.NTriplesParser;
import fr.inria.corese.core.next.impl.io.parser.rdfa.RDFaParser;
import fr.inria.corese.core.next.impl.io.parser.rdfxml.RDFXMLParser;
import fr.inria.corese.core.next.impl.io.parser.turtle.TurtleParser;
import fr.inria.corese.core.next.impl.io.parser.trig.TriGParser;

/**
 * Factory class for creating RDF parsers. Generates according to the RDFFormat provided.
 */
public class ParserFactory extends AbstractRDFParserFactory {

    /**
     * Default constructor for ParserFactory.
     * 
     * The constructor is protected to prevent instantiation from outside the
     * package.
     */
    public ParserFactory() {
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
        if (format == RDFFormat.JSONLD) {
            return new JSONLDParser(model, factory, config);
        } else if (format == RDFFormat.TURTLE) {
            return new TurtleParser(model, factory, config);
        } else if (format == RDFFormat.NTRIPLES) {
            return new NTriplesParser(model, factory, config);
        } else if (format == RDFFormat.NQUADS) {
            return new NQuadsParser(model, factory, config);
        } else if (format == RDFFormat.RDFXML) {
            return new RDFXMLParser(model, factory, config);
        } else if (format == RDFFormat.TRIG) {
             return new TriGParser(model, factory, config);
        } else if(format == RDFFormat.RDFC_1_0) {
            return new NQuadsParser(model, factory, config);
        } else if (format == RDFFormat.RDFA) {
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
        if (format == RDFFormat.JSONLD) {
            return new JSONLDParser(model, factory);
        } else if (format == RDFFormat.TURTLE) {
            return new TurtleParser(model, factory);
        } else if (format == RDFFormat.NTRIPLES) {
            return new NTriplesParser(model, factory);
        } else if (format == RDFFormat.NQUADS) {
            return new NQuadsParser(model, factory);
        } else if (format == RDFFormat.RDFXML) {
            return new RDFXMLParser(model, factory);
        } else if (format == RDFFormat.TRIG) {
            return new TriGParser(model, factory);
        } else if (format == RDFFormat.RDFA) {
            return new RDFaParser(model, factory);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

}