package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the numeric based literal in the XSD datatype hierarchy.
 */
public abstract class AbstractCoreseNumber extends AbstractLiteral implements CoreseDatatypeAdapter {

    protected final CoreseNumber coreseObject;

    /**
     * Constructor for AbstractCoreseNumber.
     *
     * @param coreseObject the CoreseNumber object
     * @param datatype     the datatype of the literal
     */
    protected AbstractCoreseNumber(CoreseNumber coreseObject, IRI datatype) {
        super(datatype);
        this.coreseObject = coreseObject;
    }


    @Override
    public Node getCoreseNode() {
        return coreseObject;
    }

    @Override
    public IDatatype getIDatatype() {
        return coreseObject;
    }

    @Override
    public String getLabel() {
        return this.coreseObject.getLabel();
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
        return this.coreseObject.floatValue();
    }

    @Override
    public abstract double doubleValue();

    @Override
    public abstract BigInteger integerValue();

    @Override
    public abstract BigDecimal decimalValue();

    @Override
    public String stringValue() {
        return this.coreseObject.getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractCoreseNumber)) return false;
        AbstractCoreseNumber that = (AbstractCoreseNumber) o;
        return this.coreseObject.equals(that.coreseObject);
    }

    @Override
    public int hashCode() {
        return this.coreseObject.hashCode();
    }

}
