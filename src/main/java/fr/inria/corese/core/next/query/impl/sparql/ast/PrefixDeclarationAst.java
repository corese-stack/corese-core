package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.Objects;

import static fr.inria.corese.core.next.util.StringUtils.trimChevronIRIs;
import static fr.inria.corese.core.next.util.StringUtils.trimPrefixWithColon;

/**
 * A {@code PREFIX p: &lt;ns&gt;} declaration from the SPARQL prologue ({@code p} without trailing colon).
 */
public record PrefixDeclarationAst(String prefix, IriAst namespace) implements VisitableAst {
    public PrefixDeclarationAst {
        if (prefix == null ) {
            throw new IllegalArgumentException("prefix must be non-null");
        }
        prefix = trimPrefixWithColon(prefix);
        namespace = Objects.requireNonNull(namespace, "namespace");
        if(! namespace.raw().isEmpty()) {
            namespace = new IriAst(trimChevronIRIs(namespace.raw()));
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.namespace.accept(visitor);
    }
}
