package fr.inria.corese.core.next.api.model.impl.literal;

/**
 * Represents a temporal literal representing a point in time. Typically a date or a date and time. Implements the comparison between temporal literals.
 */
public abstract class AbstractTemporalPointLiteral implements AbstractLiteral, Comparable<AbstractTemporalPointLiteral> {

    @Override
    public int compareTo(AbstractTemporalPointLiteral literal) {
        return this.calendarValue().compare(literal.calendarValue());
    }

}
