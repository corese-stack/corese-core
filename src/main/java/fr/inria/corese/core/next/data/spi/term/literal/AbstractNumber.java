package fr.inria.corese.core.next.data.spi.term.literal;

import fr.inria.corese.core.next.data.api.term.IRI;

/**
 * Abstract class representing a number literal in RDF.
 */
public abstract class AbstractNumber extends AbstractLiteral implements Comparable<AbstractNumber> {


    /**
     * Constructor for AbstractNumber.
     *
     * @param datatype the datatype of the number literal
     */
    protected AbstractNumber(IRI datatype) {
        super(datatype);
    }
}
