package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * IRI or QName in a triple pattern (e.g. &lt;http://...&gt;, foaf:Person, a).
 */
public record IriAst(String raw) implements TermAst {
    public IriAst {
        if (raw == null) {
            throw new IllegalArgumentException("IRI raw is null");
        }
    }
}
