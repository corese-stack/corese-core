package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.sparql.datatype.CoreseDateTime;

/**
 * There are no dedicated implementation to represent xsd:time in Corese.
 */
public class CoreseTime extends CoreseDateTime {

    public CoreDatatype getCoreDatatype() {
        return XSD.xsdTime;
    }
}
