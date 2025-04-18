package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * An implementation of the {@code xsd:boolean} datatype used by Corese.
 * The {@code xsd:boolean} type represents logical boolean values. The valid values for {@code xsd:boolean}
 * are {@code true}, {@code false}, {@code 0}, and {@code 1}.
 * Values that are capitalized (e.g. TRUE) or abbreviated (e.g. T) are not valid.
 */

public class CoreseBoolean extends AbstractLiteral {

    /**
     * The Corese object representing the boolean literal in the old API.
     */
    private final fr.inria.corese.core.sparql.datatype.CoreseBoolean coreseObject;

    /**
     * The core datatype of this literal, which is XSD.BOOLEAN.
     */
    private CoreDatatype coreDatatype;

    /**
     * The value of the boolean literal.
     */
    private Boolean value;

    /**
     * A constant representing the boolean value {@code true}.
     */
    private static final CoreseBoolean TRUE = new CoreseBoolean(true);

    /**
     * A constant representing the boolean value {@code false}.
     */
    private static final CoreseBoolean FALSE = new CoreseBoolean(false);

    /**
     * Constructs a {@link CoreseBoolean} instance from an {@link IDatatype} Corese object.
     * The Corese object should be an instance of {@link fr.inria.corese.core.sparql.datatype.CoreseBoolean}.
     *
     * @param coreseObject The {@link IDatatype} Corese object representing the boolean literal.
     * @throws IncorrectOperationException If the provided {@link IDatatype} is not a valid {@link fr.inria.corese.core.sparql.datatype.CoreseBoolean}.
     */
    public CoreseBoolean(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseBoolean) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseBoolean) coreseObject;
            this.value = this.coreseObject.booleanValue();
            this.coreDatatype = XSD.BOOLEAN;
            this.datatype = XSD.BOOLEAN.getIRI();
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDate from a non-date Corese object.");
        }
    }

    /**
     * Constructs a {@link CoreseBoolean} instance from a boolean value.
     *
     * @param value The boolean value for the literal.
     */
    public CoreseBoolean(boolean value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseBoolean(value));
        this.value = value;
        this.coreDatatype = XSD.BOOLEAN;
        this.datatype = XSD.BOOLEAN.getIRI();
    }

    /**
     * Returns the label of this boolean literal, which is either {@code "true"} or {@code "false"}.
     *
     * @return The label of the boolean literal.
     */
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

    /**
     * Returns a {@link CoreseBoolean} instance representing the boolean value {@code true} or {@code false}.
     *
     * @param value The boolean value to be returned as a {@link CoreseBoolean} instance.
     * @return The {@link CoreseBoolean} instance representing the given boolean value.
     */
    public static CoreseBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
}