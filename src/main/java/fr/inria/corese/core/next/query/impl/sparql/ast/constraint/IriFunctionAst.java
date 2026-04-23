package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code IRI(term)} or {@code URI(term)} in SPARQL 1.1.
 */
public class IriFunctionAst extends AbstractUnaryConstraintAst implements IriExpressionAst {
    public IriFunctionAst(List<TermAst> args) {
        super(args);
    }
}
