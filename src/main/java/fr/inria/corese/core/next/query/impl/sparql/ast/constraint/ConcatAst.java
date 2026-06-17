package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code CONCAT(expr1, expr2, ...)} in SPARQL 1.1.
 */
public class ConcatAst extends AbstractUnlimitedArgumentsFunctionAst implements SimpleLiteralExpressionAst {

    public ConcatAst(List<TermAst> arguments) {
        super(arguments);
    }

    @Override
    public String getName() {
        return "CONCAT";
    }
}
