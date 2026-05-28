package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 *
 * Operator {@code langMatches(A, B)}
 * {@code LANGMATCHES(lang, range)}: returns {@code true} if the language tag
 * {@code lang} matches the language range {@code range} per RFC 4647.
 */
public class LangMatchesAst extends AbstractBinaryFunctionAst implements BooleanExpressionAst {
    public LangMatchesAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "LANGMATCHES";
    }
}
