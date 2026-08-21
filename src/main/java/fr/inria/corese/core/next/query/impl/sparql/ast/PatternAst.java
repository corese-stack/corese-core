package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

/**
 * Element of a group graph pattern (BGP, optional, union, etc.).
 */
public sealed interface PatternAst extends VisitableAst permits BgpAst, BindAst, FilterAst, GroupGraphPatternAst, MinusAst, OptionalAst, ServiceAst, UnionAst, SubQueryAst {
    void accept(AstVisitor visitor);
}
