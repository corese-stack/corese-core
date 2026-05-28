package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.List;

import static fr.inria.corese.core.next.util.StringUtils.trimChevronIRIs;

/**
 * Snapshot of the SPARQL prologue: prefix declarations in source order and the effective base IRI
 * after the prologue (parser options initial base, possibly overridden by {@code BASE}).
 * <p>
 * For now this type is only attached to {@link SelectQueryAst}; other query forms still expose
 * prefix/base state via {@link fr.inria.corese.core.next.data.api.IPrefixHandler} on {@link QueryAst}.
 */
public record QueryPrologueAst(List<PrefixDeclarationAst> prefixDeclarations, IriAst baseIri) implements VisitableAst {

    public QueryPrologueAst {
        prefixDeclarations = prefixDeclarations != null ? List.copyOf(prefixDeclarations) : List.of();
        if (baseIri == null) {
            baseIri = new IriAst(IOConstants.getDefaultBaseURI());
        } else {
            baseIri = new IriAst(trimChevronIRIs(baseIri.raw()));
        }
        if (!IRIUtils.isAbsoluteIRI(baseIri.raw())) {
            throw new QuerySyntaxException("Base IRI should be absolute, got " + baseIri.raw());
        }
        // resolving relative namespaces in prefix declarations
        IriAst finalBaseIri = baseIri;
        prefixDeclarations = prefixDeclarations.stream().map(prefixDecl -> {
            if(IRIUtils.isAbsoluteIRI(prefixDecl.namespace().raw())) {
                return prefixDecl;
            }
            return new PrefixDeclarationAst(prefixDecl.prefix(), new IriAst(IRIUtils.resolveIRIAgainstBase(finalBaseIri.raw(), prefixDecl.namespace().raw())));
        }).toList();
    }

    public static QueryPrologueAst empty() {
        return new QueryPrologueAst(List.of(), new IriAst(IOConstants.getDefaultBaseURI()));
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.prefixDeclarations.forEach(prefixDeclarationAst -> {
            prefixDeclarationAst.accept(visitor);
        });
        this.baseIri.accept(visitor);
    }
}
