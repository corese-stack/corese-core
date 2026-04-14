package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRAFTER(arg1, arg2)}
 * {@code STRAFTER(string1, string2)}: returns the part of {@code string1}
 * that follows the first occurrence of {@code string2}.
 */
public class StrAfterAst extends AbstractBinaryFunctionAst implements SimpleLiteralExpressionAst {
    public StrAfterAst(List<TermAst> args) {
        super(args);
    }
}
