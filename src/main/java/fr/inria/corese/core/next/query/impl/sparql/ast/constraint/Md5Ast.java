package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code MD5(string)} in SPARQL 1.1.
 */
public class Md5Ast extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public Md5Ast(List<TermAst> args) {
        super(args);
    }
}
