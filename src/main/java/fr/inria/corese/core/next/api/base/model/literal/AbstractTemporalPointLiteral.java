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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractTemporalPointLiteral other = (AbstractTemporalPointLiteral) obj;
        return this.datatype.equals(other.datatype) && this.calendarValue().equals(other.calendarValue());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.datatype == null ? 0 : this.datatype.hashCode());
        hash = 31 * hash + (this.getCoreDatatype() == null ? 0 : this.getCoreDatatype().hashCode());
        hash = 31 * hash + (this.temporalAccessorValue() == null ? 0 : this.temporalAccessorValue().hashCode());
        return hash;
    }

}
