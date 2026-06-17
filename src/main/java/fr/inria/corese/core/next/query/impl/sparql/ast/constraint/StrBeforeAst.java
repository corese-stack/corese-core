package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRBEFORE(arg1, arg2)}
 * {@code STRBEFORE(string1, string2)}: returns the part of {@code string1}
 * that precedes the first occurrence of {@code string2}.
 */
public class StrBeforeAst extends AbstractBinaryFunctionAst implements SimpleLiteralExpressionAst {
    public StrBeforeAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "STRBEFORE";
    }
}
