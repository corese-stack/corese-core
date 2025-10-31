package fr.inria.corese.core.next.api.io.common;

import fr.inria.corese.core.next.api.io.IOOptions;

/**
 * Options for RDF parsers and serializers that support a base IRI.
 */
public interface BaseIRIOptions  {

    /**
     * @return the base IRI used to resolve relative IRIs
     */
    String getBaseIRI();
}
