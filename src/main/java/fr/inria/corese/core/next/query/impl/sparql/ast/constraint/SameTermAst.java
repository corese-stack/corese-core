package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public class SameTermAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public SameTermAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
