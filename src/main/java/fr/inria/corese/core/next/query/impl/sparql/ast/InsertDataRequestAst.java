package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * Represents the {@code INSERT DATA} operation as defined in the
 * <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#insertData">SPARQL 1.1 recommendation</a>.
 *
 * @param data the quad data to insert (ground triples only — no variables)
 */
public record InsertDataRequestAst(QuadsAst data) implements UpdateRequestUnitAst {
    public InsertDataRequestAst {
        if (data == null) throw new IllegalArgumentException("data must be non-null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        data.accept(visitor);
    }
}
