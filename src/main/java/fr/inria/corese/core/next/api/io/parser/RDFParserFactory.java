package fr.inria.corese.core.next.api.io.parser;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.IOConfig;

public interface RDFParserFactory {


    /**
     * Creates a new RDF parser for the specified format and model.
     *
     * @param format The RDF format to use for parsing.
     * @param model  The model to which the parsed data will be added.
     * @return A new instance of an RDF parser for the specified format and model.
     */
    RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory);

    /**
     * Creates a new RDF parser for the specified format and model.
     * @param format The RDF format to use for parsing.
     * @param model The model to which the parsed data will be added.
     * @param factory The value factory to use for creating RDF values.
     * @param config The configuration to use for parsing.
     * @return A new instance of an RDF parser for the specified format and model.
     */
    RDFParser createRDFParser(RdfFormat format, Model model, ValueFactory factory, IOConfig config);

}
