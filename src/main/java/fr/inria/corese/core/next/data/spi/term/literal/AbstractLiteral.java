package fr.inria.corese.core.next.data.spi.term.literal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;

/**
 * Base class for all literals. All value accessors throw an exception by default.
 */
public abstract class AbstractLiteral implements Literal {

    protected IRI datatype;

    protected static List<IRI> integerXSDCoreDatatypeIRIs = List.of(
            XSDDatatype.INTEGER.getIRI(),
            XSDDatatype.BYTE.getIRI(),
            XSDDatatype.SHORT.getIRI(),
            XSDDatatype.INT.getIRI(),
            XSDDatatype.LONG.getIRI(),
            XSDDatatype.UNSIGNED_BYTE.getIRI(),
            XSDDatatype.UNSIGNED_SHORT.getIRI(),
            XSDDatatype.UNSIGNED_INT.getIRI(),
            XSDDatatype.UNSIGNED_LONG.getIRI(),
            XSDDatatype.POSITIVE_INTEGER.getIRI(),
            XSDDatatype.NEGATIVE_INTEGER.getIRI(),
            XSDDatatype.NON_NEGATIVE_INTEGER.getIRI(),
            XSDDatatype.NON_POSITIVE_INTEGER.getIRI()
    );

    protected static List<IRI> decimalXSDCoreDatatypeIRIs = List.of(
            XSDDatatype.DECIMAL.getIRI(),
            XSDDatatype.FLOAT.getIRI(),
            XSDDatatype.DOUBLE.getIRI()
    );

    /**
     * Constructor for AbstractLiteral.
     *
     * @param datatype the datatype of the literal
     */
    protected AbstractLiteral(IRI datatype) {
        this.datatype = datatype;
    }

    /**
     * @param coreDatatype a CoreDatatype
     * @return true if the given core datatype is an integer core datatype, e.g xsd;integer, xsd:int, xsd:byte, etc, false otherwise
     */
    public static boolean isIntegerCoreDatatype(CoreDatatype coreDatatype) {
        return coreDatatype != null
                && coreDatatype.getIRI() != null
                && integerXSDCoreDatatypeIRIs.contains(coreDatatype.getIRI());
    }

    /**
     * @param iri an IRI
     * @return true if the given IRI is an integer core datatype, e.g xsd;integer, xsd:int, xsd:byte, etc, false otherwise
     */
    public static boolean isIriOfIntegerCoreDatatype(IRI iri) {
        return iri != null && integerXSDCoreDatatypeIRIs.contains(iri);
    }

    /**
     * @param coreDatatype a CoreDatatype
     * @return true if the given core datatype is a decimal core datatype, e.g xsd;decimal, xsd:float, xsd:double, etc, false otherwise
     */
    public static boolean isDecimalCoreDatatype(CoreDatatype coreDatatype) {
        return coreDatatype != null
                && coreDatatype.getIRI() != null
                && decimalXSDCoreDatatypeIRIs.contains(coreDatatype.getIRI());
    }

    /**
     * @param iri an IRI
     * @return true if the given IRI is a decimal core datatype, e.g xsd;decimal, xsd:float, xsd:double, etc, false otherwise
     */
    public static boolean isIriOfDecimalCoreDatatype(IRI iri) {
        return iri != null && decimalXSDCoreDatatypeIRIs.contains(iri);
    }
    /**
     * Sets the core datatype of the literal.
     *
     * @param coreDatatype the CoreDatatype to set
     */
    protected abstract void setCoreDatatype(CoreDatatype coreDatatype);

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    public boolean booleanValue() {
        throw new IncorrectOperationException("Cannot convert to boolean");
    }

    @Override
    public byte byteValue() {
        throw new IncorrectOperationException("Cannot convert to byte");
    }

    @Override
    public short shortValue() {
        throw new IncorrectOperationException("Cannot convert to short");
    }

    @Override
    public int intValue() {
        throw new IncorrectOperationException("Cannot convert to int");
    }

    @Override
    public long longValue() {
        throw new IncorrectOperationException("Cannot convert to long");
    }

    @Override
    public BigInteger integerValue() {
        throw new IncorrectOperationException("Cannot convert to integer");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new IncorrectOperationException("Cannot convert to decimal");
    }

    @Override
    public float floatValue() {
        throw new IncorrectOperationException("Cannot convert to float");
    }

    @Override
    public double doubleValue() {
        throw new IncorrectOperationException("Cannot convert to double");
    }

    @Override
    public TemporalAccessor temporalAccessorValue() {
        throw new IncorrectOperationException("Cannot convert to temporal accessor");
    }

    @Override
    public TemporalAmount temporalAmountValue() {
        throw new IncorrectOperationException("Cannot convert to temporal amount");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        throw new IncorrectOperationException("Cannot convert to XML calendar");
    }

    /**
     * Compares RDF literal terms by lexical form, datatype, and language tag.
     * Value-space comparisons belong to the SPARQL evaluation layer and must not
     * be implemented through {@link Object#equals(Object)}.
     *
     * @param obj the object to compare with
     * @return {@code true} when both objects denote the same RDF literal term
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Literal other)) {
            return false;
        }
        return Objects.equals(getLabel(), other.getLabel())
                && Objects.equals(getDatatype(), other.getDatatype())
                && Objects.equals(getLanguage(), other.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel(), getDatatype(), getLanguage());
    }

    @Override
    public String toString() {
        return this.stringValue();
    }
}
