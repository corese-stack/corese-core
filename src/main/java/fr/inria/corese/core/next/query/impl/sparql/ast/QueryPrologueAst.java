package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;

import java.util.List;

/**
 * Snapshot of the SPARQL prologue: prefix declarations in source order and the effective base IRI
 * after the prologue (parser options initial base, possibly overridden by {@code BASE}).
 * <p>
 * For now this type is only attached to {@link SelectQueryAst}; other query forms still expose
 * prefix/base state via {@link fr.inria.corese.core.next.data.api.IPrefixHandler} on {@link QueryAst}.
 */
public record QueryPrologueAst(List<PrefixDeclarationAst> prefixDeclarations, IriAst baseIri) {
    public QueryPrologueAst {
        prefixDeclarations = prefixDeclarations != null ? List.copyOf(prefixDeclarations) : List.of();
        if (baseIri == null) {
            baseIri = new IriAst(IOConstants.getDefaultBaseURI());
        }
    }

    public static QueryPrologueAst empty() {
        return new QueryPrologueAst(List.of(), new IriAst(IOConstants.getDefaultBaseURI()));
    }

    /**
     * Rebuilds a {@link PrefixHandler} with the same effective mappings as while parsing.
     */
    public PrefixHandler toPrefixHandler() {
        PrefixHandler prefixHandler = new PrefixHandler();
        prefixHandler.setDefaultNamespace(baseIri.raw());
        for (PrefixDeclarationAst d : prefixDeclarations) {
            prefixHandler.setPrefix(d.prefix(), d.namespace().raw());
        }
        return prefixHandler;
    }
}
