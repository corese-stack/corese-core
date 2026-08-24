package fr.inria.corese.core.next.data.api.support.term.literal;

import fr.inria.corese.core.next.data.api.term.IRI;

/**
 * Represents a temporal literal representing a point in time. Typically a date or a date and time. Implements the comparison between temporal literals.
 */
public abstract class AbstractTemporalPointLiteral extends AbstractLiteral implements Comparable<AbstractTemporalPointLiteral> {

    /**
     * Constructor for AbstractTemporalPointLiteral.
     *
     * @param datatype the datatype of the temporal point literal
     */
    protected AbstractTemporalPointLiteral(IRI datatype) {
        super(datatype);
    }

    @Override
    public int compareTo(AbstractTemporalPointLiteral literal) {
        return this.calendarValue().compare(literal.calendarValue());
    }

    /**
     * Temporal ordering compares values, while RDF equality compares terms. Two
     * different lexical forms can therefore compare as equal without being the
     * same RDF literal.
     */
    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
