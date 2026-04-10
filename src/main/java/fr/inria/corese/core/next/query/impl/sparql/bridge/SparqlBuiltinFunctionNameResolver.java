package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.sparql.triple.parser.Processor;

/**
 * Maps SPARQL IRI / QName tokens (function position) to the names used in {@link Processor}'s operator table.
 */
public final class SparqlBuiltinFunctionNameResolver {

    private SparqlBuiltinFunctionNameResolver() {
    }

    /**
     * Resolves the function name from a term that must be an {@link IriAst}.
     */
    public static String fromFunctionTerm(TermAst functionName) {
        if (!(functionName instanceof IriAst i)) {
            throw new IllegalArgumentException("Function name must be IriAst, got " + functionName);
        }
        return localNameFromIriToken(i.raw());
    }

    /**
     * Strips angle brackets if present, then returns the local part after {@code #}, {@code /}, or {@code :},
     * lowercased for {@link Processor} lookup.
     */
    public static String localNameFromIriToken(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.charAt(0) == '<' && t.charAt(t.length() - 1) == '>') {
            t = t.substring(1, t.length() - 1);
        }
        int hash = t.lastIndexOf('#');
        if (hash >= 0 && hash < t.length() - 1) {
            return t.substring(hash + 1).toLowerCase();
        }
        int slash = t.lastIndexOf('/');
        if (slash >= 0 && slash < t.length() - 1) {
            return t.substring(slash + 1).toLowerCase();
        }
        int colon = t.lastIndexOf(':');
        if (colon >= 0 && colon < t.length() - 1) {
            return t.substring(colon + 1).toLowerCase();
        }
        return t.toLowerCase();
    }
}
