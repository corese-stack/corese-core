package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.api.base.model.literal.AbstractDuration;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;

import java.time.temporal.TemporalAmount;

/**
 * CoreseDuration class that represents a duration literal in the Corese framework.
 * @ImplNote Legacy corese do not have a class dedicated to the storage of duration. The object is stored as a string literal.
 */
public class CoreseDuration extends AbstractDuration implements CoreseDatatypeAdapter {
    private CoreseUndefLiteral coreseObject;

    /**
     * Constructor for CoreseDuration.
     *
     * @param coreseObject  the CoreseUndefLiteral object
     */
    public CoreseDuration(IDatatype coreseObject) {
        if (coreseObject instanceof CoreseUndefLiteral) {
            this.coreseObject = (CoreseUndefLiteral) coreseObject;
        } else {
            throw new UnsupportedOperationException("Cannot create CoreseDuration from a non-undef Corese object.");
        }
    }

    /**
     * Constructor for CoreseDuration.
     *
     * @param duration the duration in string format
     */
    public CoreseDuration(String duration) {
        this(new CoreseUndefLiteral(duration, XSD.DURATION.getIRI().stringValue()));
    }

    /**
     * Constructor for CoreseDuration.
     *
     * @param value the duration value
     * @param datatype the datatype IRI
     */
    public CoreseDuration(String value, IRI datatype) {
        this(new CoreseUndefLiteral(value, datatype.stringValue()));
    }

    /**
     * Constructor for CoreseDuration.
     *
     * @param value the duration value
     * @param datatype the datatype IRI
     * @param coreDatatype the core datatype
     * @throws UnsupportedOperationException if the core datatype is not xsd:duration
     */
    public CoreseDuration(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(coreDatatype != null && coreDatatype != XSD.DURATION) {
            throw new UnsupportedOperationException("Cannot create CoreseDuration with a core datatype other than xsd:duration.");
        }
    }

    /**
     * Constructor for CoreseDuration.
     *
     * @param duration the duration as a TemporalAmount
     */
    public CoreseDuration(TemporalAmount duration) {
        this(duration.toString());
    }

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
    }

    @Override
    public String stringValue() {
        return coreseObject.stringValue();
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DURATION;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoreseDuration) {
            return this.coreseObject.equals(((CoreseDuration) obj).coreseObject);
        }
        return false;
    }
}
