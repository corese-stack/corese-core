package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;

public abstract class AbstractLiteral implements Literal {

    protected IRI datatype;

    public abstract void setCoreDatatype(CoreDatatype coreDatatype);

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }
}
