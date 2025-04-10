package fr.inria.corese.core.next.api.literal;

import fr.inria.corese.core.next.api.IRI;

/**
 * Represents a literal core datatype.
 * The core datatpye of a literal indicates how the literal should be interpreted.
 */
public interface CoreDatatype {

    /**
     * Returns the IRI of the datatype.
     * The IRI is used to identify the datatype in RDF.
     *
     * @return the IRI of the datatype
     */
    IRI getIRI();

}

