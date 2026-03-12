package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public class LangMatchesAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public LangMatchesAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
