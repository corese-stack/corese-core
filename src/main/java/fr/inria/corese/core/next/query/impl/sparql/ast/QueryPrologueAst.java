package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;

import java.util.List;

import static fr.inria.corese.core.next.util.StringUtils.trimChevronIRIs;

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
        } else {
            baseIri = new IriAst(trimChevronIRIs(baseIri.raw()));
        }
    }

    public static QueryPrologueAst empty() {
        return new QueryPrologueAst(List.of(), new IriAst(IOConstants.getDefaultBaseURI()));
    }
}
