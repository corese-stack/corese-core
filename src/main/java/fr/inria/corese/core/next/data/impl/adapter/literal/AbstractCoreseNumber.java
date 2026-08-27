package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractNumber;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the numeric based literal in the XSD datatype hierarchy.
 */
@SuppressWarnings({"java:S2160", "java:S3077"})
public abstract class AbstractCoreseNumber extends AbstractNumber implements CoreseDatatypeAdapter {

    private final String lexicalValue;
    private transient volatile IDatatype coreseObject;

    /**
     * Constructor for AbstractCoreseNumber.
     *
     * @param coreseObject the CoreseNumber object
     * @param datatype     the datatype of the literal
     */
    protected AbstractCoreseNumber(IDatatype coreseObject, IRI datatype) {
        this(coreseObject.getLabel(), coreseObject, datatype);
    }

    /**
     * Constructor preserving the RDF lexical form supplied by the caller.
     */
    protected AbstractCoreseNumber(String lexicalValue, IDatatype coreseObject, IRI datatype) {
        super(datatype);
        this.coreseObject = coreseObject;
        this.lexicalValue = lexicalValue;
    }

    /** Recreates the legacy value after Java deserialization. */
    protected abstract IDatatype createCoreseObject(String value);

    protected final IDatatype coreseObject() {
        IDatatype value = coreseObject;
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

}
