package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;

/**
 * There are no dedicated implementation to represent xsd:time in Corese.
 * This implementation inherits from CoreseDatetime, which is the super class for all datetime literals.
 */
public class CoreseTime extends CoreseDatetime {

    /**
     * Constructor for CoreseTime.
     *
     * @param value the value of the time literal
     */
    public CoreseTime(String value) {
        super(value);
    }

    /**
     * Constructor for CoreseTime.
     *
     * @param value the value of the time literal
     * @param datatype the datatype of the literal
     */
    public CoreseTime(String value, IRI datatype) {
        super(value, datatype);
    }

    /**
     * Constructor for CoreseTime.
     *
     * @param value the value of the time literal
     * @param datatype the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal
     */
    public CoreseTime(String value, IRI datatype, CoreDatatype coreDatatype) {
        super(value, datatype, coreDatatype);
    }

    /**
     *
     * @return XSD.TIME
     */
    public CoreDatatype getCoreDatatype() {
        return XSD.TIME;
    }
}
