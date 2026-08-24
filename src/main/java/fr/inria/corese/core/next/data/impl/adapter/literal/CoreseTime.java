package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

/**
 * There are no dedicated implementation to represent xsd:time in Corese.
 * This implementation inherits from CoreseDatetime, which is the super class for all datetime literals.
 */
@SuppressWarnings("java:S2160")
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
     * @param coreDatatype the CoreDatatype of the literal
     */
    public CoreseTime(String value, IRI datatype, CoreDatatype coreDatatype) {
        super(value, datatype, coreDatatype);
    }

    /**
     *
     * @return XSDDatatype.TIME
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.TIME;
    }
}
