package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.spi.io.IOConstants;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.HashMap;
import java.util.Map;

/** Resolves SPARQL terms against one immutable query-prologue snapshot. */
public final class SparqlTermResolver {

    private final PrefixHandler prefixes;
    private final String baseIri;
    private final Map<String, Node> variables = new HashMap<>();

    public SparqlTermResolver(QueryPrologueAst prologue) {
        QueryPrologueAst effectivePrologue = prologue == null ? QueryPrologueAst.empty() : prologue;
        this.prefixes = new PrefixHandler(true);
        for (PrefixDeclarationAst declaration : effectivePrologue.prefixDeclarations()) {
            prefixes.setPrefix(normalizePrefix(declaration.prefix()),
                    RdfText.stripAngleBrackets(declaration.namespace().raw()));
        }
        this.baseIri = effectivePrologue.baseIri().raw();
    }

    public String resolveIri(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.startsWith("<") && raw.endsWith(">")) {
            return resolveRelativeIri(RdfText.stripAngleBrackets(raw));
        }
        if (raw.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return raw;
        }
        if (raw.equals("a")) {
            return RDF.type.getIRI().stringValue();
        }
        if (raw.contains("://") && IRIUtils.isAbsoluteIRI(raw)) {
            return raw;
        }
        return resolvePrefixedIri(raw);
    }

    public Node toNode(TermAst term) {
        return switch (term) {
            case VarAst(String name) -> variables.computeIfAbsent(name, NodeImpl::forVariable);
            case IriAst(String raw) when raw.startsWith(IOConstants.BLANK_NODE_PREFIX) ->
                    NodeImpl.forBlank(raw.substring(IOConstants.BLANK_NODE_PREFIX.length()));
            case IriAst(String raw) -> NodeImpl.forIRI(resolveIri(raw));
            case LiteralAst(String lexical, String lang, String datatype) -> NodeImpl.forLiteral(
                    unquoteLexical(lexical), normalizeDatatypeIri(datatype), lang);
            default -> throw new IllegalArgumentException(
                    "A query term must be a variable, IRI or literal, got: "
                            + term.getClass().getSimpleName());
        };
    }

    /**
     * Resolves a graph-pattern term. SPARQL blank-node labels in a basic graph
     * pattern are existential variables, unlike blank nodes used as RDF values
     * (for example in a CONSTRUCT template or a VALUES row).
     */
    public Node toPatternNode(TermAst term) {
        if (term instanceof IriAst(String raw) && raw.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return variables.computeIfAbsent(raw, NodeImpl::forBlankVariable);
        }
        return toNode(term);
    }

    public String normalizeDatatypeIri(String datatype) {
        if (datatype == null || datatype.isEmpty()) {
            return null;
        }
        if (datatype.startsWith("<") && datatype.endsWith(">")) {
            return resolveRelativeIri(RdfText.stripAngleBrackets(datatype));
        }
        if (datatype.contains("://")
                || (IRIUtils.isAbsoluteIRI(datatype) && !prefixes.hasPrefix(prefix(datatype)))) {
            return datatype;
        }
        return resolvePrefixedIri(datatype);
    }

    public String unquoteLexical(String lexical) {
        if (lexical == null || lexical.length() < 2) {
            return lexical;
        }
        String unquotedTriple = stripTripleQuotes(lexical);
        if (unquotedTriple != null) {
            return unquotedTriple;
        }
        return stripSingleQuotes(lexical);
    }

    private static String stripTripleQuotes(String lexical) {
        if (lexical.length() >= 6) {
            if (lexical.startsWith("\"\"\"") && lexical.endsWith("\"\"\"")) {
                return lexical.substring(3, lexical.length() - 3);
            }
            if (lexical.startsWith("'''") && lexical.endsWith("'''")) {
                return lexical.substring(3, lexical.length() - 3);
            }
        }
        return null;
    }

    private static String stripSingleQuotes(String lexical) {
        char quote = lexical.charAt(0);
        if (quote != '"' && quote != '\'') {
            return lexical;
        }
        int endIdx = lexical.endsWith(String.valueOf(quote))
                ? lexical.length() - 1
                : lexical.lastIndexOf(quote);
        if (endIdx > 0) {
            return lexical.substring(1, endIdx);
        }
        return lexical;
    }

    private String resolvePrefixedIri(String raw) {
        int colon = raw.indexOf(':');
        if (colon < 0) {
            return raw;
        }
        String namespace = prefixes.getNamespace(raw.substring(0, colon));
        return namespace == null ? raw : namespace + unescapePName(raw.substring(colon + 1));
    }

    private String resolveRelativeIri(String iri) {
        if (IRIUtils.isAbsoluteIRI(baseIri) && !IRIUtils.isAbsoluteIRI(iri)) {
            return IRIUtils.resolveIRIAgainstBase(baseIri, iri);
        }
        return iri;
    }

    private static String normalizePrefix(String prefix) {
        return prefix != null && prefix.endsWith(":")
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
    }

    private static String prefix(String iri) {
        int colon = iri.indexOf(':');
        return colon < 0 ? iri : iri.substring(0, colon);
    }

    private static String unescapePName(String local) {
        if (local == null || !local.contains("\\")) {
            return local;
        }
        StringBuilder result = new StringBuilder(local.length());
        int index = 0;
        while (index < local.length()) {
            char character = local.charAt(index);
            if (character == '\\' && index + 1 < local.length()) {
                index = appendEscaped(result, local, index + 1);
            } else {
                result.append(character);
                index++;
            }
        }
        return result.toString();
    }

    private static int appendEscaped(StringBuilder result, String value, int escapeIndex) {
        char escape = value.charAt(escapeIndex);
        if (escape == 'u' || escape == 'U') {
            int length = escape == 'u' ? 4 : 8;
            if (escapeIndex + 1 + length <= value.length()) {
                result.appendCodePoint(Integer.parseInt(
                        value.substring(escapeIndex + 1, escapeIndex + 1 + length), 16));
                return escapeIndex + 1 + length;
            }
        }
        result.append(escape);
        return escapeIndex + 1;
    }
}
