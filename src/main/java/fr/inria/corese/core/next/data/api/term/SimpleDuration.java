package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;

import javax.xml.datatype.DatatypeFactory;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractDuration;

/** RDF duration literal preserving its lexical form. */
public final class SimpleDuration extends AbstractDuration {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final TemporalAmount temporalAmount;

    public SimpleDuration(String lexicalValue) {
        this(lexicalValue, XSDDatatype.DURATION.getIRI(), XSDDatatype.DURATION);
    }

    public SimpleDuration(TemporalAmount temporalAmount) {
        super();
        this.temporalAmount = Objects.requireNonNull(temporalAmount, "temporalAmount");
        this.label = temporalAmount.toString();
    }

    public SimpleDuration(String lexicalValue, IRI datatype) {
        this(lexicalValue, datatype, null);
    }

    public SimpleDuration(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super();
        this.label = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.temporalAmount = parseTemporalAmount(lexicalValue);
    }

    private static TemporalAmount parseTemporalAmount(String lexicalValue) {
        try {
            return Duration.parse(lexicalValue);
        } catch (DateTimeParseException durationError) {
            try {
                return Period.parse(lexicalValue);
            } catch (DateTimeParseException periodError) {
                // Mixed XML durations have no equivalent Java Duration or Period.
                // Validate them here; the inherited accessor reports unsupported conversion.
                DatatypeFactory.newDefaultInstance().newDuration(lexicalValue);
                return null;
            }
        }
    }

    @Override
    public TemporalAmount temporalAmountValue() {
        if (temporalAmount != null) {
            return temporalAmount;
        }
        return super.temporalAmountValue();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String stringValue() {
        return label;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DURATION;
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("SimpleDuration is immutable");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Literal other)) {
            return false;
        }
        return label.equals(other.getLabel())
                && datatype.equals(other.getDatatype())
                && Objects.equals(getLanguage(), other.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, datatype, getLanguage());
    }
}
