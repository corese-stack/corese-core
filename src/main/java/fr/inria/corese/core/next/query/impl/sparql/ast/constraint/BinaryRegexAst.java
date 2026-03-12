package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public class BinaryRegexAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
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
