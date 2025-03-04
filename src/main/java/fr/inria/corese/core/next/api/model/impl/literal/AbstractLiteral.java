package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;

public abstract class AbstractLiteral implements Literal {

    public abstract void setCoreDatatype(CoreDatatype coreDatatype);
}
