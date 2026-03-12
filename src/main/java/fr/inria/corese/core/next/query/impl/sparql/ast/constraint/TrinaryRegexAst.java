package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public record TrinaryRegexAst(TermAst string, TermAst pattern, TermAst flags) implements BooleanExpressionAst {
}
