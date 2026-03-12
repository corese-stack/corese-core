package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Binary operators used in SPARQL filter expressions.
 */
public enum BinaryOperator {

    /**
     * Addition
     */
    ADD,

    /**
     * Subtraction
     */
    SUB,

    /**
     * Multiplication
     */
    MUL,

    /**
     * Division
     */
    DIV,

    /**
     * Equality
     */
    EQ,

    /**
     * Inequality
     */
    NE,

    /**
     * Less than
     */
    LT,

    /**
     * Less than or equal
     */
    LE,

    /**
     * Greater than
     */
    GT,

    /**
     * Greater than or equal
     */
    GE,


    /**
     * Logical conjunction
     */
    AND,

    /**
     * Logical disjunction
     */
    OR
}