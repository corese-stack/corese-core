package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Element of a group graph pattern (BGP, optional, union, etc.).
 */
public sealed interface PatternAst permits BgpAst, FilterAst, GroupGraphPatternAst, OptionalAst, UnionAst {
}
