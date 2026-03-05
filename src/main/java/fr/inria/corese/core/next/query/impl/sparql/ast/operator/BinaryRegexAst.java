package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public class BinaryRegexAst extends AbstractBinaryOperatorAst implements BooleanOperatorAst {
    public BinaryRegexAst(TermAst left, TermAst right) {
        super(left, right);
    }

    public TermAst getString() {
        return this.getLeftArgument();
    }

    public TermAst getPattern() {
        return this.getRightArgument();
    }
}
