package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

/**
 * AST node for a SPARQL term (variable, IRI or literal) in a triple pattern.
 */
public sealed interface TermAst extends VisitableAst permits ConstraintAst, IriAst, LiteralAst, VarAst {
    String getName();
}


