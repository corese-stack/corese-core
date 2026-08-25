package fr.inria.corese.core.next.data.api.support.term.literal;

import java.time.DateTimeException;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

/**
 * Abstract class representing a duration literal in RDF.
 */
public abstract class AbstractDuration extends AbstractLiteral implements Comparable<AbstractDuration> {

    /**
     * Constructor for AbstractDuration.
     */
    protected AbstractDuration() {
        super(XSDDatatype.DURATION.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DURATION;
    }

    /**
     * Comparison between two temporal literals using their temporal amount values.
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
     * Duration ordering compares values, while RDF equality compares terms. Two
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
