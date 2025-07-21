package fr.inria.corese.core.next.api.io.parser;

import fr.inria.corese.core.next.api.io.IOOptions;

/**
 * Options for RDF parsers that support a base IRI.
 */
public interface RDFParserBaseIRIOptions extends RDFParserOptions, IOOptions {

    /**
     *
     * @return the base IRI used to resolve relative IRIs
     */
    String getBase();
}
