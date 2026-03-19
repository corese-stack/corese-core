package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code !}
 */
public class BooleanNotAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public BooleanNotAst(List<TermAst> arg) {
        super(arg);
    }
}
