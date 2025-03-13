package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.base.CoreDatatype.XSD;

/**
 * There are no dedicated implementation to represent xsd:time in Corese.
 */
public class CoreseTime extends CoreseDatetime {

    public CoreseTime(String label) {
        super(label);
    }

    public CoreseTime(String value, IRI datatype) {
        super(value, datatype);
    }

    public CoreseTime(String value, IRI datatype, CoreDatatype coreDatatype) {
        super(value, datatype, coreDatatype);
    }

    public CoreDatatype getCoreDatatype() {
        return XSD.TIME;
    }
}
