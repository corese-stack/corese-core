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

/** Immutable RDF decimal literal preserving its lexical form. */
public final class SimpleDecimal extends AbstractNumber {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final BigDecimal value;
    private final CoreDatatype coreDatatype;

    public SimpleDecimal(BigDecimal value) {
        super(XSDDatatype.DECIMAL.getIRI());
        this.value = Objects.requireNonNull(value, "value");
        this.label = value.toPlainString();
        this.coreDatatype = XSDDatatype.DECIMAL;
    }

    public SimpleDecimal(double value) {
        this(BigDecimal.valueOf(value));
    }

    public SimpleDecimal(String lexicalValue) {
        this(lexicalValue, XSDDatatype.DECIMAL.getIRI());
    }

    public SimpleDecimal(String lexicalValue, IRI datatype) {
        this(lexicalValue, datatype, null);
    }

    public SimpleDecimal(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super(datatype == null ? XSDDatatype.DECIMAL.getIRI() : datatype);
        this.label = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.value = new BigDecimal(lexicalValue);
        CoreDatatype resolved = coreDatatype != null ? coreDatatype : CoreDatatypes.from(this.datatype);
        this.coreDatatype = AbstractLiteral.isDecimalCoreDatatype(resolved) ? resolved : XSDDatatype.DECIMAL;
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
        throw new IncorrectOperationException("SimpleDecimal is immutable");
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
        return value.toBigInteger();
    }

    @Override
    public BigDecimal decimalValue() {
        return value;
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
        if (other instanceof SimpleDecimal number) {
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
