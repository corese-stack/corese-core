package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code LCASE(string)}: returns a lowercased copy of {@code string}.
 */
public class LcaseAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public LcaseAst(List<TermAst> args) {
        super(args);
    }
}
