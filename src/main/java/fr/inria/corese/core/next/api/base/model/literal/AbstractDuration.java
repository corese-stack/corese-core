package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.literal.CoreDatatype;

import java.time.DateTimeException;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;

/**
 * Abstract class representing a duration literal in RDF.
 */
public abstract class AbstractDuration extends AbstractLiteral implements Comparable<AbstractDuration> {

    /**
     * Constructor for AbstractDuration.
     */
    protected AbstractDuration() {
        super(XSD.DURATION.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DURATION;
    }

    /**
     * Comparison between two temporal literals using their temporal amount values.
     * @param o
     * @return -1 if this is less than o, 0 if they are equal, 1 if this is greater than o
     */
    @Override
    public int compareTo(AbstractDuration o) {
        SortedSet<TemporalUnit> theseTUnits = new TreeSet<>(Comparator.comparing(TemporalUnit::getDuration));
        theseTUnits.addAll(this.temporalAmountValue().getUnits());
        SortedSet<TemporalUnit> otherTUnits = new TreeSet<>(Comparator.comparing(TemporalUnit::getDuration));
        otherTUnits.addAll(o.temporalAmountValue().getUnits());

        TemporalAmount thisTemporalAmount = this.temporalAmountValue();
        TemporalAmount otherTemporalAmount = o.temporalAmountValue();

        // Check if the temporal amounts have some units in common
        Set<TemporalUnit> intersection = new HashSet<>(theseTUnits);
        intersection.retainAll(otherTUnits);
        if(intersection.isEmpty()) {
            // If the temporal amounts have no units in common, compare the units of highest order
            return theseTUnits.first().getDuration().compareTo(otherTUnits.first().getDuration());
        }

        // Get all units and sort them by duration
        SortedSet<TemporalUnit> allTU = new TreeSet<>(Comparator.comparing(TemporalUnit::getDuration));
        allTU.addAll(theseTUnits);
        allTU.addAll(otherTUnits);

        // Compare the values of the temporal amounts starting with the largest temporal amount
        for(TemporalUnit tu : allTU) {
            long thisValue = 0;
            long otherValue = 0;
            try {
                thisValue = thisTemporalAmount.get(tu);
            } catch (DateTimeException e) {
                // This unit is not present in this object, so it must be present in o, making o larger
                return -1;
            }
            try {
                otherValue = otherTemporalAmount.get(tu);
            } catch (DateTimeException e) {
                // This unit is not present in o, so it must be present in this object, making this object larger
                return 1;
            }
            if(thisValue != otherValue) {
                return Long.compare(thisValue, otherValue) > 0 ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     * Check if two temporal literals are equal.
     * @param obj the object to compare with
     * @return true if compareTo returns 0, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        AbstractDuration other = (AbstractDuration) obj;
        return this.compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.datatype == null ? 0 : this.datatype.hashCode());
        hash = 31 * hash + (this.getCoreDatatype() == null ? 0 : this.getCoreDatatype().hashCode());
        hash = 31 * hash + (this.temporalAmountValue() == null ? 0 : this.temporalAmountValue().hashCode());
        return hash;
    }

}
