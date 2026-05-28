package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import java.util.List;

/**
 * Function {@code COALESCE(expr1, expr2, ...)} in SPARQL 1.1
 * Returns the first argument that evaluates without error.
 */
public class CoalesceAst extends AbstractUnlimitedArgumentsFunctionAst {

    public CoalesceAst(List<TermAst> arguments) {
        super(arguments);
    }

    @Override
    public String getName() {
        return "COALESCE";
    }
}