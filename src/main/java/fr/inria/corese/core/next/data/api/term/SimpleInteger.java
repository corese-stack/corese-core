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
import fr.inria.corese.core.next.data.spi.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractNumber;

/** Immutable RDF integer literal preserving its lexical form. */
public final class SimpleInteger extends AbstractNumber {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final BigInteger value;
    private final CoreDatatype coreDatatype;

    public SimpleInteger(long value) {
        this(BigInteger.valueOf(value));
    }

    public SimpleInteger(BigInteger value) {
        super(XSDDatatype.INTEGER.getIRI());
        this.value = Objects.requireNonNull(value, "value");
        this.label = value.toString();
        this.coreDatatype = XSDDatatype.INTEGER;
    }

    public SimpleInteger(String lexicalValue) {
        this(lexicalValue, XSDDatatype.INTEGER.getIRI());
    }

    public SimpleInteger(String lexicalValue, IRI datatype) {
        this(lexicalValue, datatype, null);
    }

    public SimpleInteger(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super(datatype == null ? XSDDatatype.INTEGER.getIRI() : datatype);
        this.label = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.value = new BigInteger(lexicalValue);
        CoreDatatype resolved = coreDatatype != null ? coreDatatype : CoreDatatypes.from(this.datatype);
        this.coreDatatype = AbstractLiteral.isIntegerCoreDatatype(resolved) ? resolved : XSDDatatype.INTEGER;
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
        throw new IncorrectOperationException("SimpleInteger is immutable");
    }

    @Override
    public byte byteValue() {
        return value.byteValue();
    }

    @Override
    public short shortValue() {
        return value.shortValue();
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public BigInteger integerValue() {
        return value;
    }

    @Override
    public BigDecimal decimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public int compareTo(AbstractNumber other) {
        if (other instanceof SimpleInteger number) {
            return value.compareTo(number.value);
        }
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
