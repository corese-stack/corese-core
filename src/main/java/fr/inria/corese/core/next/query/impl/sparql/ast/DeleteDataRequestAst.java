package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents the {@code DELETE DATA} operation as defined in the
 * <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#deleteData">SPARQL 1.1 recommendation</a>.
 *
 * @param data the quad data to delete (ground triples only — no variables)
 */
public record DeleteDataRequestAst(QuadsAst data) implements UpdateRequestUnitAst {
    public DeleteDataRequestAst {
        if (data == null) throw new IllegalArgumentException("data must be non-null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        data.accept(visitor);
    }
}
