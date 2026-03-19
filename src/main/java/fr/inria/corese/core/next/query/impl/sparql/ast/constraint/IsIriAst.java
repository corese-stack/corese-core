package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code isIri(A)} and {@code isUri(A)}
 * {@code isIRI(term)}: returns {@code true} if {@code term} is an IRI.
 */
public class IsIriAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsIriAst(List<TermAst> args) {
        super(args);
    }
}
