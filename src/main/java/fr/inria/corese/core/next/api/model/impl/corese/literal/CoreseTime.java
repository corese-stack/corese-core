package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

/**
 * There are no dedicated implementation to represent xsd:time in Corese.
 */
public class CoreseTime extends CoreseDatetime {

    public CoreseTime(String label) {
        super(label);
    }

    public CoreDatatype getCoreDatatype() {
        return XSD.xsdTime;
    }
}
