package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.Objects;

import static fr.inria.corese.core.next.util.StringUtils.trimChevronIRIs;
import static fr.inria.corese.core.next.util.StringUtils.trimPrefixWithColon;

/**
 * A {@code PREFIX p: &lt;ns&gt;} declaration from the SPARQL prologue ({@code p} without trailing colon).
 */
public record PrefixDeclarationAst(String prefix, IriAst namespace) {
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
}
