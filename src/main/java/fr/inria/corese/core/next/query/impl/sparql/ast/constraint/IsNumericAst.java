package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import java.util.List;

/** Tests whether an RDF term is a valid numeric literal. */
public final class IsNumericAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsNumericAst(List<TermAst> arguments) {
        super(arguments);
    }

    @Override
    public String getName() {
        return "ISNUMERIC";
    }
}
