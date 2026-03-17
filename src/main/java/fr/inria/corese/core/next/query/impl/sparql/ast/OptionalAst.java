package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Optional can contain BGP, FILTER, UNION
 *
 * @param ast PatternAst
 */
public record OptionalAst(PatternAst ast) implements PatternAst {}