package fr.inria.corese.core.next.api.io.parser;

/**
 * Options for RDF parsers that support a base IRI.
 */
public interface RDFParserBaseIRIOptions extends RDFParserOptions {

    /**
     *
     * @return the base IRI used to resolve relative IRIs
     */
    String getBase();
}
