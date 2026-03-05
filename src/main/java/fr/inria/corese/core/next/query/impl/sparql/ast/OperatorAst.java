package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Root interface for all operators resolving to a term
 */
public sealed interface OperatorAst extends TermAst permits ArithmeticOperatorAst, BooleanOperatorAst, IriOperatorAst, SimpleLiteralOperatorAst {
}
