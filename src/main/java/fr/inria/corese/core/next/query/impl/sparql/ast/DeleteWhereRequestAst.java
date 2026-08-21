package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents the {@code DELETE WHERE} operation as defined in the
 * <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#deleteWhere">SPARQL 1.1 recommendation</a>.
 *
 * <p>The quad pattern is used both as the pattern to match and as the template of triples to delete.
 *
 * @param pattern the quad pattern (may contain variables)
 */
public record DeleteWhereRequestAst(QuadsAst pattern) implements UpdateRequestUnitAst {
    public DeleteWhereRequestAst {
        if (pattern == null) throw new IllegalArgumentException("pattern must be non-null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        pattern.accept(visitor);
    }
}
