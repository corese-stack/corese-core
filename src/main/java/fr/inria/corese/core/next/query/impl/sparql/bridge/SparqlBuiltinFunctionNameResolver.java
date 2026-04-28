package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.sparql.triple.parser.Processor;

import static fr.inria.corese.core.next.util.StringUtils.localNameFromIriToken;

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


}
