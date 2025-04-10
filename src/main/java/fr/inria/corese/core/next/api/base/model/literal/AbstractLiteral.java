package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.literal.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;

/**
 * Base class for all literals. All value accessors throw an exception by default.
 */
public abstract class AbstractLiteral implements Literal, Serializable {

    protected IRI datatype;

    protected static List<IRI> integerXSDCoreDatatypeIRIs = List.of(
            XSD.INTEGER.getIRI(),
            XSD.BYTE.getIRI(),
            XSD.SHORT.getIRI(),
            XSD.INT.getIRI(),
            XSD.LONG.getIRI(),
            XSD.UNSIGNED_BYTE.getIRI(),
            XSD.UNSIGNED_SHORT.getIRI(),
            XSD.UNSIGNED_INT.getIRI(),
            XSD.UNSIGNED_LONG.getIRI(),
            XSD.POSITIVE_INTEGER.getIRI(),
            XSD.NEGATIVE_INTEGER.getIRI(),
            XSD.NON_NEGATIVE_INTEGER.getIRI(),
            XSD.NON_POSITIVE_INTEGER.getIRI()
    );

    protected static List<IRI> decimalXSDCoreDatatypeIRIs = List.of(
            XSD.DECIMAL.getIRI(),
            XSD.FLOAT.getIRI(),
            XSD.DOUBLE.getIRI()
    );

    /**
     * Constructor for AbstractLiteral.
     *
     * @param datatype the datatype of the literal
     */
    protected AbstractLiteral(IRI datatype) {
        this.datatype = datatype;
    }

    public static boolean isIntegerCoreDatatype(CoreDatatype coreDatatype) {
        return integerXSDCoreDatatypeIRIs.contains(coreDatatype.getIRI());
    }

    public static boolean isIriOfIntegerCoreDatatype(IRI iri) {
        return integerXSDCoreDatatypeIRIs.contains(iri);
    }

    public static boolean isDecimalCoreDatatype(CoreDatatype coreDatatype) {
        return decimalXSDCoreDatatypeIRIs.contains(coreDatatype.getIRI());
    }

    public static boolean isIriOfDecimalCoreDatatype(IRI iri) {
        return decimalXSDCoreDatatypeIRIs.contains(iri);
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
}
