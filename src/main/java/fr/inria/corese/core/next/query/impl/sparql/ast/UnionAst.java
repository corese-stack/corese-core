package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * AST node representing a SPARQL {@code UNION} of two graph patterns.
 */
public record UnionAst(GroupGraphPatternAst left, GroupGraphPatternAst right) implements PatternAst {
}