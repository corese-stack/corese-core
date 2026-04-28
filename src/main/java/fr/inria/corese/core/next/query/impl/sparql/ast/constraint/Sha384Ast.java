package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code SHA384(string)} in SPARQL 1.1.
 */
public class Sha384Ast extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public Sha384Ast(List<TermAst> args) {
        super(args);
    }
}
