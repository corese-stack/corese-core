package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.CoreseIRI;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractLiteral;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * xsd:boolean
 * The type xsd:boolean represents logical yes/no values. The valid values for xsd:boolean are true, false, 0, and 1.
 * Values that are capitalized (e.g. TRUE) or abbreviated (e.g. T) are not valid.
 */

public class CoreseBoolean extends AbstractLiteral {

    private final fr.inria.corese.core.sparql.datatype.CoreseBoolean coreseObject;

    private CoreDatatype coreDatatype;
    private Boolean value;

    private static final CoreseBoolean TRUE = new CoreseBoolean(true);

    private static final CoreseBoolean FALSE = new CoreseBoolean(false);

    public CoreseBoolean(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseBoolean) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseBoolean) coreseObject;
            this.value = this.coreseObject.booleanValue();
            this.coreDatatype = CoreDatatype.XSD.BOOLEAN;
            this.datatype = CoreDatatype.XSD.BOOLEAN.getIRI();
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDate from a non-date Corese object.");
        }
    }

    public CoreseBoolean(boolean value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseBoolean(value));
        this.value = value;
        this.coreDatatype = CoreDatatype.XSD.BOOLEAN;
        this.datatype = CoreDatatype.XSD.BOOLEAN.getIRI();
    }

    @Override
    public String getLabel() {
        return value ? "true" : "false";
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return this.coreDatatype;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        this.coreDatatype = coreDatatype;
    }

    public boolean booleanValue() {
        return this.value;
    }

    @Override
    public String stringValue() {
        return this.value.toString();
    }

    public static CoreseBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
}