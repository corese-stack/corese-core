package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * IRI or QName in a triple pattern (e.g. &lt;http://...&gt;, foaf:Person, a).
 */
public record IriAst(String raw) implements TermAst {
    public IriAst {
        if (raw == null) {
            throw new IllegalArgumentException("IRI raw is null");
        }
    }

    @Override
    public String getName() {
        return this.raw;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
