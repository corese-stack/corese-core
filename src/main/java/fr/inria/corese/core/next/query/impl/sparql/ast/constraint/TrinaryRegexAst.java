package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Call to the REGEX function including flags
 * @param string tested string
 * @param pattern regular termFromExpression
 * @param flags treatment flags
 */
public record TrinaryRegexAst(TermAst string, TermAst pattern, TermAst flags) implements BooleanExpressionAst {
}
