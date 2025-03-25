package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the numeric based literal in the XSD datatype hierarchy.
 */
public abstract class AbstractCoreseNumber extends AbstractLiteral implements CoreseDatatypeAdapter {

    private final CoreseNumber coreseObject;

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
    public byte byteValue() {
        return (byte) this.coreseObject.longValue();
    }

    @Override
    public int intValue() {
        return this.coreseObject.intValue();
    }

    @Override
    public long longValue() {
        return this.coreseObject.longValue();
    }

    @Override
    public short shortValue() {
        return (short) this.coreseObject.longValue();
    }

    @Override
    public float floatValue() {
        return this.coreseObject.floatValue();
    }

    @Override
    public double doubleValue() {
        return this.coreseObject.doubleValue();
    }

    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(this.coreseObject.longValue());
    }

    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.coreseObject.doubleValue());
    }

    @Override
    public String stringValue() {
        return this.coreseObject.getLabel();
    }

}
