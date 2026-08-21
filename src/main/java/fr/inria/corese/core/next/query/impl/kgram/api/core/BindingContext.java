package fr.inria.corese.core.next.query.impl.kgram.api.core;

import java.util.Map;

/**
 * Binding context for global and shared variables.
 */
public interface BindingContext {

    /**
     * Retrieves the value of a global variable.
     *
     * @param variable the variable name
     * @return the associated value (Node) or null if not defined
     */
    Node getValue(String variable);

    /**
     * Sets the value of a global variable.
     *
     * @param variable the variable name
     * @param value    the value (Node) to associate
     */
    void setValue(String variable, Node value);

    /**
     * Tests if a variable is defined in this context.
     *
     * @param variable the variable name
     * @return true if the variable is defined
     */
    boolean isDefined(String variable);

    /**
     * Returns all defined variables with their values.
     *
     * @return a Map variable → value
     */
    Map<String, Node> getBindings();

    /**
     * Copies all values from another context to this one.
     *
     * @param other the source context
     */
    void copy(BindingContext other);

    /**
     * Shares data with another context.
     * Used by Memory to share global variables.
     *
     * @param other the other context
     */
    default void share(BindingContext other) {
        // By default, copy all values
        if (other != null) {
            copy(other);
        }
    }

    /**
     * Tests if the context is empty.
     *
     * @return true if no variable is defined
     */
    default boolean isEmpty() {
        return getBindings().isEmpty();
    }

    /**
     * Tests if this context has bound variables.
     * Used by Memory.hasBind()
     *
     * @return true if at least one variable is bound
     */
    default boolean hasBind() {
        return !isEmpty();
    }

    /**
     * Retrieves the value of an Expr variable.
     * Used by Mapping.get(Expr) and Memory.get(Expr)
     *
     * @param varExpr the variable expression
     * @return the value (Node) or null
     */
    default Object get(Expr varExpr) {
        return getValue(varExpr.getLabel());
    }

    /**
     * Retrieves the ProcessVisitor associated with this context.
     * Used to share the visitor between different evaluations.
     *
     * @return the ProcessVisitor or null
     */
    default Object getVisitor() {
        return null;
    }

    /**
     * Sets the ProcessVisitor associated with this context.
     *
     * @param visitor the ProcessVisitor to associate
     */
    default void setVisitor(Object visitor) {
        // By default, do nothing
    }

    /**
     * Visit method for reporting.
     * Used by ProcessVisitorDefault.
     *
     * @param e  the expression
     * @param g  the graph node
     * @param m1 the mappings 1
     * @param m2 the mappings 2
     */
    default void visit(Object e, Object g, Object m1, Object m2) {
        // By default, do nothing
    }
}