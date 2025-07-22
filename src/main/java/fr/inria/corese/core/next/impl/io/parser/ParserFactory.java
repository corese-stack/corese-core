package fr.inria.corese.core.next.impl.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParserFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.io.parser.jsonld.JSONLDParser;
import fr.inria.corese.core.next.impl.io.parser.nquads.ANTLRNQuadsParser;
import fr.inria.corese.core.next.impl.io.parser.ntriples.ANTLRNTriplesParser;
import fr.inria.corese.core.next.impl.io.parser.rdfxml.RdfXmlParser;
import fr.inria.corese.core.next.impl.io.parser.turtle.ANTLRTurtleParser;

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
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory, RDFParserOptions config) {
        if (format == RDFFormat.JSONLD) {
            return new JSONLDParser(model, factory, config);
        } else if (format == RDFFormat.TURTLE) {
            return new ANTLRTurtleParser(model, factory, config);
        } else if (format == RDFFormat.NTRIPLES) {
            return new ANTLRNTriplesParser(model, factory, config);
        } else if (format == RDFFormat.NQUADS) {
            return new ANTLRNQuadsParser(model, factory, config);
        } else if (format == RDFFormat.RDFXML) {
            return new RdfXmlParser(model, factory, config);
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
            return new ANTLRTurtleParser(model, factory);
        } else if (format == RDFFormat.NTRIPLES) {
            return new ANTLRNTriplesParser(model, factory);
        } else if (format == RDFFormat.NQUADS) {
            return new ANTLRNQuadsParser(model, factory);
        } else if (format == RDFFormat.RDFXML) {
            return new RdfXmlParser(model, factory);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

}
