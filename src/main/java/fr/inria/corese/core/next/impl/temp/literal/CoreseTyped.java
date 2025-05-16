package fr.inria.corese.core.next.impl.temp.literal;

import java.util.Objects;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractStringLiteral;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.util.literal.CoreDatatypeHelper;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * An implementation of the {@code xsd:string} datatype used by Corese.
 * This class represents a typed literal of string type and can be used with
 * other XSD types as well.
 * It extends {@link AbstractStringLiteral} and implements
 * {@link CoreseDatatypeAdapter}.
 */

public class CoreseTyped extends AbstractStringLiteral implements CoreseDatatypeAdapter {

    /**
     * The Corese object representing the string literal in the old API.
     */
    private final fr.inria.corese.core.sparql.datatype.CoreseString coreseObject;

    /**
     * The core datatype of this literal.
     */
    private CoreDatatype coreDatatype;
    /**
     * The value of the string literal.
     */
    private String value;
    /**
     * The datatype IRI of the literal.
     */
    private IRI dataype;

    /**
     * Constructs a {@link CoreseTyped} instance from an {@link IDatatype} Corese
     * object.
     * The Corese object should be an instance of
     * {@link fr.inria.corese.core.sparql.datatype.CoreseString}.
     * 
     * @param coreseObject The {@link IDatatype} Corese object representing the
     *                     string literal.
     * @throws IncorrectOperationException If the provided {@link IDatatype} is not
     *                                     a
     *                                     {@link fr.inria.corese.core.sparql.datatype.CoreseString}.
     */
    public CoreseTyped(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseString) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseString) coreseObject;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseString from a non-string Corese object");
        }
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value.
     * The datatype is set to XSD.STRING.
     * 
     * @param value The string value for the literal.
     */
    public CoreseTyped(String value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.coreDatatype = XSD.STRING;
        this.datatype = XSD.STRING.getIRI();
        this.value = value;
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value and a specified
     * datatype IRI.
     * If the datatype is {@code null}, the datatype is set to XSD.STRING.
     * If the datatype is non-null, the {@link CoreDatatype} is determined from the
     * datatype IRI.
     *
     * @param value    The string value for the literal.
     * @param datatype The datatype IRI for the literal.
     */
    public CoreseTyped(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.value = value;
        if (datatype == null) {
            this.datatype = XSD.STRING.getIRI();
            this.coreDatatype = XSD.STRING;
        } else {
            this.datatype = datatype;
            this.coreDatatype = CoreDatatypeHelper.from(datatype);
        }
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value and a specified
     * {@link CoreDatatype}.
     * The datatype IRI is derived from the {@link CoreDatatype}.
     *
     * @param value        The string value for the literal.
     * @param coreDatatype The core datatype for the literal.
     */
    public CoreseTyped(String value, CoreDatatype coreDatatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));
        this.datatype = coreDatatype.getIRI();
        this.value = value;
        this.coreDatatype = Objects.requireNonNull(coreDatatype);
        this.datatype = coreDatatype.getIRI();
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value, a datatype
     * IRI, and a {@link CoreDatatype}.
     * This constructor ensures that the datatype IRI matches the
     * {@link CoreDatatype}.
     * If they do not match or if either value is {@code null}, an exception is
     * thrown.
     *
     * @param value        The string value for the literal.
     * @param datatype     The datatype IRI for the literal.
     * @param coreDatatype The core datatype for the literal.
     * @throws IncorrectOperationException If the datatype IRI does not match the
     *                                     core datatype's IRI or if either value is
     *                                     {@code null}.
     */
    public CoreseTyped(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseString(value));

        if (datatype == null || coreDatatype == null) {
            throw new IncorrectOperationException("Datatype and CoreDatatype cannot be null");
        }

        if (coreDatatype != CoreDatatype.NONE && !datatype.equals(coreDatatype.getIRI())) {
            throw new IncorrectOperationException("Datatype IRI does not match CoreDatatype's IRI");
        }

        this.value = value;
        this.datatype = datatype;
        this.coreDatatype = coreDatatype;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set core datatype for this string object");
    }

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return this.coreDatatype;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }
}
