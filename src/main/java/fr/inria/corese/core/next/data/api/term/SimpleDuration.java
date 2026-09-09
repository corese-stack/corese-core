package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    // Serialized explicitly because TemporalAmount does not extend Serializable.
    private transient TemporalAmount temporalAmount;

    @Serial
    private void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeObject(label);
        output.writeBoolean(temporalAmount instanceof Period);
    }

    @Serial
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        String lexicalValue = (String) input.readObject();
        temporalAmount = input.readBoolean() ? Period.parse(lexicalValue) : parseTemporalAmount(lexicalValue);
    }

    public SimpleDuration(String lexicalValue) {
        this(lexicalValue, XSDDatatype.DURATION.getIRI());
    }

    public SimpleDuration(TemporalAmount temporalAmount) {
        super();
        this.temporalAmount = Objects.requireNonNull(temporalAmount, "temporalAmount");
        this.label = temporalAmount.toString();
    }

    public SimpleDuration(String lexicalValue, IRI datatype) {
        super(datatype);
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
