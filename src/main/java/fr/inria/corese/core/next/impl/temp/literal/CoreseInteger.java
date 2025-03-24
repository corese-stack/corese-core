package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;

/**
 * Super class for all the integer based literal in the XD datatype hierarchy
 */
public class CoreseInteger extends AbstractCoreseNumber {

    public CoreseInteger(long value) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), CoreDatatype.XSD.INTEGER.getIRI());
    }

    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject) {
        super(coreseObject, CoreDatatype.XSD.INTEGER.getIRI());
    }

    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    public CoreseInteger(String value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value));
    }

    public CoreseInteger(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), datatype);
    }

    public CoreseInteger(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(AbstractLiteral.integerXSDCoreDatatypes.contains(coreDatatype)) {
            this.setCoreDatatype(coreDatatype);
        } else {
            throw new IncorrectDatatypeException("Cannot create CoreseInteger with a non-integer CoreDatatype.");
        }
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return CoreDatatype.XSD.INTEGER;
    }
}
