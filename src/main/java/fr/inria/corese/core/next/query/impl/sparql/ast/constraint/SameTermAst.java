package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code sameTerm(term1, term2)}
 */
public class SameTermAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public SameTermAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
