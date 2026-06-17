package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code datatype(A)}
 * {@code DATATYPE(literal)}: returns the datatype IRI of a typed literal.
 * For a plain literal with no language tag, returns {@code xsd:string}.
 */
public class DatatypeAst extends AbstractUnaryConstraintAst implements IriExpressionAst {
    public DatatypeAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "DATATYPE";
    }
}
