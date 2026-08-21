package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractDuration;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;

import java.time.temporal.TemporalAmount;

/**
 * CoreseDuration class that represents a duration literal in the Corese framework.
 * @ImplNote Legacy corese do not have a class dedicated to the storage of duration. The object is stored as a string literal.
 */
public class CoreseDuration extends AbstractDuration implements CoreseDatatypeAdapter {
    private final String lexicalValue;
    private final String legacyDatatypeIri;
    private transient volatile CoreseUndefLiteral coreseObject;

    /**
     * Constructor for CoreseDuration.
     *
     * @param coreseObject  the CoreseUndefLiteral object
     */
    public CoreseDuration(IDatatype coreseObject) {
        if (coreseObject instanceof CoreseUndefLiteral undefLiteral) {
            this.coreseObject = undefLiteral;
            this.lexicalValue = undefLiteral.getLabel();
            this.legacyDatatypeIri = undefLiteral.getDatatypeURI();
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
        this(new CoreseUndefLiteral(duration, XSDDatatype.DURATION.getIRI().stringValue()));
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
        if(coreDatatype != null && coreDatatype != XSDDatatype.DURATION) {
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
        return lexicalValue;
    }

    @Override
    public String stringValue() {
        return lexicalValue;
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
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DURATION;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoreseDuration coreseDuration) {
            return this.coreseObject().equals(coreseDuration.coreseObject());
        } else if (obj instanceof AbstractDuration) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.coreseObject().hashCode();
    }

    private CoreseUndefLiteral coreseObject() {
        CoreseUndefLiteral result = coreseObject;
        if (result == null) {
            result = new CoreseUndefLiteral(lexicalValue, legacyDatatypeIri);
            coreseObject = result;
        }
        return result;
    }
}
