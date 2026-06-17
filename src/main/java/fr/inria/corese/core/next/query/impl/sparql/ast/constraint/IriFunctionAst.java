package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code IRI(term)} or {@code URI(term)} in SPARQL 1.1.
 */
public class IriFunctionAst extends AbstractUnaryConstraintAst implements IriExpressionAst {
    public IriFunctionAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "IRI";
    }
}
