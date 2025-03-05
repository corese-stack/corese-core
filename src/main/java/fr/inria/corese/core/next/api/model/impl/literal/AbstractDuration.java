package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import java.time.DateTimeException;
import java.time.temporal.TemporalUnit;
import java.util.*;

public abstract class AbstractDuration extends AbstractLiteral implements Comparable<AbstractDuration> {

    protected AbstractDuration() {
        super(XSD.xsdDuration.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.xsdDuration;
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
                thisValue = this.temporalAmountValue().get(tu);
            } catch (DateTimeException e) {
                // This unit is not present in this object, so it must be present in o, making o larger
                return -1;
            }
            try {
                otherValue = o.temporalAmountValue().get(tu);
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
        return Objects.hash(this.temporalAmountValue());
    }

}
