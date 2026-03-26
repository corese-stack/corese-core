package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.Objects;

/**
 * A {@code PREFIX p: &lt;ns&gt;} declaration from the SPARQL prologue ({@code p} without trailing colon).
 */
public record PrefixDeclarationAst(String prefix, IriAst namespace) {
    public PrefixDeclarationAst {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must be non-null and non-empty");
        }
        namespace = Objects.requireNonNull(namespace, "namespace");
    }
}
