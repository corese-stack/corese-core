package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * BIND(expression AS ?var) clause in SPARQL 1.1
 */
public record BindAst(TermAst expression, VarAst variable) implements PatternAst {}