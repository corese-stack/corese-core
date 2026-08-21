package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractNumber;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the numeric based literal in the XSD datatype hierarchy.
 */
public abstract class AbstractCoreseNumber extends AbstractNumber implements CoreseDatatypeAdapter {

    private final String lexicalValue;
    private transient volatile CoreseNumber coreseObject;

    /**
     * Constructor for AbstractCoreseNumber.
     *
     * @param coreseObject the CoreseNumber object
     * @param datatype     the datatype of the literal
     */
    protected AbstractCoreseNumber(CoreseNumber coreseObject, IRI datatype) {
        super(datatype);
        this.coreseObject = coreseObject;
        this.lexicalValue = coreseObject.getLabel();
    }

    /** Recreates the legacy value after Java deserialization. */
    protected abstract CoreseNumber createCoreseObject(String value);

    protected final CoreseNumber coreseObject() {
        CoreseNumber value = coreseObject;
        if (value == null) {
            value = createCoreseObject(lexicalValue);
            coreseObject = value;
        }
        return value;
    }

    @Override
    public Node getCoreseNode() {
        return coreseObject();
    }

    @Override
    public IDatatype getIDatatype() {
        return coreseObject();
    }

    @Override
    public String getLabel() {
        return lexicalValue;
    }

    @Override
    public abstract byte byteValue();

    @Override
    public abstract int intValue();

    @Override
    public abstract long longValue();

    @Override
    public abstract short shortValue();

    @Override
    public float floatValue() {
        return coreseObject().floatValue();
    }

    @Override
    public abstract double doubleValue();

    @Override
    public abstract BigInteger integerValue();

    @Override
    public abstract BigDecimal decimalValue();

    @Override
    public String stringValue() {
        return lexicalValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractNumber)) return false;
        if (!(o instanceof AbstractCoreseNumber that)) return super.equals(o);
        return this.coreseObject().equals(that.coreseObject());
    }

    @Override
    public int hashCode() {
        return this.coreseObject().hashCode();
    }

}
