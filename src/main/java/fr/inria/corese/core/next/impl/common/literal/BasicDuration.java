package fr.inria.corese.core.next.impl.common.literal;

import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.api.base.model.literal.AbstractDuration;
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
 * A basic implementation of the {@link Literal} interface for durations.
 * As there are no Java class available to represent duration as defined in the XSD specification, this class uses its own representation of durations using the {@link TemporalAmount} interface.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema-2/#duration">XSD duration</a>
 */
public class BasicDuration extends AbstractDuration {

    private static final Logger logger = LoggerFactory.getLogger(BasicDuration.class);
    private final XSDDuration temporalAmount;

    /**
     * A list of temporal units that contains all ChronoUnit covered by the XSD duration.
     */
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
                        "(\\.(?<nano>\\d*[1-9]+\\d*)?)+S)?" +
                    ")?"
    );

    /**
     * Constructor for BasicDuration.
     *
     * @param temporalAmount  the {@link TemporalAmount} representing the duration
     */
    public BasicDuration(TemporalAmount temporalAmount) {
        this.temporalAmount = new XSDDuration(temporalAmount);
    }

    /**
     * The string value of the duration. Uses the string representation of the {@link TemporalAmount} to create a string representation of the duration.
     */
    @Override
    public String stringValue() {
        return String.format("\"%s\"^^<%s>", this.temporalAmount.toString(), this.datatype.stringValue());
    }

    @Override
    public String getLabel() {
        return this.temporalAmount.toString();
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

    /**
     * Creates a new {@link BasicDuration} object from a string representation of a duration. Uses a regular expression to parse the string and extract the duration components.
     * @param durationString the string representation of the duration. Expectes a string in the format "PnYnMnDTnHnMnS" where n is a number and P, Y, M, D, T, H, M, S are the duration components.
     * @return a new {@link BasicDuration} object representing the duration
     */
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
     * This class is necessary to be able to represent a duration in a way that is compatible with the XSD specification.
     */
    private static class XSDDuration implements TemporalAmount {
        private final Map<ChronoUnit, Long> values;
        private boolean isNegative;

        /**
         * Default constructor for XSDDuration.
         */
        public XSDDuration() {
            this.values = new EnumMap<>(ChronoUnit.class);
            this.isNegative = false;
        }

        /**
         * Constructor for XSDDuration.
         *
         * @param temporalAmount  the {@link TemporalAmount} representing the duration
         */
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
         * @param temporalUnit Expected to be a ChronoUnit, see {@link ChronoUnit}.
         * @return 0 by default.
         */
        @Override
        public long get(TemporalUnit temporalUnit) {
            return values.getOrDefault(temporalUnit, 0L);
        }

        /**
         * Sets the value of the given temporal unit.
         * @param temporalUnit the temporal unit to set, see {@link ChronoUnit}.
         * @param value the value to set
         */
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

            if(get(YEARS) != 0) {
                builder.append(get(YEARS)).append("Y");
            }

            if(get(MONTHS) != 0) {
                builder.append(get(MONTHS)).append("M");
            }

            if(get(DAYS) != 0) {
                builder.append(get(DAYS)).append("D");
            }

            if(get(HOURS) != 0 || get(MINUTES) != 0 || get(SECONDS) != 0 || get(NANOS) != 0) {
                builder.append("T");

                if(get(HOURS) != 0) {
                    builder.append(get(HOURS)).append("H");
                }

                if(get(MINUTES) != 0) {
                    builder.append(get(MINUTES)).append("M");
                }

                if(get(SECONDS) != 0 || get(NANOS) != 0) {
                    builder.append(get(SECONDS));

                    if(get(NANOS) != 0) {
                        String nanosString = String.valueOf(get(NANOS));
                        nanosString = nanosString.replaceAll("0+$", ""); // removing the zeros at the end
                        builder.append(".").append(nanosString);
                    }

                    builder.append("S");
                }
            }

            return builder.toString();
        }

        /**
         * Sets the duration to negative.
         */
        public void setNegative() {
            this.isNegative = true;
        }

        /**
         * @return true if the duration is negative, false otherwise.
         */
        public boolean isNegative() {
            return this.isNegative;
        }
    }
}
