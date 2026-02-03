package fr.inria.corese.core.next.data.io.parser;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.io.IOOptions;

/**
 * A factory for creating RDF parsers.
 */
public interface RDFParserFactory {

    /**
     * Creates a new RDF parser for the specified format and model.
     *
     * @param format The RDF format to use for parsing.
     * @param model  The model to which the parsed data will be added.
     * @return A new instance of an RDF parser for the specified format and model.
     */
    RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory);

    /**
     * Creates a new RDF parser for the specified format and model.
     * 
     * @param format  The RDF format to use for parsing.
     * @param model   The model to which the parsed data will be added.
     * @param factory The value factory to use for creating RDF values.
     * @param config  The configuration to use for parsing.
     * @return A new instance of an RDF parser for the specified format and model.
     */
    RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory, IOOptions config);

}
