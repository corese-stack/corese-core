package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseDouble;

/**
 * Super class for all numeric literal containing floating points
 */
public class CoreseDecimal extends AbstractCoreseNumber {

    public CoreseDecimal(double value) {
        super(new CoreseDouble(value), CoreDatatype.XSD.DECIMAL.getIRI());
    }

    public CoreseDecimal(CoreseDouble coreseObject) {
        super(coreseObject, CoreDatatype.XSD.DECIMAL.getIRI());
    }

    public CoreseDecimal(CoreseDouble coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    public CoreseDecimal(String value) {
        this(new CoreseDouble(value));
    }

    public CoreseDecimal(String value, IRI datatype) {
        this(new CoreseDouble(value), datatype);
    }

    public CoreseDecimal(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(AbstractLiteral.decimalXSDCoreDatatypes.contains(coreDatatype)) {
            this.setCoreDatatype(coreDatatype);
        } else {
            throw new IncorrectDatatypeException("Cannot create CoreseDecimal with a non-integer CoreDatatype.");
        }
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return CoreDatatype.XSD.DECIMAL;
    }
}
