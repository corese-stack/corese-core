package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages BASE URI and PREFIX declarations during SPARQL query and update parsing.
 */
public class SparqlPrologueBuilder {

    protected String baseUri;
    protected boolean baseDeclarationSeen;
    protected final List<PrefixDeclarationAst> prefixDeclarations = new ArrayList<>();

    public SparqlPrologueBuilder(final String initialBaseUri) {
        this.baseUri = initialBaseUri;
    }

    public void setBaseUri(final String uri) {
        if (this.baseDeclarationSeen) {
            throw new QuerySyntaxException("Base URI already set, multiple BASE declarations are forbidden.");
        }
        this.baseDeclarationSeen = true;
        this.baseUri = uri != null ? RdfText.stripAngleBrackets(uri) : null;
    }

    public void addPrefix(final String prefix, final String uri) {
        final String p = normalizePrefix(prefix);
        String ns = uri != null ? RdfText.stripAngleBrackets(uri) : "";
        if (this.baseUri != null && !IRIUtils.isAbsoluteIRI(ns)) {
            ns = IRIUtils.resolveIRIAgainstBase(this.baseUri, ns);
        }
        final PrefixDeclarationAst declarationAst = new PrefixDeclarationAst(p, new IriAst(ns));
        if (this.prefixDeclarations.stream().anyMatch(declaration ->
                Objects.equals(declaration.prefix(), declarationAst.prefix())
        )) {
            throw new QuerySyntaxException("Prefix " + prefix + " has already been declared");
        }
        this.prefixDeclarations.add(declarationAst);
    }

    public void replacePrefix(final String prefix, final String uri) {
        final String name = RdfText.stripTrailingColon(prefix);
        this.prefixDeclarations.removeIf(declaration -> declaration.prefix().equals(name));
        addPrefix(prefix, uri);
    }

    public void updateBaseUri(final String uri) {
        final String iri = RdfText.stripAngleBrackets(uri);
        this.baseUri = IRIUtils.isAbsoluteIRI(iri) ? iri : IRIUtils.resolveIRIAgainstBase(this.baseUri, iri);
    }

    public static String normalizePrefix(final String prefix) {
        if (prefix == null) {
            return "";
        }
        if (prefix.endsWith(":")) {
            return prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    public String getBaseUri() {
        return this.baseUri;
    }

    public List<PrefixDeclarationAst> getPrefixDeclaration() {
        return this.prefixDeclarations;
    }

    public QueryPrologueAst buildPrologue() {
        return new QueryPrologueAst(List.copyOf(this.prefixDeclarations), new IriAst(this.baseUri));
    }
}
