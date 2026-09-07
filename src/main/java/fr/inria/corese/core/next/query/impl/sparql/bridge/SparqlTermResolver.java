package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.spi.io.IOConstants;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;

import java.util.Objects;

/** Resolves SPARQL terms against one immutable query-prologue snapshot. */
final class SparqlTermResolver {

    private final PrefixHandler prefixes;
    private final String baseIri;

    SparqlTermResolver(QueryPrologueAst prologue) {
        QueryPrologueAst effectivePrologue = prologue == null ? QueryPrologueAst.empty() : prologue;
        this.prefixes = new PrefixHandler(true);
        for (PrefixDeclarationAst declaration : effectivePrologue.prefixDeclarations()) {
            prefixes.setPrefix(normalizePrefix(declaration.prefix()),
                    RdfText.stripAngleBrackets(declaration.namespace().raw()));
        }
        this.baseIri = effectivePrologue.baseIri().raw();
    }

    String resolveIri(String raw) {
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

    String normalizeDatatypeIri(String datatype) {
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

    String unquoteLexical(String lexical) {
        Objects.requireNonNull(lexical, "lexical");
        if (lexical.length() < 2 || !lexical.startsWith("\"")) {
            return lexical;
        }
        if (lexical.endsWith("\"")) {
            return lexical.substring(1, lexical.length() - 1);
        }
        int closingQuote = lexical.lastIndexOf('"');
        return closingQuote > 0 ? lexical.substring(1, closingQuote) : lexical;
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
