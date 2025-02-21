package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public abstract class AbstractLiteral implements Literal {

    private CoreDatatype coreDatatype = XSD.xsdString;

    @Override
    public String getLabel() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public Optional<String> getLanguage() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public IRI getDatatype() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public boolean booleanValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public byte byteValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public short shortValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public int intValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public long longValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public BigInteger integerValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public float floatValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public double doubleValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return this.coreDatatype;
    }

    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        this.coreDatatype = coreDatatype;
    }

    @Override
    public String stringValue() {
        throw new UnsupportedOperationException("This method is not implemented for this type of literal.");
    }
}
