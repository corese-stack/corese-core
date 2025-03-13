package fr.inria.corese.core.next.api.model.impl.basic.literal;

import fr.inria.corese.core.next.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.base.CoreDatatype.XSD;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.*;

/**
 * A basic implementation of the {@link fr.inria.corese.core.next.api.model.Literal} interface for durations.
 * As there are no Java class available to represent duration as defined in the XSD specification, this class uses its own representation of durations using the {@link TemporalAmount} interface.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema-2/#duration">XSD duration</a>
 */
public class BasicDuration extends AbstractDuration {

    private static final Logger logger = LoggerFactory.getLogger(BasicDuration.class);
    private final XSDDuration temporalAmount;

    private static final List<TemporalUnit> UNITS = new ArrayList<>(EnumSet.of(
            YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, NANOS
    ));

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?<sign>-?)P" +
                    "((?<year>\\d+)Y)?" +
                    "((?<month>\\d+)M)?" +
                    "(((?<day>\\d+))D)?" +
                    "(?<time>T" +
                        "((?<hour>\\d+)H)?" +
                        "((?<minute>\\d+)M)?" +
                        "((?<second>\\d+)" +
                        "(\\.(?<nano>\\d+)?)+S)?" +
                    ")?"
    );

    public BasicDuration(TemporalAmount temporalAmount) {
        this.temporalAmount = new XSDDuration(temporalAmount);
    }

    @Override
    public String stringValue() {
        return this.temporalAmount.toString();
    }

    @Override
    public String getLabel() {
        return this.stringValue();
    }

    @Override
    public TemporalAmount temporalAmountValue() {
        return this.temporalAmount;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DURATION;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set the core datatype of a duration.");
    }

    public static BasicDuration parse(String durationString) {
        Matcher matcher = DURATION_PATTERN.matcher(durationString);
        XSDDuration xsdDuration = new XSDDuration();

        if(!matcher.matches()) {
            throw new IncorrectFormatException("Invalid duration string: " + durationString);
        }

        if(matcher.group("year") == null && matcher.group("month") == null && matcher.group("day") == null && matcher.group("hour") == null && matcher.group("minute") == null && matcher.group("second") == null) {
            throw new IncorrectFormatException("No duration found in string: " + durationString);
        }

        if(matcher.group("sign") != null && matcher.group("sign").equals("-")) {
            xsdDuration.setNegative();
        }

        if(matcher.group("year") != null) {
            xsdDuration.set(YEARS, Long.parseLong(matcher.group("year")));
        }

        if(matcher.group("month") != null) {
            xsdDuration.set(MONTHS, Long.parseLong(matcher.group("month")));
        }

        if(matcher.group("day") != null) {
            xsdDuration.set(DAYS, Long.parseLong(matcher.group("day")));
        }

        if(matcher.group("hour") != null) {
            xsdDuration.set(HOURS, Long.parseLong(matcher.group("hour")));
        }

        if(matcher.group("minute") != null) {
            xsdDuration.set(MINUTES, Long.parseLong(matcher.group("minute")));
        }

        if(matcher.group("second") != null) {
            BigDecimal seconds = new BigDecimal(matcher.group("second"));
            xsdDuration.set(SECONDS, seconds.longValue());
        }

        if(matcher.group("nano") != null) {
            BigDecimal nanos = new BigDecimal(matcher.group("nano"));
            nanos = nanos.movePointRight(9); // Nanosecond is 10^-9 of a second
            xsdDuration.set(NANOS, nanos.longValue());
        }

        // The time part of the duration should not be empty if there is a 'T' in the duration string
        if(matcher.group("time") != null && xsdDuration.get(HOURS) == 0 && xsdDuration.get(MINUTES) == 0 && xsdDuration.get(SECONDS) == 0 && xsdDuration.get(NANOS) == 0) {
            throw new IncorrectFormatException("Time part of duration is empty: " + durationString);
        }

        return new BasicDuration(xsdDuration);
    }

    /**
     * A class to represent a duration as defined in the XSD specification.
     * The specificity of this class compared to existing ones in Java is that a duration can be negative and that it can have a precision from years to nanoseconds.
     */
    private static class XSDDuration implements TemporalAmount {
        private final Map<ChronoUnit, Long> values;
        private boolean isNegative;

        public XSDDuration() {
            this.values = new EnumMap<>(ChronoUnit.class);
            this.isNegative = false;
        }

        public XSDDuration(TemporalAmount temporalAmount) {
            this();
            for(TemporalUnit unit : temporalAmount.getUnits()) {
                values.put((ChronoUnit) unit, temporalAmount.get(unit));
            }
            if(temporalAmount instanceof XSDDuration) {
                this.isNegative = ((XSDDuration) temporalAmount).isNegative;
            }
        }

        /**
         * @param temporalUnit Expected to be a ChronoUnit.
         * @return 0 by default.
         */
        @Override
        public long get(TemporalUnit temporalUnit) {
            return values.getOrDefault(temporalUnit, 0L);
        }

        private void set(ChronoUnit temporalUnit, long value) {
            values.put(temporalUnit, value);
        }

        @Override
        public List<TemporalUnit> getUnits() {
            return UNITS;
        }

        @Override
        public Temporal addTo(Temporal temporal) {
            if(this.isNegative) {
                return subtractFromRegardlessOfSign(temporal);
            } else {
                return addToRegardlessOfSign(temporal);
            }
        }

        @Override
        public Temporal subtractFrom(Temporal temporal) {
            if(this.isNegative) {
                return addToRegardlessOfSign(temporal);
            } else {
                return subtractFromRegardlessOfSign(temporal);
            }
        }

        /**
         * Adds the values of this duration to the given temporal, regardless of the sign of the duration.
         * @param temporal the temporal to add the values to
         * @return the temporal with the values of this duration added to it
         */
        private Temporal addToRegardlessOfSign(Temporal temporal) {
            for(Map.Entry<ChronoUnit, Long> entry : values.entrySet()) {
                if(entry.getValue() != 0 && temporal.isSupported(entry.getKey())) {
                    temporal = temporal.plus(entry.getValue(), entry.getKey());
                } else if (entry.getValue() != 0 && !temporal.isSupported(entry.getKey())) {
                    throw new IncorrectOperationException("Temporal unit " + entry.getKey() + " is not supported by the temporal object");
                }
            }
            return temporal;
        }

        /**
         * Subtracts the values of this duration from the given temporal, regardless of the sign of the duration.
         * @param temporal the temporal to subtract the values from
         * @return the temporal with the values of this duration subtracted from it
         */
        private Temporal subtractFromRegardlessOfSign(Temporal temporal) {
            for(Map.Entry<ChronoUnit, Long> entry : values.entrySet()) {
                if(entry.getValue() != 0 && temporal.isSupported(entry.getKey())) {
                    temporal = temporal.minus(entry.getValue(), entry.getKey());
                } else if (entry.getValue() != 0 && !temporal.isSupported(entry.getKey())) {
                    throw new IncorrectOperationException("Temporal unit " + entry.getKey() + " is not supported by the temporal object");
                }
            }
            return temporal;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }

            if(!(obj instanceof TemporalAmount)) {
                return false;
            }

            TemporalAmount other = (TemporalAmount) obj;
            for(TemporalUnit unit : UNITS) {
                if(this.get(unit) != other.get(unit)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }

        /**
         * Returns the XSD duration format of the duration.
         * @return
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("");

            if(isNegative()) {
                builder.append("-");
            }

            builder.append("P");

            if(values.containsKey(YEARS) && get(YEARS) != 0) {
                builder.append(get(YEARS)).append("Y");
            }

            if(values.containsKey(MONTHS) && get(MONTHS) != 0) {
                builder.append(get(MONTHS)).append("M");
            }

            if(values.containsKey(DAYS) && get(DAYS) != 0) {
                builder.append(get(DAYS)).append("D");
            }

            if((values.containsKey(HOURS) || values.containsKey(MINUTES) || values.containsKey(SECONDS) || values.containsKey(NANOS)) && (get(HOURS) != 0 || get(MINUTES) != 0 || get(SECONDS) != 0 || get(NANOS) != 0)) {
                builder.append("T");

                if(values.containsKey(HOURS) && get(HOURS) != 0) {
                    builder.append(get(HOURS)).append("H");
                }

                if(values.containsKey(MINUTES) && get(MINUTES) != 0) {
                    builder.append(get(MINUTES)).append("M");
                }

                if(values.containsKey(SECONDS) && get(SECONDS) != 0 || values.containsKey(NANOS) && get(NANOS) != 0) {
                    builder.append(get(SECONDS));

                    if(values.containsKey(NANOS) && get(NANOS) != 0) {
                        String nanosString = String.valueOf(get(NANOS));
                        nanosString = nanosString.replaceAll("0+$", ""); // removing the zeros at the end
                        builder.append(".").append(nanosString);
                    }

                    builder.append("S");
                }
            }

            return builder.toString();
        }

        public void setNegative() {
            this.isNegative = true;
        }

        public boolean isNegative() {
            return this.isNegative;
        }
    }
}
