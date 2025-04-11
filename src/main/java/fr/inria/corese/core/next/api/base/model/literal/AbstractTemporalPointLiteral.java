package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;

/**
 * Represents a temporal literal representing a point in time. Typically a date or a date and time. Implements the comparison between temporal literals.
 */
public abstract class AbstractTemporalPointLiteral extends AbstractLiteral implements Comparable<AbstractTemporalPointLiteral> {

    protected AbstractTemporalPointLiteral(IRI datatype) {
        super(datatype);
    }

    @Override
    public int compareTo(AbstractTemporalPointLiteral literal) {
        return this.calendarValue().compare(literal.calendarValue());
    }

}
