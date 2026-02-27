package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * AST node for a SPARQL term (variable, IRI or literal) in a triple pattern.
 */
public sealed interface TermAst permits VarAst, IriAst, LiteralAst {
}


