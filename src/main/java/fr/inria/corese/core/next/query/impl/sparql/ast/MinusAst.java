package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * AST node representing a SPARQL {@code MINUS} graph pattern.
 */
public record MinusAst(GroupGraphPatternAst pattern) implements PatternAst {
}
