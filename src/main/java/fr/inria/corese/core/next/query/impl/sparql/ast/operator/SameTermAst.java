package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public class SameTermAst extends AbstractBinaryOperatorAst implements BooleanOperatorAst {
    public SameTermAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
