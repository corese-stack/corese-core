package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRLANG(string, language)}: constructs a plain literal with a
 * language tag from a lexical string and a language tag string.
 */
public class StrLangAst extends AbstractBinaryFunctionAst implements SimpleLiteralExpressionAst {
    public StrLangAst(List<TermAst> args) {
        super(args);
    }
}
