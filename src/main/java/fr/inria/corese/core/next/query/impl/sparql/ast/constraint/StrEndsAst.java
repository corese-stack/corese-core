package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRENDS(arg1, arg2)}
 * {@code STRENDS(string1, string2)}: returns {@code true} if {@code string1}
 * ends with {@code string2}.
 */
public class StrEndsAst extends AbstractBinaryFunctionAst implements BooleanExpressionAst {
    public StrEndsAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "STRENDS";
    }
}
