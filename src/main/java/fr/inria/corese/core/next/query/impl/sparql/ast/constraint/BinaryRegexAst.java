package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Call to the REGEX function with a tested string and a pattern
 */
public class BinaryRegexAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public BinaryRegexAst(TermAst string, TermAst pattern) {
        super(string, pattern);
    }

    public TermAst getString() {
        return this.getLeftArgument();
    }

    public TermAst getPattern() {
        return this.getRightArgument();
    }
}
