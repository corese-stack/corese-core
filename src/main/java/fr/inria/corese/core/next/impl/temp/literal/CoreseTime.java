package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.XSD;

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
