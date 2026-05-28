package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code ENCODE_FOR_URI(string)} in SPARQL 1.1.
 */
public class EncodeForUriAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public EncodeForUriAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "ENCODE_FOR_URI";
    }
}
