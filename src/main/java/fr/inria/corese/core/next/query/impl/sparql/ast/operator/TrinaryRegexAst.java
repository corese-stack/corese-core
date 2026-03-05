package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public record TrinaryRegexAst(TermAst string, TermAst pattern, TermAst flags) implements BooleanOperatorAst {
}
