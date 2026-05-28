package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code SHA512(string)} in SPARQL 1.1.
 */
public class Sha512Ast extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public Sha512Ast(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "SHA512";
    }
}
