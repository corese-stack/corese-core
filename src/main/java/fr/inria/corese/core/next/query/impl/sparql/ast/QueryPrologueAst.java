package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.data.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;

import java.util.List;

/**
 * Snapshot of the SPARQL prologue: prefix declarations in source order and the effective base IRI
 * after the prologue (parser options initial base, possibly overridden by {@code BASE}).
 * <p>
 * For now this type is only attached to {@link SelectQueryAst}; other query forms still expose
 * prefix/base state via {@link fr.inria.corese.core.next.data.api.IPrefixHandler} on {@link QueryAst}.
 */
public record QueryPrologueAst(List<PrefixDeclarationAst> prefixDeclarations, IriAst baseIri, PrefixHandler prefixHandler) {
    public QueryPrologueAst(List<PrefixDeclarationAst> prefixDeclarations, IriAst baseIri) {
        this(prefixDeclarations, baseIri, null);
    }

    public QueryPrologueAst {
        prefixDeclarations = prefixDeclarations != null ? List.copyOf(prefixDeclarations) : List.of();
        if (baseIri == null) {
            baseIri = new IriAst(IOConstants.getDefaultBaseURI());
        } else {
            baseIri = new IriAst(trimURI(baseIri.raw()));
        }
        if (prefixHandler == null) {
            prefixHandler = new PrefixHandler();
            if(! IRIUtils.isAbsoluteIRI(baseIri.raw())) {
                throw new QuerySyntaxException("Base IRI should be absolute, got " + baseIri.raw());
            }
            prefixHandler.setDefaultNamespace(baseIri.raw());
            for (PrefixDeclarationAst d : prefixDeclarations) {
                String prefix = trimPrefix(d.prefix());
                if(prefixHandler.hasPrefix(prefix)) {
                    throw new QuerySyntaxException("Prefix " + prefix + " is declared twice in query");
                }
                String namespace = trimURI(d.namespace().raw());
                if(! IRIUtils.isAbsoluteIRI(namespace)) {
                    if(IRIUtils.isAbsoluteIRI(prefixHandler.getDefaultNamespace() + namespace)) {
                        namespace = prefixHandler.getDefaultNamespace() + namespace;
                    } else {
                        throw new QuerySyntaxException(namespace + " should be absolute or resolve to an absolute IRI using the base IRI");
                    }
                }
                prefixHandler.setPrefix(prefix, namespace);
            }
        }
    }

    public static QueryPrologueAst empty() {
        return new QueryPrologueAst(List.of(), new IriAst(IOConstants.getDefaultBaseURI()));
    }

    private static String trimURI(String uri) {
        if(uri.startsWith("<") && uri.endsWith(">")) {
            uri = uri.substring(0, uri.lastIndexOf(">"));
            uri = uri.substring(uri.indexOf("<") +1);
        }
        return uri;
    }

    private static String trimPrefix(String prefix) {
        if(prefix.endsWith(":")) {
            prefix = prefix.substring(0, prefix.lastIndexOf(":"));
        }
        return prefix;
    }
}
