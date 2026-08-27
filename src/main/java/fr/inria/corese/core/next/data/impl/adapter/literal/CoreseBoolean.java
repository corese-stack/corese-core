package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.exception.InvalidDatatypeException;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * An implementation of the {@code xsd:boolean} datatype used by Corese.
 * The {@code xsd:boolean} type represents logical boolean values. The valid
 * values for {@code xsd:boolean}
 * are {@code true}, {@code false}, {@code 0}, and {@code 1}.
 * Values that are capitalized (e.g. TRUE) or abbreviated (e.g. T) are not
 * valid.
 */

@SuppressWarnings({"java:S2160", "java:S3077"})
public class CoreseBoolean extends AbstractLiteral implements CoreseDatatypeAdapter {

    /**
     * The Corese object representing the boolean literal in the old API.
     */
    private transient volatile fr.inria.corese.core.sparql.datatype.CoreseBoolean coreseObject;

    /**
     * The value of the boolean literal.
     */
    private final boolean value;

    /** The RDF lexical form, including the valid numeric forms {@code 0} and {@code 1}. */
    private final String lexicalValue;

    /**
     * A constant representing the boolean value {@code true}.
     */
    private static final CoreseBoolean TRUE = new CoreseBoolean(true);

    /**
     * A constant representing the boolean value {@code false}.
     */
    private static final CoreseBoolean FALSE = new CoreseBoolean(false);

    /**
     * Constructs a {@link CoreseBoolean} instance from an {@link IDatatype} Corese
     * object.
     * The Corese object should be an instance of
     * {@link fr.inria.corese.core.sparql.datatype.CoreseBoolean}.
     *
     * @param coreseObject The {@link IDatatype} Corese object representing the
     *                     boolean literal.
     * @throws IncorrectOperationException If the provided {@link IDatatype} is not
     *                                     a valid
     *                                     {@link fr.inria.corese.core.sparql.datatype.CoreseBoolean}.
     */
    public CoreseBoolean(IDatatype coreseObject) {
        super(XSDDatatype.BOOLEAN.getIRI());
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseBoolean booleanValue) {
            this.coreseObject = booleanValue;
            this.value = this.coreseObject.booleanValue();
            this.lexicalValue = this.coreseObject.getLabel();
        } else {
            throw new IncorrectOperationException("Cannot create CoreseBoolean from a non-boolean Corese object.");
        }
    }

    /**
     * Constructs a {@link CoreseBoolean} instance from a boolean value.
     *
     * @param value The boolean value for the literal.
     */
    public CoreseBoolean(boolean value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseBoolean(value));
    }

    /**
     * Constructs a boolean literal from an XML Schema lexical form.
     *
     * @param lexicalValue one of {@code true}, {@code false}, {@code 1}, or {@code 0}
     * @throws InvalidDatatypeException if the lexical form is invalid
     */
    public CoreseBoolean(String lexicalValue) {
        super(XSDDatatype.BOOLEAN.getIRI());
        this.value = parseLexicalValue(lexicalValue);
        this.lexicalValue = lexicalValue;
        this.coreseObject = new fr.inria.corese.core.sparql.datatype.CoreseBoolean(value);
    }

    /**
     * Returns the label of this boolean literal, which is either {@code "true"} or
     * {@code "false"}.
     *
     * @return The label of the boolean literal.
     */
    @Override
    public String getLabel() {
        return lexicalValue;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.BOOLEAN;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if (coreDatatype != XSDDatatype.BOOLEAN) {
            throw new IncorrectOperationException("CoreseBoolean only supports xsd:boolean.");
        }
    }

    @Override
    public boolean booleanValue() {
        return value;
    }

    @Override
    public String stringValue() {
        return lexicalValue;
    }

    /**
     * Returns a {@link CoreseBoolean} instance representing the boolean value
     * {@code true} or {@code false}.
     *
     * @param value The boolean value to be returned as a {@link CoreseBoolean}
     *              instance.
     * @return The {@link CoreseBoolean} instance representing the given boolean
     *         value.
     */
    public static CoreseBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public Node getCoreseNode() {
        return coreseObject();
    }

    @Override
    public IDatatype getIDatatype() {
        return coreseObject();
    }

    private fr.inria.corese.core.sparql.datatype.CoreseBoolean coreseObject() {
        var result = coreseObject;
        if (result == null) {
            result = new fr.inria.corese.core.sparql.datatype.CoreseBoolean(value);
            coreseObject = result;
        }
        return result;
    }

    private static boolean parseLexicalValue(String lexicalValue) {
        return switch (lexicalValue) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            case null, default -> throw new InvalidDatatypeException(
                    "Invalid xsd:boolean lexical value: " + lexicalValue);
        };
    }

    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
