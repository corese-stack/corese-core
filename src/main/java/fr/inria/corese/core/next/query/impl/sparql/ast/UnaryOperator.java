package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Unary operators used in SPARQL filter expressions.
 */
public enum UnaryOperator {

    // --- Logical operator ---

    /**
     * Logical negation: {@code !operand}.
     */
    NOT,

    // --- Arithmetic operators ---

    /** Unary plus: {@code +operand}. No-op for numeric values, but enforces numeric type. */
    PLUS,

    /** Unary minus: {@code -operand}. Negates a numeric value. */
    MINUS
}