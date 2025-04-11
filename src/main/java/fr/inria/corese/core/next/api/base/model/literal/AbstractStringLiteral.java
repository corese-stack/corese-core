package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;

/**
 * Abstract class representing a string literal in Corese. It extends {@link AbstractLiteral}
 * and implements {@link Comparable} to allow comparison of string literals based on their string value.
 * This class serves as a base class for all string-based literals in Corese.
 */

public abstract class AbstractStringLiteral extends AbstractLiteral implements Comparable<AbstractStringLiteral> {
    /**
     * Constructs an instance of AbstractStringLiteral with the specified datatype.
     *
     * @param datatype The IRI representing the datatype of the literal.
     */
    protected AbstractStringLiteral(IRI datatype) {
        super(datatype);
    }

    /**
     * Returns the string value of this string literal.
     *
     * @return The string value of the literal.
     */
    public String stringValue() {
        return getLabel();
    }

    @Override
    public int compareTo(AbstractStringLiteral abstractStringLiteral) {
        return stringValue().compareTo(abstractStringLiteral.stringValue());
    }
}