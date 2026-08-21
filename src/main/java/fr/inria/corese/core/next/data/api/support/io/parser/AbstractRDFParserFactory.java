package fr.inria.corese.core.next.data.api.support.io.parser;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.parser.RDFParserFactory;

/**
 * Abstract base class for {@link RDFParserFactory}s.
 */
public abstract class AbstractRDFParserFactory implements RDFParserFactory {

    /**
     * Default constructor for AbstractRDFParserFactory.
     *
     * The constructor is protected to prevent instantiation from outside the
     * package.
     */
    protected AbstractRDFParserFactory() {
    }

    /**
     * Creates a new RDF parser for the given RDF format and model.
     *
     * @param format  The {@link RDFFormat} to use for parsing.
     * @param model   The {@link Model} to which the parsed data will be added.
     * @param factory The {@link ValueFactory} factory to use for creating RDF
     *                values.
     * @return
     */
    @Override
    public RDFParser createRDFParser(RDFFormat format, Model model, ValueFactory factory) {
        return createRDFParser(format, model, factory, null);
    }
}
