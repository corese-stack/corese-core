package fr.inria.corese.core.next.data.impl.adapter.literal;

import java.util.Objects;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractStringLiteral;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatypes;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseGeneric;
import fr.inria.corese.core.sparql.datatype.CoreseString;

import static fr.inria.corese.core.next.data.api.vocabulary.XSD.xsdString;

/**
 * An implementation of the {@code xsd:string} datatype used by Corese.
 * This class represents a typed literal of string type and can be used with
 * other XSD types as well.
 * It extends {@link AbstractStringLiteral} and implements
 * {@link CoreseDatatypeAdapter}.
 */

@SuppressWarnings("java:S2160")
public class CoreseTyped extends AbstractStringLiteral implements CoreseDatatypeAdapter {

    /**
     * The Corese object representing the string literal in the old API.
     */
    private final transient CoreseString coreseObject;

    /**
     * The core datatype of this literal.
     */
    private transient CoreDatatype coreDatatype;

    /**
     * Constructs a {@link CoreseTyped} instance from an {@link IDatatype} Corese
     * object.
     * The Corese object should be an instance of
     * {@link CoreseString}.
     *
     * @param coreseObject The {@link IDatatype} Corese object representing the
     *                     string literal.
     * @throws IncorrectOperationException If the provided {@link IDatatype} is not
     *                                     a
     *                                     {@link CoreseString}.
     */
    public CoreseTyped(IDatatype coreseObject) {
        super(coreseObject.getDatatypeURI() != null && ! coreseObject.getDatatypeURI().isEmpty() ? new CoreseIRI(coreseObject.getDatatypeURI()) : new CoreseIRI(xsdString.getIRI()));
        if (coreseObject instanceof CoreseString stringObj) {
            this.coreseObject = stringObj;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseString from a non-string Corese object");
        }
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value.
     * The datatype is set to XSDDatatype.STRING.
     *
     * @param value The string value for the literal.
     */
    public CoreseTyped(String value) {
        this(new CoreseString(value));
        this.coreDatatype = XSDDatatype.STRING;
        this.datatype = XSDDatatype.STRING.getIRI();
    }

    /**
     * Constructs a {@link CoreseTyped} instance from a string value and a specified
     * datatype IRI.
     * If the datatype is {@code null}, the datatype is set to XSDDatatype.STRING.
     * If the datatype is non-null, the {@link CoreDatatype} is determined from the
     * datatype IRI.
     *
     * @param value    The string value for the literal.
     * @param datatype The datatype IRI for the literal.
     */
    public CoreseTyped(String value, IRI datatype) {
        this(new CoreseGeneric(value, datatype == null ? xsdString.getIRI().stringValue() : datatype.stringValue()));
        if (datatype == null) {
            this.datatype = XSDDatatype.STRING.getIRI();
            this.coreDatatype = XSDDatatype.STRING;
        } else {
            this.datatype = datatype;
            this.coreDatatype = CoreDatatypes.from(datatype);
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
        this(new CoreseString(value));
        this.datatype = coreDatatype.getIRI();
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
        this(new CoreseString(value));

        if (datatype == null || coreDatatype == null) {
            throw new IncorrectOperationException("Datatype and CoreDatatype cannot be null");
        }

        if (coreDatatype != CoreDatatype.NONE && !datatype.equals(coreDatatype.getIRI())) {
            throw new IncorrectOperationException("Datatype IRI does not match CoreDatatype's IRI");
        }

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
