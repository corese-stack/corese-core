package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.util.literal.CoreDatatypeHelper;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractString;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.Objects;

/**
 * An implementation of the xsd:string datatype used by Corese
 * it can also be used with other xsd type as a typed Literal
 */

public class CoreseTyped extends AbstractString implements CoreseDatatypeAdapter {

    private final fr.inria.corese.core.sparql.datatype.CoreseString coreseObject;

    private CoreDatatype coreDatatype;
    private String value;
    private IRI dataype;

    public CoreseTyped(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseString) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseString) coreseObject;
        }
        else {
            throw new IncorrectOperationException("Cannot create CoreseString from a non-string Corese object");
        }
    }

    public CoreseTyped(String value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.coreDatatype = XSD.STRING;
        this.datatype = XSD.STRING.getIRI();
        this.value = value;
    }

    public CoreseTyped(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.value = value;
        if (datatype == null) {
            this.datatype = XSD.STRING.getIRI();
            this.coreDatatype = XSD.STRING;
        }
        else {
            this.datatype = datatype;
            this.coreDatatype = CoreDatatypeHelper.from(datatype);
        }
    }

    public CoreseTyped(String value, CoreDatatype coreDatatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.datatype = coreDatatype.getIRI();
        this.value = value;
        this.coreDatatype = Objects.requireNonNull(coreDatatype);
        this.datatype = coreDatatype.getIRI();

    }

    public CoreseTyped(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));

        if (datatype == null || coreDatatype == null) {
            throw new IncorrectOperationException("Datatype and CoreDatatype cannot be null");
        }

        if (coreDatatype != CoreDatatype.NONE && !datatype.equals(coreDatatype.getIRI())) {
            throw new IncorrectOperationException("Datatype IRI does not match CoreDatatype's IRI");
        }

        this.value = value;
        this.datatype = datatype;
        this.coreDatatype = coreDatatype;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set core datatype for this string object");
    }

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return this.coreDatatype;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }
}
