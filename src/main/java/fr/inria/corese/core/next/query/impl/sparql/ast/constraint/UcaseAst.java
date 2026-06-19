package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code UCASE(string)}: returns an uppercased copy of {@code string}.
 */
public class UcaseAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public UcaseAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "UCASE";
    }
}
