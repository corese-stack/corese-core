package fr.inria.corese.core.next.api.base.model;

import fr.inria.corese.core.next.api.Statement;
import java.util.Objects;

/**
 * An abstract implementation of the {@link Statement} interface.
 * This class provides default implementations for the {@code equals}, {@code hashCode},
 * and {@code toString} methods based on the subject, predicate, object, and context of the statement.
 *
 * <p>Any subclass of {@code AbstractStatement} should implement the methods from the
 * {@code Statement} interface such as {@code getSubject}, {@code getPredicate}, {@code getObject},
 * and {@code getContext} to represent a specific type of statement.</p>
 *
 * <p>This class ensures that statements are compared correctly, generate a proper hash code,
 * and provide a meaningful string representation.</p>
 */

public abstract class AbstractStatement implements Statement {

    /**
     * Compares this statement to another object for equality.
     * Two statements are considered equal if they have the same subject, predicate, object, and context.
     *
     * @param obj the object to compare this statement to.
     * @return {@code true} if the statements are equal, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Statement
                && getObject().equals(((Statement) obj).getObject())
                && getSubject().equals(((Statement) obj).getSubject())
                && getPredicate().equals(((Statement) obj).getPredicate())
                && Objects.equals(getContext(), ((Statement) obj).getContext());
    }

    /**
     * Returns the hash code value for this statement.
     * The hash code is computed based on the subject, predicate, object, and context of the statement.
     *
     * @return the hash code value for this statement.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (getSubject() == null ? 0 : getSubject().hashCode());
        hash = 31 * hash + (getPredicate() == null ? 0 : getPredicate().hashCode());
        hash = 31 * hash + (getObject() == null ? 0 : getObject().hashCode());
        hash = 31 * hash + (getContext() == null ? 0 : getContext().hashCode());
        return hash;
    }

    /**
     * Returns a string representation of this statement.
     * The string representation includes the subject, predicate, object, and context (if present).
     *
     * @return a string representation of this statement.
     */
    @Override
    public String toString() {
        return "("
                + getSubject()
                + ", " + getPredicate()
                + ", " + getObject()
                + (getContext() == null ? "" : ", " + getContext())
                + ")";
    }
}