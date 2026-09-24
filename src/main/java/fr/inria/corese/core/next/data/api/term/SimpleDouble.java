package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatypes;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractNumber;

/** Immutable RDF double literal preserving its lexical form. */
public final class SimpleDouble extends AbstractNumber {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final double value;
    private final CoreDatatype coreDatatype;

    public SimpleDouble(double value) {
        this(Double.toString(value), XSDDatatype.DOUBLE.getIRI(), XSDDatatype.DOUBLE);
    }

    public SimpleDouble(float value) {
        this(Float.toString(value), XSDDatatype.FLOAT.getIRI(), XSDDatatype.FLOAT);
    }

    public SimpleDouble(String lexicalValue) {
        this(lexicalValue, XSDDatatype.DOUBLE.getIRI());
    }

    public SimpleDouble(String lexicalValue, IRI datatype) {
        this(lexicalValue, datatype, null);
    }

    public SimpleDouble(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super(datatype == null ? XSDDatatype.DOUBLE.getIRI() : datatype);
        this.label = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.value = parseXsdDouble(lexicalValue);
        CoreDatatype resolved = coreDatatype != null ? coreDatatype : CoreDatatypes.from(this.datatype);
        this.coreDatatype = (resolved == XSDDatatype.FLOAT || resolved == XSDDatatype.DOUBLE) ? resolved : XSDDatatype.DOUBLE;
    }

    private static double parseXsdDouble(String s) {
        return switch (s) {
            case "INF", "+INF" -> Double.POSITIVE_INFINITY;
            case "-INF" -> Double.NEGATIVE_INFINITY;
            case "NaN" -> Double.NaN;
            default -> Double.parseDouble(s);
        };
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
        return coreDatatype;
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("SimpleDouble is immutable");
    }

    @Override
    public byte byteValue() {
        return (byte) value;
    }

    @Override
    public short shortValue() {
        return (short) value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf((long) value);
    }

    @Override
    public BigDecimal decimalValue() {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IncorrectOperationException("Cannot convert NaN/INF to BigDecimal");
        }
        return BigDecimal.valueOf(value);
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public int compareTo(AbstractNumber other) {
        return Double.compare(doubleValue(), other.doubleValue());
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
