package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractDuration;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;

import java.time.temporal.TemporalAmount;

public class CoreseDuration extends AbstractDuration implements CoreseDatatypeAdapter {
    private CoreseUndefLiteral coreseObject;

    public CoreseDuration(IDatatype coreseObject) {
        if (coreseObject instanceof CoreseUndefLiteral) {
            this.coreseObject = (CoreseUndefLiteral) coreseObject;
        } else {
            throw new UnsupportedOperationException("Cannot create CoreseDuration from a non-undef Corese object.");
        }
    }

    public CoreseDuration(String duration) {
        this(new CoreseUndefLiteral(duration, XSD.xsdDuration.getIRI().stringValue()));
    }

    public CoreseDuration(String value, IRI datatype) {
        this(new CoreseUndefLiteral(value, datatype.stringValue()));
    }

    public CoreseDuration(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(coreDatatype != null && coreDatatype != XSD.xsdDuration) {
            throw new UnsupportedOperationException("Cannot create CoreseDuration with a core datatype other than xsd:duration.");
        }
    }

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
        return XSD.xsdDuration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoreseDuration) {
            return this.coreseObject.equals(((CoreseDuration) obj).coreseObject);
        }
        return false;
    }
}
